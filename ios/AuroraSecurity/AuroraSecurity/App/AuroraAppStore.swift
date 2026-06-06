import AVFoundation
import Foundation
import Observation
import SwiftUI
import UIKit
import UserNotifications

@MainActor
@Observable
final class AuroraAppStore {
    var selectedTab: AppTab = .detect
    var isHistoryPresented = false
    var historyFilter: HistoryFilter = .all
    var themeMode: AppThemeMode
    var appLanguage: AppLanguageOption
    var uiState: NoiseAlarmUiState
    var contactSettings: AlertContactSettings
    var aiSettings: AiSettings
    var aiState: AiFeatureUiState
    var movementSettings: MovementSettings
    var loudSosSound: LoudSosSound
    var telegramContacts: [TelegramContact]
    var pushContacts: [PushContact]
    var lineContacts: [LineContact]
    var historyRecords: [HistoryRecord]
    var latestAlertDeliverySummary: AlertDeliverySummary?
    var trustedPushBindCode = ""
    var hasLocationPermission = false
    var hasNotificationPermission = false
    var hasMotionCapability = true
    var isMonitoringPaused: Bool
    var contactsSyncMessage = "Contacts not synced."
    var pushSyncMessage = "Push contacts not synced."
    var lineSyncMessage = "LINE contacts not synced."
    var permissionMessage = ""
    var isBusy = false

    private let preferences: PreferencesStore
    private let backendClient: BackendClient
    private let historyStore: HistoryStore
    private let noiseMonitor: NoiseMonitor
    private let motionMonitor: MotionMonitor
    private let crisisAudioRecorder: CrisisAudioRecorder
    private let locationProvider: LocationSnapshotProvider
    private let alarmPlayer: LocalAlarmPlayer
    private let aiAnalysisService: AiAnalysisService

    private var didStart = false
    private var countdownTask: Task<Void, Never>?
    private var currentEmergencySessionId: Int64?
    private var currentEmergencyTriggerSource: EmergencyTriggerSource?
    private var textAlertDeliveryStatuses: [Int64: TextAlertDeliveryStatus] = [:]
    private var processedClipIds: Set<Int64> = []

    init(
        preferences: PreferencesStore = .shared,
        backendClient: BackendClient = BackendClient(),
        historyStore: HistoryStore = HistoryStore(),
        noiseMonitor: NoiseMonitor = NoiseMonitor(),
        motionMonitor: MotionMonitor = MotionMonitor(),
        crisisAudioRecorder: CrisisAudioRecorder = CrisisAudioRecorder(),
        locationProvider: LocationSnapshotProvider = LocationSnapshotProvider(),
        alarmPlayer: LocalAlarmPlayer = LocalAlarmPlayer(),
        aiAnalysisService: AiAnalysisService = PlaceholderAiAnalysisService()
    ) {
        self.preferences = preferences
        self.backendClient = backendClient
        self.historyStore = historyStore
        self.noiseMonitor = noiseMonitor
        self.motionMonitor = motionMonitor
        self.crisisAudioRecorder = crisisAudioRecorder
        self.locationProvider = locationProvider
        self.alarmPlayer = alarmPlayer
        self.aiAnalysisService = aiAnalysisService

        let threshold = preferences.loadThresholdDb()
        let movement = preferences.loadMovementSettings()
        themeMode = preferences.loadThemeMode()
        appLanguage = preferences.loadAppLanguage()
        contactSettings = preferences.loadContactSettings()
        aiSettings = preferences.loadAiSettings()
        aiState = AiFeatureUiState()
        movementSettings = movement
        loudSosSound = preferences.loadLoudSosSound()
        telegramContacts = preferences.loadTelegramContacts()
        pushContacts = preferences.loadPushContacts()
        lineContacts = preferences.loadLineContacts()
        historyRecords = historyStore.loadRecords()
        latestAlertDeliverySummary = historyRecords.compactMap(\.deliverySummary).first
        isMonitoringPaused = preferences.loadMonitoringPaused()
        uiState = NoiseAlarmUiState(
            thresholdDb: threshold,
            hasMicPermission: noiseMonitor.hasMicrophonePermission,
            useShakeDetection: movement.useShakeDetection,
            shakeThresholdG: movement.shakeThresholdG
        )

        wireServiceCallbacks()
    }

