import CoreLocation
import Foundation
import SwiftUI

enum AppTab: String, CaseIterable, Identifiable {
    case detect
    case ai
    case settings

    var id: String { rawValue }

    var title: String {
        switch self {
        case .detect: "Detect"
        case .ai: "AI"
        case .settings: "Settings"
        }
    }

    var systemImage: String {
        switch self {
        case .detect: "shield.lefthalf.filled"
        case .ai: "sparkles"
        case .settings: "gearshape"
        }
    }
}

enum HistoryFilter: String, CaseIterable, Identifiable {
    case all
    case local
    case incoming

    var id: String { rawValue }

    var title: String {
        switch self {
        case .all: "All"
        case .local: "Local"
        case .incoming: "Incoming"
        }
    }
}

struct AlertContactSettings: Codable, Equatable {
    var apiUrl: String
    var apiToken: String
    var botName: String
    var lineBotBasicId: String
    var deviceName: String
    var userName: String
    var mobileNumber: String
    var alertMessageTemplate: String
    var useLine: Bool
    var useTelegram: Bool
    var usePush: Bool
    var useSms: Bool
    var usePhoneCall: Bool
    var emergencyPhoneNumber: String
    var secondaryPhoneNumber: String
    var phoneCallPhoneNumber: String

    static func defaults(apiUrl: String, apiToken: String, deviceName: String) -> AlertContactSettings {
        AlertContactSettings(
            apiUrl: apiUrl,
            apiToken: apiToken,
            botName: "",
            lineBotBasicId: "@286hihbt",
            deviceName: deviceName,
            userName: "User",
            mobileNumber: "",
            alertMessageTemplate: defaultAlertMessageTemplate,
            useLine: false,
            useTelegram: false,
            usePush: false,
            useSms: false,
            usePhoneCall: false,
            emergencyPhoneNumber: "",
            secondaryPhoneNumber: "",
            phoneCallPhoneNumber: ""
        )
    }

    static let defaultAlertMessageTemplate = """
    <User Name> may be experiencing an emergency. Please try to contact them immediately. If you cannot reach them promptly, check on their safety in person and seek professional emergency assistance if needed.

    Date: <Date>
    Time: <Time>
    Mobile number: <Mobile Number>
    Device: <Device Name>
    Current sound level: <Current dB> dB
    Location: <Location>
    """
}

struct AiSettings: Codable, Equatable {
    var isAiEnabled: Bool = true
    var sendAudioToContacts: Bool = true
    var preloadAiInBackground: Bool = false
    var sosDispatchMode: SosDispatchMode = .immediate
    var modelType: String = GemmaModelType.e2b.rawValue
    var uploadTrainingTriggerAudio: Bool = false
}

enum SosDispatchMode: String, Codable, CaseIterable, Identifiable {
    case immediate = "Immediate"
    case aiFirst = "AiFirst"

    var id: String { rawValue }

    var title: String {
        switch self {
        case .immediate: "Instant SOS"
        case .aiFirst: "Verified SOS"
        }
    }

    var subtitle: String {
        switch self {
        case .immediate: "Quick action"
        case .aiFirst: "Higher accuracy"
        }
    }
}

enum LoudSosSound: String, Codable, CaseIterable, Identifiable {
    case siren = "Siren"
    case ringtone = "Ringtone"

    var id: String { rawValue }
}

enum AppThemeMode: String, Codable, CaseIterable, Identifiable {
    case light = "Light"
    case dark = "Dark"

    var id: String { rawValue }

    var colorScheme: ColorScheme {
        switch self {
        case .light: .light
        case .dark: .dark
        }
    }
}

enum AppLanguageOption: String, Codable, CaseIterable, Identifiable {
    case english = "en"
    case traditionalChinese = "zh-TW"
    case spanish = "es"
    case korean = "ko"
    case japanese = "ja"

    var id: String { rawValue }

    var label: String {
        switch self {
        case .english: "English"
        case .traditionalChinese: "Traditional Chinese"
        case .spanish: "Spanish"
        case .korean: "Korean"
        case .japanese: "Japanese"
        }
    }
}

enum TelegramBindingStatus: String, Codable {
    case pending = "Pending"
    case bound = "Bound"

    var label: String {
        switch self {
        case .pending: "Pending"
        case .bound: "Bound"
        }
    }
}

struct TelegramContact: Codable, Identifiable, Equatable {
    var id: String
    var name: String
    var telegramHandle: String
    var status: TelegramBindingStatus
}

struct PushContact: Codable, Identifiable, Equatable {
    var id: String
    var name: String
    var deviceName: String
    var status: TelegramBindingStatus
}

