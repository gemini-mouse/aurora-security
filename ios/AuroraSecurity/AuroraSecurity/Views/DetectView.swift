import SwiftUI

struct DetectView: View {
    @Bindable var store: AuroraAppStore

    private var pendingPermissionCount: Int {
        var count = 0
        if !store.uiState.hasMicPermission { count += 1 }
        if !store.hasLocationPermission { count += 1 }
        if !store.hasNotificationPermission { count += 1 }
        if !store.hasMotionCapability { count += 1 }
        return count
    }

    var body: some View {
        ScrollView(showsIndicators: false) {
            VStack(spacing: 18) {
                if pendingPermissionCount > 0 {
                    PermissionStatusBanner(count: pendingPermissionCount) {
                        store.selectedTab = .settings
                    }
                }

                SectionHeader(title: "Auto detection")
                HeroCard(store: store)

                SectionHeader(title: "Emergency")
                SosActionsCard(
                    onSilent: { store.triggerSilentSos() },
                    onLoud: { store.triggerLoudSos() }
                )

                if let summary = store.latestAlertDeliverySummary {
                    SectionHeader(title: "Alert delivery")
                    AlertDeliveryResultCard(summary: summary)
                }

                MonitoringPauseCard(
                    isPaused: store.isMonitoringPaused,
                    onTogglePause: { store.toggleMonitoringPause($0) }
                )
            }
            .padding(.horizontal, 18)
            .padding(.top, 8)
            .padding(.bottom, 128)
        }
    }
}

private struct PermissionStatusBanner: View {
    var count: Int
    var action: () -> Void

    var body: some View {
        Button(action: action) {
            HStack(spacing: 14) {
                ZStack {
                    Circle()
                        .fill(Color(hex: 0xFF8C42, alpha: 0.16))
                    Image(systemName: "exclamationmark.triangle")
                        .font(.headline.weight(.bold))
                        .foregroundStyle(Color(hex: 0xFF8C42))
                }
                .frame(width: 38, height: 38)

                VStack(alignment: .leading, spacing: 3) {
                    Text("\(count) setup item\(count == 1 ? "" : "s") need attention")
                        .font(.headline.weight(.bold))
                    Text("Complete permissions and contacts in Settings.")
                        .font(.subheadline)
                }
                Spacer()
            }
            .foregroundStyle(Color(hex: 0xB5622C))
            .padding(16)
            .background(
                LinearGradient(
                    colors: [Color(hex: 0xFF8C42, alpha: 0.18), Color(hex: 0xFFB347, alpha: 0.10)],
                    startPoint: .leading,
                    endPoint: .trailing
                )
            )
            .clipShape(RoundedRectangle(cornerRadius: 22, style: .continuous))
            .overlay(
                RoundedRectangle(cornerRadius: 22, style: .continuous)
                    .stroke(Color(hex: 0xFF8C42, alpha: 0.28), lineWidth: 1)
            )
        }
        .buttonStyle(.plain)
    }
}

private struct HeroCard: View {
    @Bindable var store: AuroraAppStore
    @Environment(\.colorScheme) private var colorScheme

