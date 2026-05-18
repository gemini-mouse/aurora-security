import Foundation
import SwiftUI

struct AlertContactSettings: Codable, Equatable {
    var apiUrl: String
    var apiToken: String
    var botName: String
    var deviceName: String
    var userName: String
    var mobileNumber: String
    var alertMessageTemplate: String
    var useTelegram: Bool
    var usePush: Bool
    var useSms: Bool
    var usePhoneCall: Bool
    var emergencyPhoneNumber: String
    var secondaryPhoneNumber: String

    static let `default` = AlertContactSettings(
        apiUrl: "",
        apiToken: "",
        botName: "",
        deviceName: "iPhone",
        userName: "User",
        mobileNumber: "",
        alertMessageTemplate: "SOS from <User Name>\nDate: <Date>\nTime: <Time>\nDevice: <Device Name>\nSound level: <Current dB> dB",
        useTelegram: true,
        usePush: true,
        useSms: false,
        usePhoneCall: false,
        emergencyPhoneNumber: "",
        secondaryPhoneNumber: ""
    )
}

struct AiSettings: Codable, Equatable {
    var isAiEnabled: Bool
    var sendAudioToContacts: Bool
    var enableAiAnalysis: Bool
    var preloadAiInBackground: Bool
    var sosDispatchMode: SosDispatchMode
    var modelType: String
    var uploadTrainingTriggerAudio: Bool

    static let `default` = AiSettings(
        isAiEnabled: true,
        sendAudioToContacts: true,
        enableAiAnalysis: true,
        preloadAiInBackground: false,
        sosDispatchMode: .immediate,
        modelType: GemmaLiteRTModel.e2b.rawValue,
        uploadTrainingTriggerAudio: false
    )
}

enum SosDispatchMode: String, Codable, CaseIterable, Identifiable {
    case immediate
    case aiFirst

    var id: String { rawValue }

    var title: String {
        switch self {
        case .immediate: "Instant SOS"
        case .aiFirst: "Verified SOS"
        }
    }
}

struct MovementSettings: Codable, Equatable {
    var useShakeDetection: Bool
    var shakeThresholdG: Double

    static let `default` = MovementSettings(useShakeDetection: true, shakeThresholdG: 3.0)
}

enum AppThemeMode: String, Codable, CaseIterable, Identifiable {
    case light
    case dark
    case system

    var id: String { rawValue }

    var title: String {
        switch self {
        case .light: "Light"
        case .dark: "Dark"
        case .system: "System"
        }
    }

    var colorScheme: ColorScheme? {
        switch self {
        case .light: .light
        case .dark: .dark
        case .system: nil
        }
    }
}

enum EmergencyMode: String, Codable, CaseIterable {
    case silent
    case loud

    var title: String {
        switch self {
        case .silent: "Silent SOS"
        case .loud: "Loud SOS"
        }
    }
}

enum EmergencyTriggerSource: String, Codable, CaseIterable {
    case soundDetection
    case silentSosButton
    case loudSosButton

    var title: String {
        switch self {
        case .soundDetection: "Sound detection"
        case .silentSosButton: "Silent SOS"
        case .loudSosButton: "Loud SOS"
        }
    }
}

struct AlertLocation: Codable, Equatable {
    var latitude: Double
    var longitude: Double

    var mapsURL: String {
        "https://maps.apple.com/?ll=\(latitude),\(longitude)"
    }
}

enum TextAlertDeliveryStatus: String, Codable {
    case skipped
    case sent
    case failed
    case partial

    var displayName: String {
        switch self {
        case .skipped: "Alert skipped"
        case .sent: "Alert sent"
        case .failed: "Alert failed"
        case .partial: "Alert partially sent"
        }
    }
}

enum AiModelStatus: String, Codable {
    case notDownloaded
    case downloading
    case downloaded
    case initializing
    case ready
    case error

    var title: String {
        switch self {
        case .notDownloaded: "Not downloaded"
        case .downloading: "Downloading"
        case .downloaded: "Downloaded"
        case .initializing: "Initializing"
        case .ready: "Ready"
        case .error: "Unavailable"
        }
    }
}

enum GemmaLiteRTModel: String, Codable, CaseIterable, Identifiable {
    case e2b = "E2B"
    case e4b = "E4B"