struct LineContact: Codable, Identifiable, Equatable {
    var id: String
    var name: String
    var status: TelegramBindingStatus
}

struct MovementSettings: Codable, Equatable {
    var useShakeDetection: Bool = true
    var shakeThresholdG: Double = 3.0
}

struct NoiseAlarmUiState: Equatable {
    var currentDb: Double = 0
    var thresholdDb: Double = 90
    var alarmTriggered = false
    var isSilenced = false
    var hasMicPermission = false
    var isMonitoring = false
    var isThresholdExceeded = false
    var activeEmergencyMode: EmergencyMode?
    var pendingEmergencyMode: EmergencyMode?
    var pendingTriggerSource: EmergencyTriggerSource?
    var countdownSecondsRemaining = 0
    var useShakeDetection = true
    var shakeThresholdG: Double = 3.0
    var currentG: Double = 1.0
    var isShaking = false
    var triggerDb: Double?
}

enum EmergencyMode: String, Codable, Equatable {
    case silent = "Silent"
    case loud = "Loud"

    var title: String {
        switch self {
        case .silent: "Silent SOS"
        case .loud: "Loud SOS"
        }
    }
}

enum EmergencyTriggerSource: String, Codable, Equatable {
    case soundDetection = "SoundDetection"
    case silentSosButton = "SilentSosButton"
    case loudSosButton = "LoudSosButton"
}

enum AiModelStatus: String, Codable {
    case notDownloaded
    case downloaded
    case initializing
    case ready
    case analyzing
    case error

    var label: String {
        switch self {
        case .notDownloaded: "Not downloaded"
        case .downloaded: "Downloaded"
        case .initializing: "Initializing"
        case .ready: "Ready"
        case .analyzing: "Analyzing"
        case .error: "Unavailable"
        }
    }
}

enum GemmaModelType: String, Codable, CaseIterable, Identifiable {
    case e2b = "E2B"
    case e4b = "E4B"

    var id: String { rawValue }
}

struct AiFeatureUiState: Equatable {
    var modelStatus: AiModelStatus = .notDownloaded
    var statusMessage: String = "iOS AI runtime adapter is ready for integration."
    var accelerator: String = "Not initialized"
    var currentModelType: GemmaModelType = .e2b
    var lastAnalysisText: String = ""
}

struct AiAnalysisResult: Equatable {
    var analysisText: String
    var dangerLevel: String?
}

struct AlertLocation: Codable, Equatable {
    var latitude: Double
    var longitude: Double

    var googleMapsUrl: String {
        "https://maps.google.com/?q=\(latitude),\(longitude)"
    }

    init(coordinate: CLLocationCoordinate2D) {
        latitude = coordinate.latitude
        longitude = coordinate.longitude
    }

    init(latitude: Double, longitude: Double) {
        self.latitude = latitude
        self.longitude = longitude
    }
}

struct AlertMessageDetails: Codable, Equatable {
    var date: String
    var time: String
    var deviceName: String
    var mobileNumber: String
    var currentSoundLevel: String
    var locationLabel: String
    var locationLink: String
}

struct AlertMessagePayload: Codable, Equatable {
    var text: String
    var historyMessage: String
    var details: AlertMessageDetails
}

enum TextAlertDeliveryStatus: String, Codable, CaseIterable {
    case sent = "Sent"
    case failed = "Failed"
    case skipped = "Skipped"

    var label: String {
        switch self {
        case .sent: "Sent"
        case .failed: "Failed"
        case .skipped: "Skipped"
        }
    }
}

struct AlertDeliverySummary: Identifiable, Equatable {
    var id: String
    var sosAlertStatus: TextAlertDeliveryStatus
    var audioEvidenceStatus: TextAlertDeliveryStatus
    var aiSummaryAnalysisStatus: TextAlertDeliveryStatus
}

enum HistoryRecordType: String, Codable {
    case localAlert = "LocalAlert"
    case incomingPush = "IncomingPush"
}

struct HistoryRecord: Codable, Identifiable, Equatable {
    var id: String
    var timestampMs: Int64
    var type: HistoryRecordType
    var eventId: String
    var sourceUserId: String
    var messageType: String
    var isTest: Bool
    var notificationTitle: String
    var sosText: String
    var sosMessage: String
    var sosDate: String
    var sosTime: String
    var sosDeviceName: String
    var sosMobileNumber: String
    var sosCurrentSoundLevel: String
    var sosLocationLabel: String
    var sosLocationLink: String
    var audioFilePath: String
    var originalAudioFilePath: String
    var aiResultText: String
    var textAlertDeliveryStatus: String
    var sosAlertStatus: String
    var audioEvidenceStatus: String
    var aiSummaryAnalysisStatus: String

