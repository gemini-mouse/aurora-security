import SwiftUI

struct SettingsView: View {
    @Bindable var store: AuroraAppStore

    var body: some View {
        ScrollView(showsIndicators: false) {
            VStack(spacing: 14) {
                SectionHeader(title: "Security mode")
                SecurityModePresetCard(store: store)
                LoudSosSoundCard(store: store)

                SectionHeader(title: "Detection")
                ThresholdCard(store: store)
                MovementSecurityCard(store: store)

                SectionHeader(title: "Permissions")
                PermissionsSection(store: store)

                SectionHeader(title: "Contacts & alerts")
                AlertProfileCard(store: store)
                ContactMethodsCard(store: store)

                SectionHeader(title: "Appearance")
                AppearanceCard(store: store)

                SectionHeader(title: "Support")
                SupportCard(store: store)
            }
            .padding(.horizontal, 20)
            .padding(.top, 8)
            .padding(.bottom, 132)
        }
    }
}

private struct SecurityModePresetCard: View {
    @Bindable var store: AuroraAppStore

    private var currentPreset: SecurityModePreset {
        SecurityModePreset.current(
            thresholdDb: store.uiState.thresholdDb,
            movementSettings: store.movementSettings,
            aiSettings: store.aiSettings
        )
    }

    var body: some View {
        AuroraCard {
            VStack(alignment: .leading, spacing: 16) {
                Text("Choose a preset that matches where you are going.")
                    .font(.body)
                    .foregroundStyle(AuroraPalette.graphite)

                LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible())], spacing: 12) {
                    ForEach(SecurityModePreset.allCases.filter { $0 != .custom }) { preset in
                        Button {
                            store.applyPreset(preset)
                        } label: {
                            VStack(alignment: .leading, spacing: 10) {
                                Image(systemName: preset.systemImage)
                                    .font(.title2.weight(.bold))
                                Text(preset.label)
                                    .font(.headline.weight(.black))
                                Text(preset.description)
                                    .font(.caption)
                                    .lineLimit(3)
                                    .fixedSize(horizontal: false, vertical: true)
                            }
                            .frame(maxWidth: .infinity, alignment: .leading)
                            .padding(16)
                            .foregroundStyle(currentPreset == preset ? .white : AuroraPalette.ink)
                            .background(currentPreset == preset ? AuroraPalette.ocean : Color.white.opacity(0.34))
                            .clipShape(RoundedRectangle(cornerRadius: 22, style: .continuous))
                        }
                        .buttonStyle(.plain)
                    }
                }

                if currentPreset == .custom {
                    Label("Custom settings active", systemImage: "slider.horizontal.3")
                        .font(.headline.weight(.bold))
                        .foregroundStyle(AuroraPalette.ocean)
                }
            }
            .padding(22)
        }
    }
}

private struct LoudSosSoundCard: View {
    @Bindable var store: AuroraAppStore

    var body: some View {
        AuroraCard {
            VStack(alignment: .leading, spacing: 14) {
                Text("Loud SOS sound")
                    .font(.title3.weight(.black))
                Text("Choose the audible alarm style for Loud SOS.")
                    .font(.body)
                    .foregroundStyle(AuroraPalette.graphite)
                Picker("Loud SOS sound", selection: soundBinding) {
                    ForEach(LoudSosSound.allCases) { sound in
                        Text(sound.rawValue).tag(sound)
                    }
                }
                .pickerStyle(.segmented)
            }
            .padding(22)
        }
    }

    private var soundBinding: Binding<LoudSosSound> {
        Binding(
            get: { store.loudSosSound },
            set: { store.updateLoudSosSound($0) }
        )
    }
}

private struct ThresholdCard: View {
    @Bindable var store: AuroraAppStore

    var body: some View {
        AuroraCard {
            VStack(alignment: .leading, spacing: 20) {
                VStack(alignment: .leading, spacing: 8) {
                    Text("Sound sensitivity")
                        .font(.system(size: 32, weight: .black, design: .rounded))
                        .foregroundStyle(AuroraPalette.ink)
                    Text("Adjust how sensitive the alarm should be for your environment.")
                        .font(.title3)
                        .foregroundStyle(AuroraPalette.graphite)
                        .fixedSize(horizontal: false, vertical: true)
                }

                HStack {
                    Text("\(Int(store.uiState.thresholdDb.rounded())) dB")
                        .font(.system(size: 34, weight: .black, design: .rounded))
                    Spacer()
                    Text("Live: \(Int(store.uiState.currentDb.rounded())) dB")
                        .font(.title3.weight(.black))
                        .foregroundStyle(AuroraPalette.graphite)
                }

                Slider(value: thresholdBinding, in: 60...110, step: 1)
                    .tint(AuroraPalette.ocean)
            }
            .padding(24)
        }
    }

