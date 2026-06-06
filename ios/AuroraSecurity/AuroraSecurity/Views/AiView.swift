import SwiftUI

struct AiView: View {
    @Bindable var store: AuroraAppStore

    var body: some View {
        ScrollView(showsIndicators: false) {
            VStack(spacing: 16) {
                SectionHeader(title: "AI detection")
                AiModelStatusCard(store: store)

                SectionHeader(title: "SOS flow")
                SosProcessCard(store: store)

                SectionHeader(title: "Options")
                AiOptionsCard(store: store)

                SectionHeader(title: "Test & improve")
                AiAudioTestCard(store: store)
            }
            .padding(.horizontal, 20)
            .padding(.top, 8)
            .padding(.bottom, 128)
        }
    }
}

private struct SosProcessCard: View {
    @Bindable var store: AuroraAppStore

    var body: some View {
        AuroraCard {
            VStack(alignment: .leading, spacing: 22) {
                VStack(alignment: .leading, spacing: 6) {
                    Text("SOS Process")
                        .font(.system(size: 34, weight: .black, design: .rounded))
                        .foregroundStyle(AuroraPalette.ink)
                    Text("When an emergency is triggered, Aurora follows these steps:")
                        .font(.title2)
                        .foregroundStyle(AuroraPalette.graphite)
                        .fixedSize(horizontal: false, vertical: true)
                }

                Picker("SOS dispatch mode", selection: dispatchModeBinding) {
                    ForEach(SosDispatchMode.allCases) { mode in
                        VStack {
                            Text(mode.title)
                            Text(mode.subtitle)
                        }
                        .tag(mode)
                    }
                }
                .pickerStyle(.segmented)

                VStack(spacing: 22) {
                    ProcessStep(
                        number: 1,
                        title: "AI Smart Analysis",
                        bodyText: "Uses on-device AI to identify sounds such as shouting, crashes, and distress cues.",
                        enabled: store.aiSettings.isAiEnabled
                    )
                    ProcessStep(
                        number: 2,
                        title: "SOS Alert",
                        bodyText: store.aiSettings.sosDispatchMode == .aiFirst
                            ? "Sends your location and emergency message after AI returns Danger or High. If AI fails, Aurora still sends the alert."
                            : "Sends your location and emergency message immediately after the countdown.",
                        enabled: true
                    )
                    ProcessStep(
                        number: 3,
                        title: "Audio Evidence",
                        bodyText: "Optionally sends a bounded 5-second recording of the scene to enabled backend channels.",
                        enabled: store.aiSettings.sendAudioToContacts
                    )
                }
            }
            .padding(24)
        }
    }

    private var dispatchModeBinding: Binding<SosDispatchMode> {
        Binding(
            get: { store.aiSettings.sosDispatchMode },
            set: { mode in
                var settings = store.aiSettings
                settings.sosDispatchMode = mode
                store.updateAiSettings(settings)
            }
        )
    }
}

private struct ProcessStep: View {
    var number: Int
    var title: String
    var bodyText: String
    var enabled: Bool

    var body: some View {
        HStack(alignment: .top, spacing: 16) {
            VStack(spacing: 0) {
                ZStack {
                    Circle()
                        .fill(AuroraPalette.ocean)
                    Text("\(number)")
                        .font(.title2.weight(.black))
                        .foregroundStyle(.white)
                }
                .frame(width: 48, height: 48)
                Rectangle()
                    .fill(AuroraPalette.mist)
                    .frame(width: 2, height: 56)
                    .opacity(number == 3 ? 0 : 1)
            }

            VStack(alignment: .leading, spacing: 8) {
                HStack {
                    Text(title)
                        .font(.title2.weight(.black))
                        .foregroundStyle(AuroraPalette.ink)
                    Spacer()
                    Image(systemName: enabled ? "checkmark.square.fill" : "minus.square.fill")
                        .font(.title2)
                        .foregroundStyle(enabled ? AuroraPalette.ocean : AuroraPalette.graphite.opacity(0.55))
                }
                Text(bodyText)
                    .font(.body)
                    .lineSpacing(4)
                    .foregroundStyle(AuroraPalette.graphite)
            }
        }
    }
}

private struct AiModelStatusCard: View {
    @Bindable var store: AuroraAppStore