    func start() async {
        guard !didStart else { return }
        didStart = true
        observeNotifications()
        refreshPermissionState()
        aiState = await aiAnalysisService.refreshState(modelType: GemmaModelType(rawValue: aiSettings.modelType) ?? .e2b)
        if !isMonitoringPaused {
            await startMonitoringIfPossible()
        }
    }

    func requestAllPermissions() {
        Task {
            _ = await requestMicrophonePermission()
            requestLocationPermission()
            await requestNotificationPermission()
            await startMonitoringIfPossible()
        }
    }

    func requestMicrophonePermission() async -> Bool {
        let granted = await noiseMonitor.requestMicrophonePermission()
        uiState.hasMicPermission = granted
        permissionMessage = granted ? "Microphone permission granted." : "Microphone permission is required for detection."
        return granted
    }

    func requestLocationPermission() {
        locationProvider.requestLocationPermission()
        refreshPermissionState()
    }

    func requestNotificationPermission() async {
        let center = UNUserNotificationCenter.current()
        let granted = (try? await center.requestAuthorization(options: [.alert, .sound, .badge])) ?? false
        hasNotificationPermission = granted
        if granted {
            UIApplication.shared.registerForRemoteNotifications()
        }
    }

    func startMonitoringIfPossible() async {
        refreshPermissionState()
        guard !isMonitoringPaused else {
            uiState.isMonitoring = false
            return
        }
        guard uiState.hasMicPermission else {
            permissionMessage = "Allow microphone access to start protection."
            return
        }

        do {
            try noiseMonitor.start()
            uiState.isMonitoring = true
            if movementSettings.useShakeDetection {
                motionMonitor.start(thresholdG: movementSettings.shakeThresholdG)
            }
            await scheduleLocalNotification(title: "Protection active", body: "Aurora is guarding you.")
        } catch {
            uiState.isMonitoring = false
            permissionMessage = error.localizedDescription
        }
    }

    func stopMonitoring() {
        noiseMonitor.stop()
        motionMonitor.stop()
        uiState.isMonitoring = false
    }

    func toggleMonitoringPause(_ paused: Bool) {
        isMonitoringPaused = paused
        preferences.saveMonitoringPaused(paused)
        if paused {
            stopMonitoring()
        } else {
            Task { await startMonitoringIfPossible() }
        }
    }

    func updateThreshold(_ value: Double, persist: Bool = true) {
        uiState.thresholdDb = value
        if persist {
            preferences.saveThresholdDb(value)
        }
        reevaluateThreshold(canResetSilence: true)
    }

    func updateMovementSettings(_ settings: MovementSettings, persist: Bool = true) {
        movementSettings = settings
        uiState.useShakeDetection = settings.useShakeDetection
        uiState.shakeThresholdG = settings.shakeThresholdG
        if persist {
            preferences.saveMovementSettings(settings)
        }
        if uiState.isMonitoring {
            motionMonitor.stop()
            if settings.useShakeDetection {
                motionMonitor.start(thresholdG: settings.shakeThresholdG)
            }
        }
        reevaluateThreshold(canResetSilence: false)
    }

    func updateAiSettings(_ settings: AiSettings) {
        aiSettings = settings
        preferences.saveAiSettings(settings)
        Task {
            aiState = await aiAnalysisService.refreshState(modelType: GemmaModelType(rawValue: settings.modelType) ?? .e2b)
        }
    }

    func updateContactSettings(_ settings: AlertContactSettings) {
        contactSettings = settings
        preferences.saveContactSettings(settings)
    }

    func updateThemeMode(_ mode: AppThemeMode) {
        themeMode = mode
        preferences.saveThemeMode(mode)
    }

    func updateAppLanguage(_ language: AppLanguageOption) {
        appLanguage = language
        preferences.saveAppLanguage(language)
    }

    func updateLoudSosSound(_ sound: LoudSosSound) {
        loudSosSound = sound
        preferences.saveLoudSosSound(sound)
    }

