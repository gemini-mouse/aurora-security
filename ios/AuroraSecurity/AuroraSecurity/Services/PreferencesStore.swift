import CryptoKit
import Foundation
import UIKit

final class PreferencesStore {
    static let shared = PreferencesStore()

    private let defaults: UserDefaults
    private let encoder = JSONEncoder()
    private let decoder = JSONDecoder()

    init(defaults: UserDefaults = .standard) {
        self.defaults = defaults
    }

    var backendBootstrapApiToken: String {
        infoString("AuroraBackendBootstrapToken")
    }

    var defaultBackendUrl: String {
        infoString("AuroraBackendURL")
    }

    var userId: String {
        existingOrGeneratedString(key: Keys.userId) {
            let seed = stableDeviceSeed()
            return "device-\(sha256("\(seed):app.aurorasecurity.ios").prefix(24))"
        }
    }

    var bindCode: String {
        existingOrGeneratedString(key: Keys.bindCode) {
            let seed = stableDeviceSeed()
            return String(sha256("bind:\(seed):app.aurorasecurity.ios").prefix(12)).uppercased()
        }
    }

    var backendInstallSecret: String {
        existingOrGeneratedString(key: Keys.backendInstallSecret) {
            let seed = stableDeviceSeed()
            return sha256("backend-install:\(seed):app.aurorasecurity.ios")
        }
    }

    func loadThresholdDb() -> Double {
        let value = defaults.double(forKey: Keys.thresholdDb)
        return value > 0 ? value : 90
    }

    func saveThresholdDb(_ value: Double) {
        defaults.set(value, forKey: Keys.thresholdDb)
    }

    func loadContactSettings() -> AlertContactSettings {
        if let stored: AlertContactSettings = loadCodable(Keys.contactSettings) {
            return stored
        }
        return .defaults(
            apiUrl: defaultBackendUrl,
            apiToken: backendDeviceToken.isEmpty ? backendBootstrapApiToken : backendDeviceToken,
            deviceName: UIDevice.current.name.isEmpty ? "iPhone" : UIDevice.current.name
        )
    }

    func saveContactSettings(_ settings: AlertContactSettings) {
        saveCodable(settings, key: Keys.contactSettings)
    }

    func loadAiSettings() -> AiSettings {
        loadCodable(Keys.aiSettings) ?? AiSettings()
    }

    func saveAiSettings(_ settings: AiSettings) {
        saveCodable(settings, key: Keys.aiSettings)
    }

    func loadMovementSettings() -> MovementSettings {
        loadCodable(Keys.movementSettings) ?? MovementSettings()
    }

    func saveMovementSettings(_ settings: MovementSettings) {
        saveCodable(settings, key: Keys.movementSettings)
    }

    func loadLoudSosSound() -> LoudSosSound {
        guard let raw = defaults.string(forKey: Keys.loudSosSound) else { return .ringtone }
        return LoudSosSound(rawValue: raw) ?? .ringtone
    }

    func saveLoudSosSound(_ sound: LoudSosSound) {
        defaults.set(sound.rawValue, forKey: Keys.loudSosSound)
    }

    func loadThemeMode() -> AppThemeMode {
        guard let raw = defaults.string(forKey: Keys.themeMode) else { return .light }
        return AppThemeMode(rawValue: raw) ?? .light
    }

    func saveThemeMode(_ mode: AppThemeMode) {
        defaults.set(mode.rawValue, forKey: Keys.themeMode)
    }

    func loadAppLanguage() -> AppLanguageOption {
        guard let raw = defaults.string(forKey: Keys.appLanguage) else { return .english }
        return AppLanguageOption(rawValue: raw) ?? .english
    }

    func saveAppLanguage(_ language: AppLanguageOption) {
        defaults.set(language.rawValue, forKey: Keys.appLanguage)
    }

    var backendDeviceToken: String {
        defaults.string(forKey: Keys.backendDeviceToken) ?? ""
    }

