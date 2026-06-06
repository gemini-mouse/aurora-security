import SwiftUI

struct AuroraRootView: View {
    @Environment(\.colorScheme) private var colorScheme
    @Bindable var store: AuroraAppStore

    var body: some View {
        ZStack {
            AuroraPalette.background(colorScheme)
                .ignoresSafeArea()

            VStack(spacing: 0) {
                AuroraHeader {
                    store.isHistoryPresented = true
                }

                TabView(selection: $store.selectedTab) {
                    DetectView(store: store)
                        .tag(AppTab.detect)
                    AiView(store: store)
                        .tag(AppTab.ai)
                    SettingsView(store: store)
                        .tag(AppTab.settings)
                }
                .tabViewStyle(.page(indexDisplayMode: .never))
            }
        }
        .safeAreaInset(edge: .bottom) {
            AuroraBottomTabBar(selectedTab: $store.selectedTab)
                .padding(.bottom, 8)
        }
        .sheet(isPresented: $store.isHistoryPresented) {
            HistoryView(store: store)
                .presentationDetents([.large])
                .presentationDragIndicator(.visible)
        }
    }
}

#Preview {
    AuroraRootView(store: AuroraAppStore())
}
