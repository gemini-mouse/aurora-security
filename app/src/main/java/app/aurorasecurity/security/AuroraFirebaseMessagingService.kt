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
            PushBackendApi().registerDevice(
                settings = settings,
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

        serviceScope.launch {
            HistoryManager(applicationContext).upsertIncomingPushRecord(
                eventId = eventId,
                sourceUserId = sourceUserId,
                messageType = messageType,
                title = title,
                message = body,
            )
        }

        PushNotificationHelper.showEmergencyAlert(this, title, body)
    }
}
