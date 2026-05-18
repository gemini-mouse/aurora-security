package app.aurorasecurity.security

import android.content.Context
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

object PushRegistrationManager {
    suspend fun registerCurrentDevice(
        context: Context,
        settings: AlertContactSettings,
        backendApi: PushBackendApi = PushBackendApi(),
    ): Result<String> {
        FirebaseInitializer.ensureInitialized(context)
            ?: return Result.failure(IllegalStateException("FCM is not configured."))

        val token = awaitFirebaseToken()
            ?: return Result.failure(IllegalStateException("Unable to get FCM token."))

        val userId = AlarmPreferences(context).getUserId()
        val registered = backendApi.registerDevice(settings, userId, token)
        return if (registered) {
            Result.success(token)
        } else {
            Result.failure(IllegalStateException("Unable to register this device for push alerts."))
        }
    }

    private suspend fun awaitFirebaseToken(): String? = suspendCancellableCoroutine { continuation ->
        FirebaseMessaging.getInstance().token
            .addOnSuccessListener { token -> continuation.resume(token) }
            .addOnFailureListener { continuation.resume(null) }
    }
}