    func applyPreset(_ preset: SecurityModePreset) {
        guard preset != .custom else { return }
        updateThreshold(preset.thresholdDb)
        updateMovementSettings(
            MovementSettings(
                useShakeDetection: preset.shakeDetectionEnabled,
                shakeThresholdG: preset.movementThresholdG ?? movementSettings.shakeThresholdG
            )
        )
        updateAiSettings(
            AiSettings(
                isAiEnabled: aiSettings.isAiEnabled,
                sendAudioToContacts: aiSettings.sendAudioToContacts,
                preloadAiInBackground: preset.preloadAiEngine,
                sosDispatchMode: preset.sosDispatchMode,
                modelType: aiSettings.modelType,
                uploadTrainingTriggerAudio: aiSettings.uploadTrainingTriggerAudio
            )
        )
    }

    func triggerSilentSos() {
        Task {
            if !uiState.isMonitoring {
                await startMonitoringIfPossible()
            }
            startCountdown(mode: .silent, source: .silentSosButton)
        }
    }

    func triggerLoudSos() {
        Task {
            if !uiState.isMonitoring {
                await startMonitoringIfPossible()
            }
            startCountdown(mode: .loud, source: .loudSosButton)
        }
    }

    func stopAlarm() {
        countdownTask?.cancel()
        countdownTask = nil
        crisisAudioRecorder.cancelCapture(currentEmergencySessionId)
        alarmPlayer.stop()
        currentEmergencySessionId = nil
        currentEmergencyTriggerSource = nil
        textAlertDeliveryStatuses.removeAll()
        uiState.isSilenced = true
        uiState.alarmTriggered = false
        uiState.activeEmergencyMode = nil
        uiState.pendingEmergencyMode = nil
        uiState.pendingTriggerSource = nil
        uiState.countdownSecondsRemaining = 0
        uiState.triggerDb = nil
    }

    func syncTelegramContacts() {
        Task {
            guard contactSettings.useTelegram else {
                telegramContacts = []
                preferences.saveTelegramContacts([])
                contactsSyncMessage = "Telegram alerts are off."
                return
            }
            await syncContacts(kind: "Telegram") {
                let settings = await ensureAuthenticatedSettings()
                try await backendClient.registerUser(settings: settings, userId: preferences.userId, bindCode: preferences.bindCode)
                let contacts = try await backendClient.fetchTelegramContacts(settings: settings, userId: preferences.userId)
                telegramContacts = contacts
                preferences.saveTelegramContacts(contacts)
                contactsSyncMessage = contacts.isEmpty ? "No Telegram contacts yet." : "Telegram sync complete."
            }
        }
    }

    func syncLineContacts() {
        Task {
            guard contactSettings.useLine else {
                lineContacts = []
                preferences.saveLineContacts([])
                lineSyncMessage = "LINE alerts are off."
                return
            }
            await syncContacts(kind: "LINE") {
                let settings = await ensureAuthenticatedSettings()
                try await backendClient.registerUser(settings: settings, userId: preferences.userId, bindCode: preferences.bindCode)
                let contacts = try await backendClient.fetchLineContacts(settings: settings, userId: preferences.userId)
                lineContacts = contacts
                preferences.saveLineContacts(contacts)
                lineSyncMessage = contacts.isEmpty ? "No LINE contacts yet." : "LINE sync complete."
            }
        }
    }

    func syncPushContacts() {
        Task {
            guard contactSettings.usePush else {
                pushContacts = []
                preferences.savePushContacts([])
                pushSyncMessage = "Push alerts are off."
                return
            }
            do {
                pushSyncMessage = "Registering this iPhone..."
                await requestNotificationPermission()
                let token = preferences.apnsDeviceToken
                guard !token.isEmpty else {
                    pushSyncMessage = "APNs token is not available yet. Try again after notifications are enabled."
                    return
                }
                let settings = await ensureAuthenticatedSettings()
                try await backendClient.registerPushDevice(settings: settings, userId: preferences.userId, deviceToken: token)
                let contacts = try await backendClient.fetchPushContacts(settings: settings, userId: preferences.userId)
                pushContacts = contacts
                preferences.savePushContacts(contacts)
                pushSyncMessage = contacts.isEmpty ? "No push contacts yet." : "Push sync complete."
            } catch {
                pushSyncMessage = "Push sync failed: \(error.localizedDescription)"
            }
        }
    }