    var body: some View {
        ZStack {
            RoundedRectangle(cornerRadius: 34, style: .continuous)
                .fill(AuroraPalette.heroFill(colorScheme, active: store.uiState.isMonitoring && !store.isMonitoringPaused))

            VStack(spacing: 0) {
                HStack(alignment: .center) {
                    HStack(spacing: 10) {
                        StatusDot(color: statusDotColor)
                        Text(statusTitle)
                            .font(.system(size: 28, weight: .black, design: .rounded))
                            .foregroundStyle(.white)
                            .lineLimit(2)
                            .minimumScaleFactor(0.75)
                    }
                    Spacer()
                    Text(aiBadgeTitle)
                        .font(.headline.weight(.black))
                        .foregroundStyle(AuroraPalette.graphite)
                        .padding(.horizontal, 16)
                        .padding(.vertical, 10)
                        .background(Color(hex: 0xE8E2D8).opacity(0.9))
                        .clipShape(Capsule())
                }

                Spacer(minLength: 20)

                ZStack {
                    Circle()
                        .stroke(AuroraPalette.auroraTeal.opacity(0.14), lineWidth: 24)
                        .frame(width: 190, height: 190)
                    RoundedRectangle(cornerRadius: 34, style: .continuous)
                        .fill(Color.white.opacity(0.42))
                        .frame(width: 126, height: 126)
                    Image(systemName: heroSymbol)
                        .font(.system(size: 48, weight: .black))
                        .foregroundStyle(.white)
                }
                .overlay {
                    if store.uiState.countdownSecondsRemaining > 0 {
                        Text("\(store.uiState.countdownSecondsRemaining)")
                            .font(.system(size: 64, weight: .black, design: .rounded))
                            .foregroundStyle(.white)
                            .shadow(radius: 8)
                    }
                }

                Spacer(minLength: 20)

                HStack {
                    HeroMetric(value: "\(Int(store.uiState.currentDb.rounded()))", label: "dB live")
                    Rectangle()
                        .fill(Color.white.opacity(0.28))
                        .frame(width: 1, height: 60)
                    HeroMetric(value: "\(Int(store.uiState.thresholdDb.rounded()))", label: "dB threshold", trailing: true)
                }

                if store.uiState.activeEmergencyMode != nil || store.uiState.pendingEmergencyMode != nil {
                    Button("Stop alarm") {
                        store.stopAlarm()
                    }
                    .buttonStyle(AuroraButtonStyle(fill: .white.opacity(0.28), foreground: .white))
                    .padding(.top, 16)
                }
            }
            .padding(26)
        }
        .frame(minHeight: 390)
        .clipShape(RoundedRectangle(cornerRadius: 34, style: .continuous))
    }

    private var statusTitle: String {
        if !store.uiState.hasMicPermission { return "Permission required" }
        if store.isMonitoringPaused { return "Protection paused" }
        if let mode = store.uiState.pendingEmergencyMode { return "\(mode.title) pending" }
        if let mode = store.uiState.activeEmergencyMode { return "\(mode.title) active" }
        return store.uiState.isMonitoring ? "Protection active" : "Protection standby"
    }

    private var statusDotColor: Color {
        if store.uiState.activeEmergencyMode != nil || store.uiState.pendingEmergencyMode != nil { return AuroraPalette.ember }
        if store.uiState.isMonitoring && !store.isMonitoringPaused { return AuroraPalette.auroraTeal }
        return AuroraPalette.graphite.opacity(0.7)
    }

    private var aiBadgeTitle: String {
        guard store.aiSettings.isAiEnabled else { return "AI off" }
        if store.aiSettings.preloadAiInBackground, store.aiState.modelStatus == .ready {
            return "AI ready"
        }
        return store.aiSettings.sosDispatchMode == .aiFirst ? "Verified" : "Instant"
    }

    private var heroSymbol: String {
        if store.uiState.activeEmergencyMode == .loud { return "bell.and.waves.left.and.right.fill" }
        if store.uiState.activeEmergencyMode == .silent { return "speaker.slash.fill" }
        return "shield.lefthalf.filled"
    }
}

private struct HeroMetric: View {
    var value: String
    var label: String
    var trailing = false

    var body: some View {
        VStack(alignment: trailing ? .trailing : .leading, spacing: 2) {
            Text(value)
                .font(.system(size: 44, weight: .black, design: .rounded))
            Text(label)
                .font(.title3.weight(.bold))
        }
        .foregroundStyle(.white)
        .frame(maxWidth: .infinity, alignment: trailing ? .trailing : .leading)
    }
}

private struct SosActionsCard: View {
    var onSilent: () -> Void
    var onLoud: () -> Void

