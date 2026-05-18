import Foundation

struct AlertDispatchResult {
    var status: TextAlertDeliveryStatus
    var message: String
    var aiSummary: String?
}

final class AlertDispatcher {
    private let client = BackendHTTPClient()

    func dispatch(
        settings: AlertContactSettings,
        userId: String,
        currentDb: Double,
        location: AlertLocation?,
        clip: CrisisAudioClip?,
        aiSettings: AiSettings,
        aiSummary: String?,
        shouldSendSOS: Bool
    ) async -> AlertDispatchResult {
        let message = AlertMessageFormatter.format(settings: settings, currentDb: currentDb, location: location)
        guard shouldSendSOS else {
            return AlertDispatchResult(
                status: .skipped,
                message: message,
                aiSummary: aiSummary ?? "Verified SOS did not detect a high-danger audio pattern."
            )
        }

        var sendResults: [Bool] = []

        if settings.useTelegram {
            sendResults.append(await sendTelegramAlert(settings: settings, userId: userId, message: message))
            if aiSettings.sendAudioToContacts, let clip {
                _ = await sendAudio(settings: settings, userId: userId, clip: clip)
            }
        }

        if settings.usePush {
            sendResults.append(await sendPushAlert(settings: settings, userId: userId, message: message))
        }

        let status: TextAlertDeliveryStatus
        if sendResults.isEmpty {
            status = .skipped
        } else if sendResults.allSatisfy({ $0 }) {
            status = .sent
        } else if sendResults.contains(true) {
            status = .partial
        } else {
            status = .failed
        }

        let resolvedAiSummary = aiSummary ?? (aiSettings.enableAiAnalysis
            ? "LiteRT-LM audio analysis was unavailable, so Aurora used the direct SOS safety path."
            : nil)
        return AlertDispatchResult(status: status, message: message, aiSummary: resolvedAiSummary)
    }

    private func sendTelegramAlert(settings: AlertContactSettings, userId: String, message: String) async -> Bool {
        guard let baseURL = resolveBackendBaseURL(settings.apiUrl) else { return false }
        let url = baseURL.appendingPathComponent("alerts")
        do {
            _ = try await client.sendJSON(
                url: url,
                token: settings.apiToken,
                method: "POST",
                body: ["userId": userId, "message": message]
            )
            return true
        } catch {
            return false
        }
    }

    private func sendPushAlert(settings: AlertContactSettings, userId: String, message: String) async -> Bool {
        guard let baseURL = resolveBackendBaseURL(settings.apiUrl) else { return false }
        let url = baseURL.appendingPathComponent("push/alerts")
        do {
            _ = try await client.sendJSON(
                url: url,
                token: settings.apiToken,
                method: "POST",
                body: [
                    "userId": userId,
                    "eventId": "\(userId)-\(Date().timeIntervalSince1970)",
                    "messageType": "sos",
                    "title": "SOS Triggered",
                    "message": message
                ]
            )
            return true
        } catch {
            return false
        }
    }

    private func sendAudio(settings: AlertContactSettings, userId: String, clip: CrisisAudioClip) async -> Bool {
        guard let baseURL = resolveBackendBaseURL(settings.apiUrl) else { return false }
        let url = baseURL.appendingPathComponent("alerts/audio")
        do {
            _ = try await client.sendJSON(
                url: url,
                token: settings.apiToken,
                method: "POST",
                body: [
                    "userId": userId,
                    "caption": "5-second crisis audio clip captured by Aurora Security for iOS.",
                    "filename": clip.fileURL.lastPathComponent,
                    "audioBase64": clip.wavData.base64EncodedString()
                ]
            )
            return true
        } catch {
            return false
        }
    }
}