    var id: String { rawValue }

    var modelName: String {
        switch self {
        case .e2b: "Gemma-4-E2B-it"
        case .e4b: "Gemma-4-E4B-it"
        }
    }

    var filename: String {
        switch self {
        case .e2b: "gemma-4-E2B-it.litertlm"
        case .e4b: "gemma-4-E4B-it.litertlm"
        }
    }

    var repository: String {
        switch self {
        case .e2b: "litert-community/gemma-4-E2B-it-litert-lm"
        case .e4b: "litert-community/gemma-4-E4B-it-litert-lm"
        }
    }

    var version: String {
        switch self {
        case .e2b: "b4f4f4df93418ddb4aa7da8bf33b584602a5b9f8"
        case .e4b: "10848a680ad0ab45b556566152371b38c238d6f0"
        }
    }

    var downloadURL: URL? {
        URL(string: "https://huggingface.co/\(repository)/resolve/\(version)/\(filename)?download=true")
    }

    var minimumValidBytes: Int64 {
        switch self {
        case .e2b: 2_000_000_000
        case .e4b: 3_000_000_000
        }
    }

    var displaySize: String {
        switch self {
        case .e2b: "2.6 GB"
        case .e4b: "3.7 GB"
        }
    }

    var summary: String {
        switch self {
        case .e2b:
            "Fast MTP-capable LiteRT-LM model. Best first target for iPhone memory and startup reliability."
        case .e4b:
            "Larger LiteRT-LM model with stronger analysis potential. Best for newer Pro-class devices."
        }
    }

    static func fromSettings(_ settings: AiSettings) -> GemmaLiteRTModel {
        GemmaLiteRTModel(rawValue: settings.modelType) ?? .e2b
    }
}

struct AiFeatureState: Codable, Equatable {
    var currentModel: GemmaLiteRTModel = .e2b
    var modelStatus: AiModelStatus = .notDownloaded
    var downloadProgress: Double = 0
    var statusMessage: String = "Download a Gemma LiteRT-LM model to enable local audio analysis."
    var accelerator: String = "LiteRT-LM Swift adapter not linked"
    var isAnalyzing = false
    var lastAnalysisResult: String?
}

enum PermissionAvailability: String, Codable {
    case notDetermined
    case granted
    case denied
    case limited
    case unsupported

    var isReady: Bool {
        self == .granted || self == .limited
    }
}

struct PermissionState: Codable, Equatable {
    var microphone: PermissionAvailability = .notDetermined
    var location: PermissionAvailability = .notDetermined
    var notifications: PermissionAvailability = .notDetermined
}

struct NoiseAlarmState: Codable, Equatable {
    var currentDb: Double = 0
    var thresholdDb: Double = 90
    var alarmTriggered = false
    var isSilenced = false
    var isMonitoring = false
    var isThresholdExceeded = false
    var activeEmergencyMode: EmergencyMode?
    var pendingEmergencyMode: EmergencyMode?
    var lastTriggerSource: EmergencyTriggerSource?
    var countdownSecondsRemaining = 0
    var triggerDb: Double?

    mutating func updateLevel(_ db: Double) {
        currentDb = db
        if db < thresholdDb {
            isSilenced = false
        }
        isThresholdExceeded = isMonitoring && !isSilenced && db >= thresholdDb
    }

    mutating func startCountdown(mode: EmergencyMode, source: EmergencyTriggerSource) {
        isSilenced = false
        alarmTriggered = false
        activeEmergencyMode = nil
        pendingEmergencyMode = mode
        lastTriggerSource = source
        countdownSecondsRemaining = 3
        triggerDb = source == .soundDetection ? currentDb : triggerDb ?? currentDb
    }

    mutating func activatePendingEmergency() {
        guard let mode = pendingEmergencyMode else { return }
        activeEmergencyMode = mode
        alarmTriggered = mode == .loud
        pendingEmergencyMode = nil
        countdownSecondsRemaining = 0
    }

    mutating func stopAlarm() {
        isSilenced = true
        alarmTriggered = false
        activeEmergencyMode = nil
        pendingEmergencyMode = nil
        countdownSecondsRemaining = 0
        triggerDb = nil
    }
}
