import AVFoundation
import Foundation

final class AudioMonitor {
    static let sampleRate: Double = 16_000

    private let engine = AVAudioEngine()
    private let audioSession = AVAudioSession.sharedInstance()
    private var isRunning = false

    var authorizationStatus: PermissionAvailability {
        switch audioSession.recordPermission {
        case .granted: .granted
        case .denied: .denied
        case .undetermined: .notDetermined
        @unknown default: .notDetermined
        }
    }

    func requestPermission() async -> Bool {
        await withCheckedContinuation { continuation in
            audioSession.requestRecordPermission { granted in
                continuation.resume(returning: granted)
            }
        }
    }

    func ensurePermission() async -> Bool {
        switch authorizationStatus {
        case .granted, .limited:
            return true
        case .notDetermined:
            return await requestPermission()
        case .denied, .unsupported:
            return false
        }
    }

    func start(
        onLevel: @escaping (Double) -> Void,
        onSamples: @escaping ([Float]) -> Void
    ) -> Bool {
        guard !isRunning else { return true }
        isRunning = true

        do {
            try audioSession.setCategory(.playAndRecord, mode: .measurement, options: [.allowBluetooth, .defaultToSpeaker])
            try audioSession.setPreferredSampleRate(Self.sampleRate)
            try audioSession.setActive(true)
        } catch {
            isRunning = false
            return false
        }

        let input = engine.inputNode
        let format = input.outputFormat(forBus: 0)
        input.removeTap(onBus: 0)
        input.installTap(onBus: 0, bufferSize: 512, format: format) { buffer, _ in
            guard let channel = buffer.floatChannelData?[0] else { return }
            let count = Int(buffer.frameLength)
            guard count > 0 else { return }

            var samples = [Float]()
            samples.reserveCapacity(count)
            var sumSquares: Double = 0
            for index in 0..<count {
                let sample = channel[index]
                samples.append(sample)
                sumSquares += Double(sample * sample)
            }

            let rms = sqrt(sumSquares / Double(count)).clamped(to: 0.000_001...1)
            let db = (20 * log10(rms) + 90).clamped(to: 0...120)
            onSamples(samples)
            onLevel(db)
        }

        do {
            engine.prepare()
            try engine.start()
            return true
        } catch {
            input.removeTap(onBus: 0)
            try? audioSession.setActive(false)
            isRunning = false
            return false
        }
    }

    func stop() {
        guard isRunning else { return }
        engine.inputNode.removeTap(onBus: 0)
        engine.stop()
        try? audioSession.setActive(false)
        isRunning = false
    }
}

private extension Comparable {
    func clamped(to range: ClosedRange<Self>) -> Self {
        min(max(self, range.lowerBound), range.upperBound)
    }
}
