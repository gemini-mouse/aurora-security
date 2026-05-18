import CoreLocation
import Foundation

final class LocationSnapshotProvider: NSObject, CLLocationManagerDelegate {
    private let manager = CLLocationManager()
    private let lock = NSLock()
    private var continuation: CheckedContinuation<AlertLocation?, Never>?

    override init() {
        super.init()
        manager.delegate = self
        manager.desiredAccuracy = kCLLocationAccuracyBest
    }

    var authorizationStatus: PermissionAvailability {
        switch manager.authorizationStatus {
        case .authorizedAlways, .authorizedWhenInUse: .granted
        case .denied, .restricted: .denied
        case .notDetermined: .notDetermined
        @unknown default: .notDetermined
        }
    }

    func requestAuthorization() {
        manager.requestWhenInUseAuthorization()
    }

    func currentLocation() async -> AlertLocation? {
        guard authorizationStatus.isReady else { return nil }
        return await withCheckedContinuation { continuation in
            lock.lock()
            self.continuation?.resume(returning: nil)
            self.continuation = continuation
            lock.unlock()
            manager.requestLocation()
            Task { [weak self] in
                try? await Task.sleep(nanoseconds: 10_000_000_000)
                self?.complete(nil)
            }
        }
    }

    func locationManager(_ manager: CLLocationManager, didUpdateLocations locations: [CLLocation]) {
        guard let location = locations.last else {
            complete(nil)
            return
        }
        complete(
            AlertLocation(latitude: location.coordinate.latitude, longitude: location.coordinate.longitude)
        )
    }

    func locationManager(_ manager: CLLocationManager, didFailWithError error: Error) {
        complete(nil)
    }

    private func complete(_ location: AlertLocation?) {
        lock.lock()
        let activeContinuation = continuation
        continuation = nil
        lock.unlock()
        activeContinuation?.resume(returning: location)
    }
}