    func bindCurrentDeviceToPushCode() {
        Task {
            do {
                let code = trustedPushBindCode.trimmingCharacters(in: .whitespacesAndNewlines).uppercased()
                guard !code.isEmpty else {
                    pushSyncMessage = "Enter a trusted-contact bind code first."
                    return
                }
                let settings = await ensureAuthenticatedSettings()
                try await backendClient.bindCurrentDevice(settings: settings, bindCode: code, contactUserId: preferences.userId)
                trustedPushBindCode = ""
                pushSyncMessage = "This iPhone is bound for trusted-contact push alerts."
                syncPushContacts()
            } catch {
                pushSyncMessage = "Push binding failed: \(error.localizedDescription)"
            }
        }
    }

    func deleteTelegramContact(_ contact: TelegramContact) {
        Task {
            do {
                let settings = await ensureAuthenticatedSettings()
                try await backendClient.deleteTelegramContact(settings: settings, userId: preferences.userId, contactId: contact.id)
                telegramContacts.removeAll { $0.id == contact.id }
                preferences.saveTelegramContacts(telegramContacts)
                contactsSyncMessage = "Contact removed."
            } catch {
                contactsSyncMessage = "Remove failed: \(error.localizedDescription)"
            }
        }
    }

    func deleteLineContact(_ contact: LineContact) {
        Task {
            do {
                let settings = await ensureAuthenticatedSettings()
                try await backendClient.deleteLineContact(settings: settings, userId: preferences.userId, contactId: contact.id)
                lineContacts.removeAll { $0.id == contact.id }
                preferences.saveLineContacts(lineContacts)
                lineSyncMessage = "LINE contact removed."
            } catch {
                lineSyncMessage = "Remove failed: \(error.localizedDescription)"
            }
        }
    }

    func deletePushContact(_ contact: PushContact) {
        Task {
            do {
                let settings = await ensureAuthenticatedSettings()
                try await backendClient.deletePushContact(settings: settings, userId: preferences.userId, contactId: contact.id)
                pushContacts.removeAll { $0.id == contact.id }
                preferences.savePushContacts(pushContacts)
                pushSyncMessage = "Push contact removed."
            } catch {
                pushSyncMessage = "Remove failed: \(error.localizedDescription)"
            }
        }
    }

    func resetLocalSettings() {
        preferences.resetLocalSettings()
        contactSettings = preferences.loadContactSettings()
        aiSettings = preferences.loadAiSettings()
        movementSettings = preferences.loadMovementSettings()
        loudSosSound = preferences.loadLoudSosSound()
        themeMode = preferences.loadThemeMode()
        appLanguage = preferences.loadAppLanguage()
        updateThreshold(preferences.loadThresholdDb(), persist: false)
        telegramContacts = []
        pushContacts = []
        lineContacts = []
        contactsSyncMessage = "Local settings reset."
    }

    func handleIncomingURL(_ url: URL) {
        guard let components = URLComponents(url: url, resolvingAgainstBaseURL: false) else { return }
        let isPushBind =
            (components.scheme == "https" && components.host == "aurorasecurity.app" && components.path.trimmingCharacters(in: CharacterSet(charactersIn: "/")) == "push-bind") ||
            (components.scheme == "aurora" && components.host == "push-bind")
        guard isPushBind else { return }
        let code = components.queryItems?.first(where: { $0.name == "code" })?.value?
            .filter { $0.isLetter || $0.isNumber }
            .uppercased() ?? ""
        trustedPushBindCode = code
        pushSyncMessage = code.isEmpty ? "Push setup link opened." : "Push bind code loaded."
        selectedTab = .settings
    }

