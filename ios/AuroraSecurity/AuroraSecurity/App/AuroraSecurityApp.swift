import SwiftUI
import UIKit
import UserNotifications

extension Notification.Name {
    static let auroraApnsTokenUpdated = Notification.Name("auroraApnsTokenUpdated")
    static let auroraRemoteNotificationReceived = Notification.Name("auroraRemoteNotificationReceived")
}

@main
@MainActor
struct AuroraSecurityApp: App {
    @UIApplicationDelegateAdaptor(AuroraAppDelegate.self) private var appDelegate
    @State private var store = AuroraAppStore()

    var body: some Scene {
        WindowGroup {
            AuroraRootView(store: store)
                .preferredColorScheme(store.themeMode.colorScheme)
                .task {
                    await store.start()
                }
                .onOpenURL { url in
                    store.handleIncomingURL(url)
                }
                .onContinueUserActivity(NSUserActivityTypeBrowsingWeb) { activity in
                    if let url = activity.webpageURL {
                        store.handleIncomingURL(url)
                    }
                }
        }
    }
}

final class AuroraAppDelegate: NSObject, UIApplicationDelegate, UNUserNotificationCenterDelegate {
    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil
    ) -> Bool {
        UNUserNotificationCenter.current().delegate = self
        return true
    }

    func application(_ application: UIApplication, didRegisterForRemoteNotificationsWithDeviceToken deviceToken: Data) {
        let token = deviceToken.map { String(format: "%02x", $0) }.joined()
        PreferencesStore.shared.saveAPNsDeviceToken(token)
        NotificationCenter.default.post(name: .auroraApnsTokenUpdated, object: token)
    }

    func application(_ application: UIApplication, didFailToRegisterForRemoteNotificationsWithError error: Error) {
        PreferencesStore.shared.saveAPNsDeviceToken("")
    }

    func application(
        _ application: UIApplication,
        didReceiveRemoteNotification userInfo: [AnyHashable: Any],
        fetchCompletionHandler completionHandler: @escaping (UIBackgroundFetchResult) -> Void
    ) {
        NotificationCenter.default.post(name: .auroraRemoteNotificationReceived, object: userInfo)
        completionHandler(.newData)
    }

    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        willPresent notification: UNNotification
    ) async -> UNNotificationPresentationOptions {
        [.banner, .sound, .list]
    }
}
