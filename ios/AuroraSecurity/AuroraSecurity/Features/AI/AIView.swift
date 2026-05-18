import SwiftUI

struct AIView: View {
    @EnvironmentObject private var appState: AppState

    var body: some View {
        List {
            Section {
                VStack(alignment: .leading, spacing: 12) {
                    Label("Gemma 4 LiteRT-LM", systemImage: "sparkles")
                        .font(.title3.weight(.semibold))
                    Text("Aurora iOS uses the same LiteRT-LM model family as Android: Gemma-4-E2B-it and Gemma-4-E4B-it from Hugging Face. Downloaded models are stored in Application Support and used for 5-second emergency audio review.")
                        .font(.subheadline)
                        .foregroundStyle(MorandiPalette.mutedText)
                }
                .padding(.vertical, 8)
            }
            .listRowBackground(MorandiPalette.surface)

            Section("Model") {
                Picker("Model", selection: Binding(
                    get: { appState.aiFeatureState.currentModel },
                    set: { appState.selectAiModel($0) }
                )) {
                    ForEach(GemmaLiteRTModel.allCases) { model in
                        Text(model.modelName).tag(model)
                    }
                }

                ForEach(GemmaLiteRTModel.allCases) { model in
                    GemmaModelRow(
                        model: model,
                        isSelected: appState.aiFeatureState.currentModel == model
                    )
                }
            }

            Section("LiteRT-LM status") {
                LabeledContent("Status", value: appState.aiFeatureState.modelStatus.title)
                LabeledContent("Accelerator", value: appState.aiFeatureState.accelerator)

                if appState.aiFeatureState.modelStatus == .downloading {
                    ProgressView(value: appState.aiFeatureState.downloadProgress) {
                        Text("Downloading \(Int(appState.aiFeatureState.downloadProgress * 100))%")
                    }
                }

                Text(appState.aiFeatureState.statusMessage)
                    .font(.footnote)
                    .foregroundStyle(MorandiPalette.mutedText)

                HStack {
                    Button {
                        appState.downloadAiModel()
                    } label: {
                        Label("Download", systemImage: "arrow.down.circle")
                    }
                    .disabled(appState.aiFeatureState.modelStatus == .downloading)

                    Button {
                        appState.initializeAiModel()
                    } label: {
                        Label("Initialize", systemImage: "cpu")
                    }
                    .disabled(appState.aiFeatureState.modelStatus == .downloading)

                    Button(role: .destructive) {
                        appState.deleteAiModel()
                    } label: {
                        Label("Delete", systemImage: "trash")
                    }
                }
                .buttonStyle(.bordered)
            }

            Section("Dispatch") {
                Picker("SOS mode", selection: Binding(
                    get: { appState.aiSettings.sosDispatchMode },
                    set: { appState.saveAiSettings(appState.aiSettings.updating(\.sosDispatchMode, to: $0)) }
                )) {
                    ForEach(SosDispatchMode.allCases) { mode in
                        Text(mode.title).tag(mode)
                    }
                }

                Toggle("Enable LiteRT audio analysis", isOn: Binding(
                    get: { appState.aiSettings.enableAiAnalysis },
                    set: { enabled in
                        var settings = appState.aiSettings
                        settings.enableAiAnalysis = enabled
                        settings.isAiEnabled = enabled
                        appState.saveAiSettings(settings)
                    }
                ))

                Toggle("Attach audio evidence when available", isOn: Binding(
                    get: { appState.aiSettings.sendAudioToContacts },
                    set: { appState.saveAiSettings(appState.aiSettings.updating(\.sendAudioToContacts, to: $0)) }
                ))
            }

            if let result = appState.aiFeatureState.lastAnalysisResult {
                Section("Latest analysis") {
                    Text(result)
                        .font(.callout.monospaced())
                }
            }

            Section("iOS runtime note") {
                Text("The Hugging Face model files are wired now. LiteRT-LM's public Swift API is still emerging, so inference is isolated behind a runtime adapter. When the official Swift runtime is linked, the adapter can load the downloaded .litertlm file without changing this UI or alert flow.")
                    .font(.footnote)
                    .foregroundStyle(MorandiPalette.mutedText)
            }
        }
        .scrollContentBackground(.hidden)
        .background(MorandiPalette.appBackground)
        .navigationTitle("AI")
    }
}

private struct GemmaModelRow: View {
    var model: GemmaLiteRTModel
    var isSelected: Bool

    var body: some View {
        HStack(alignment: .top, spacing: 12) {
            Image(systemName: isSelected ? "checkmark.circle.fill" : "circle")
                .foregroundStyle(isSelected ? MorandiPalette.sage : MorandiPalette.mutedText)
            VStack(alignment: .leading, spacing: 4) {
                HStack {
                    Text(model.modelName)
                        .font(.subheadline.weight(.semibold))
                    Spacer()
                    Text(model.displaySize)
                        .font(.caption.weight(.semibold))
                        .foregroundStyle(MorandiPalette.mutedText)
                }
                Text(model.summary)
                    .font(.caption)
                    .foregroundStyle(MorandiPalette.mutedText)
                Text(model.repository)
                    .font(.caption2)
                    .foregroundStyle(MorandiPalette.mistBlue)
            }
        }
        .padding(.vertical, 4)
    }
}

private extension AiSettings {
    func updating<Value>(_ keyPath: WritableKeyPath<AiSettings, Value>, to value: Value) -> AiSettings {
        var copy = self
        copy[keyPath: keyPath] = value
        return copy
    }
}