    func filteredHistoryRecords() -> [HistoryRecord] {
        switch historyFilter {
        case .all:
            return historyRecords
        case .local:
            return historyRecords.filter { $0.type == .localAlert }
        case .incoming:
            return historyRecords.filter { $0.type == .incomingPush }
        }
    }

    func deleteHistoryRecord(_ record: HistoryRecord) {
        historyStore.deleteRecord(record)
        historyRecords = historyStore.loadRecords()
        latestAlertDeliverySummary = historyRecords.compactMap(\.deliverySummary).first
    }

    func clearHistory() {
        historyStore.clearAll()
        historyRecords = []
        latestAlertDeliverySummary = nil
    }

    func setupLink(for method: ContactMethod) -> URL? {
        switch method {
        case .line:
            let id = contactSettings.lineBotBasicId.trimmingCharacters(in: .whitespacesAndNewlines)
            guard !id.isEmpty else { return nil }
            return URL(string: "https://line.me/R/ti/p/\(id)")
        case .telegram:
            let bot = contactSettings.botName.trimmingCharacters(in: .whitespacesAndNewlines)
            guard !bot.isEmpty else { return nil }
            return URL(string: "https://t.me/\(bot)?start=\(preferences.bindCode)")
        case .push:
            return URL(string: "https://aurorasecurity.app/push-bind?code=\(preferences.bindCode)")
        }
    }

    private func wireServiceCallbacks() {
        noiseMonitor.onLevelChanged = { [weak self] db in
            Task { @MainActor in
                self?.handleLevelUpdate(db)
            }
        }
        noiseMonitor.onPcmBuffer = { [weak self] buffer in
            self?.crisisAudioRecorder.append(buffer: buffer)
        }
        motionMonitor.onMotionUpdate = { [weak self] gForce, isShaking in
            Task { @MainActor in
                self?.handleMotionUpdate(gForce: gForce, isShaking: isShaking)
            }
        }
        crisisAudioRecorder.onClipReady = { [weak self] clip in
            Task { @MainActor in
                await self?.processCompletedClip(clip)
            }
        }
    }

    private func observeNotifications() {
        NotificationCenter.default.addObserver(
            forName: .auroraApnsTokenUpdated,
            object: nil,
            queue: .main
        ) { [weak self] notification in
            Task { @MainActor in
                guard let token = notification.object as? String, !token.isEmpty else { return }
                self?.pushSyncMessage = "APNs token ready."
            }
        }
        NotificationCenter.default.addObserver(
            forName: .auroraRemoteNotificationReceived,
            object: nil,
            queue: .main
        ) { [weak self] notification in
            Task { @MainActor in
                guard let self, let userInfo = notification.object as? [AnyHashable: Any] else { return }
                if self.historyStore.upsertIncomingPushRecord(userInfo: userInfo) != nil {
                    self.historyRecords = self.historyStore.loadRecords()
                }
            }
        }
    }

    private func refreshPermissionState() {
        uiState.hasMicPermission = noiseMonitor.hasMicrophonePermission
        hasLocationPermission = locationProvider.hasLocationPermission
        hasMotionCapability = motionMonitor.isAvailable
        Task {
            let settings = await UNUserNotificationCenter.current().notificationSettings()
            hasNotificationPermission = settings.authorizationStatus == .authorized || settings.authorizationStatus == .provisional
        }
    }

    private func handleLevelUpdate(_ db: Double) {
        uiState.currentDb = db
        reevaluateThreshold(canResetSilence: true)
        if uiState.isThresholdExceeded,
           uiState.pendingEmergencyMode == nil,
           uiState.activeEmergencyMode == nil {
            startCountdown(mode: .loud, source: .soundDetection)
        }
    }

    private func handleMotionUpdate(gForce: Double, isShaking: Bool) {
        uiState.currentG = gForce
        uiState.isShaking = isShaking
        reevaluateThreshold(canResetSilence: false)
    }

    private func reevaluateThreshold(canResetSilence: Bool) {
        if canResetSilence, uiState.currentDb < uiState.thresholdDb {
            uiState.isSilenced = false
        }
        let soundExceeded = uiState.hasMicPermission &&
            uiState.isMonitoring &&
            !uiState.isSilenced &&
            uiState.currentDb >= uiState.thresholdDb
        uiState.isThresholdExceeded = uiState.useShakeDetection ? (soundExceeded && uiState.isShaking) : soundExceeded
    }

