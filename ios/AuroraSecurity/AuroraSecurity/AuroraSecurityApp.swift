import SwiftUI

@main
struct AuroraSecurityApp: App {
    @StateObject private var appState = AppState()

    var body: some Scene {
        WindowGroup {
            AppShellView()
                .environmentObject(appState)
                .tint(MorandiPalette.sage)
        }
    }
}
