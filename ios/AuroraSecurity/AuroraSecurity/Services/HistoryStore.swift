import Foundation

final class HistoryStore {
    private let fileURL: URL

    init() {
        let documents = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask).first
            ?? FileManager.default.temporaryDirectory
        fileURL = documents.appendingPathComponent("aurora-history.json")
    }

    func loadRecords() -> [HistoryRecord] {
        guard let data = try? Data(contentsOf: fileURL) else { return [] }
        let records = (try? JSONDecoder.aurora.decode([HistoryRecord].self, from: data)) ?? []
        return records.sorted { $0.createdAt > $1.createdAt }
    }

    func save(_ record: HistoryRecord) {
        var records = loadRecords()
        records.insert(record, at: 0)
        records = Array(records.prefix(100))
        guard let data = try? JSONEncoder.aurora.encode(records) else { return }
        try? data.write(to: fileURL, options: [.atomic])
    }
}

private extension JSONDecoder {
    static var aurora: JSONDecoder {
        let decoder = JSONDecoder()
        decoder.dateDecodingStrategy = .iso8601
        return decoder
    }
}

private extension JSONEncoder {
    static var aurora: JSONEncoder {
        let encoder = JSONEncoder()
        encoder.dateEncodingStrategy = .iso8601
        encoder.outputFormatting = [.prettyPrinted, .sortedKeys]
        return encoder
    }
}