    private var thresholdBinding: Binding<Double> {
        Binding(
            get: { store.uiState.thresholdDb },
            set: { store.updateThreshold($0) }
        )
    }
}

private struct MovementSecurityCard: View {
    @Bindable var store: AuroraAppStore

    var body: some View {
        AuroraCard {
            VStack(alignment: .leading, spacing: 20) {
                HStack {
                    Text("Motion sensitivity")
                        .font(.system(size: 30, weight: .black, design: .rounded))
                    Spacer()
                    Toggle("", isOn: enabledBinding)
                        .labelsHidden()
                }

                Text("Enable device shaking as a secondary confirm. Alerts trigger only when sound exceeds the limit and the device is shaken intensely.")
                    .font(.title3)
                    .foregroundStyle(AuroraPalette.graphite)
                    .fixedSize(horizontal: false, vertical: true)

                HStack {
                    Text(String(format: "%.1f G", store.movementSettings.shakeThresholdG))
                        .font(.system(size: 34, weight: .black, design: .rounded))
                    Spacer()
                    Text(String(format: "Live: %.1f G", store.uiState.currentG))
                        .font(.title3.weight(.black))
                        .foregroundStyle(AuroraPalette.graphite)
                }

                Slider(value: thresholdBinding, in: 1.2...5.0, step: 0.1)
                    .disabled(!store.movementSettings.useShakeDetection)
                    .tint(AuroraPalette.ocean)

                Text("Tip: 1.0G is normal gravity. Try shaking your iPhone to test.")
                    .font(.subheadline.weight(.semibold))
                    .foregroundStyle(AuroraPalette.graphite)
            }
            .padding(24)
        }
    }

    private var enabledBinding: Binding<Bool> {
        Binding(
            get: { store.movementSettings.useShakeDetection },
            set: { value in
                var settings = store.movementSettings
                settings.useShakeDetection = value
                store.updateMovementSettings(settings)
            }
        )
    }

    private var thresholdBinding: Binding<Double> {
        Binding(
            get: { store.movementSettings.shakeThresholdG },
            set: { value in
                var settings = store.movementSettings
                settings.shakeThresholdG = value
                store.updateMovementSettings(settings)
            }
        )
    }
}

private struct PermissionsSection: View {
    @Bindable var store: AuroraAppStore

    var body: some View {
        AuroraCard {
            VStack(spacing: 10) {
                PermissionStateRow(
                    kind: .microphone,
                    isGranted: store.uiState.hasMicPermission,
                    detail: store.uiState.hasMicPermission ? "Sound monitoring can run." : "Needed to monitor audio and capture crisis clips.",
                    actionTitle: "Allow"
                ) {
                    Task {
                        _ = await store.requestMicrophonePermission()
                        await store.startMonitoringIfPossible()
                    }
                }
                PermissionStateRow(
                    kind: .location,
                    isGranted: store.hasLocationPermission,
                    detail: store.hasLocationPermission ? "Alerts can include a Google Maps link." : "Needed to send current location in SOS messages.",
                    actionTitle: "Allow"
                ) {
                    store.requestLocationPermission()
                }
                PermissionStateRow(
                    kind: .notifications,
                    isGranted: store.hasNotificationPermission,
                    detail: store.hasNotificationPermission ? "Emergency notifications are enabled." : "Needed for status and emergency alerts.",
                    actionTitle: "Allow"
                ) {
                    Task { await store.requestNotificationPermission() }
                }
                PermissionStateRow(
                    kind: .motion,
                    isGranted: store.hasMotionCapability,
                    detail: store.hasMotionCapability ? "Accelerometer is available." : "This device does not expose motion data.",
                    actionTitle: "Check"
                ) {}
                PermissionStateRow(
                    kind: .phoneCall,
                    isGranted: true,
                    detail: "iOS can open the Phone app, but cannot silently auto-place calls like Android.",
                    actionTitle: "Open"
                ) {}
                PermissionStateRow(
                    kind: .sms,
                    isGranted: true,
                    detail: "iOS can open an SMS composer, but cannot send SMS without user confirmation.",
                    actionTitle: "Open"
                ) {}
            }
            .padding(14)
        }
    }
}

