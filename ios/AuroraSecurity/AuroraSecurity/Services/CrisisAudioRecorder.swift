import Foundation

struct CrisisAudioClip {
    var id: UUID
    var triggerSource: EmergencyTriggerSource
    var emergencyMode: EmergencyMode
    var fileURL: URL
    var wavData: Data
}

final class CrisisAudioRecorder {
    private var sampleRate: Double = 16_000
    private var ringBuffer: [Int16] = []
    private var writeIndex = 0
    private let durationSeconds = 5
    private let lock = NSLock()

    func reset(sampleRate: Double) {
        lock.lock()
        defer { lock.unlock() }
        self.sampleRate = sampleRate
        let capacity = Int(sampleRate) * durationSeconds
        ringBuffer = Array(repeating: 0, count: capacity)
        writeIndex = 0
    }

    func append(samples: [Float]) {
        lock.lock()
        defer { lock.unlock() }
        guard !ringBuffer.isEmpty else { return }
        for sample in samples {
            let clamped = max(-1, min(1, sample))
            ringBuffer[writeIndex] = Int16(clamped * Float(Int16.max))
            writeIndex = (writeIndex + 1) % ringBuffer.count
        }
    }

    func makeClip(triggerSource: EmergencyTriggerSource, emergencyMode: EmergencyMode) -> CrisisAudioClip? {
        lock.lock()
        let orderedSamples = Array(ringBuffer[writeIndex..<ringBuffer.count]) + Array(ringBuffer[0..<writeIndex])
        lock.unlock()

        guard orderedSamples.contains(where: { $0 != 0 }) else { return nil }
        let wavData = WAVEncoder.encodePCM16(samples: orderedSamples, sampleRate: Int(sampleRate))
        let fileURL = Self.outputDirectory()
            .appendingPathComponent("crisis-\(ISO8601DateFormatter().string(from: Date())).wav")
        try? FileManager.default.createDirectory(
            at: fileURL.deletingLastPathComponent(),
            withIntermediateDirectories: true,
            attributes: nil
        )
        try? wavData.write(to: fileURL, options: [.atomic])

        return CrisisAudioClip(
            id: UUID(),
            triggerSource: triggerSource,
            emergencyMode: emergencyMode,
            fileURL: fileURL,
            wavData: wavData
        )
    }

    private static func outputDirectory() -> URL {
        let documents = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask).first
            ?? FileManager.default.temporaryDirectory
        return documents.appendingPathComponent("CrisisAudio", isDirectory: true)
    }
}

enum WAVEncoder {
    static func encodePCM16(samples: [Int16], sampleRate: Int) -> Data {
        let channelCount = 1
        let bitsPerSample = 16
        let byteRate = sampleRate * channelCount * bitsPerSample / 8
        let blockAlign = channelCount * bitsPerSample / 8
        let dataSize = samples.count * MemoryLayout<Int16>.size

        var data = Data()
        data.append("RIFF")
        data.appendUInt32(UInt32(36 + dataSize))
        data.append("WAVE")
        data.append("fmt ")
        data.appendUInt32(16)
        data.appendUInt16(1)
        data.appendUInt16(UInt16(channelCount))
        data.appendUInt32(UInt32(sampleRate))
        data.appendUInt32(UInt32(byteRate))
        data.appendUInt16(UInt16(blockAlign))
        data.appendUInt16(UInt16(bitsPerSample))
        data.append("data")
        data.appendUInt32(UInt32(dataSize))

        for sample in samples {
            data.appendUInt16(UInt16(bitPattern: sample))
        }
        return data
    }
}

private extension Data {
    mutating func append(_ string: String) {
        append(Data(string.utf8))
    }

    mutating func appendUInt16(_ value: UInt16) {
        var littleEndian = value.littleEndian
        append(Data(bytes: &littleEndian, count: MemoryLayout<UInt16>.size))
    }

    mutating func appendUInt32(_ value: UInt32) {
        var littleEndian = value.littleEndian
        append(Data(bytes: &littleEndian, count: MemoryLayout<UInt32>.size))
    }
}
