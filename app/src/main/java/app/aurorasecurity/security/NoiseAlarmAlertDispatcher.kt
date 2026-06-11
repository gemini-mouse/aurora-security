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
    private val lineBackendApi: LineBackendApi = LineBackendApi(),
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
        val contactSettings = getAuthenticatedContactSettings()
        val canUseBackend = contactSettings.hasBackendCredentials()
        val hasBackendContactMethod = canUseBackend &&
            (contactSettings.useLine || contactSettings.useTelegram || contactSettings.usePush)
        if (!hasBackendContactMethod && !contactSettings.useSms) {
            return TextAlertDeliveryStatus.Skipped
        }
        val location = locationSnapshotProvider.getCurrentLocation()
        val alertPayload = AlertMessageFormatter.formatPayload(contactSettings, state.currentDb, location)
        val alertMessage = alertPayload.text
        val userId = alarmPreferences.getUserId()
        val sendResults = mutableListOf<Boolean>()

        if (contactSettings.useLine && canUseBackend) {
            sendResults += sendLineAlert(
                settings = contactSettings,
                userId = userId,
                message = alertMessage,
            )
        }

        if (contactSettings.useTelegram && canUseBackend) {
            sendResults += sendTelegramAlert(
                settings = contactSettings,
                userId = userId,
                message = alertMessage,
            )
        }

        if (contactSettings.usePush && canUseBackend) {
            sendResults += sendPushAlert(
                settings = contactSettings,
                userId = userId,
                sessionId = emergencySessionId ?: System.currentTimeMillis(),
                alertPayload = alertPayload,
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
        sessionId: Long,
        sendToTelegram: Boolean = settings.useTelegram,
        sendToLine: Boolean = settings.useLine,
        sendToPush: Boolean = settings.usePush,
    ): TextAlertDeliveryStatus {
        if (!sendToTelegram && !sendToLine && !sendToPush) return TextAlertDeliveryStatus.Skipped
        val authenticatedSettings = getAuthenticatedContactSettings(settings)
        if (!authenticatedSettings.hasBackendCredentials()) return TextAlertDeliveryStatus.Skipped
        val audioFile = clip.file ?: return TextAlertDeliveryStatus.Skipped
        val filename = audioFile.name.let { name ->
            if (name.endsWith(".m4a", ignoreCase = true)) name
            else name.substringBeforeLast('.') + ".m4a"
        }
        val payload = PendingAudioPayload(
            userId = userId,
            apiUrl = settings.apiUrl,
            apiToken = settings.apiToken,
            filePath = audioFile.absolutePath,
            filename = filename,
            caption = context.getString(R.string.notification_audio_caption),
            sendToTelegram = sendToTelegram,
            sendToLine = sendToLine,
            sendToPush = sendToPush,
            pushEventId = buildPushEventId(userId, sessionId),
            pushTitle = context.getString(R.string.audio_evidence_title),
            pushMessage = context.getString(R.string.notification_audio_caption),
            durationMs = clip.durationMs,
        )
        val sent = backendApi.sendPendingAudio(payload)
        if (!sent) alarmPreferences.savePendingAudio(payload)
        return if (sent) TextAlertDeliveryStatus.Sent else TextAlertDeliveryStatus.Failed
    }

    suspend fun sendLineAudioWithAiSummaryForStatus(
        userId: String,
        settings: AlertContactSettings,
        clip: CrisisAudioClip,
        analysisText: String,
    ): TextAlertDeliveryStatus {
        if (!settings.useLine) return TextAlertDeliveryStatus.Skipped
        val authenticatedSettings = getAuthenticatedContactSettings(settings)
        if (!authenticatedSettings.hasBackendCredentials()) return TextAlertDeliveryStatus.Skipped
        val audioFile = clip.file ?: return TextAlertDeliveryStatus.Skipped
        val filename = audioFile.name.let { name ->
            if (name.endsWith(".m4a", ignoreCase = true)) name
            else name.substringBeforeLast('.') + ".m4a"
        }
        val payload = PendingLineAudioAnalysisPayload(
            userId = userId,
            apiUrl = authenticatedSettings.apiUrl,
            apiToken = authenticatedSettings.apiToken,
            filePath = audioFile.absolutePath,
            filename = filename,
            caption = context.getString(R.string.notification_audio_caption),
            analysisText = formatAiAnalysisMessage(analysisText),
            durationMs = clip.durationMs,
        )
        val sent = lineBackendApi.sendPendingAudioAnalysis(payload)
        if (!sent) alarmPreferences.savePendingLineAudioAnalysis(payload)
        return if (sent) TextAlertDeliveryStatus.Sent else TextAlertDeliveryStatus.Failed
    }

    suspend fun sendAiSummaryForStatus(
        userId: String,
        settings: AlertContactSettings,
        sessionId: Long,
        analysisText: String,
        sendToLine: Boolean = settings.useLine,
    ): TextAlertDeliveryStatus {
        val authenticatedSettings = getAuthenticatedContactSettings(settings)
        val canUseBackend = authenticatedSettings.hasBackendCredentials()
        val hasBackendContactMethod =
            canUseBackend && (sendToLine || authenticatedSettings.useTelegram || authenticatedSettings.usePush)
        if (!hasBackendContactMethod && !authenticatedSettings.useSms) {
            return TextAlertDeliveryStatus.Skipped
        }

        val sendResults = mutableListOf<Boolean>()
        if (sendToLine && canUseBackend) {
            val payload = PendingLineAlertPayload(
                userId = userId,
                apiUrl = authenticatedSettings.apiUrl,
                apiToken = authenticatedSettings.apiToken,
                message = formatAiAnalysisMessage(analysisText),
            )
            val sent = lineBackendApi.sendPendingAlert(payload)
            if (!sent) alarmPreferences.savePendingLineAlert(payload)
            sendResults += sent
        }

        if (authenticatedSettings.useTelegram && canUseBackend) {
            val payload = PendingAnalysisPayload(
                userId = userId,
                apiUrl = authenticatedSettings.apiUrl,
                apiToken = authenticatedSettings.apiToken,
                analysisText = analysisText,
            )
            val sent = backendApi.sendPendingAnalysis(payload)
            if (!sent) alarmPreferences.savePendingAnalysis(payload)
            sendResults += sent
        }

        if (authenticatedSettings.usePush && canUseBackend) {
            val payload = PendingPushAlertPayload(
                userId = userId,
                apiUrl = authenticatedSettings.apiUrl,
                apiToken = authenticatedSettings.apiToken,
                eventId = buildPushEventId(userId, sessionId),
                messageType = "ai_analysis",
                title = context.getString(R.string.notification_ai_analysis_result),
                message = formatAiAnalysisMessage(analysisText),
            )
            val sent = pushBackendApi.sendAlert(payload)
            if (!sent) alarmPreferences.savePendingPushAlert(payload)
            sendResults += sent
        }

        if (authenticatedSettings.useSms) {
            sendResults += sendSmsAnalysisMessage(authenticatedSettings, analysisText)
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
        val audioFile = clip.file ?: return
        serviceScope.launch {
            val authenticatedSettings = getAuthenticatedContactSettings(settings)
            if (!authenticatedSettings.hasBackendCredentials()) return@launch
            val filename = audioFile.name.let { name ->
                if (name.endsWith(".m4a", ignoreCase = true)) name
                else name.substringBeforeLast('.') + ".m4a"
            }
            alarmPreferences.savePendingAudio(
                PendingAudioPayload(
                    userId = userId,
                    apiUrl = authenticatedSettings.apiUrl,
                    apiToken = authenticatedSettings.apiToken,
                    filePath = audioFile.absolutePath,
                    filename = filename,
                    caption = context.getString(R.string.notification_audio_caption),
                    sendToTelegram = authenticatedSettings.useTelegram,
                    sendToLine = authenticatedSettings.useLine,
                    sendToPush = authenticatedSettings.usePush,
                    pushEventId = buildPushEventId(userId, clip.sessionId),
                    pushTitle = context.getString(R.string.audio_evidence_title),
                    pushMessage = context.getString(R.string.notification_audio_caption),
                ),
            )
            flushPendingMessages()
        }
    }

    fun queueTelegramAnalysis(
        userId: String,
        settings: AlertContactSettings,
        analysisText: String,
    ) {
        serviceScope.launch {
            val authenticatedSettings = getAuthenticatedContactSettings(settings)
            if (!authenticatedSettings.hasBackendCredentials()) return@launch
            alarmPreferences.savePendingAnalysis(
                PendingAnalysisPayload(
                    userId = userId,
                    apiUrl = authenticatedSettings.apiUrl,
                    apiToken = authenticatedSettings.apiToken,
                    analysisText = analysisText,
                ),
            )
            flushPendingMessages()
        }
    }

    fun queueTrainingAudioUpload(
        userId: String,
        settings: AlertContactSettings,
        clip: CrisisAudioClip,
        dangerLevel: String?,
    ) {
        serviceScope.launch {
            val authenticatedSettings = getAuthenticatedContactSettings(settings)
            if (!authenticatedSettings.hasBackendCredentials()) return@launch
            alarmPreferences.savePendingTrainingAudio(
                PendingTrainingAudioPayload(
                    userId = userId,
                    apiUrl = authenticatedSettings.apiUrl,
                    apiToken = authenticatedSettings.apiToken,
                    wavFilePath = clip.wavFile.absolutePath,
                    filename = clip.wavFile.name.ifBlank { "crisis-training.wav" },
                    triggerSource = clip.triggerSource.name,
                    dangerLevel = dangerLevel,
                    capturedAtEpochMs = clip.wavFile.lastModified(),
                    sampleRateHz = CrisisAudioConfig.SAMPLE_RATE_HZ,
                    durationMs = clip.durationMs,
                ),
            )
            flushPendingMessages()
        }
    }

    fun queuePushAnalysis(
        userId: String,
        settings: AlertContactSettings,
        sessionId: Long,
        analysisText: String,
    ) {
        serviceScope.launch {
            val authenticatedSettings = getAuthenticatedContactSettings(settings)
            if (!authenticatedSettings.hasBackendCredentials()) return@launch
            alarmPreferences.savePendingPushAlert(
                PendingPushAlertPayload(
                    userId = userId,
                    apiUrl = authenticatedSettings.apiUrl,
                    apiToken = authenticatedSettings.apiToken,
                    eventId = buildPushEventId(userId, sessionId),
                    messageType = "ai_analysis",
                    title = context.getString(R.string.notification_ai_analysis_result),
                    message = formatAiAnalysisMessage(analysisText),
                ),
            )
            flushPendingMessages()
        }
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
                    if (!payload.hasBackendCredentials()) continue
                    val sent = backendApi.sendPendingAlert(payload)
                    if (!sent) alarmPreferences.savePendingAlert(payload)
                }
            }

            val pendingAudios = alarmPreferences.getPendingAudios()
            if (pendingAudios.isNotEmpty()) {
                alarmPreferences.clearPendingAudios()
                for (payload in pendingAudios) {
                    if (!payload.hasBackendCredentials()) continue
                    val sent = backendApi.sendPendingAudio(payload)
                    if (!sent) alarmPreferences.savePendingAudio(payload)
                }
            }

            val pendingAnalyses = alarmPreferences.getPendingAnalyses()
            if (pendingAnalyses.isNotEmpty()) {
                alarmPreferences.clearPendingAnalyses()
                for (payload in pendingAnalyses) {
                    if (!payload.hasBackendCredentials()) continue
                    val sent = backendApi.sendPendingAnalysis(payload)
                    if (!sent) alarmPreferences.savePendingAnalysis(payload)
                }
            }

            val pendingTrainingAudios = alarmPreferences.getPendingTrainingAudios()
            if (pendingTrainingAudios.isNotEmpty()) {
                alarmPreferences.clearPendingTrainingAudios()
                for (payload in pendingTrainingAudios) {
                    if (!payload.hasBackendCredentials()) continue
                    val sent = backendApi.sendPendingTrainingAudio(payload)
                    if (!sent) alarmPreferences.savePendingTrainingAudio(payload)
                }
            }

            val pendingPushAlerts = alarmPreferences.getPendingPushAlerts()
            if (pendingPushAlerts.isNotEmpty()) {
                alarmPreferences.clearPendingPushAlerts()
                for (payload in pendingPushAlerts) {
                    if (!payload.hasBackendCredentials()) continue
                    val sent = pushBackendApi.sendAlert(payload)
                    if (!sent) alarmPreferences.savePendingPushAlert(payload)
                }
            }

            val pendingLineAlerts = alarmPreferences.getPendingLineAlerts()
            if (pendingLineAlerts.isNotEmpty()) {
                alarmPreferences.clearPendingLineAlerts()
                for (payload in pendingLineAlerts) {
                    if (!payload.hasBackendCredentials()) continue
                    val sent = lineBackendApi.sendPendingAlert(payload)
                    if (!sent) alarmPreferences.savePendingLineAlert(payload)
                }
            }

            val pendingLineAudioAnalyses = alarmPreferences.getPendingLineAudioAnalyses()
            if (pendingLineAudioAnalyses.isNotEmpty()) {
                alarmPreferences.clearPendingLineAudioAnalyses()
                for (payload in pendingLineAudioAnalyses) {
                    if (!payload.hasBackendCredentials()) continue
                    val sent = lineBackendApi.sendPendingAudioAnalysis(payload)
                    if (!sent) alarmPreferences.savePendingLineAudioAnalysis(payload)
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
                "AI analysis unavailable. ${localizeAiStatusMessage(context, aiState)}"
            AiModelStatus.Ready ->
                "AI analysis could not be completed."
        }
    }

    private suspend fun getAuthenticatedContactSettings(
        settings: AlertContactSettings = alarmPreferences.getAlertContactSettings(),
    ): AlertContactSettings {
        return BackendDeviceTokenManager.ensureRegistered(
            context = context,
            settings = settings,
            backendApi = backendApi,
        )
    }

    private suspend fun sendTelegramAlert(
        settings: AlertContactSettings,
        userId: String,
        message: String,
    ): Boolean {
        if (!settings.hasBackendCredentials()) return false
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

    private suspend fun sendLineAlert(
        settings: AlertContactSettings,
        userId: String,
        message: String,
    ): Boolean {
        if (!settings.hasBackendCredentials()) return false
        val payload = PendingLineAlertPayload(
            userId = userId,
            apiUrl = settings.apiUrl,
            apiToken = settings.apiToken,
            message = message,
        )
        val sent = runCatching { lineBackendApi.sendPendingAlert(payload) }.getOrDefault(false)
        if (!sent) alarmPreferences.savePendingLineAlert(payload)
        return sent
    }

    private suspend fun sendPushAlert(
        settings: AlertContactSettings,
        userId: String,
        sessionId: Long,
        alertPayload: AlertMessagePayload,
    ): Boolean {
        if (!settings.hasBackendCredentials()) return false
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
            message = alertPayload.text,
            sosMessage = alertPayload.historyMessage,
            sosDate = alertPayload.details.date,
            sosTime = alertPayload.details.time,
            sosDeviceName = alertPayload.details.deviceName,
            sosMobileNumber = alertPayload.details.mobileNumber,
            sosCurrentSoundLevel = alertPayload.details.currentSoundLevel,
            sosLocationLabel = alertPayload.details.locationLabel,
            sosLocationLink = alertPayload.details.locationLink,
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

private fun hasBackendCredentials(apiUrl: String, apiToken: String): Boolean {
    return apiUrl.isNotBlank() &&
        apiToken.isNotBlank() &&
        apiToken != BuildConfig.TELEGRAM_ALERT_API_TOKEN.trim()
}

private fun AlertContactSettings.hasBackendCredentials(): Boolean {
    return hasBackendCredentials(apiUrl, apiToken)
}

private fun PendingAlertPayload.hasBackendCredentials(): Boolean {
    return hasBackendCredentials(apiUrl, apiToken)
}

private fun PendingAudioPayload.hasBackendCredentials(): Boolean {
    return hasBackendCredentials(apiUrl, apiToken)
}

private fun PendingAnalysisPayload.hasBackendCredentials(): Boolean {
    return hasBackendCredentials(apiUrl, apiToken)
}

private fun PendingTrainingAudioPayload.hasBackendCredentials(): Boolean {
    return hasBackendCredentials(apiUrl, apiToken)
}

private fun PendingPushAlertPayload.hasBackendCredentials(): Boolean {
    return hasBackendCredentials(apiUrl, apiToken)
}

private fun PendingLineAlertPayload.hasBackendCredentials(): Boolean {
    return hasBackendCredentials(apiUrl, apiToken)
}

private fun PendingLineAudioAnalysisPayload.hasBackendCredentials(): Boolean {
    return hasBackendCredentials(apiUrl, apiToken)
}
