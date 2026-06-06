package app.aurorasecurity.security

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AuroraFirebaseMessagingService : FirebaseMessagingService() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        val preferences = AlarmPreferences(applicationContext)
        val settings = preferences.getAlertContactSettings()
        if (!settings.usePush || token.isBlank()) return

        serviceScope.launch {
            val authenticatedSettings = BackendDeviceTokenManager.ensureRegistered(
                context = applicationContext,
                settings = settings,
            )
            if (preferences.getBackendDeviceToken().isBlank()) return@launch

            PushBackendApi().registerDevice(
                settings = authenticatedSettings,
                userId = preferences.getUserId(),
                fcmToken = token,
            )
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val title = message.notification?.title
            ?: message.data["title"]
            ?: "Aurora Security alert"
        val body = message.notification?.body
            ?: message.data["message"]
            ?: "A trusted Aurora contact needs help."
        val eventId = message.data["eventId"]?.ifBlank { null }
            ?: "${message.data["ownerUserId"].orEmpty()}-${System.currentTimeMillis()}"
        val sourceUserId = message.data["ownerUserId"].orEmpty()
        val messageType = message.data["messageType"]?.ifBlank { null }
            ?: if (title.equals("AI Analysis Result", ignoreCase = true)) "ai_analysis" else "sos"
        val audioUrl = message.data["audioUrl"].orEmpty()
        val sosMessage = message.data["sosMessage"].orEmpty()
        val sosDetails = message.data.toAlertMessageDetailsOrNull()

        serviceScope.launch {
            HistoryManager(applicationContext).upsertIncomingPushRecord(
                eventId = eventId,
                sourceUserId = sourceUserId,
                messageType = messageType,
                title = title,
                message = body,
                sosMessage = sosMessage,
                sosDetails = sosDetails,
                audioUrl = audioUrl,
            )
        }

        PushNotificationHelper.showEmergencyAlert(
            context = this,
            title = title,
            body = body,
            eventId = eventId,
            messageType = messageType,
        )
    }

    private fun Map<String, String>.toAlertMessageDetailsOrNull(): AlertMessageDetails? {
        val date = this["sosDate"].orEmpty()
        val time = this["sosTime"].orEmpty()
        val deviceName = this["sosDeviceName"].orEmpty()
        val mobileNumber = this["sosMobileNumber"].orEmpty()
        val currentSoundLevel = this["sosCurrentSoundLevel"].orEmpty()
        val locationLabel = this["sosLocationLabel"].orEmpty()
        val locationLink = this["sosLocationLink"].orEmpty()

        if (
            date.isBlank() &&
            time.isBlank() &&
            deviceName.isBlank() &&
            mobileNumber.isBlank() &&
            currentSoundLevel.isBlank() &&
            locationLabel.isBlank() &&
            locationLink.isBlank()
        ) {
            return null
        }

        return AlertMessageDetails(
            date = date,
            time = time,
            deviceName = deviceName,
            mobileNumber = mobileNumber,
            currentSoundLevel = currentSoundLevel,
            locationLabel = locationLabel,
            locationLink = locationLink,
        )
    }
}
