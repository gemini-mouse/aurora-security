import AVFoundation
import AudioToolbox
import Foundation

final class LocalAlarmPlayer {
    private var player: AVAudioPlayer?
    private var vibrationTask: Task<Void, Never>?

    func play(sound: LoudSosSound) {
        if player?.isPlaying == true { return }
        let session = AVAudioSession.sharedInstance()
        try? session.setCategory(.playback, mode: .default, options: [.duckOthers])
        try? session.setActive(true)

        if sound == .siren, let url = Bundle.main.url(forResource: "loud_sos_alarm", withExtension: "m4a") {
            player = try? AVAudioPlayer(contentsOf: url)
            player?.numberOfLoops = -1
            player?.volume = 1
            player?.prepareToPlay()
            player?.play()
        } else {
            AudioServicesPlaySystemSound(1005)
        }
        startHeartbeatVibration()
    }

    func stop() {
        player?.stop()
        player = nil
        vibrationTask?.cancel()
        vibrationTask = nil
        try? AVAudioSession.sharedInstance().setActive(false, options: .notifyOthersOnDeactivation)
    }

    private func startHeartbeatVibration() {
        vibrationTask?.cancel()
        vibrationTask = Task {
            while !Task.isCancelled {
                AudioServicesPlaySystemSound(kSystemSoundID_Vibrate)
                try? await Task.sleep(nanoseconds: 1_200_000_000)
            }
        }
    }
}
