import Foundation
import UIKit

@MainActor
final class EmergencyActionService {
    func openPhone(_ rawNumber: String) {
        let number = sanitizedPhoneNumber(rawNumber)
        guard !number.isEmpty, let url = URL(string: "tel://\(number)") else { return }
        UIApplication.shared.open(url)
    }

    func openSMS(_ rawNumber: String, body: String) {
        let number = sanitizedPhoneNumber(rawNumber)
        guard !number.isEmpty else { return }
        var components = URLComponents(string: "sms:\(number)")
        components?.queryItems = [URLQueryItem(name: "body", value: body)]
        guard let url = components?.url else { return }
        UIApplication.shared.open(url)
    }

    private func sanitizedPhoneNumber(_ value: String) -> String {
        value.filter { "+0123456789".contains($0) }
    }
}
