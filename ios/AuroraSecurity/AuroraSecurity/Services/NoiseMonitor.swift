import AVFoundation
import Foundation

final class NoiseMonitor {
    private let engine = AVAudioEngine()
    private let audioQueue = DispatchQueue(label: "app.aurorasecurity.noise-monitor")
    private var isRunning = false
    private var lastDbUpdate = Date.distantPast
    private var peakDb: Double = 0

    var onLevelChanged: ((Double) -> Void)?
    var onPcmBuffer: ((AVAudioPCMBuffer) -> Void)?

    var hasMicrophonePermission: Bool {
        AVAudioSession.sharedInstance().recordPermission == .granted
    }

    func requestMicrophonePermission() async -> Bool {
        await withCheckedContinuation { continuation in
            AVAudioSession.sharedInstance().requestRecordPermission { granted in
                continuation.resume(returning: granted)
            }
        }
    }

    func start() throws {
        guard !isRunning else { return }
        guard hasMicrophonePermission else {
            throw NoiseMonitorError.microphonePermissionDenied
        }

        let session = AVAudioSession.sharedInstance()
        try session.setCategory(.playAndRecord, mode: .measurement, options: [.mixWithOthers, .defaultToSpeaker, .allowBluetooth])
        try session.setActive(true)

        let input = engine.inputNode
        let format = input.outputFormat(forBus: 0)
        input.removeTap(onBus: 0)
        input.installTap(onBus: 0, bufferSize: 1024, format: format) { [weak self] buffer, _ in
            self?.handle(buffer: buffer)
        }

        engine.prepare()
        try engine.start()
        isRunning = true
    }

    func stop() {
        guard isRunning else { return }
        engine.inputNode.removeTap(onBus: 0)
        engine.stop()
        try? AVAudioSession.sharedInstance().setActive(false, options: .notifyOthersOnDeactivation)
        isRunning = false
        peakDb = 0
        lastDbUpdate = .distantPast
    }

    private func handle(buffer: AVAudioPCMBuffer) {
        audioQueue.async { [weak self] in
            guard let self else { return }
            let db = self.estimatedDb(from: buffer)
            self.peakDb = max(self.peakDb, db)
            self.onPcmBuffer?(buffer)

            let now = Date()
            guard now.timeIntervalSince(self.lastDbUpdate) >= 1 else { return }
            let displayDb = self.peakDb
            self.peakDb = 0
            self.lastDbUpdate = now
            DispatchQueue.main.async {
                self.onLevelChanged?(displayDb)
            }
        }
    }

    private func estimatedDb(from buffer: AVAudioPCMBuffer) -> Double {
        guard let channelData = buffer.floatChannelData else { return 0 }
        let frameLength = Int(buffer.frameLength)
        guard frameLength > 0 else { return 0 }

        var sum: Float = 0
        let channelCount = Int(buffer.format.channelCount)
        for channel in 0..<max(channelCount, 1) {
            let samples = channelData[channel]
            for index in 0..<frameLength {
                let sample = samples[index]
                sum += sample * sample
            }
        }

        let meanSquare = Double(sum) / Double(frameLength * max(channelCount, 1))
        let rms = sqrt(max(meanSquare, 0.000_000_01))
        let calibrated = 20 * log10(rms) + 90
        return min(max(calibrated, 0), 120)
    }
}

enum NoiseMonitorError: LocalizedError {
    case microphonePermissionDenied

    var errorDescription: String? {
        "Microphone permission is required before monitoring can start."
    }
}
