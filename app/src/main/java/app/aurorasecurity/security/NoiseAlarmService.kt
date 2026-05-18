package app.aurorasecurity.security

import android.Manifest
import android.app.Notification
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.os.IBinder
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Instant
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

class NoiseAlarmService : Service(), SensorEventListener {
    private val monitor = NoiseMonitor()
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var alarmPlayer: AlarmPlayer
    private lateinit var alarmVibrator: AlarmVibrator
    private lateinit var alarmPreferences: AlarmPreferences
    private lateinit var locationSnapshotProvider: LocationSnapshotProvider
    private lateinit var alertDispatcher: NoiseAlarmAlertDispatcher
    private lateinit var crisisAudioRecorder: CrisisAudioRecorder
    private var telegramAlertSentForCurrentAlarm = false
    private var callFallbackShownForCurrentAlarm = false
    private var lastNotificationKey: String? = null
    private var countdownJob: Job? = null
    private var currentEmergencySessionId: Long? = null
    private var currentEmergencyTriggerSource: EmergencyTriggerSource? = null
    private val completedAudioClips = mutableMapOf<Long, CrisisAudioClip>()
    private val processedAudioSessions = mutableSetOf<Long>()
    private val textAlertSentSessions = mutableSetOf<Long>()
    private val textAlertDeliveryStatuses = ConcurrentHashMap<Long, TextAlertDeliveryStatus>()
    private val historyRecordIdsBySession = ConcurrentHashMap<Long, String>()
    private lateinit var sensorManager: SensorManager
    private lateinit var notificationManager: NotificationManager
    private lateinit var notificationFactory: NoiseAlarmNotificationFactory
    private var lastShakeDetectedAtMs: Long = 0L
    private val SHAKE_MEMORY_MS = 2_000L
    private lateinit var connectivityManager: ConnectivityManager
    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            alertDispatcher.flushPendingMessagesAsync()
        }
    }

    override fun onCreate() {
        super.onCreate()
        alarmPlayer = AlarmPlayer(this)
        alarmVibrator = AlarmVibrator(this)
        alarmPreferences = AlarmPreferences(this)
        locationSnapshotProvider = LocationSnapshotProvider(this)
        alertDispatcher = NoiseAlarmAlertDispatcher(
            context = this,
            alarmPreferences = alarmPreferences,
            locationSnapshotProvider = locationSnapshotProvider,
            serviceScope = serviceScope,
        )
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationFactory = NoiseAlarmNotificationFactory(this)
        crisisAudioRecorder = CrisisAudioRecorder(this) { clip ->
            serviceScope.launch {
                onCrisisClipReady(clip)
            }
        }
        notificationFactory.createNotificationChannels(notificationManager)
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        registerShakeListener()
        connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        connectivityManager.registerNetworkCallback(networkRequest, networkCallback)
        // Flush any messages that failed before last shutdown
        alertDispatcher.flushPendingMessagesAsync()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_SILENCE -> {
                countdownJob?.cancel()
                countdownJob = null
                NoiseAlarmController.stopAlarm()
                alarmPlayer.stop()
                alarmVibrator.stopAll()
                telegramAlertSentForCurrentAlarm = false
                callFallbackShownForCurrentAlarm = false
                crisisAudioRecorder.cancelCapture(currentEmergencySessionId)
                currentEmergencySessionId = null
                currentEmergencyTriggerSource = null
                completedAudioClips.clear()
                processedAudioSessions.clear()
                textAlertSentSessions.clear()
                dismissCallFallbackNotification()
                lastNotificationKey = null
                updateNotification()
                return START_STICKY
            }

            ACTION_TRIGGER_SILENT_SOS -> {
                startAsForeground()
                startCountdown(
                    mode = EmergencyMode.Silent,
                    source = EmergencyTriggerSource.SilentSosButton,
                )
                return START_STICKY
            }

            ACTION_TRIGGER_LOUD_SOS -> {
                startAsForeground()
                startCountdown(
                    mode = EmergencyMode.Loud,
                    source = EmergencyTriggerSource.LoudSosButton,
                )
                return START_STICKY
            }

            ACTION_START, null -> {
                if (alarmPreferences.getMonitoringPaused()) {
                    stopSelf()
                    return START_NOT_STICKY
                }
                startMonitoring()
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        monitor.stop()
        unregisterShakeListener()
        runCatching { connectivityManager.unregisterNetworkCallback(networkCallback) }
        alarmPlayer.release()
        alarmVibrator.stopAll()
        serviceScope.cancel()
        NoiseAlarmController.setMonitoring(false)
        lastNotificationKey = null
        countdownJob = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startMonitoring() {
        startAsForeground()
        NoiseAlarmController.setMonitoring(true)
        monitor.start(
            onLevelChanged = { db ->
            NoiseAlarmController.updateLevel(db)
            val state = NoiseAlarmController.uiState.value
            if (
                state.isThresholdExceeded &&
                state.pendingEmergencyMode == null &&
                state.activeEmergencyMode == null
            ) {
                startCountdown(
                    mode = EmergencyMode.Loud,
                    source = EmergencyTriggerSource.SoundDetection,
                )
                return@start
            }
            if (state.pendingEmergencyMode != null) {
                alarmPlayer.stop()
                alarmVibrator.stopHeartbeat()
            } else if (state.alarmTriggered) {
                alarmPlayer.play()
                alarmVibrator.startHeartbeatPattern()
                if (shouldDispatchImmediately()) {
                    dispatchTextAlertOnce(state)
                }
            } else if (state.activeEmergencyMode == EmergencyMode.Silent) {
                alarmPlayer.stop()
                alarmVibrator.stopHeartbeat()
            } else {
                alarmPlayer.stop()
                alarmVibrator.stopHeartbeat()
                telegramAlertSentForCurrentAlarm = false
                callFallbackShownForCurrentAlarm = false
                currentEmergencyTriggerSource = null
                dismissCallFallbackNotification()
            }
            updateNotification()
            },
            onPcmFrame = { frame, samplesRead ->
                crisisAudioRecorder.appendFrame(frame, samplesRead)
            },
        )
    }

    private fun startCountdown(mode: EmergencyMode, source: EmergencyTriggerSource) {
        val state = NoiseAlarmController.uiState.value
        if (state.pendingEmergencyMode != null || state.activeEmergencyMode != null) return

        countdownJob?.cancel()
        countdownJob = null
        completedAudioClips.clear()
        processedAudioSessions.clear()
        textAlertSentSessions.clear()
        textAlertDeliveryStatuses.clear()
        historyRecordIdsBySession.clear()
        currentEmergencySessionId = crisisAudioRecorder.startCapture(source, mode)
        currentEmergencyTriggerSource = source
        NoiseAlarmController.startCountdown(mode, source)
        alarmPlayer.stop()
        alarmVibrator.stopHeartbeat()
        telegramAlertSentForCurrentAlarm = false
        callFallbackShownForCurrentAlarm = false
        dismissCallFallbackNotification()
        updateNotification()
        countdownJob = serviceScope.launch {
            for (secondsRemaining in COUNTDOWN_SECONDS downTo 1) {
                NoiseAlarmController.updateCountdown(secondsRemaining)
                updateNotification()
                delay(1_000)
            }
            countdownJob = null
            NoiseAlarmController.activatePendingEmergency()
            val activatedState = NoiseAlarmController.uiState.value
            when (activatedState.activeEmergencyMode) {
                EmergencyMode.Silent -> {
                    alarmPlayer.stop()
                    alarmVibrator.stopHeartbeat()
                }

                EmergencyMode.Loud -> {
                    alarmPlayer.play()
                    alarmVibrator.startHeartbeatPattern()
                }

                null -> Unit
            }
            if (shouldDispatchImmediately()) {
                dispatchTextAlertOnce(activatedState)
            }
            processCompletedClipIfReady(currentEmergencySessionId)
            updateNotification()
        }
    }

    private fun registerShakeListener() {
        val accel = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        if (accel != null) {
            sensorManager.registerListener(this, accel, SensorManager.SENSOR_DELAY_UI)
        }
        val settings = alarmPreferences.getMovementSettings()
        NoiseAlarmController.setMovementSettings(settings.useShakeDetection, settings.shakeThresholdG)
    }

    private fun unregisterShakeListener() {
        sensorManager.unregisterListener(this)
        NoiseAlarmController.updateShakeLevel(1.0f, false)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {
            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]
            val magnitude = Math.sqrt((x * x + y * y + z * z).toDouble()).toFloat()
            val gForce = magnitude / 9.81f
            
            val settings = alarmPreferences.getMovementSettings()
            if (gForce >= settings.shakeThresholdG) {
                lastShakeDetectedAtMs = System.currentTimeMillis()
            }
            
            val isShaking = (System.currentTimeMillis() - lastShakeDetectedAtMs) < SHAKE_MEMORY_MS
            NoiseAlarmController.updateShakeLevel(gForce, isShaking)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun isManualSosTrigger(triggerSource: EmergencyTriggerSource?): Boolean {
        return triggerSource == EmergencyTriggerSource.SilentSosButton ||
            triggerSource == EmergencyTriggerSource.LoudSosButton
    }

    private fun shouldBypassAiVerification(triggerSource: EmergencyTriggerSource?): Boolean {
        return isManualSosTrigger(triggerSource)
    }

    private fun shouldDispatchImmediately(
        triggerSource: EmergencyTriggerSource? = currentEmergencyTriggerSource,
        aiSettings: AiSettings = alarmPreferences.getAiSettings(),
    ): Boolean {
        return shouldBypassAiVerification(triggerSource) ||
            aiSettings.sosDispatchMode == SosDispatchMode.Immediate ||
            !aiSettings.enableAiAnalysis
    }

    private fun parseDangerLevelFromAnalysis(analysisText: String?): String? {
        if (analysisText.isNullOrBlank()) return null
        return analysisText.lineSequence()
            .map { it.trim().removePrefix("-").trim() }
            .firstOrNull { it.startsWith("Danger Level:", ignoreCase = true) }
            ?.substringAfter(":", "")
            ?.trim()
            ?.takeIf { it.isNotBlank() }
    }

    private fun shouldDispatchAfterAnalysis(
        aiSettings: AiSettings,
        triggerSource: EmergencyTriggerSource?,
        analysisText: String?,
    ): Boolean {
        if (shouldBypassAiVerification(triggerSource)) return true
        if (aiSettings.sosDispatchMode == SosDispatchMode.Immediate || !aiSettings.enableAiAnalysis) return true
        val normalizedLevel = parseDangerLevelFromAnalysis(analysisText)
            ?.lowercase()
            ?.trim()
        return normalizedLevel == null ||
            normalizedLevel.startsWith("danger") ||
            normalizedLevel.startsWith("high")
    }

    private fun sendExternalAlerts(state: NoiseAlarmUiState) {
        val contactSettings = alarmPreferences.getAlertContactSettings()
        alertDispatcher.dispatchExternalAlerts(state, currentEmergencySessionId)
        showCallFallbackIfNeeded(contactSettings)
    }

    private fun showCallFallbackIfNeeded(contactSettings: AlertContactSettings) {
        if (
            contactSettings.usePhoneCall &&
            contactSettings.emergencyPhoneNumber.isNotBlank() &&
            !AlarmAppVisibility.isInForeground &&
            !callFallbackShownForCurrentAlarm
        ) {
            callFallbackShownForCurrentAlarm = true
            showCallFallbackNotification(contactSettings.emergencyPhoneNumber)
        }
    }

    private fun dispatchTextAlertOnce(state: NoiseAlarmUiState) {
        val contactSettings = alarmPreferences.getAlertContactSettings()
        serviceScope.launch {
            dispatchTextAlertForResult(state)
        }
        showCallFallbackIfNeeded(contactSettings)
    }

    private suspend fun dispatchTextAlertForResult(state: NoiseAlarmUiState): TextAlertDeliveryStatus {
        val sessionId = currentEmergencySessionId
        if (sessionId != null && !textAlertSentSessions.add(sessionId)) {
            return awaitTextAlertStatus(sessionId) ?: TextAlertDeliveryStatus.Failed
        }
        telegramAlertSentForCurrentAlarm = true
        val status = alertDispatcher.dispatchExternalAlertsForStatus(
            state = state,
            emergencySessionId = sessionId,
        )
        if (sessionId != null) {
            textAlertDeliveryStatuses[sessionId] = status
            historyRecordIdsBySession[sessionId]?.let { recordId ->
                HistoryManager(this@NoiseAlarmService).updateTextAlertDeliveryStatus(
                    recordId = recordId,
                    status = status,
                )
            }
        }
        return status
    }

    private suspend fun awaitTextAlertStatus(sessionId: Long): TextAlertDeliveryStatus? {
        textAlertDeliveryStatuses[sessionId]?.let { return it }
        repeat(12) {
            delay(250)
            textAlertDeliveryStatuses[sessionId]?.let { return it }
        }
        return null
    }

    private suspend fun onCrisisClipReady(clip: CrisisAudioClip) {
        completedAudioClips[clip.sessionId] = clip
        if (clip.sessionId == currentEmergencySessionId) {
            processCompletedClipIfReady(clip.sessionId)
        }
    }

    private fun processCompletedClipIfReady(sessionId: Long?) {
        val resolvedSessionId = sessionId ?: return
        if (!processedAudioSessions.add(resolvedSessionId)) return
        val clip = completedAudioClips.remove(resolvedSessionId) ?: run {
            processedAudioSessions.remove(resolvedSessionId)
            return
        }

        serviceScope.launch(Dispatchers.IO) {
            val contactSettings = alarmPreferences.getAlertContactSettings()
            val aiSettings = alarmPreferences.getAiSettings()
            val userId = alarmPreferences.getUserId()

            // --- Run AI analysis (on-device, no network needed) ---
            var analysisText: String? = null
            var historyAiResultText = "AI analysis disabled."
            if (aiSettings.enableAiAnalysis) {
                val currentModel = GemmaModelType.fromName(aiSettings.modelType)
                GemmaAudioAnalysisManager.refreshState(this@NoiseAlarmService, currentModel)
                historyAiResultText = if (GemmaAudioAnalysisManager.isModelDownloaded(this@NoiseAlarmService, currentModel)) {
                    analysisText = runCatching {
                        GemmaAudioAnalysisManager.analyzeClip(this@NoiseAlarmService, clip, currentModel)
                    }.getOrNull()
                    analysisText ?: alertDispatcher.unavailableAiAnalysisMessage()
                } else {
                    alertDispatcher.unavailableAiAnalysisMessage()
                }
            }

            val shouldSendSosAlert = if (aiSettings.enableAiAnalysis) {
                shouldDispatchAfterAnalysis(aiSettings, clip.triggerSource, analysisText)
            } else {
                true
            }
            val shouldSendFollowUpArtifacts =
                shouldBypassAiVerification(clip.triggerSource) || shouldSendSosAlert

            val state = NoiseAlarmController.uiState.value
            val sosText = AlertMessageFormatter.format(contactSettings, state.triggerDb ?: state.currentDb, locationSnapshotProvider.getCurrentLocation())

            val shouldSendSosFromClipPath =
                !shouldDispatchImmediately(
                    triggerSource = clip.triggerSource,
                    aiSettings = aiSettings,
                ) &&
                    shouldSendSosAlert

            val sosAlertStatus = if (shouldSendSosFromClipPath) {
                dispatchTextAlertForResult(state)
            } else if (shouldDispatchImmediately(triggerSource = clip.triggerSource, aiSettings = aiSettings)) {
                awaitTextAlertStatus(resolvedSessionId) ?: TextAlertDeliveryStatus.Skipped
            } else {
                TextAlertDeliveryStatus.Skipped
            }

            val audioEvidenceStatus =
                if (contactSettings.useTelegram && aiSettings.sendAudioToContacts && shouldSendFollowUpArtifacts) {
                    alertDispatcher.sendAudioAlertForStatus(
                        userId = userId,
                        settings = contactSettings,
                        clip = clip,
                    )
                } else {
                    TextAlertDeliveryStatus.Skipped
                }

            val aiSummaryAnalysisStatus =
                if (
                    aiSettings.enableAiAnalysis &&
                    analysisText != null &&
                    shouldSendFollowUpArtifacts
                ) {
                    alertDispatcher.sendAiSummaryForStatus(
                        userId = userId,
                        settings = contactSettings,
                        sessionId = clip.sessionId,
                        analysisText = analysisText,
                    )
                } else {
                    TextAlertDeliveryStatus.Skipped
                }

            // --- Save history record ---
            val historyManager = HistoryManager(this@NoiseAlarmService)
            val recordId = historyManager.saveRecord(
                isTest = false,
                sosText = sosText,
                sourceAudioFile = clip.file,
                aiResultText = historyAiResultText,
                textAlertDeliveryStatus = sosAlertStatus,
                sosAlertStatus = sosAlertStatus,
                audioEvidenceStatus = audioEvidenceStatus,
                aiSummaryAnalysisStatus = aiSummaryAnalysisStatus,
            )
            historyRecordIdsBySession[resolvedSessionId] = recordId

            if (shouldSendSosFromClipPath) {
                showCallFallbackIfNeeded(contactSettings)
            }

            val shouldUploadTrainingAudio =
                aiSettings.uploadTrainingTriggerAudio &&
                    clip.triggerSource == EmergencyTriggerSource.SoundDetection
            val trainingDangerLevel = if (aiSettings.enableAiAnalysis) {
                parseDangerLevelFromAnalysis(analysisText)
            } else {
                null
            }

            if (shouldUploadTrainingAudio) {
                alertDispatcher.queueTrainingAudioUpload(
                    userId = userId,
                    settings = contactSettings,
                    clip = clip,
                    dangerLevel = trainingDangerLevel,
                )
            } else {
                clip.wavFile.delete()
            }

            if (contactSettings.usePush && aiSettings.enableAiAnalysis && shouldSendFollowUpArtifacts && analysisText == null) {
                alertDispatcher.queuePushAnalysis(
                    userId = userId,
                    settings = contactSettings,
                    sessionId = clip.sessionId,
                    analysisText = analysisText ?: alertDispatcher.aiAnalysisFallbackMessage(),
                )
            }

            if (contactSettings.useSms && aiSettings.enableAiAnalysis && shouldSendFollowUpArtifacts && analysisText == null) {
                launch {
                    alertDispatcher.sendSmsAnalysisResult(
                        settings = contactSettings,
                        analysisText = analysisText ?: alertDispatcher.aiAnalysisFallbackMessage(),
                    )
                }
            }
        }
    }

    private fun startAsForeground() {
        val notification = notificationFactory.buildServiceNotification(NoiseAlarmController.uiState.value)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            startForeground(
                MONITOR_NOTIFICATION_ID,
                notification,
                notificationFactory.resolveForegroundServiceType(
                    hasLocationPermission = locationSnapshotProvider.hasLocationPermission(),
                ),
            )
        } else {
            startForeground(MONITOR_NOTIFICATION_ID, notification)
        }
    }

    private fun updateNotification() {
        val currentKey = buildNoiseAlarmNotificationKey(NoiseAlarmController.uiState.value)
        if (currentKey == lastNotificationKey) return
        lastNotificationKey = currentKey
        notificationManager.notify(
            MONITOR_NOTIFICATION_ID,
            notificationFactory.buildServiceNotification(NoiseAlarmController.uiState.value),
        )
    }

    private fun showCallFallbackNotification(phoneNumber: String) {
        notificationManager.notify(
            CALL_FALLBACK_NOTIFICATION_ID,
            notificationFactory.buildCallFallbackNotification(phoneNumber),
        )
    }

    private fun dismissCallFallbackNotification() {
        notificationManager.cancel(CALL_FALLBACK_NOTIFICATION_ID)
    }

    companion object {
        const val ACTION_START = "app.aurorasecurity.security.action.START"
        const val ACTION_SILENCE = "app.aurorasecurity.security.action.SILENCE"
        const val ACTION_TRIGGER_SILENT_SOS = "app.aurorasecurity.security.action.TRIGGER_SILENT_SOS"
        const val ACTION_TRIGGER_LOUD_SOS = "app.aurorasecurity.security.action.TRIGGER_LOUD_SOS"
        private const val COUNTDOWN_SECONDS = 3
    }
}
