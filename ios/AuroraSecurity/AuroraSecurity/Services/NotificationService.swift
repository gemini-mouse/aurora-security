import Foundation
import UserNotifications

final class NotificationService {
    func authorizationStatus() async -> PermissionAvailability {
        let settings = await UNUserNotificationCenter.current().notificationSettings()
        switch settings.authorizationStatus {
        case .authorized, .provisional, .ephemeral:
            return .granted
        case .denied:
            return .denied
        case .notDetermined:
            return .notDetermined
        @unknown default:
            return .notDetermined
        }
    }

    func requestAuthorization() async -> Bool {
        (try? await UNUserNotificationCenter.current().requestAuthorization(options: [.alert, .badge, .sound])) ?? false
    }

    func postLocalEmergencyNotification(status: String) async {
        let content = UNMutableNotificationContent()
        content.title = "Aurora SOS"
        content.body = status
        content.sound = .default

        let request = UNNotificationRequest(
            identifier: "aurora-sos-\(UUID().uuidString)",
            content: content,
            trigger: nil
        )
        try? await UNUserNotificationCenter.current().add(request)
    }
}
