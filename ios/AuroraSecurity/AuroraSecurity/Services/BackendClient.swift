import Foundation

struct BackendClient {
    var session: URLSession = .shared

    func registerBackendDevice(
        settings: AlertContactSettings,
        userId: String,
        bindCode: String,
        installSecret: String
    ) async -> String? {
        guard !settings.apiUrl.isEmpty, !settings.apiToken.isEmpty else { return nil }
        let body: [String: String] = [
            "userId": userId,
            "bindCode": bindCode,
            "deviceName": settings.deviceName,
            "installSecret": installSecret
        ]
        let response: RegisterDeviceResponse? = try? await request(
            path: "/devices/register",
            settings: settings,
            method: "POST",
            body: body
        )
        return response?.deviceToken
    }

    func registerUser(settings: AlertContactSettings, userId: String, bindCode: String) async throws {
        let body: [String: String] = [
            "userId": userId,
            "bindCode": bindCode,
            "deviceName": settings.deviceName
        ]
        let _: EmptyResponse = try await request(path: "/users/register", settings: settings, method: "POST", body: body)
    }

    func fetchTelegramContacts(settings: AlertContactSettings, userId: String) async throws -> [TelegramContact] {
        let response: ContactsResponse<TelegramContact> = try await request(
            path: "/users/\(userId)/contacts",
            settings: settings,
            method: "GET",
            body: Optional<EmptyBody>.none
        )
        return response.contacts
    }

    func fetchLineContacts(settings: AlertContactSettings, userId: String) async throws -> [LineContact] {
        let response: ContactsResponse<LineContact> = try await request(
            path: "/users/\(userId)/line-contacts",
            settings: settings,
            method: "GET",
            body: Optional<EmptyBody>.none
        )
        return response.contacts
    }

    func fetchPushContacts(settings: AlertContactSettings, userId: String) async throws -> [PushContact] {
        let response: ContactsResponse<PushContact> = try await request(
            path: "/users/\(userId)/push-contacts",
            settings: settings,
            method: "GET",
            body: Optional<EmptyBody>.none
        )
        return response.contacts
    }

    func deleteTelegramContact(settings: AlertContactSettings, userId: String, contactId: String) async throws {
        let encoded = contactId.addingPercentEncoding(withAllowedCharacters: .urlPathAllowed) ?? contactId
        let _: EmptyResponse = try await request(
            path: "/users/\(userId)/contacts/\(encoded)",
            settings: settings,
            method: "DELETE",
            body: Optional<EmptyBody>.none
        )
    }

    func deleteLineContact(settings: AlertContactSettings, userId: String, contactId: String) async throws {
        let encoded = contactId.addingPercentEncoding(withAllowedCharacters: .urlPathAllowed) ?? contactId
        let _: EmptyResponse = try await request(
            path: "/users/\(userId)/line-contacts/\(encoded)",
            settings: settings,
            method: "DELETE",
            body: Optional<EmptyBody>.none
        )
    }

    func deletePushContact(settings: AlertContactSettings, userId: String, contactId: String) async throws {
        let encoded = contactId.addingPercentEncoding(withAllowedCharacters: .urlPathAllowed) ?? contactId
        let _: EmptyResponse = try await request(
            path: "/users/\(userId)/push-contacts/\(encoded)",
            settings: settings,
            method: "DELETE",
            body: Optional<EmptyBody>.none
        )
    }

    func registerPushDevice(settings: AlertContactSettings, userId: String, deviceToken: String) async throws {
        let body: [String: String] = [
            "userId": userId,
            "name": settings.userName.isEmpty ? "Aurora contact" : settings.userName,
            "deviceName": settings.deviceName.isEmpty ? "iPhone" : settings.deviceName,
            "fcmToken": deviceToken
        ]
        let _: EmptyResponse = try await request(path: "/push/register-device", settings: settings, method: "POST", body: body)
    }

    func bindCurrentDevice(settings: AlertContactSettings, bindCode: String, contactUserId: String) async throws {
        let body: [String: String] = [
            "bindCode": bindCode,
            "contactUserId": contactUserId
        ]
        let _: EmptyResponse = try await request(path: "/push/bind", settings: settings, method: "POST", body: body)
    }

    func sendTelegramAlert(settings: AlertContactSettings, userId: String, message: String) async -> Bool {
        let body: [String: String] = ["userId": userId, "message": message]
        return await succeeds {
            let _: EmptyResponse = try await request(path: "/alerts", settings: settings, method: "POST", body: body)
        }
    }

    func sendLineAlert(settings: AlertContactSettings, userId: String, message: String) async -> Bool {
        let body: [String: String] = ["userId": userId, "message": message]
        return await succeeds {
            let _: EmptyResponse = try await request(path: "/line/alerts", settings: settings, method: "POST", body: body)
        }
    }

