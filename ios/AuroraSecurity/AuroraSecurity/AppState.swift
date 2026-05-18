import AVFoundation
import Combine
import Foundation
import SwiftUI

@MainActor
final class AppState: ObservableObject {
    @Published var alarmState = NoiseAlarmState()
    @Published var contactSettings: AlertContactSettings
    @Published var aiSettings: AiSettings
    @Published var movementSettings: MovementSettings
    @Published var historyRecords: [HistoryRecord] = []
    @Published var permissionState = PermissionState()
    @Published var themeMode: AppThemeMode
    @Published var shouldShowOnboarding = false
    @Published var lastStatusMessage = "Ready"
    @Published var isDispatchingAlert = false
    @Published var aiFeatureState = AiFeatureState()

    private let preferencesStore = PreferencesStore()
    private let historyStore = HistoryStore()
    private let audioMonitor = AudioMonitor()
    private let crisisRecorder = CrisisAudioRecorder()
    private let locationProvider = LocationSnapshotProvider()
    private let alertDispatcher = AlertDispatcher()
    private let aiModelManager = GemmaLiteRTModelManager()
    private let notificationService = NotificationService()
    private let emergencyActionService = EmergencyActionService()
    private var countdownTask: Task<Void, Never>?
    private var cancellables = Set<AnyCancellable>()
    private var hasBootstrapped = false

    init() {
        contactSettings = preferencesStore.loadContactSettings()
        aiSettings = preferencesStore.loadAiSettings()
        movementSettings = preferencesStore.loadMovementSettings()
        themeMode = preferencesStore.loadThemeMode()
        alarmState.thresholdDb = preferencesStore.loadThresholdDb()
        aiModelManager.$state
            .receive(on: DispatchQueue.main)
            .sink { [weak self] state in
                Task { @MainActor in
                    self?.aiFeatureState = state
                }
            }
            .store(in: &cancellables)
        aiModelManager.refresh(selectedModel: GemmaLiteRTModel.fromSettings(aiSettings))
    }

    func bootstrap() {
        guard !hasBootstrapped else { return }
        hasBootstrapped = true
        shouldShowOnboarding = !preferencesStore.hasAcceptedSafetyNotice()
        historyRecords = historyStore.loadRecords()
        refreshPermissionState()
    }

    func acceptSafetyNotice() {
        preferencesStore.setAcceptedSafetyNotice(true)
        shouldShowOnboarding = false
    }

    func refreshPermissionState() {
        permissionState.microphone = audioMonitor.authorizationStatus
        permissionState.location = locationProvider.authorizationStatus
        Task {
            let notifications = await notificationService.authorizationStatus()
            await MainActor.run {
                self.permissionState.notifications = notifications
            }
        }
    }

    func requestMicrophone() {
        Task {
            let granted = await audioMonitor.requestPermission()
            await MainActor.run {
                self.permissionState.microphone = granted ? .granted : .denied
                self.lastStatusMessage = granted ? "Microphone ready" : "Microphone access denied"
            }
        }
    }

    func requestLocation() {
        locationProvider.requestAuthorization()
        refreshPermissionState()
    }

    func requestNotifications() {
        Task {
            let granted = await notificationService.requestAuthorization()
            await MainActor.run {
                self.permissionState.notifications = granted ? .granted : .denied
                self.lastStatusMessage = granted ? "Notifications ready" : "Notifications disabled"
            }
        }
    }

    func startMonitoring() {
        guard !alarmState.isMonitoring else { return }
        Task {
            await startMonitoringAfterPermission()
        }
    }

    private func startMonitoringAfterPermission() async {
        let granted = await audioMonitor.ensurePermission()
        permissionState.microphone = granted ? .granted : .denied
        guard granted else {
            lastStatusMessage = "Microphone access is required before protection can start"
            return
        }

        crisisRecorder.reset(sampleRate: AudioMonitor.sampleRate)
        let didStart = audioMonitor.start(
            onLevel: { [weak self] db in
                Task { @MainActor in
                    self?.handleLevelUpdate(db)
                }
            },
            onSamples: { [weak self] samples in
                Task { @MainActor in
                    self?.crisisRecorder.append(samples: samples)
                }
            }
        )
        if didStart {
            alarmState.isMonitoring = true
            lastStatusMessage = "Aurora is guarding"
        } else {
            alarmState.isMonitoring = false
            lastStatusMessage = "Unable to start microphone monitoring"
        }
    }

    func stopMonitoring() {
        audioMonitor.stop()
        countdownTask?.cancel()
        countdownTask = nil
        alarmState.isMonitoring = false
        alarmState.pendingEmergencyMode = nil
        alarmState.countdownSecondsRemaining = 0
        alarmState.alarmTriggered = false
        alarmState.activeEmergencyMode = nil
        lastStatusMessage = "Protection paused"
    }

    func updateThreshold(_ value: Double) {
        alarmState.thresholdDb = value
        preferencesStore.saveThresholdDb(value)
    }

    func saveContactSettings(_ settings: AlertContactSettings) {
        contactSettings = settings
        preferencesStore.saveContactSettings(settings)
    }

    func saveAiSettings(_ settings: AiSettings) {
        aiSettings = settings
        preferencesStore.saveAiSettings(settings)
        aiModelManager.refresh(selectedModel: GemmaLiteRTModel.fromSettings(settings))
    }

    func selectAiModel(_ model: GemmaLiteRTModel) {
        var settings = aiSettings
        settings.modelType = model.rawValue
        saveAiSettings(settings)
        aiModelManager.selectModel(model)
    }

