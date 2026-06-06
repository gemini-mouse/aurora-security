import CoreMotion
import Foundation

final class MotionMonitor {
    private let motionManager = CMMotionManager()
    private let queue = OperationQueue()
    private var lastShakeDate = Date.distantPast
    private var peakG: Double = 1
    private var lastUiUpdate = Date.distantPast

    var onMotionUpdate: ((Double, Bool) -> Void)?

    var isAvailable: Bool {
        motionManager.isAccelerometerAvailable
    }

    func start(thresholdG: Double) {
        guard motionManager.isAccelerometerAvailable, !motionManager.isAccelerometerActive else { return }
        queue.name = "app.aurorasecurity.motion-monitor"
        motionManager.accelerometerUpdateInterval = 0.15
        motionManager.startAccelerometerUpdates(to: queue) { [weak self] data, _ in
            guard let self, let acceleration = data?.acceleration else { return }
            let gForce = sqrt(
                acceleration.x * acceleration.x +
                acceleration.y * acceleration.y +
                acceleration.z * acceleration.z
            )
            if gForce >= thresholdG {
                self.lastShakeDate = Date()
            }
            let isShaking = Date().timeIntervalSince(self.lastShakeDate) < 2
            self.peakG = max(self.peakG, gForce)
            guard Date().timeIntervalSince(self.lastUiUpdate) >= 1 else { return }
            let displayG = self.peakG
            self.peakG = 1
            self.lastUiUpdate = Date()
            DispatchQueue.main.async {
                self.onMotionUpdate?(displayG, isShaking)
            }
        }
    }

    func stop() {
        motionManager.stopAccelerometerUpdates()
        lastShakeDate = .distantPast
        peakG = 1
        lastUiUpdate = .distantPast
    }
}