    var body: some View {
        HStack(spacing: 14) {
            SosActionButton(
                title: "Silent",
                subtitle: "Discreet alert",
                image: "speaker.slash.fill",
                colors: [Color(hex: 0x526B83), Color(hex: 0x3F5870)],
                action: onSilent
            )
            SosActionButton(
                title: "Loud SOS",
                subtitle: "Full alarm",
                image: "bell.and.waves.left.and.right.fill",
                colors: [AuroraPalette.ember, Color(hex: 0xD9948B)],
                action: onLoud
            )
        }
        .frame(height: 220)
    }
}

private struct SosActionButton: View {
    var title: String
    var subtitle: String
    var image: String
    var colors: [Color]
    var action: () -> Void

    var body: some View {
        Button(action: action) {
            VStack(spacing: 18) {
                Image(systemName: image)
                    .font(.system(size: 42, weight: .black))
                    .foregroundStyle(.white)
                VStack(spacing: 2) {
                    Text(title)
                        .font(.system(size: 30, weight: .black, design: .rounded))
                    Text(subtitle)
                        .font(.title3)
                }
                .foregroundStyle(.white)
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity)
            .background(LinearGradient(colors: colors, startPoint: .topLeading, endPoint: .bottomTrailing))
            .clipShape(RoundedRectangle(cornerRadius: 30, style: .continuous))
        }
        .buttonStyle(.plain)
    }
}

private struct AlertDeliveryResultCard: View {
    var summary: AlertDeliverySummary

    var body: some View {
        AuroraCard {
            HStack(spacing: 10) {
                DeliveryStep(image: "paperplane.fill", title: "SOS Alert", status: summary.sosAlertStatus)
                Connector()
                DeliveryStep(image: "mic.fill", title: "Audio Evidence", status: summary.audioEvidenceStatus)
                Connector()
                DeliveryStep(image: "sparkles", title: "AI Summary", status: summary.aiSummaryAnalysisStatus)
            }
            .padding(18)
        }
    }
}

private struct DeliveryStep: View {
    var image: String
    var title: String
    var status: TextAlertDeliveryStatus

    var body: some View {
        VStack(spacing: 7) {
            ZStack {
                Circle()
                    .fill(color.opacity(0.16))
                    .frame(width: 46, height: 46)
                Image(systemName: image)
                    .foregroundStyle(color)
            }
            Text(title)
                .font(.caption.weight(.bold))
                .multilineTextAlignment(.center)
                .lineLimit(2)
            DeliveryStatusBadge(status: status)
        }
        .frame(maxWidth: .infinity)
    }

    private var color: Color {
        switch status {
        case .sent: AuroraPalette.auroraTeal
        case .failed: AuroraPalette.ember
        case .skipped: AuroraPalette.graphite
        }
    }
}

private struct Connector: View {
    var body: some View {
        Rectangle()
            .fill(AuroraPalette.mist)
            .frame(width: 10, height: 1)
            .padding(.top, -24)
    }
}

private struct MonitoringPauseCard: View {
    var isPaused: Bool
    var onTogglePause: (Bool) -> Void

    var body: some View {
        AuroraCard {
            HStack(spacing: 14) {
                StatusDot(color: isPaused ? AuroraPalette.ember : AuroraPalette.auroraTeal)
                    .frame(width: 16, height: 16)
                VStack(alignment: .leading, spacing: 2) {
                    Text(isPaused ? "Protection paused" : "Active protection")
                        .font(.title3.weight(.black))
                        .foregroundStyle(AuroraPalette.ink)
                    Text(isPaused ? "Aurora is not monitoring right now" : "Aurora is guarding you")
                        .font(.subheadline)
                        .foregroundStyle(AuroraPalette.graphite)
                }
                Spacer()
                Button(isPaused ? "Resume" : "Pause") {
                    onTogglePause(!isPaused)
                }
                .font(.headline.weight(.black))
                .foregroundStyle(isPaused ? AuroraPalette.ocean : AuroraPalette.emberDeep)
                .padding(.horizontal, 18)
                .padding(.vertical, 12)
                .background((isPaused ? AuroraPalette.ocean : AuroraPalette.ember).opacity(0.12))
                .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
            }
            .padding(18)
        }
    }
}
