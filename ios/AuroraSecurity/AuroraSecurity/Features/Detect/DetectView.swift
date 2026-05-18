import SwiftUI

struct DetectView: View {
    @EnvironmentObject private var appState: AppState

    var body: some View {
        ScrollView {
            VStack(spacing: 18) {
                statusHeader
                decibelPanel
                emergencyPanel
                permissionPanel
                fallbackPanel
            }
            .padding(20)
        }
        .background(MorandiPalette.appBackground.ignoresSafeArea())
        .navigationTitle("Detect")
        .toolbar {
            ToolbarItem(placement: .topBarTrailing) {
                Button {
                    appState.alarmState.isMonitoring ? appState.stopMonitoring() : appState.startMonitoring()
                } label: {
                    Image(systemName: appState.alarmState.isMonitoring ? "pause.fill" : "play.fill")
                }
                .accessibilityLabel(appState.alarmState.isMonitoring ? "Pause protection" : "Start protection")
            }
        }
    }

    private var statusHeader: some View {
        HStack(alignment: .center, spacing: 14) {
            Image(systemName: appState.alarmState.isMonitoring ? "shield.checkered" : "shield")
                .font(.system(size: 32, weight: .semibold))
                .foregroundStyle(appState.alarmState.isMonitoring ? MorandiPalette.sage : MorandiPalette.mutedText)
                .frame(width: 48, height: 48)
                .background(MorandiPalette.elevatedSurface)
                .clipShape(Circle())

            VStack(alignment: .leading, spacing: 4) {
                Text(appState.alarmState.isMonitoring ? "Active protection" : "Protection paused")
                    .font(.headline)
                    .foregroundStyle(MorandiPalette.graphite)
                Text(appState.lastStatusMessage)
                    .font(.subheadline)
                    .foregroundStyle(MorandiPalette.mutedText)
            }
            Spacer()
        }
        .auroraPanel()
    }

    private var decibelPanel: some View {
        VStack(spacing: 16) {
            ZStack {
                Circle()
                    .stroke(MorandiPalette.elevatedSurface, lineWidth: 18)
                Circle()
                    .trim(from: 0, to: min(appState.alarmState.currentDb / 120, 1))
                    .stroke(
                        appState.alarmState.isThresholdExceeded ? MorandiPalette.danger : MorandiPalette.mistBlue,
                        style: StrokeStyle(lineWidth: 18, lineCap: .round)
                    )
                    .rotationEffect(.degrees(-90))
                    .animation(.easeInOut(duration: 0.25), value: appState.alarmState.currentDb)

                VStack(spacing: 6) {
                    Text("\(Int(appState.alarmState.currentDb.rounded()))")
                        .font(.system(size: 58, weight: .bold, design: .rounded))
                        .foregroundStyle(MorandiPalette.graphite)
                    Text("dB")
                        .font(.headline)
                        .foregroundStyle(MorandiPalette.mutedText)
                }
            }
            .frame(width: 220, height: 220)

            VStack(alignment: .leading, spacing: 8) {
                HStack {
                    Text("Trigger threshold")
                    Spacer()
                    Text("\(Int(appState.alarmState.thresholdDb.rounded())) dB")
                        .fontWeight(.semibold)
                }
                .font(.subheadline)
                .foregroundStyle(MorandiPalette.graphite)

                Slider(
                    value: Binding(
                        get: { appState.alarmState.thresholdDb },
                        set: { appState.updateThreshold($0) }
                    ),
                    in: 60...115,
                    step: 1
                )
            }
        }
        .auroraPanel()
    }

    private var emergencyPanel: some View {
        VStack(alignment: .leading, spacing: 14) {
            HStack {
                VStack(alignment: .leading, spacing: 4) {
                    Text("SOS")
                        .font(.title3.weight(.semibold))
                    Text("A 3-second cancellation window runs before alerts are sent.")
                        .font(.footnote)
                        .foregroundStyle(MorandiPalette.mutedText)
                }
                Spacer()
                if appState.alarmState.countdownSecondsRemaining > 0 {
                    Text("\(appState.alarmState.countdownSecondsRemaining)")
                        .font(.title.weight(.bold))
                        .foregroundStyle(MorandiPalette.danger)
                }
            }

            HStack(spacing: 12) {
                Button {
                    appState.triggerSilentSOS()
                } label: {
                    Label("Silent", systemImage: "bell.slash")
                        .frame(maxWidth: .infinity)
                }
                .buttonStyle(AuroraProminentButtonStyle(color: MorandiPalette.mauve))

                Button {
                    appState.triggerLoudSOS()
                } label: {
                    Label("Loud", systemImage: "speaker.wave.3")
                        .frame(maxWidth: .infinity)
                }
                .buttonStyle(AuroraProminentButtonStyle(color: MorandiPalette.danger))
            }

            if appState.alarmState.pendingEmergencyMode != nil || appState.alarmState.activeEmergencyMode != nil {
                Button(role: .cancel) {
                    appState.cancelEmergency()
                } label: {
                    Label("Cancel emergency", systemImage: "xmark.circle")
                        .frame(maxWidth: .infinity)
                }
                .buttonStyle(.bordered)
            }
        }
        .auroraPanel()
    }

    private var permissionPanel: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("Readiness")
                .font(.headline)

            PermissionRow(
                title: "Microphone",
                detail: "Required for live sound detection",
                status: appState.permissionState.microphone,
                action: appState.requestMicrophone
            )
            PermissionRow(
                title: "Location",
                detail: "Adds a map link to emergency alerts",
                status: appState.permissionState.location,
                action: appState.requestLocation
            )
            PermissionRow(
                title: "Notifications",
                detail: "Shows emergency status on iPhone",
                status: appState.permissionState.notifications,
                action: appState.requestNotifications
            )
        }
        .auroraPanel()
    }

    private var fallbackPanel: some View {
        VStack(alignment: .leading, spacing: 10) {
            Text("iOS fallback")
                .font(.headline)
            Text("iPhone requires confirmation before phone calls or SMS. Aurora opens the phone or message screen as a backup while Telegram and Push use the backend path.")
                .font(.footnote)
                .foregroundStyle(MorandiPalette.mutedText)

            HStack {
                Button {
                    appState.openEmergencyPhoneFallback()
                } label: {
                    Label("Phone", systemImage: "phone")
                }
                .buttonStyle(.bordered)

                Button {
                    appState.openEmergencyMessageFallback()
                } label: {
                    Label("Message", systemImage: "message")
                }
                .buttonStyle(.bordered)
            }
        }
        .auroraPanel()
    }
}

private struct PermissionRow: View {
    var title: String
    var detail: String
    var status: PermissionAvailability
    var action: () -> Void

    var body: some View {
        HStack(spacing: 12) {
            Image(systemName: status.isReady ? "checkmark.circle.fill" : "exclamationmark.circle")
                .foregroundStyle(status.isReady ? MorandiPalette.sage : MorandiPalette.clay)
            VStack(alignment: .leading, spacing: 2) {
                Text(title)
                    .font(.subheadline.weight(.semibold))
                Text(detail)
                    .font(.caption)
                    .foregroundStyle(MorandiPalette.mutedText)
            }
            Spacer()
            if !status.isReady {
                Button("Allow", action: action)
                    .buttonStyle(.bordered)
            }
        }
    }
}

struct AuroraProminentButtonStyle: ButtonStyle {
    var color: Color

    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .font(.headline)
            .foregroundStyle(.white)
            .padding(.vertical, 14)
            .background(color.opacity(configuration.isPressed ? 0.75 : 1))
            .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
    }
}
