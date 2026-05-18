import SwiftUI

struct OnboardingView: View {
    @EnvironmentObject private var appState: AppState
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 20) {
                    VStack(alignment: .leading, spacing: 10) {
                        Image(systemName: "shield.lefthalf.filled")
                            .font(.system(size: 54))
                            .foregroundStyle(MorandiPalette.sage)
                        Text("Aurora Security")
                            .font(.largeTitle.weight(.bold))
                        Text("A personal safety assistant for sound-triggered SOS, trusted contact alerts, and emergency context.")
                            .font(.body)
                            .foregroundStyle(MorandiPalette.mutedText)
                    }
                    .auroraPanel()

                    VStack(alignment: .leading, spacing: 12) {
                        NoticeRow(icon: "mic", title: "Microphone", detail: "Used to estimate sound level and capture a short emergency clip.")
                        NoticeRow(icon: "location", title: "Location", detail: "Included in emergency alerts when permission is granted.")
                        NoticeRow(icon: "bell", title: "Notifications", detail: "Shows local emergency status on this iPhone.")
                        NoticeRow(icon: "phone", title: "Calls and SMS", detail: "iOS requires user confirmation. Aurora opens the system screen as a fallback.")
                    }
                    .auroraPanel()

                    Text("Aurora is not an emergency dispatch service and cannot guarantee rescue, message delivery, AI accuracy, or background execution. In immediate danger, contact official emergency services.")
                        .font(.footnote)
                        .foregroundStyle(MorandiPalette.mutedText)
                        .auroraPanel()
                }
                .padding(20)
            }
            .background(MorandiPalette.appBackground)
            .navigationTitle("Safety notice")
            .toolbar {
                ToolbarItem(placement: .bottomBar) {
                    Button {
                        appState.acceptSafetyNotice()
                        dismiss()
                    } label: {
                        Text("Agree and continue")
                            .frame(maxWidth: .infinity)
                    }
                    .buttonStyle(AuroraProminentButtonStyle(color: MorandiPalette.sage))
                }
            }
        }
    }
}

private struct NoticeRow: View {
    var icon: String
    var title: String
    var detail: String

    var body: some View {
        HStack(alignment: .top, spacing: 12) {
            Image(systemName: icon)
                .foregroundStyle(MorandiPalette.sage)
                .frame(width: 26)
            VStack(alignment: .leading, spacing: 3) {
                Text(title)
                    .font(.subheadline.weight(.semibold))
                Text(detail)
                    .font(.footnote)
                    .foregroundStyle(MorandiPalette.mutedText)
            }
        }
    }
}