    func downloadAiModel() {
        aiModelManager.downloadSelectedModel()
    }

    func initializeAiModel() {
        Task {
            await aiModelManager.initializeSelectedModel()
        }
    }

    func deleteAiModel() {
        aiModelManager.deleteSelectedModel()
    }

    func saveMovementSettings(_ settings: MovementSettings) {
        movementSettings = settings
        preferencesStore.saveMovementSettings(settings)
    }

    func saveThemeMode(_ mode: AppThemeMode) {
        themeMode = mode
        preferencesStore.saveThemeMode(mode)
    }

    func triggerSilentSOS() {
        prepareAudioCaptureForManualSOSIfNeeded()
        startCountdown(mode: .silent, source: .silentSosButton)
    }

    func triggerLoudSOS() {
        prepareAudioCaptureForManualSOSIfNeeded()
        startCountdown(mode: .loud, source: .loudSosButton)
    }

    func cancelEmergency() {
        countdownTask?.cancel()
        countdownTask = nil
        alarmState.stopAlarm()
        lastStatusMessage = "Emergency cancelled"
    }

    func openEmergencyPhoneFallback() {
        emergencyActionService.openPhone(contactSettings.emergencyPhoneNumber)
    }

    func openEmergencyMessageFallback() {
        let message = AlertMessageFormatter.format(
            settings: contactSettings,
            currentDb: alarmState.triggerDb ?? alarmState.currentDb,
            location: nil
        )
        emergencyActionService.openSMS(
            contactSettings.emergencyPhoneNumber,
            body: message
        )
    }

    private func handleLevelUpdate(_ db: Double) {
        alarmState.updateLevel(db)
        if alarmState.isThresholdExceeded && alarmState.pendingEmergencyMode == nil && alarmState.activeEmergencyMode == nil {
            startCountdown(mode: .loud, source: .soundDetection)
        }
    }

    private func prepareAudioCaptureForManualSOSIfNeeded() {
        if !alarmState.isMonitoring {
            crisisRecorder.reset(sampleRate: AudioMonitor.sampleRate)
        }
    }

    private func startCountdown(mode: EmergencyMode, source: EmergencyTriggerSource) {
        guard alarmState.pendingEmergencyMode == nil else { return }
        countdownTask?.cancel()
        alarmState.startCountdown(mode: mode, source: source)
        lastStatusMessage = mode == .silent ? "Silent SOS pending" : "Emergency countdown"

        countdownTask = Task { [weak self] in
            for seconds in stride(from: 3, through: 1, by: -1) {
                guard !Task.isCancelled else { return }
                await MainActor.run {
                    self?.alarmState.countdownSecondsRemaining = seconds
                }
                try? await Task.sleep(nanoseconds: 1_000_000_000)
            }
            guard !Task.isCancelled else { return }
            await MainActor.run {
                self?.activateEmergency()
            }
        }
    }

    private func activateEmergency() {
        countdownTask = nil
        alarmState.activatePendingEmergency()
        let clip = crisisRecorder.makeClip(
            triggerSource: alarmState.lastTriggerSource ?? .soundDetection,
            emergencyMode: alarmState.activeEmergencyMode ?? .loud
        )
        Task {
            await dispatchEmergency(clip: clip)
        }
    }

    private func dispatchEmergency(clip: CrisisAudioClip?) async {
        isDispatchingAlert = true
        defer { isDispatchingAlert = false }

        let location = await locationProvider.currentLocation()
        let aiSummary = await analyzeIfEnabled(clip: clip)
        let shouldSendSOS = shouldDispatchSOSAfterAnalysis(aiSummary)
        let result = await alertDispatcher.dispatch(
            settings: contactSettings,
            userId: preferencesStore.userId,
            currentDb: alarmState.triggerDb ?? alarmState.currentDb,
            location: location,
            clip: clip,
            aiSettings: aiSettings,
            aiSummary: aiSummary,
            shouldSendSOS: shouldSendSOS
        )

        if contactSettings.usePhoneCall {
            openEmergencyPhoneFallback()
        }

        if contactSettings.useSms {
            openEmergencyMessageFallback()
        }

        let record = HistoryRecord(
            type: .sos,
            triggerSource: alarmState.lastTriggerSource ?? .soundDetection,
            emergencyMode: alarmState.activeEmergencyMode ?? .loud,
            currentDb: alarmState.triggerDb ?? alarmState.currentDb,
            location: location,
            message: result.message,
            deliveryStatus: result.status,
            audioFilePath: clip?.fileURL.path,
            aiSummary: result.aiSummary
        )
        historyStore.save(record)
        historyRecords = historyStore.loadRecords()
        lastStatusMessage = result.status.displayName
        await notificationService.postLocalEmergencyNotification(status: result.status.displayName)
    }

    private func analyzeIfEnabled(clip: CrisisAudioClip?) async -> String? {
        guard aiSettings.enableAiAnalysis, let clip else { return nil }
        return await aiModelManager.analyze(clip: clip)
    }

    private func shouldDispatchSOSAfterAnalysis(_ analysisText: String?) -> Bool {
        guard aiSettings.enableAiAnalysis, aiSettings.sosDispatchMode == .aiFirst else {
            return true
        }
        guard let analysisText, !analysisText.isEmpty else {
            return true
        }
        let normalized = analysisText.lowercased()
        if normalized.contains("danger level: danger") || normalized.contains("danger level: high") {
            return true
        }
        return false
    }
}
