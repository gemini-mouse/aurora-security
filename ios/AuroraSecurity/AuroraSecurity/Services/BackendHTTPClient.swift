import Foundation

final class BackendHTTPClient {
    func sendJSON(
        url: URL,
        token: String,
        method: String,
        body: [String: Any]?
    ) async throws -> Data {
        var request = URLRequest(url: url)
        request.httpMethod = method
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        if !token.isEmpty {
            request.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
        }
        if let body {
            request.httpBody = try JSONSerialization.data(withJSONObject: body)
        }

        let (data, response) = try await URLSession.shared.data(for: request)
        guard let httpResponse = response as? HTTPURLResponse,
              (200..<300).contains(httpResponse.statusCode) else {
            throw URLError(.badServerResponse)
        }
        return data
    }
}

func resolveBackendBaseURL(_ apiUrl: String) -> URL? {
    guard let url = URL(string: apiUrl.trimmingCharacters(in: .whitespacesAndNewlines)) else { return nil }
    let path = url.path.trimmingCharacters(in: CharacterSet(charactersIn: "/"))
    if path == "telegram-alert" || path == "alerts" || path == "alerts/audio" || path == "alerts/analysis" {
        var components = URLComponents(url: url, resolvingAgainstBaseURL: false)
        components?.path = ""
        components?.query = nil
        return components?.url
    }
    return url
}
