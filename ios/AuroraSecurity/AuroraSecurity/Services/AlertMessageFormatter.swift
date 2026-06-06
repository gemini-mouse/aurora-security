import Foundation

enum AlertMessageFormatter {
    static func format(settings: AlertContactSettings, currentDb: Double, location: AlertLocation?) -> String {
        formatPayload(settings: settings, currentDb: currentDb, location: location).text
    }

    static func formatPayload(
        settings: AlertContactSettings,
        currentDb: Double,
        location: AlertLocation?
    ) -> AlertMessagePayload {
        let now = Date()
        let dateFormatter = DateFormatter()
        dateFormatter.dateFormat = "yyyy-MM-dd"
        let timeFormatter = DateFormatter()
        timeFormatter.dateFormat = "HH:mm:ss"

        let date = dateFormatter.string(from: now)
        let time = timeFormatter.string(from: now)
        let currentSoundLevel = "\(Int(currentDb.rounded())) dB"
        let locationValue = location?.googleMapsUrl ?? "unavailable"
        let templateContainsLocation = settings.alertMessageTemplate.contains("<Location>")

        let body = settings.alertMessageTemplate
            .replacingOccurrences(of: "<User Name>", with: settings.userName.isEmpty ? "User" : settings.userName)
            .replacingOccurrences(of: "<Date>", with: date)
            .replacingOccurrences(of: "<Time>", with: time)
            .replacingOccurrences(of: "<Mobile Number>", with: settings.mobileNumber.isEmpty ? "Not provided" : settings.mobileNumber)
            .replacingOccurrences(of: "<Current dB>", with: "\(Int(currentDb.rounded()))")
            .replacingOccurrences(of: "<Device Name>", with: settings.deviceName.isEmpty ? "iPhone" : settings.deviceName)
            .replacingOccurrences(of: "<Location>", with: locationValue)
            .trimmingCharacters(in: .whitespacesAndNewlines)

        var lines = [body]
        if !settings.alertMessageTemplate.contains("<Mobile Number>"), !settings.mobileNumber.isEmpty {
            lines.append("Mobile number: \(settings.mobileNumber)")
        }
        if !templateContainsLocation {
            lines.append("Location: \(locationValue)")
        }

        return AlertMessagePayload(
            text: lines.joined(separator: "\n"),
            historyMessage: body.components(separatedBy: "\n\n").first?.trimmingCharacters(in: .whitespacesAndNewlines) ?? body,
            details: AlertMessageDetails(
                date: date,
                time: time,
                deviceName: settings.deviceName.isEmpty ? "iPhone" : settings.deviceName,
                mobileNumber: settings.mobileNumber.trimmingCharacters(in: .whitespacesAndNewlines),
                currentSoundLevel: currentSoundLevel,
                locationLabel: location == nil ? "unavailable" : "",
                locationLink: location?.googleMapsUrl ?? ""
            )
        )
    }
}