    private func startCountdown(mode: EmergencyMode, source: EmergencyTriggerSource) {
        guard uiState.pendingEmergencyMode == nil, uiState.activeEmergencyMode == nil else { return }

        countdownTask?.cancel()
        processedClipIds.removeAll()
        textAlertDeliveryStatuses.removeAll()
        currentEmergencyTriggerSource = source
        currentEmergencySessionId = uiState.hasMicPermission ? crisisAudioRecorder.startCapture(source: source, mode: mode) : nil
        uiState.isSilenced = false
        uiState.alarmTriggered = false
        uiState.activeEmergencyMode = nil
        uiState.pendingEmergencyMode = mode
        uiState.pendingTriggerSource = source
        uiState.countdownSecondsRemaining = 3
        uiState.triggerDb = source == .soundDetection ? uiState.currentDb : uiState.currentDb
        alarmPlayer.stop()

        countdownTask = Task { [weak self] in
            for seconds in stride(from: 3, through: 1, by: -1) {
                guard !Task.isCancelled else { return }
                await MainActor.run {
                    self?.uiState.countdownSecondsRemaining = seconds
                }
                try? await Task.sleep(nanoseconds: 1_000_000_000)
            }
            guard !Task.isCancelled else { return }
            await MainActor.run {
                self?.activatePendingEmergency()
            }
        }
    }

    private func activatePendingEmergency() {
        guard let mode = uiState.pendingEmergencyMode else { return }
        uiState.isSilenced = false
        uiState.alarmTriggered = mode == .loud
        uiState.activeEmergencyMode = mode
        uiState.pendingEmergencyMode = nil
        uiState.pendingTriggerSource = nil
        uiState.countdownSecondsRemaining = 0

        if mode == .loud {
            alarmPlayer.play(sound: loudSosSound)
        } else {
            alarmPlayer.stop()
        }

        Task {
            await scheduleLocalNotification(
                title: mode.title,
                body: mode == .loud ? "Full alarm active. Trusted-contact dispatch is in progress." : "Silent SOS active. Trusted-contact dispatch is in progress."
            )
            if EmergencyDecisionPolicy.shouldDispatchImmediately(triggerSource: currentEmergencyTriggerSource, aiSettings: aiSettings) {
                _ = await dispatchTextAlertOnce(sessionId: currentEmergencySessionId)
            }
            if currentEmergencySessionId == nil {
                await saveHistoryWithoutClip()
            }
        }
    }