    func saveBackendDeviceToken(_ token: String) {
        defaults.set(token.trimmingCharacters(in: .whitespacesAndNewlines), forKey: Keys.backendDeviceToken)
    }

    var apnsDeviceToken: String {
        defaults.string(forKey: Keys.apnsDeviceToken) ?? ""
    }

    func saveAPNsDeviceToken(_ token: String) {
        defaults.set(token, forKey: Keys.apnsDeviceToken)
    }

    func loadTelegramContacts() -> [TelegramContact] {
        loadCodable(Keys.telegramContacts) ?? []
    }

    func saveTelegramContacts(_ contacts: [TelegramContact]) {
        saveCodable(contacts, key: Keys.telegramContacts)
    }

    func loadPushContacts() -> [PushContact] {
        loadCodable(Keys.pushContacts) ?? []
    }

    func savePushContacts(_ contacts: [PushContact]) {
        saveCodable(contacts, key: Keys.pushContacts)
    }

    func loadLineContacts() -> [LineContact] {
        loadCodable(Keys.lineContacts) ?? []
    }

    func saveLineContacts(_ contacts: [LineContact]) {
        saveCodable(contacts, key: Keys.lineContacts)
    }

    func loadMonitoringPaused() -> Bool {
        defaults.bool(forKey: Keys.monitoringPaused)
    }

    func saveMonitoringPaused(_ paused: Bool) {
        defaults.set(paused, forKey: Keys.monitoringPaused)
    }

    func resetLocalSettings() {
        Keys.all.forEach { defaults.removeObject(forKey: $0) }
    }

    private func saveCodable<T: Encodable>(_ value: T, key: String) {
        guard let data = try? encoder.encode(value) else { return }
        defaults.set(data, forKey: key)
    }

    private func loadCodable<T: Decodable>(_ key: String) -> T? {
        guard let data = defaults.data(forKey: key) else { return nil }
        return try? decoder.decode(T.self, from: data)
    }

    private func infoString(_ key: String) -> String {
        (Bundle.main.object(forInfoDictionaryKey: key) as? String)?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
    }

    private func stableDeviceSeed() -> String {
        if let vendorId = UIDevice.current.identifierForVendor?.uuidString, !vendorId.isEmpty {
            return vendorId
        }
        return existingOrGeneratedString(key: Keys.localInstallId) { UUID().uuidString }
    }

    private func existingOrGeneratedString(key: String, generator: () -> String) -> String {
        if let existing = defaults.string(forKey: key), !existing.isEmpty {
            return existing
        }
        let generated = generator()
        defaults.set(generated, forKey: key)
        return generated
    }

    private func sha256(_ value: String) -> String {
        let digest = SHA256.hash(data: Data(value.utf8))
        return digest.map { String(format: "%02x", $0) }.joined()
    }

    private enum Keys {
        static let localInstallId = "ios_local_install_id"
        static let userId = "user_id"
        static let bindCode = "bind_code"
        static let backendInstallSecret = "backend_install_secret"
        static let backendDeviceToken = "backend_device_token"
        static let apnsDeviceToken = "apns_device_token"
        static let thresholdDb = "threshold_db"
        static let contactSettings = "contact_settings"
        static let aiSettings = "ai_settings"
        static let movementSettings = "movement_settings"
        static let loudSosSound = "loud_sos_sound"
        static let themeMode = "theme_mode"
        static let appLanguage = "app_language"
        static let telegramContacts = "telegram_contacts"
        static let pushContacts = "push_contacts"
        static let lineContacts = "line_contacts"
        static let monitoringPaused = "monitoring_paused"

        static let all = [
            localInstallId,
            userId,
            bindCode,
            backendInstallSecret,
            backendDeviceToken,
            apnsDeviceToken,
            thresholdDb,
            contactSettings,
            aiSettings,
            movementSettings,
            loudSosSound,
            themeMode,
            appLanguage,
            telegramContacts,
            pushContacts,
            lineContacts,
            monitoringPaused
        ]
    }
}
