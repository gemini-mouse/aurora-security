import SwiftUI

struct AppShellView: View {
    @EnvironmentObject private var appState: AppState
    @State private var selectedTab: AppTab = .detect

    var body: some View {
        TabView(selection: $selectedTab) {
            NavigationStack {
                DetectView()
            }
            .tabItem { Label("Detect", systemImage: "shield.lefthalf.filled") }
            .tag(AppTab.detect)

            NavigationStack {
                AIView()
            }
            .tabItem { Label("AI", systemImage: "sparkles") }
            .tag(AppTab.ai)

            NavigationStack {
                HistoryView()
            }
            .tabItem { Label("History", systemImage: "clock") }
            .tag(AppTab.history)

            NavigationStack {
                SettingsView()
            }
            .tabItem { Label("Settings", systemImage: "gearshape") }
            .tag(AppTab.settings)
        }
        .preferredColorScheme(appState.themeMode.colorScheme)
        .background(MorandiPalette.appBackground.ignoresSafeArea())
        .sheet(isPresented: $appState.shouldShowOnboarding) {
            OnboardingView()
                .interactiveDismissDisabled()
        }
        .task {
            appState.bootstrap()
        }
    }
}

private enum AppTab {
    case detect
    case ai
    case history
    case settings
}