private struct AlertProfileCard: View {
    @Bindable var store: AuroraAppStore

    var body: some View {
        AuroraCard {
            VStack(alignment: .leading, spacing: 14) {
                Text("Alert profile")
                    .font(.title2.weight(.black))
                    .foregroundStyle(AuroraPalette.ink)
                TextField("Backend API URL", text: contactBinding(\.apiUrl))
                    .textInputAutocapitalization(.never)
                    .keyboardType(.URL)
                    .textFieldStyle(.roundedBorder)
                SecureField("API token", text: contactBinding(\.apiToken))
                    .textInputAutocapitalization(.never)
                    .textFieldStyle(.roundedBorder)
                TextField("User name", text: contactBinding(\.userName))
                    .textFieldStyle(.roundedBorder)
                TextField("Mobile number", text: contactBinding(\.mobileNumber))
                    .keyboardType(.phonePad)
                    .textFieldStyle(.roundedBorder)
                TextField("Device name", text: contactBinding(\.deviceName))
                    .textFieldStyle(.roundedBorder)
                TextEditor(text: contactBinding(\.alertMessageTemplate))
                    .frame(minHeight: 150)
                    .padding(8)
                    .background(Color.white.opacity(0.42))
                    .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
            }
            .padding(22)
        }
    }

    private func contactBinding(_ keyPath: WritableKeyPath<AlertContactSettings, String>) -> Binding<String> {
        Binding(
            get: { store.contactSettings[keyPath: keyPath] },
            set: { value in
                var settings = store.contactSettings
                settings[keyPath: keyPath] = value
                store.updateContactSettings(settings)
            }
        )
    }
}

private struct ContactMethodsCard: View {
    @Bindable var store: AuroraAppStore
    @Environment(\.openURL) private var openURL

    var body: some View {
        AuroraCard {
            VStack(alignment: .leading, spacing: 18) {
                Text("Contact methods")
                    .font(.title2.weight(.black))
                    .foregroundStyle(AuroraPalette.ink)
                Text("Enable at least one channel so Aurora has somewhere to send SOS alerts.")
                    .font(.body)
                    .foregroundStyle(AuroraPalette.graphite)

                ContactToggleRow(title: "LINE", isOn: contactBoolBinding(\.useLine), syncMessage: store.lineSyncMessage) {
                    store.syncLineContacts()
                } openSetup: {
                    if let url = store.setupLink(for: .line) { openURL(url) }
                }
                ContactList(contacts: store.lineContacts.map { "\($0.name.isEmpty ? "LINE contact" : $0.name) - \($0.status.label)" })

                Divider()

                ContactToggleRow(title: "Telegram", isOn: contactBoolBinding(\.useTelegram), syncMessage: store.contactsSyncMessage) {
                    store.syncTelegramContacts()
                } openSetup: {
                    if let url = store.setupLink(for: .telegram) { openURL(url) }
                }
                TextField("Telegram bot name", text: contactBinding(\.botName))
                    .textInputAutocapitalization(.never)
                    .textFieldStyle(.roundedBorder)
                ContactList(contacts: store.telegramContacts.map { "\($0.name.isEmpty ? "Telegram contact" : $0.name) \($0.telegramHandle) - \($0.status.label)" })

                Divider()

                ContactToggleRow(title: "Push", isOn: contactBoolBinding(\.usePush), syncMessage: store.pushSyncMessage) {
                    store.syncPushContacts()
                } openSetup: {
                    if let url = store.setupLink(for: .push) { openURL(url) }
                }
                HStack {
                    TextField("Trusted-contact bind code", text: $store.trustedPushBindCode)
                        .textInputAutocapitalization(.characters)
                        .textFieldStyle(.roundedBorder)
                    Button("Bind") {
                        store.bindCurrentDeviceToPushCode()
                    }
                    .buttonStyle(.borderedProminent)
                }
                ContactList(contacts: store.pushContacts.map { "\($0.name.isEmpty ? "Push contact" : $0.name) \($0.deviceName) - \($0.status.label)" })

                Divider()

                Toggle("SMS text alert", isOn: contactBoolBinding(\.useSms))
                    .font(.headline.weight(.bold))
                TextField("SMS primary phone", text: contactBinding(\.emergencyPhoneNumber))
                    .keyboardType(.phonePad)
                    .textFieldStyle(.roundedBorder)
                TextField("SMS secondary phone", text: contactBinding(\.secondaryPhoneNumber))
                    .keyboardType(.phonePad)
                    .textFieldStyle(.roundedBorder)

                Divider()

                Toggle("Phone-call fallback", isOn: contactBoolBinding(\.usePhoneCall))
                    .font(.headline.weight(.bold))
                TextField("Phone-call number", text: contactBinding(\.phoneCallPhoneNumber))
                    .keyboardType(.phonePad)
                    .textFieldStyle(.roundedBorder)
            }
            .padding(22)
        }
    }

