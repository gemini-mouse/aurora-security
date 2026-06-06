import Foundation

final class HistoryStore {
    private let encoder = JSONEncoder()
    private let decoder = JSONDecoder()
    private let recordsURL: URL
    private let audioDirectoryURL: URL

    init() {
        let support = FileManager.default.urls(for: .applicationSupportDirectory, in: .userDomainMask).first!
        let root = support.appendingPathComponent("AuroraSecurity", isDirectory: true)
        recordsURL = root.appendingPathComponent("history-records.json")
        audioDirectoryURL = root.appendingPathComponent("history-audio", isDirectory: true)
        try? FileManager.default.createDirectory(at: audioDirectoryURL, withIntermediateDirectories: true)
    }

    func loadRecords() -> [HistoryRecord] {
        guard let data = try? Data(contentsOf: recordsURL) else { return [] }
        return (try? decoder.decode([HistoryRecord].self, from: data))?
            .sorted { $0.timestampMs > $1.timestampMs } ?? []
    }

    func saveRecord(
        isTest: Bool,
        sosText: String,
        sosMessage: String,
        sosDetails: AlertMessageDetails?,
        sourceAudioURL: URL?,
        aiResultText: String,
        sosAlertStatus: TextAlertDeliveryStatus?,
        audioEvidenceStatus: TextAlertDeliveryStatus?,
        aiSummaryAnalysisStatus: TextAlertDeliveryStatus?
    ) -> HistoryRecord {
        let id = UUID().uuidString
        let audioPath = copyAudioToHistory(recordId: id, sourceAudioURL: sourceAudioURL)
        let nowMs = Int64(Date().timeIntervalSince1970 * 1000)
        let record = HistoryRecord(
            id: id,
            timestampMs: nowMs,
            type: .localAlert,
            eventId: id,
            sourceUserId: "",
            messageType: "local_alert",
            isTest: isTest,
            notificationTitle: "",
            sosText: sosText,
            sosMessage: sosMessage,
            sosDate: sosDetails?.date ?? "",
            sosTime: sosDetails?.time ?? "",
            sosDeviceName: sosDetails?.deviceName ?? "",
            sosMobileNumber: sosDetails?.mobileNumber ?? "",
            sosCurrentSoundLevel: sosDetails?.currentSoundLevel ?? "",
            sosLocationLabel: sosDetails?.locationLabel ?? "",
            sosLocationLink: sosDetails?.locationLink ?? "",
            audioFilePath: audioPath,
            originalAudioFilePath: sourceAudioURL?.path ?? "",
            aiResultText: aiResultText,
            textAlertDeliveryStatus: sosAlertStatus?.rawValue ?? "",
            sosAlertStatus: sosAlertStatus?.rawValue ?? "",
            audioEvidenceStatus: audioEvidenceStatus?.rawValue ?? "",
            aiSummaryAnalysisStatus: aiSummaryAnalysisStatus?.rawValue ?? ""
        )
        var records = loadRecords()
        records.insert(record, at: 0)
        persist(records)
        return record
    }

    func upsertIncomingPushRecord(userInfo: [AnyHashable: Any]) -> HistoryRecord? {
        let eventId = stringValue(userInfo["eventId"]) ?? stringValue(userInfo["gcm.message_id"]) ?? UUID().uuidString
        let messageType = stringValue(userInfo["messageType"]) ?? "sos"
        let title = stringValue(userInfo["title"]) ?? "SOS Triggered"
        let message = stringValue(userInfo["message"]) ?? stringValue(userInfo["aps"]) ?? ""
        let sourceUserId = stringValue(userInfo["sourceUserId"]) ?? ""
        let nowMs = Int64(Date().timeIntervalSince1970 * 1000)

        var records = loadRecords()
        if let index = records.firstIndex(where: { $0.type == .incomingPush && $0.eventId == eventId }) {
            var existing = records[index]
            if messageType == "ai_analysis" {
                existing.aiResultText = message
            } else {
                existing.notificationTitle = title
                existing.sosText = message
                existing.sosMessage = message
            }
            records[index] = existing
            persist(records)
            return existing
        }

        let record = HistoryRecord(
            id: UUID().uuidString,
            timestampMs: nowMs,
            type: .incomingPush,
            eventId: eventId,
            sourceUserId: sourceUserId,
            messageType: messageType,
            isTest: false,
            notificationTitle: title,
            sosText: messageType == "ai_analysis" ? "" : message,
            sosMessage: messageType == "ai_analysis" ? "" : message,
            sosDate: stringValue(userInfo["sosDate"]) ?? "",
            sosTime: stringValue(userInfo["sosTime"]) ?? "",
            sosDeviceName: stringValue(userInfo["sosDeviceName"]) ?? "",
            sosMobileNumber: stringValue(userInfo["sosMobileNumber"]) ?? "",
            sosCurrentSoundLevel: stringValue(userInfo["sosCurrentSoundLevel"]) ?? "",
            sosLocationLabel: stringValue(userInfo["sosLocationLabel"]) ?? "",
            sosLocationLink: stringValue(userInfo["sosLocationLink"]) ?? "",
            audioFilePath: stringValue(userInfo["audioUrl"]) ?? "",
            originalAudioFilePath: "",
            aiResultText: messageType == "ai_analysis" ? message : "",
            textAlertDeliveryStatus: "",
            sosAlertStatus: "",
            audioEvidenceStatus: "",
            aiSummaryAnalysisStatus: ""
        )
        records.insert(record, at: 0)
        persist(records)
        return record
    }

    func deleteRecord(_ record: HistoryRecord) {
        if !record.audioFilePath.isEmpty {
            try? FileManager.default.removeItem(at: URL(fileURLWithPath: record.audioFilePath))
        }
        persist(loadRecords().filter { $0.id != record.id })
    }

    func clearAll() {
        let records = loadRecords()
        for record in records where !record.audioFilePath.isEmpty {
            try? FileManager.default.removeItem(at: URL(fileURLWithPath: record.audioFilePath))
        }
        persist([])
    }

    private func copyAudioToHistory(recordId: String, sourceAudioURL: URL?) -> String {
        guard let sourceAudioURL, FileManager.default.fileExists(atPath: sourceAudioURL.path) else {
            return ""
        }
        let target = audioDirectoryURL.appendingPathComponent("\(recordId).wav")
        do {
            if FileManager.default.fileExists(atPath: target.path) {
                try FileManager.default.removeItem(at: target)
            }
            try FileManager.default.copyItem(at: sourceAudioURL, to: target)
            return target.path
        } catch {
            return ""
        }
    }

    private func persist(_ records: [HistoryRecord]) {
        do {
            try FileManager.default.createDirectory(at: recordsURL.deletingLastPathComponent(), withIntermediateDirectories: true)
            let data = try encoder.encode(records.sorted { $0.timestampMs > $1.timestampMs })
            try data.write(to: recordsURL, options: [.atomic])
        } catch {
            assertionFailure("Failed to persist history: \(error.localizedDescription)")
        }
    }

    private func stringValue(_ value: Any?) -> String? {
        if let value = value as? String {
            return value
        }
        if let value = value as? CustomStringConvertible {
            return value.description
        }
        return nil
    }
}
