import Foundation

struct HistoryRecord: Identifiable, Codable, Equatable {
    var id: UUID
    var createdAt: Date
    var type: HistoryRecordType
    var triggerSource: EmergencyTriggerSource
    var emergencyMode: EmergencyMode
    var currentDb: Double
    var location: AlertLocation?
    var message: String
    var deliveryStatus: TextAlertDeliveryStatus
    var audioFilePath: String?
    var aiSummary: String?

    init(
        id: UUID = UUID(),
        createdAt: Date = Date(),
        type: HistoryRecordType,
        triggerSource: EmergencyTriggerSource,
        emergencyMode: EmergencyMode,
        currentDb: Double,
        location: AlertLocation?,
        message: String,
        deliveryStatus: TextAlertDeliveryStatus,
        audioFilePath: String?,
        aiSummary: String?
    ) {
        self.id = id
        self.createdAt = createdAt
        self.type = type
        self.triggerSource = triggerSource
        self.emergencyMode = emergencyMode
        self.currentDb = currentDb
        self.location = location
        self.message = message
        self.deliveryStatus = deliveryStatus
        self.audioFilePath = audioFilePath
        self.aiSummary = aiSummary
    }
}

enum HistoryRecordType: String, Codable, CaseIterable {
    case sos
    case incomingPush
    case test
}