    private func contactBoolBinding(_ keyPath: WritableKeyPath<AlertContactSettings, Bool>) -> Binding<Bool> {
        Binding(
            get: { store.contactSettings[keyPath: keyPath] },
            set: { value in
                var settings = store.contactSettings
                settings[keyPath: keyPath] = value
                store.updateContactSettings(settings)
            }
        )
    }

    private func contactBinding(_ keyPath: WritableKeyPath<AlertContactSettings, String>) -> Binding<String> {
        Binding(
            get: { store.contactSettings[keyPath: keyPath] },
            set: { value in
                var settings = store.contactSettings
                settings[keyPath: keyPath] = value
                store.updateContactSettings(settings)
            }
        )
    }
}

private struct ContactToggleRow: View {
    var title: String
    @Binding var isOn: Bool
    var syncMessage: String
    var sync: () -> Void
    var openSetup: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack {
                Toggle(title, isOn: $isOn)
                    .font(.headline.weight(.black))
                Spacer()
            }
            HStack(spacing: 10) {
                Button("Sync", action: sync)
                    .buttonStyle(.bordered)
                Button("Setup", action: openSetup)
                    .buttonStyle(.bordered)
                Text(syncMessage)
                    .font(.caption.weight(.semibold))
                    .foregroundStyle(AuroraPalette.graphite)
                    .lineLimit(2)
            }
        }
    }
}

private struct ContactList: View {
    var contacts: [String]

    var body: some View {
        if contacts.isEmpty {
            Text("No bound contacts yet.")
                .font(.caption)
                .foregroundStyle(AuroraPalette.graphite)
        } else {
            VStack(alignment: .leading, spacing: 6) {
                ForEach(contacts, id: \.self) { contact in
                    Label(contact, systemImage: "person.crop.circle.badge.checkmark")
                        .font(.caption.weight(.semibold))
                        .foregroundStyle(AuroraPalette.graphite)
                }
            }
        }
    }
}

private struct AppearanceCard: View {
    @Bindable var store: AuroraAppStore

    var body: some View {
        AuroraCard {
            VStack(spacing: 16) {
                Picker("Theme", selection: themeBinding) {
                    ForEach(AppThemeMode.allCases) { mode in
                        Text(mode.rawValue).tag(mode)
                    }
                }
                .pickerStyle(.segmented)

                Picker("Language", selection: languageBinding) {
                    ForEach(AppLanguageOption.allCases) { language in
                        Text(language.label).tag(language)
                    }
                }
                .pickerStyle(.menu)
                .frame(maxWidth: .infinity, alignment: .leading)
            }
            .padding(22)
        }
    }

    private var themeBinding: Binding<AppThemeMode> {
        Binding(get: { store.themeMode }, set: { store.updateThemeMode($0) })
    }

    private var languageBinding: Binding<AppLanguageOption> {
        Binding(get: { store.appLanguage }, set: { store.updateAppLanguage($0) })
    }
}

private struct SupportCard: View {
    @Bindable var store: AuroraAppStore

    var body: some View {
        AuroraCard {
            VStack(alignment: .leading, spacing: 14) {
                Text("Guided setup")
                    .font(.title2.weight(.black))
                Text("Review permissions, contact channels, and test dispatch before relying on the app.")
                    .font(.body)
                    .foregroundStyle(AuroraPalette.graphite)
                HStack(spacing: 12) {
                    Button("Request all permissions") {
                        store.requestAllPermissions()
                    }
                    .buttonStyle(AuroraButtonStyle(fill: AuroraPalette.ocean))

                    Button("Reset local settings") {
                        store.resetLocalSettings()
                    }
                    .buttonStyle(AuroraButtonStyle(fill: AuroraPalette.ember, foreground: .white))
                }
            }
            .padding(22)
        }
    }
}
