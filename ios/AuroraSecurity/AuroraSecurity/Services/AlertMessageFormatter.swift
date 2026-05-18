import Foundation

enum AlertMessageFormatter {
    static func format(
        settings: AlertContactSettings,
        currentDb: Double,
        location: AlertLocation?
    ) -> String {
        let dateFormatter = DateFormatter()
        dateFormatter.dateFormat = "yyyy-MM-dd"
        let timeFormatter = DateFormatter()
        timeFormatter.dateFormat = "HH:mm:ss"

        let body = settings.alertMessageTemplate
            .replacingOccurrences(of: "<User Name>", with: settings.userName.isEmpty ? "User" : settings.userName)
            .replacingOccurrences(of: "<Date>", with: dateFormatter.string(from: Date()))
            .replacingOccurrences(of: "<Time>", with: timeFormatter.string(from: Date()))
            .replacingOccurrences(of: "<Mobile Number>", with: settings.mobileNumber.isEmpty ? "Not provided" : settings.mobileNumber)
            .replacingOccurrences(of: "<Current dB>", with: "\(Int(currentDb.rounded()))")
            .replacingOccurrences(of: "<Device Name>", with: settings.deviceName.isEmpty ? "iPhone" : settings.deviceName)
            .replacingOccurrences(of: "<Location>", with: "")
            .trimmingCharacters(in: .whitespacesAndNewlines)

        let locationLine = location.map { "Location: \($0.mapsURL)" } ?? "Location: unavailable"
        let mobileLine = settings.mobileNumber.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
            ? nil
            : "Mobile number: \(settings.mobileNumber)"

        var lines = [body]
        if !settings.alertMessageTemplate.contains("<Mobile Number>"), let mobileLine {
            lines.append(mobileLine)
        }
        lines.append(locationLine)
        return lines.joined(separator: "\n")
    }
}
