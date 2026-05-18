package app.aurorasecurity.security

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.telephony.SmsManager
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal class NoiseAlarmAlertDispatcher(
    private val context: Context,
    private val alarmPreferences: AlarmPreferences,
    private val locationSnapshotProvider: LocationSnapshotProvider,
    private val serviceScope: CoroutineScope,
    private val backendApi: TelegramBackendApi = TelegramBackendApi(),
    private val pushBackendApi: PushBackendApi = PushBackendApi(),
) {
    private val flushMutex = Mutex()

    fun dispatchExternalAlerts(
        state: NoiseAlarmUiState,
        emergencySessionId: Long?,
    ) {
        serviceScope.launch {
            dispatchExternalAlertsForStatus(
                state = state,
                emergencySessionId = emergencySessionId,
            )
        }
    }

    suspend fun dispatchExternalAlertsForStatus(
        state: NoiseAlarmUiState,
        emergencySessionId: Long?,
    ): TextAlertDeliveryStatus {
        val contactSettings = alarmPreferences.getAlertContactSettings()
        if (!contactSettings.useTelegram && !contactSettings.usePush && !contactSettings.useSms) {
            return TextAlertDeliveryStatus.Skipped
        }
        val location = locationSnapshotProvider.getCurrentLocation()
        val alertMessage = AlertMessageFormatter.format(contactSettings, state.currentDb, location)
        val userId = alarmPreferences.getUserId()
        val sendResults = mutableListOf<Boolean>()

        if (contactSettings.useTelegram) {
            sendResults += sendTelegramAlert(
                settings = contactSettings,
                userId = userId,
                message = alertMessage,
            )
        }

        if (contactSettings.usePush) {
            sendResults += sendPushAlert(
                settings = contactSettings,
                userId = userId,
                sessionId = emergencySessionId ?: System.currentTimeMillis(),
                message = alertMessage,
            )
        }

        if (contactSettings.useSms) {
            sendResults += sendSmsAlert(
                settings = contactSettings,
                message = alertMessage,
            )
        }

        return if (sendResults.any { it }) {
            TextAlertDeliveryStatus.Sent
        } else {
            TextAlertDeliveryStatus.Failed
        }
    }

    suspend fun sendAudioAlertForStatus(
        userId: String,
        settings: AlertContactSettings,
        clip: CrisisAudioClip,
    ): TextAlertDeliveryStatus {
        if (!settings.useTelegram) return TextAlertDeliveryStatus.Skipped
        val filename = clip.file.name.let { name ->
            if (name.endsWith(".m4a", ignoreCase = true)) name
            else name.substringBeforeLast('.') + ".m4a"
        }
        val payload = PendingAudioPayload(
            userId = userId,
            apiUrl = settings.apiUrl,
            apiToken = settings.apiToken,
            filePath = clip.file.absolutePath,
            filename = filename,
            caption = context.getString(R.string.notification_audio_caption),
        )
        val sent = backendApi.sendPendingAudio(payload)
        if (!sent) alarmPreferences.savePendingAudio(payload)
        return if (sent) TextAlertDeliveryStatus.Sent else TextAlertDeliveryStatus.Failed
    }

    suspend fun sendAiSummaryForStatus(
        userId: String,
        settings: AlertContactSettings,
        sessionId: Long,
        analysisText: String,
    ): TextAlertDeliveryStatus {
        if (!settings.useTelegram && !settings.usePush && !settings.useSms) {
            return TextAlertDeliveryStatus.Skipped
        }

        val sendResults = mutableListOf<Boolean>()
        if (settings.useTelegram) {
            val payload = PendingAnalysisPayload(
                userId = userId,
                apiUrl = settings.apiUrl,
                apiToken = settings.apiToken,
                analysisText = analysisText,
            )
            val sent = backendApi.sendPendingAnalysis(payload)
            if (!sent) alarmPreferences.savePendingAnalysis(payload)
            sendResults += sent
        }

        if (settings.usePush) {
            val payload = PendingPushAlertPayload(
                userId = userId,
                apiUrl = settings.apiUrl,
                apiToken = settings.apiToken,
                eventId = buildPushEventId(userId, sessionId),
                messageType = "ai_analysis",
                title = context.getString(R.string.notification_ai_analysis_result),
                message = formatAiAnalysisMessage(analysisText),
            )
            val sent = pushBackendApi.sendAlert(payload)
            if (!sent) alarmPreferences.savePendingPushAlert(payload)
            sendResults += sent
        }

        if (settings.useSms) {
            sendResults += sendSmsAnalysisMessage(settings, analysisText)
        }

        return if (sendResults.any { it }) {
            TextAlertDeliveryStatus.Sent
        } else {
            TextAlertDeliveryStatus.Failed
        }
    }

    fun queueAudioAlert(
        userId: String,
        settings: AlertContactSettings,
        clip: CrisisAudioClip,
    ) {
        val filename = clip.file.name.let { name ->
            if (name.endsWith(".m4a", ignoreCase = true)) name
            else name.substringBeforeLast('.') + ".m4a"
        }
        alarmPreferences.savePendingAudio(
            PendingAudioPayload(
                userId = userId,
                apiUrl = settings.apiUrl,
                apiToken = settings.apiToken,
                filePath = clip.file.absolutePath,
                filename = filename,
                caption = context.getString(R.string.notification_audio_caption),
            ),
        )
        flushPendingMessagesAsync()
    }

    fun queueTelegramAnalysis(
        userId: String,
        settings: AlertContactSettings,
        analysisText: String,
    ) {
        alarmPreferences.savePendingAnalysis(
            PendingAnalysisPayload(
                userId = userId,
                apiUrl = settings.apiUrl,
                apiToken = settings.apiToken,
                analysisText = analysisText,
            ),
        )
        flushPendingMessagesAsync()
    }

    fun queueTrainingAudioUpload(
        userId: String,
        settings: AlertContactSettings,
        clip: CrisisAudioClip,
        dangerLevel: String?,
    ) {
        alarmPreferences.savePendingTrainingAudio(
            PendingTrainingAudioPayload(
                userId = userId,
                apiUrl = settings.apiUrl,
                apiToken = settings.apiToken,
                wavFilePath = clip.wavFile.absolutePath,
                filename = clip.wavFile.name.ifBlank { "crisis-training.wav" },
                triggerSource = clip.triggerSource.name,
                dangerLevel = dangerLevel,
                capturedAtEpochMs = clip.wavFile.lastModified(),
                sampleRateHz = 16_000,
                durationMs = 5_000,
            ),
        )
        flushPendingMessagesAsync()
    }

    fun queuePushAnalysis(
        userId: String,
        settings: AlertContactSettings,
        sessionId: Long,
        analysisText: String,
    ) {
        alarmPreferences.savePendingPushAlert(
            PendingPushAlertPayload(
                userId = userId,
                apiUrl = settings.apiUrl,
                apiToken = settings.apiToken,
                eventId = buildPushEventId(userId, sessionId),
                messageType = "ai_analysis",
                title = context.getString(R.string.notification_ai_analysis_result),
                message = formatAiAnalysisMessage(analysisText),
            ),
        )
        flushPendingMessagesAsync()
    }

    fun sendSmsAnalysisResult(
        settings: AlertContactSettings,
        analysisText: String,
    ) {
        sendSmsAnalysisMessage(settings, analysisText)
    }

    private fun sendSmsAnalysisMessage(
        settings: AlertContactSettings,
        analysisText: String,
    ): Boolean {
        val message = formatAiAnalysisMessage(analysisText)
        val primarySent = sendSmsMessage(settings.emergencyPhoneNumber.trim(), message)
        val secondarySent = if (settings.secondaryPhoneNumber.isNotBlank()) {
            sendSmsMessage(settings.secondaryPhoneNumber.trim(), message)
        } else {
            false
        }
        return primarySent || secondarySent
    }

    suspend fun flushPendingMessages() {
        flushMutex.withLock {
            val pendingAlerts = alarmPreferences.getPendingAlerts()
            if (pendingAlerts.isNotEmpty()) {
                alarmPreferences.clearPendingAlerts()
                for (payload in pendingAlerts) {
                    val sent = backendApi.sendPendingAlert(payload)
                    if (!sent) alarmPreferences.savePendingAlert(payload)
                }
            }

            val pendingAudios = alarmPreferences.getPendingAudios()
            if (pendingAudios.isNotEmpty()) {
                alarmPreferences.clearPendingAudios()
                for (payload in pendingAudios) {
                    val sent = backendApi.sendPendingAudio(payload)
                    if (!sent) alarmPreferences.savePendingAudio(payload)
                }
            }

            val pendingAnalyses = alarmPreferences.getPendingAnalyses()
            if (pendingAnalyses.isNotEmpty()) {
                alarmPreferences.clearPendingAnalyses()
                for (payload in pendingAnalyses) {
                    val sent = backendApi.sendPendingAnalysis(payload)
                    if (!sent) alarmPreferences.savePendingAnalysis(payload)
                }
            }

            val pendingTrainingAudios = alarmPreferences.getPendingTrainingAudios()
            if (pendingTrainingAudios.isNotEmpty()) {
                alarmPreferences.clearPendingTrainingAudios()
                for (payload in pendingTrainingAudios) {
                    val sent = backendApi.sendPendingTrainingAudio(payload)
                    if (!sent) alarmPreferences.savePendingTrainingAudio(payload)
                }
            }

            val pendingPushAlerts = alarmPreferences.getPendingPushAlerts()
            if (pendingPushAlerts.isNotEmpty()) {
                alarmPreferences.clearPendingPushAlerts()
                for (payload in pendingPushAlerts) {
                    val sent = pushBackendApi.sendAlert(payload)
                    if (!sent) alarmPreferences.savePendingPushAlert(payload)
                }
            }
        }
    }

    fun flushPendingMessagesAsync() {
        serviceScope.launch {
            flushPendingMessages()
        }
    }

    fun aiAnalysisFallbackMessage(): String {
        return context.getString(R.string.notification_ai_fallback)
    }

    fun unavailableAiAnalysisMessage(): String {
        val aiState = GemmaAudioAnalysisManager.uiState.value
        return when (aiState.modelStatus) {
            AiModelStatus.NotDownloaded ->
                "AI analysis unavailable. Download and initialize the model first."
            AiModelStatus.Downloaded ->
                "AI analysis unavailable. The model is downloaded, but the AI engine is not initialized yet."
            AiModelStatus.Downloading ->
                "AI analysis unavailable. The model is still downloading."
            AiModelStatus.Initializing ->
                "AI analysis unavailable. The AI engine is still initializing."
            AiModelStatus.Error ->
                "AI analysis unavailable. ${aiState.statusMessage}"
            AiModelStatus.Ready ->
                "AI analysis could not be completed."
        }
    }

    private suspend fun sendTelegramAlert(
        settings: AlertContactSettings,
        userId: String,
        message: String,
    ): Boolean {
        val payload = PendingAlertPayload(
            userId = userId,
            apiUrl = settings.apiUrl,
            apiToken = settings.apiToken,
            message = message,
        )
        val sent = runCatching { backendApi.sendPendingAlert(payload) }.getOrDefault(false)
        if (!sent) alarmPreferences.savePendingAlert(payload)
        return sent
    }

    private suspend fun sendPushAlert(
        settings: AlertContactSettings,
        userId: String,
        sessionId: Long,
        message: String,
    ): Boolean {
        val payload = PendingPushAlertPayload(
            userId = userId,
            apiUrl = settings.apiUrl,
            apiToken = settings.apiToken,
            eventId = buildPushEventId(
                userId = userId,
                sessionId = sessionId,
            ),
            messageType = "sos",
            title = context.getString(R.string.notification_sos_triggered),
            message = message,
        )
        val sent = runCatching { pushBackendApi.sendAlert(payload) }.getOrDefault(false)
        if (!sent) alarmPreferences.savePendingPushAlert(payload)
        return sent
    }

    private fun sendSmsAlert(
        settings: AlertContactSettings,
        message: String,
    ): Boolean {
        val primarySent = sendSmsMessage(settings.emergencyPhoneNumber.trim(), message)
        val secondarySent = if (settings.secondaryPhoneNumber.isNotBlank()) {
            sendSmsMessage(settings.secondaryPhoneNumber.trim(), message)
        } else {
            false
        }
        return primarySent || secondarySent
    }

    private fun sendSmsMessage(
        recipient: String,
        message: String,
    ): Boolean {
        if (recipient.isBlank()) return false
        val hasSmsPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.SEND_SMS,
        ) == PackageManager.PERMISSION_GRANTED
        if (!hasSmsPermission) return false

        val smsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(SmsManager::class.java)
        } else {
            @Suppress("DEPRECATION")
            SmsManager.getDefault()
        } ?: return false

        val parts = smsManager.divideMessage(message)
        if (parts.isEmpty()) return false
        return runCatching {
            smsManager.sendMultipartTextMessage(recipient, null, parts, null, null)
        }.isSuccess
    }

    private fun formatAiAnalysisMessage(analysisText: String): String {
        return context.getString(R.string.notification_ai_audio_result, analysisText)
    }

    private fun buildPushEventId(userId: String, sessionId: Long): String {
        return "$userId-$sessionId"
    }
}