    var timestamp: Date {
        Date(timeIntervalSince1970: TimeInterval(timestampMs) / 1000)
    }

    var deliverySummary: AlertDeliverySummary? {
        guard !sosAlertStatus.isEmpty || !audioEvidenceStatus.isEmpty || !aiSummaryAnalysisStatus.isEmpty else {
            return nil
        }
        return AlertDeliverySummary(
            id: id,
            sosAlertStatus: TextAlertDeliveryStatus(rawValue: sosAlertStatus) ?? .skipped,
            audioEvidenceStatus: TextAlertDeliveryStatus(rawValue: audioEvidenceStatus) ?? .skipped,
            aiSummaryAnalysisStatus: TextAlertDeliveryStatus(rawValue: aiSummaryAnalysisStatus) ?? .skipped
        )
    }
}

struct CrisisAudioClip: Identifiable, Equatable {
    var id: Int64
    var triggerSource: EmergencyTriggerSource
    var mode: EmergencyMode
    var fileURL: URL?
    var wavData: Data?
    var capturedAt: Date
    var sampleRate: Double
    var durationMs: Int
}

enum SecurityModePreset: String, CaseIterable, Identifiable {
    case daily
    case travel
    case workout
    case nightWalk
    case custom

    var id: String { rawValue }

    var label: String {
        switch self {
        case .daily: "Daily"
        case .travel: "Travel"
        case .workout: "Workout"
        case .nightWalk: "Night walk"
        case .custom: "Custom"
        }
    }

    var description: String {
        switch self {
        case .daily: "Balanced monitoring for everyday commutes."
        case .travel: "More sensitive when moving through unfamiliar places."
        case .workout: "Avoid false alarms during high movement."
        case .nightWalk: "Lower sound threshold and immediate dispatch."
        case .custom: "Your current manual settings."
        }
    }

    var systemImage: String {
        switch self {
        case .daily: "figure.walk"
        case .travel: "suitcase"
        case .workout: "figure.run"
        case .nightWalk: "moon.stars"
        case .custom: "slider.horizontal.3"
        }
    }

    var thresholdDb: Double {
        switch self {
        case .daily: 90
        case .travel: 85
        case .workout: 100
        case .nightWalk: 80
        case .custom: 0
        }
    }

    var shakeDetectionEnabled: Bool {
        switch self {
        case .daily, .travel, .workout: true
        case .nightWalk, .custom: false
        }
    }

    var movementThresholdG: Double? {
        switch self {
        case .daily: 3.0
        case .travel: 2.0
        case .workout: 4.5
        case .nightWalk, .custom: nil
        }
    }

    var preloadAiEngine: Bool {
        switch self {
        case .travel, .nightWalk: true
        case .daily, .workout, .custom: false
        }
    }

    var sosDispatchMode: SosDispatchMode {
        switch self {
        case .travel, .nightWalk: .immediate
        case .daily, .workout: .aiFirst
        case .custom: .immediate
        }
    }

    static func current(thresholdDb: Double, movementSettings: MovementSettings, aiSettings: AiSettings) -> SecurityModePreset {
        SecurityModePreset.allCases.first { preset in
            guard preset != .custom else { return false }
            let thresholdMatches = abs(thresholdDb - preset.thresholdDb) < 0.05
            let shakeMatches = movementSettings.useShakeDetection == preset.shakeDetectionEnabled
            let movementMatches: Bool
            if preset.shakeDetectionEnabled {
                movementMatches = abs(movementSettings.shakeThresholdG - (preset.movementThresholdG ?? movementSettings.shakeThresholdG)) < 0.05
            } else {
                movementMatches = true
            }
            let preloadMatches = !aiSettings.isAiEnabled || aiSettings.preloadAiInBackground == preset.preloadAiEngine
            return thresholdMatches && shakeMatches && movementMatches && preloadMatches && aiSettings.sosDispatchMode == preset.sosDispatchMode
        } ?? .custom
    }
}

enum PermissionKind: String, CaseIterable, Identifiable {
    case microphone
    case location
    case motion
    case notifications
    case phoneCall
    case sms

    var id: String { rawValue }

    var title: String {
        switch self {
        case .microphone: "Microphone"
        case .location: "Location"
        case .motion: "Motion"
        case .notifications: "Notifications"
        case .phoneCall: "Phone call"
        case .sms: "SMS"
        }
    }

    var systemImage: String {
        switch self {
        case .microphone: "mic"
        case .location: "location"
        case .motion: "sensor.tag.radiowaves.forward"
        case .notifications: "bell.badge"
        case .phoneCall: "phone"
        case .sms: "message"
        }
    }
}