    var body: some View {
        AuroraCard {
            VStack(alignment: .leading, spacing: 16) {
                HStack(spacing: 14) {
                    ZStack {
                        Circle()
                            .fill(AuroraPalette.auroraBlue.opacity(0.16))
                        Image(systemName: "sparkles")
                            .font(.title2.weight(.bold))
                            .foregroundStyle(AuroraPalette.ocean)
                    }
                    .frame(width: 52, height: 52)

                    VStack(alignment: .leading, spacing: 2) {
                        Text("AI Engine")
                            .font(.title2.weight(.black))
                        Text(store.aiState.modelStatus.label)
                            .font(.headline)
                            .foregroundStyle(AuroraPalette.ocean)
                    }
                    Spacer()
                    Toggle("", isOn: aiEnabledBinding)
                        .labelsHidden()
                }

                Text(store.aiState.statusMessage)
                    .font(.body)
                    .foregroundStyle(AuroraPalette.graphite)
                    .fixedSize(horizontal: false, vertical: true)

                HStack {
                    Label(store.aiState.currentModelType.rawValue, systemImage: "cpu")
                    Spacer()
                    Label(store.aiState.accelerator, systemImage: "speedometer")
                }
                .font(.caption.weight(.bold))
                .foregroundStyle(AuroraPalette.graphite)
            }
            .padding(22)
        }
    }

    private var aiEnabledBinding: Binding<Bool> {
        Binding(
            get: { store.aiSettings.isAiEnabled },
            set: { enabled in
                var settings = store.aiSettings
                settings.isAiEnabled = enabled
                if !enabled {
                    settings.sosDispatchMode = .immediate
                    settings.preloadAiInBackground = false
                }
                store.updateAiSettings(settings)
            }
        )
    }
}

private struct AiOptionsCard: View {
    @Bindable var store: AuroraAppStore

    var body: some View {
        AuroraCard {
            VStack(spacing: 18) {
                ToggleRow(
                    title: "Preload AI in background",
                    detail: "Keeps the verifier warm for sensitive modes.",
                    isOn: preloadBinding
                )
                Divider()
                ToggleRow(
                    title: "Send audio evidence",
                    detail: "Shares the bounded 5-second clip only when enabled.",
                    isOn: sendAudioBinding
                )
                Divider()
                ToggleRow(
                    title: "Opt-in training audio",
                    detail: "Reserved for future iOS backend upload policy.",
                    isOn: trainingAudioBinding
                )
            }
            .padding(22)
        }
    }

    private var preloadBinding: Binding<Bool> {
        Binding(
            get: { store.aiSettings.preloadAiInBackground },
            set: { value in
                var settings = store.aiSettings
                settings.preloadAiInBackground = value && settings.isAiEnabled
                store.updateAiSettings(settings)
            }
        )
    }

    private var sendAudioBinding: Binding<Bool> {
        Binding(
            get: { store.aiSettings.sendAudioToContacts },
            set: { value in
                var settings = store.aiSettings
                settings.sendAudioToContacts = value
                store.updateAiSettings(settings)
            }
        )
    }

    private var trainingAudioBinding: Binding<Bool> {
        Binding(
            get: { store.aiSettings.uploadTrainingTriggerAudio },
            set: { value in
                var settings = store.aiSettings
                settings.uploadTrainingTriggerAudio = value
                store.updateAiSettings(settings)
            }
        )
    }
}

private struct ToggleRow: View {
    var title: String
    var detail: String
    @Binding var isOn: Bool

    var body: some View {
        HStack(spacing: 14) {
            VStack(alignment: .leading, spacing: 4) {
                Text(title)
                    .font(.headline.weight(.black))
                    .foregroundStyle(AuroraPalette.ink)
                Text(detail)
                    .font(.subheadline)
                    .foregroundStyle(AuroraPalette.graphite)
            }
            Spacer()
            Toggle("", isOn: $isOn)
                .labelsHidden()
        }
    }
}

private struct AiAudioTestCard: View {
    @Bindable var store: AuroraAppStore

    var body: some View {
        AuroraCard {
            VStack(alignment: .leading, spacing: 16) {
                Text("Audio test")
                    .font(.title2.weight(.black))
                    .foregroundStyle(AuroraPalette.ink)
                Text("Use Silent SOS to verify the full text-delivery path without playing the loud alarm.")
                    .font(.body)
                    .foregroundStyle(AuroraPalette.graphite)
                HStack(spacing: 12) {
                    Button("Run silent test") {
                        store.triggerSilentSos()
                    }
                    .buttonStyle(AuroraButtonStyle(fill: AuroraPalette.ocean))

                    Button("Refresh AI") {
                        store.updateAiSettings(store.aiSettings)
                    }
                    .buttonStyle(AuroraButtonStyle(fill: AuroraPalette.mist, foreground: AuroraPalette.ink))
                }
            }
            .padding(22)
        }
    }
}