    private func processCompletedClip(_ clip: CrisisAudioClip) async {
        guard processedClipIds.insert(clip.id).inserted else { return }

        let settings = contactSettings
        let location = await locationProvider.currentLocation()
        let currentDb = uiState.triggerDb ?? uiState.currentDb
        let payload = AlertMessageFormatter.formatPayload(settings: settings, currentDb: currentDb, location: location)

        var analysisText: String?
        var historyAiResultText = "AI analysis disabled."
        var dangerLevel: String?
        if aiSettings.isAiEnabled {
            aiState.modelStatus = .analyzing
            do {
                let result = try await aiAnalysisService.analyze(
                    clip: clip,
                    modelType: GemmaModelType(rawValue: aiSettings.modelType) ?? .e2b
                )
                analysisText = result.analysisText
                dangerLevel = result.dangerLevel
                historyAiResultText = result.analysisText
                aiState.lastAnalysisText = result.analysisText
                aiState.modelStatus = .ready
            } catch {
                historyAiResultText = "AI analysis unavailable: \(error.localizedDescription)"
                aiState.statusMessage = historyAiResultText
                aiState.modelStatus = .error
            }
        }

        let shouldSendSosAlert = aiSettings.isAiEnabled
            ? EmergencyDecisionPolicy.shouldDispatchAfterAnalysis(aiSettings: aiSettings, triggerSource: clip.triggerSource, analysisText: analysisText)
            : true
        let shouldSendFollowUpArtifacts = EmergencyDecisionPolicy.shouldBypassAiVerification(clip.triggerSource) || shouldSendSosAlert

        let shouldSendSosFromClipPath =
            !EmergencyDecisionPolicy.shouldDispatchImmediately(triggerSource: clip.triggerSource, aiSettings: aiSettings) &&
            shouldSendSosAlert

        let sosStatus: TextAlertDeliveryStatus
        if shouldSendSosFromClipPath {
            sosStatus = await dispatchTextAlertOnce(sessionId: clip.id, payload: payload)
        } else if EmergencyDecisionPolicy.shouldDispatchImmediately(triggerSource: clip.triggerSource, aiSettings: aiSettings) {
            sosStatus = textAlertDeliveryStatuses[clip.id] ?? .skipped
        } else {
            sosStatus = .skipped
        }

        let audioStatus: TextAlertDeliveryStatus
        if aiSettings.sendAudioToContacts &&
            shouldSendFollowUpArtifacts &&
            (settings.useTelegram || settings.useLine || settings.usePush) {
            let sent = await backendClient.sendAudioAlert(
                settings: await ensureAuthenticatedSettings(),
                userId: preferences.userId,
                clip: clip,
                sendTelegram: settings.useTelegram,
                sendLine: settings.useLine,
                sendPush: settings.usePush,
                eventId: clip.id.description,
                title: "SOS Triggered",
                message: payload.text
            )
            audioStatus = sent ? .sent : .failed
        } else {
            audioStatus = .skipped
        }

        let aiSummaryStatus: TextAlertDeliveryStatus
        if let analysisText,
           aiSettings.isAiEnabled,
           shouldSendFollowUpArtifacts,
           settings.useTelegram || settings.useLine || settings.usePush {
            let sent = await backendClient.sendAnalysisResult(
                settings: await ensureAuthenticatedSettings(),
                userId: preferences.userId,
                analysisText: analysisText
            )
            aiSummaryStatus = sent ? .sent : .failed
        } else {
            aiSummaryStatus = .skipped
        }

        if aiSettings.uploadTrainingTriggerAudio {
            uploadTrainingAudioPlaceholder(clip: clip, dangerLevel: dangerLevel)
        }

        let record = historyStore.saveRecord(
            isTest: false,
            sosText: payload.text,
            sosMessage: payload.historyMessage,
            sosDetails: payload.details,
            sourceAudioURL: clip.fileURL,
            aiResultText: historyAiResultText,
            sosAlertStatus: sosStatus,
            audioEvidenceStatus: audioStatus,
            aiSummaryAnalysisStatus: aiSummaryStatus
        )
        historyRecords = historyStore.loadRecords()
        latestAlertDeliverySummary = record.deliverySummary
    }

    private func saveHistoryWithoutClip() async {
        let location = await locationProvider.currentLocation()
        let payload = AlertMessageFormatter.formatPayload(
            settings: contactSettings,
            currentDb: uiState.triggerDb ?? uiState.currentDb,
            location: location
        )
        let status = textAlertDeliveryStatuses.values.first ?? .skipped
        let record = historyStore.saveRecord(
            isTest: false,
            sosText: payload.text,
            sosMessage: payload.historyMessage,
            sosDetails: payload.details,
            sourceAudioURL: nil,
            aiResultText: "No crisis audio clip was available.",
            sosAlertStatus: status,
            audioEvidenceStatus: .skipped,
            aiSummaryAnalysisStatus: .skipped
        )
        historyRecords = historyStore.loadRecords()
        latestAlertDeliverySummary = record.deliverySummary
    }

