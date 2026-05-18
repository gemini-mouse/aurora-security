import SwiftUI

struct SettingsView: View {
    @EnvironmentObject private var appState: AppState

    var body: some View {
        Form {
            Section("Protection") {
                Stepper(
                    value: Binding(
                        get: { appState.alarmState.thresholdDb },
                        set: { appState.updateThreshold($0) }
                    ),
                    in: 60...115,
                    step: 1
                ) {
                    LabeledContent("Threshold", value: "\(Int(appState.alarmState.thresholdDb)) dB")
                }

                Toggle("Shake pairing", isOn: Binding(
                    get: { appState.movementSettings.useShakeDetection },
                    set: { appState.saveMovementSettings(appState.movementSettings.updating(\.useShakeDetection, to: $0)) }
                ))
            }

            Section("Emergency contacts") {
                TextField("Your name", text: Binding(
                    get: { appState.contactSettings.userName },
                    set: { appState.saveContactSettings(appState.contactSettings.updating(\.userName, to: $0)) }
                ))

                TextField("Mobile number", text: Binding(
                    get: { appState.contactSettings.mobileNumber },
                    set: { appState.saveContactSettings(appState.contactSettings.updating(\.mobileNumber, to: $0)) }
                ))
                .keyboardType(.phonePad)

                TextField("Primary emergency phone", text: Binding(
                    get: { appState.contactSettings.emergencyPhoneNumber },
                    set: { appState.saveContactSettings(appState.contactSettings.updating(\.emergencyPhoneNumber, to: $0)) }
                ))
                .keyboardType(.phonePad)

                TextField("Secondary phone", text: Binding(
                    get: { appState.contactSettings.secondaryPhoneNumber },
                    set: { appState.saveContactSettings(appState.contactSettings.updating(\.secondaryPhoneNumber, to: $0)) }
                ))
                .keyboardType(.phonePad)
            }

            Section("Backend") {
                TextField("Backend API URL", text: Binding(
                    get: { appState.contactSettings.apiUrl },
                    set: { appState.saveContactSettings(appState.contactSettings.updating(\.apiUrl, to: $0)) }
                ))
                .textInputAutocapitalization(.never)
                .keyboardType(.URL)

                SecureField("API token", text: Binding(
                    get: { appState.contactSettings.apiToken },
                    set: { appState.saveContactSettings(appState.contactSettings.updating(\.apiToken, to: $0)) }
                ))

                TextField("Device name", text: Binding(
                    get: { appState.contactSettings.deviceName },
                    set: { appState.saveContactSettings(appState.contactSettings.updating(\.deviceName, to: $0)) }
                ))
            }

            Section("Alert methods") {
                Toggle("Telegram", isOn: Binding(
                    get: { appState.contactSettings.useTelegram },
                    set: { appState.saveContactSettings(appState.contactSettings.updating(\.useTelegram, to: $0)) }
                ))

                Toggle("Push", isOn: Binding(
                    get: { appState.contactSettings.usePush },
                    set: { appState.saveContactSettings(appState.contactSettings.updating(\.usePush, to: $0)) }
                ))

                Toggle("SMS fallback", isOn: Binding(
                    get: { appState.contactSettings.useSms },
                    set: { appState.saveContactSettings(appState.contactSettings.updating(\.useSms, to: $0)) }
                ))

                Toggle("Phone fallback", isOn: Binding(
                    get: { appState.contactSettings.usePhoneCall },
                    set: { appState.saveContactSettings(appState.contactSettings.updating(\.usePhoneCall, to: $0)) }
                ))

                Text("On iOS, SMS and phone call are confirmation-based fallbacks. Backend SMS or voice calling should be used for automatic delivery.")
                    .font(.footnote)
                    .foregroundStyle(MorandiPalette.mutedText)
            }

            Section("Message") {
                TextEditor(text: Binding(
                    get: { appState.contactSettings.alertMessageTemplate },
                    set: { appState.saveContactSettings(appState.contactSettings.updating(\.alertMessageTemplate, to: $0)) }
                ))
                .frame(minHeight: 130)
            }

            Section("Appearance") {
                Picker("Theme", selection: Binding(
                    get: { appState.themeMode },
                    set: { appState.saveThemeMode($0) }
                )) {
                    ForEach(AppThemeMode.allCases) { mode in
                        Text(mode.title).tag(mode)
                    }
                }
            }
        }
        .scrollContentBackground(.hidden)
        .background(MorandiPalette.appBackground)
        .navigationTitle("Settings")
    }
}

private extension AlertContactSettings {
    func updating<Value>(_ keyPath: WritableKeyPath<AlertContactSettings, Value>, to value: Value) -> AlertContactSettings {
        var copy = self
        copy[keyPath: keyPath] = value
        return copy
    }
}

private extension MovementSettings {
    func updating<Value>(_ keyPath: WritableKeyPath<MovementSettings, Value>, to value: Value) -> MovementSettings {
        var copy = self
        copy[keyPath: keyPath] = value
        return copy
    }
}