    func sendPushAlert(
        settings: AlertContactSettings,
        userId: String,
        eventId: String,
        title: String,
        message: String,
        details: AlertMessageDetails
    ) async -> Bool {
        let body = PushAlertRequest(
            userId: userId,
            eventId: eventId,
            messageType: "sos",
            title: title,
            message: message,
            sosMessage: message,
            sosDate: details.date,
            sosTime: details.time,
            sosDeviceName: details.deviceName,
            sosMobileNumber: details.mobileNumber,
            sosCurrentSoundLevel: details.currentSoundLevel,
            sosLocationLabel: details.locationLabel,
            sosLocationLink: details.locationLink
        )
        return await succeeds {
            let _: EmptyResponse = try await request(path: "/push/alerts", settings: settings, method: "POST", body: body)
        }
    }

    func sendAudioAlert(
        settings: AlertContactSettings,
        userId: String,
        clip: CrisisAudioClip,
        sendTelegram: Bool,
        sendLine: Bool,
        sendPush: Bool,
        eventId: String,
        title: String,
        message: String
    ) async -> Bool {
        guard let data = clip.wavData, let filename = clip.fileURL?.lastPathComponent else { return false }
        let body = AudioAlertRequest(
            userId: userId,
            caption: "5-second crisis audio clip captured by Aurora Security.",
            filename: filename,
            audioBase64: data.base64EncodedString(),
            sendTelegram: sendTelegram,
            sendLine: sendLine,
            sendPush: sendPush,
            pushEventId: eventId,
            pushTitle: title,
            pushMessage: message,
            durationMs: clip.durationMs
        )
        return await succeeds {
            let _: EmptyResponse = try await request(path: "/alerts/audio", settings: settings, method: "POST", body: body)
        }
    }

    func sendAnalysisResult(settings: AlertContactSettings, userId: String, analysisText: String) async -> Bool {
        let body: [String: String] = [
            "userId": userId,
            "message": "AI audio analysis result:\n\(analysisText)"
        ]
        return await succeeds {
            let _: EmptyResponse = try await request(path: "/alerts/analysis", settings: settings, method: "POST", body: body)
        }
    }

    private func succeeds(_ operation: () async throws -> Void) async -> Bool {
        do {
            try await operation()
            return true
        } catch {
            return false
        }
    }

    private func request<Response: Decodable, Body: Encodable>(
        path: String,
        settings: AlertContactSettings,
        method: String,
        body: Body?
    ) async throws -> Response {
        guard var components = URLComponents(string: resolveBaseURL(settings.apiUrl)) else {
            throw BackendError.invalidURL(settings.apiUrl)
        }
        let basePath = components.path.trimmingCharacters(in: CharacterSet(charactersIn: "/"))
        let appendedPath = path.trimmingCharacters(in: CharacterSet(charactersIn: "/"))
        components.path = "/" + [basePath, appendedPath].filter { !$0.isEmpty }.joined(separator: "/")
        guard let url = components.url else {
            throw BackendError.invalidURL(settings.apiUrl)
        }

        var request = URLRequest(url: url)
        request.httpMethod = method
        request.timeoutInterval = 10
        if !settings.apiToken.isEmpty {
            request.setValue("Bearer \(settings.apiToken)", forHTTPHeaderField: "Authorization")
        }
        if let body {
            request.setValue("application/json; charset=utf-8", forHTTPHeaderField: "Content-Type")
            request.httpBody = try JSONEncoder().encode(body)
        }

        let (data, response) = try await session.data(for: request)
        let statusCode = (response as? HTTPURLResponse)?.statusCode ?? 0
        guard (200...299).contains(statusCode) else {
            let message = String(data: data, encoding: .utf8) ?? ""
            throw BackendError.http(statusCode, message)
        }
        if Response.self == EmptyResponse.self {
            return EmptyResponse() as! Response
        }
        guard !data.isEmpty else {
            throw BackendError.emptyResponse
        }
        return try JSONDecoder().decode(Response.self, from: data)
    }

    private func resolveBaseURL(_ apiUrl: String) -> String {
        let trimmed = apiUrl.trimmingCharacters(in: .whitespacesAndNewlines).trimmingCharacters(in: CharacterSet(charactersIn: "/"))
        guard !trimmed.isEmpty else { return "" }
        return trimmed.hasPrefix("http") ? trimmed : "https://\(trimmed)"
    }
}

private enum BackendError: Error {
    case invalidURL(String)
    case http(Int, String)
    case emptyResponse
}

private struct EmptyBody: Encodable {}

private struct EmptyResponse: Decodable {
    init() {}
}

private struct RegisterDeviceResponse: Decodable {
    var deviceToken: String?
}

private struct ContactsResponse<Contact: Decodable>: Decodable {
    var contacts: [Contact]
}

private struct PushAlertRequest: Encodable {
    var userId: String
    var eventId: String
    var messageType: String
    var title: String
    var message: String
    var sosMessage: String
    var sosDate: String
    var sosTime: String
    var sosDeviceName: String
    var sosMobileNumber: String
    var sosCurrentSoundLevel: String
    var sosLocationLabel: String
    var sosLocationLink: String
}

private struct AudioAlertRequest: Encodable {
    var userId: String
    var caption: String
    var filename: String
    var audioBase64: String
    var sendTelegram: Bool
    var sendLine: Bool
    var sendPush: Bool
    var pushEventId: String
    var pushTitle: String
    var pushMessage: String
    var durationMs: Int
}
