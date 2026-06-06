import AVFoundation
import Foundation

final class CrisisAudioRecorder {
    private struct CaptureSession {
        var id: Int64
        var source: EmergencyTriggerSource
        var mode: EmergencyMode
        var samples: [Float]
        var sampleRate: Double
        var startedAt: Date
    }

    private let lock = NSLock()
    private let outputDirectory: URL
    private var preRollSamples: [Float] = []
    private var sampleRate: Double = 44_100
    private var currentSession: CaptureSession?
    private var completionTask: Task<Void, Never>?
    private var nextSessionId: Int64 = 1

    var onClipReady: ((CrisisAudioClip) -> Void)?

    init() {
        let caches = FileManager.default.urls(for: .cachesDirectory, in: .userDomainMask).first!
        outputDirectory = caches.appendingPathComponent("aurora-crisis-audio", isDirectory: true)
        try? FileManager.default.createDirectory(at: outputDirectory, withIntermediateDirectories: true)
    }

    func append(buffer: AVAudioPCMBuffer) {
        guard let floats = buffer.floatChannelData else { return }
        let frames = Int(buffer.frameLength)
        guard frames > 0 else { return }
        let rate = buffer.format.sampleRate
        let channelCount = Int(buffer.format.channelCount)
        var monoSamples = [Float]()
        monoSamples.reserveCapacity(frames)

        for frame in 0..<frames {
            var value: Float = 0
            for channel in 0..<max(channelCount, 1) {
                value += floats[channel][frame]
            }
            monoSamples.append(value / Float(max(channelCount, 1)))
        }

        lock.lock()
        sampleRate = rate
        preRollSamples.append(contentsOf: monoSamples)
        trimPreRollLocked()
        if currentSession != nil {
            currentSession?.samples.append(contentsOf: monoSamples)
        }
        lock.unlock()
    }

    @discardableResult
    func startCapture(source: EmergencyTriggerSource, mode: EmergencyMode) -> Int64 {
        lock.lock()
        let id = nextSessionId
        nextSessionId += 1
        currentSession = CaptureSession(
            id: id,
            source: source,
            mode: mode,
            samples: preRollSamples,
            sampleRate: sampleRate,
            startedAt: Date()
        )
        lock.unlock()

        completionTask?.cancel()
        completionTask = Task { [weak self] in
            try? await Task.sleep(nanoseconds: 3_000_000_000)
            self?.finishCapture(id: id)
        }
        return id
    }

    func cancelCapture(_ id: Int64?) {
        completionTask?.cancel()
        completionTask = nil
        lock.lock()
        if id == nil || currentSession?.id == id {
            currentSession = nil
        }
        lock.unlock()
    }

    private func finishCapture(id: Int64) {
        lock.lock()
        guard let session = currentSession, session.id == id else {
            lock.unlock()
            return
        }
        currentSession = nil
        lock.unlock()

        let limitedSamples = limitedFiveSecondWindow(samples: session.samples, sampleRate: session.sampleRate)
        let data = makeWavData(samples: limitedSamples, sampleRate: session.sampleRate)
        let url = outputDirectory.appendingPathComponent("aurora-crisis-\(id).wav")
        try? data.write(to: url, options: [.atomic])
        let durationMs = Int((Double(limitedSamples.count) / max(session.sampleRate, 1)) * 1000)
        let clip = CrisisAudioClip(
            id: id,
            triggerSource: session.source,
            mode: session.mode,
            fileURL: url,
            wavData: data,
            capturedAt: session.startedAt,
            sampleRate: session.sampleRate,
            durationMs: durationMs
        )
        DispatchQueue.main.async { [weak self] in
            self?.onClipReady?(clip)
        }
    }

    private func trimPreRollLocked() {
        let limit = max(Int(sampleRate * 2), 1)
        if preRollSamples.count > limit {
            preRollSamples.removeFirst(preRollSamples.count - limit)
        }
    }

    private func limitedFiveSecondWindow(samples: [Float], sampleRate: Double) -> [Float] {
        let limit = max(Int(sampleRate * 5), 1)
        guard samples.count > limit else { return samples }
        return Array(samples.suffix(limit))
    }

    private func makeWavData(samples: [Float], sampleRate: Double) -> Data {
        var data = Data()
        let clampedSamples = samples.map { Int16(max(-1, min(1, $0)) * Float(Int16.max)) }
        let subchunk2Size = UInt32(clampedSamples.count * MemoryLayout<Int16>.size)
        let chunkSize = UInt32(36) + subchunk2Size
        let byteRate = UInt32(sampleRate) * 2
        let blockAlign = UInt16(2)

        data.appendASCII("RIFF")
        data.appendLittleEndian(chunkSize)
        data.appendASCII("WAVE")
        data.appendASCII("fmt ")
        data.appendLittleEndian(UInt32(16))
        data.appendLittleEndian(UInt16(1))
        data.appendLittleEndian(UInt16(1))
        data.appendLittleEndian(UInt32(sampleRate))
        data.appendLittleEndian(byteRate)
        data.appendLittleEndian(blockAlign)
        data.appendLittleEndian(UInt16(16))
        data.appendASCII("data")
        data.appendLittleEndian(subchunk2Size)
        for sample in clampedSamples {
            data.appendLittleEndian(sample)
        }
        return data
    }
}

private extension Data {
    mutating func appendASCII(_ string: String) {
        append(contentsOf: string.utf8)
    }

    mutating func appendLittleEndian<T: FixedWidthInteger>(_ value: T) {
        var littleEndian = value.littleEndian
        withUnsafeBytes(of: &littleEndian) { bytes in
            append(contentsOf: bytes)
        }
    }
}
