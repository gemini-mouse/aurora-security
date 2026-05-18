import CryptoKit
import Foundation

final class PreferencesStore {
    private let defaults = UserDefaults.standard

    var userId: String {
        if let existing = defaults.string(forKey: Keys.userId), !existing.isEmpty {
            return existing
        }
        let generated = "ios-\(UUID().uuidString.lowercased())"
        defaults.set(generated, forKey: Keys.userId)
        return generated
    }

    var bindCode: String {
        if let existing = defaults.string(forKey: Keys.bindCode), !existing.isEmpty {
            return existing
        }
        let seed = "\(userId):aurora-ios"
        let hash = SHA256.hash(data: Data(seed.utf8))
            .map { String(format: "%02x", $0) }
            .joined()
            .prefix(8)
            .uppercased()
        defaults.set(hash, forKey: Keys.bindCode)
        return hash
    }

    func hasAcceptedSafetyNotice() -> Bool {
        defaults.bool(forKey: Keys.acceptedSafetyNotice)
    }

    func setAcceptedSafetyNotice(_ accepted: Bool) {
        defaults.set(accepted, forKey: Keys.acceptedSafetyNotice)
    }

    func loadThresholdDb() -> Double {
        let stored = defaults.double(forKey: Keys.thresholdDb)
        return stored > 0 ? stored : 90
    }

    func saveThresholdDb(_ value: Double) {
        defaults.set(value, forKey: Keys.thresholdDb)
    }

    func loadContactSettings() -> AlertContactSettings {
        loadCodable(AlertContactSettings.self, key: Keys.contactSettings) ?? .default
    }

    func saveContactSettings(_ settings: AlertContactSettings) {
        saveCodable(settings, key: Keys.contactSettings)
    }

    func loadAiSettings() -> AiSettings {
        loadCodable(AiSettings.self, key: Keys.aiSettings) ?? .default
    }

    func saveAiSettings(_ settings: AiSettings) {
        saveCodable(settings, key: Keys.aiSettings)
    }

    func loadMovementSettings() -> MovementSettings {
        loadCodable(MovementSettings.self, key: Keys.movementSettings) ?? .default
    }

    func saveMovementSettings(_ settings: MovementSettings) {
        saveCodable(settings, key: Keys.movementSettings)
    }

    func loadThemeMode() -> AppThemeMode {
        guard let rawValue = defaults.string(forKey: Keys.themeMode) else { return .system }
        return AppThemeMode(rawValue: rawValue) ?? .system
    }

    func saveThemeMode(_ mode: AppThemeMode) {
        defaults.set(mode.rawValue, forKey: Keys.themeMode)
    }

    private func loadCodable<T: Decodable>(_ type: T.Type, key: String) -> T? {
        guard let data = defaults.data(forKey: key) else { return nil }
        return try? JSONDecoder().decode(type, from: data)
    }

    private func saveCodable<T: Encodable>(_ value: T, key: String) {
        guard let data = try? JSONEncoder().encode(value) else { return }
        defaults.set(data, forKey: key)
    }

    private enum Keys {
        static let userId = "userId"
        static let bindCode = "bindCode"
        static let acceptedSafetyNotice = "acceptedSafetyNotice"
        static let thresholdDb = "thresholdDb"
        static let contactSettings = "contactSettings"
        static let aiSettings = "aiSettings"
        static let movementSettings = "movementSettings"
        static let themeMode = "themeMode"
    }
}