    private func dispatchTextAlertOnce(
        sessionId: Int64?,
        payload providedPayload: AlertMessagePayload? = nil
    ) async -> TextAlertDeliveryStatus {
        if let sessionId, let status = textAlertDeliveryStatuses[sessionId] {
            return status
        }
        let settings = await ensureAuthenticatedSettings()
        let payload: AlertMessagePayload
        if let providedPayload {
            payload = providedPayload
        } else {
            let location = await locationProvider.currentLocation()
            payload = AlertMessageFormatter.formatPayload(
                settings: settings,
                currentDb: uiState.triggerDb ?? uiState.currentDb,
                location: location
            )
        }

        var statuses: [TextAlertDeliveryStatus] = []
        if settings.useTelegram {
            statuses.append(await backendClient.sendTelegramAlert(settings: settings, userId: preferences.userId, message: payload.text) ? .sent : .failed)
        }
        if settings.useLine {
            statuses.append(await backendClient.sendLineAlert(settings: settings, userId: preferences.userId, message: payload.text) ? .sent : .failed)
        }
        if settings.usePush {
            statuses.append(await backendClient.sendPushAlert(
                settings: settings,
                userId: preferences.userId,
                eventId: sessionId?.description ?? UUID().uuidString,
                title: "SOS Triggered",
                message: payload.text,
                details: payload.details
            ) ? .sent : .failed)
        }
        if settings.useSms {
            statuses.append(.skipped)
        }
        if settings.usePhoneCall {
            openPhoneCallFallbackIfPossible(settings.phoneCallPhoneNumber)
            statuses.append(.skipped)
        }

        let status = mergeDeliveryStatuses(statuses)
        if let sessionId {
            textAlertDeliveryStatuses[sessionId] = status
        }
        latestAlertDeliverySummary = AlertDeliverySummary(
            id: sessionId?.description ?? UUID().uuidString,
            sosAlertStatus: status,
            audioEvidenceStatus: .skipped,
            aiSummaryAnalysisStatus: .skipped
        )
        return status
    }

    private func mergeDeliveryStatuses(_ statuses: [TextAlertDeliveryStatus]) -> TextAlertDeliveryStatus {
        if statuses.contains(.sent) { return .sent }
        if statuses.contains(.failed) { return .failed }
        return .skipped
    }

    private func ensureAuthenticatedSettings() async -> AlertContactSettings {
        var settings = contactSettings
        guard !settings.apiUrl.isEmpty else { return settings }
        if !preferences.backendDeviceToken.isEmpty {
            settings.apiToken = preferences.backendDeviceToken
            contactSettings = settings
            preferences.saveContactSettings(settings)
            return settings
        }
        guard !preferences.backendBootstrapApiToken.isEmpty else { return settings }
        settings.apiToken = preferences.backendBootstrapApiToken
        if let token = await backendClient.registerBackendDevice(
            settings: settings,
            userId: preferences.userId,
            bindCode: preferences.bindCode,
            installSecret: preferences.backendInstallSecret
        ) {
            preferences.saveBackendDeviceToken(token)
            settings.apiToken = token
            contactSettings = settings
            preferences.saveContactSettings(settings)
        }
        return settings
    }

    private func syncContacts(kind: String, operation: @escaping () async throws -> Void) async {
        do {
            if kind == "Telegram" { contactsSyncMessage = "Telegram sync in progress..." }
            if kind == "LINE" { lineSyncMessage = "LINE sync in progress..." }
            try await operation()
        } catch {
            if kind == "Telegram" {
                contactsSyncMessage = "Telegram sync failed: \(error.localizedDescription)"
            } else {
                lineSyncMessage = "LINE sync failed: \(error.localizedDescription)"
            }
        }
    }

    private func openPhoneCallFallbackIfPossible(_ number: String) {
        let sanitized = number.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !sanitized.isEmpty, let url = URL(string: "tel://\(sanitized)") else { return }
        UIApplication.shared.open(url)
    }

    private func scheduleLocalNotification(title: String, body: String) async {
        let content = UNMutableNotificationContent()
        content.title = title
        content.body = body
        content.sound = .default
        let request = UNNotificationRequest(identifier: UUID().uuidString, content: content, trigger: nil)
        try? await UNUserNotificationCenter.current().add(request)
    }

    private func uploadTrainingAudioPlaceholder(clip: CrisisAudioClip, dangerLevel: String?) {
        // Android queues opt-in training audio. The iOS backend endpoint can be wired here once
        // the App Store privacy disclosure and server retention policy are finalized.
    }
}

enum ContactMethod {
    case line
    case telegram
    case push
}
