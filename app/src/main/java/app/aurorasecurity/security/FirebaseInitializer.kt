package app.aurorasecurity.security

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions

object FirebaseInitializer {
    fun isConfigured(): Boolean {
        return BuildConfig.FCM_PROJECT_ID.isNotBlank() &&
            BuildConfig.FCM_APPLICATION_ID.isNotBlank() &&
            BuildConfig.FCM_API_KEY.isNotBlank() &&
            BuildConfig.FCM_GCM_SENDER_ID.isNotBlank()
    }

    fun ensureInitialized(context: Context): FirebaseApp? {
        if (!isConfigured()) return null

        val existingApp = FirebaseApp.getApps(context)
            .firstOrNull { it.name == FirebaseApp.DEFAULT_APP_NAME }
        if (existingApp != null) return existingApp

        val options = FirebaseOptions.Builder()
            .setProjectId(BuildConfig.FCM_PROJECT_ID)
            .setApplicationId(BuildConfig.FCM_APPLICATION_ID)
            .setApiKey(BuildConfig.FCM_API_KEY)
            .setGcmSenderId(BuildConfig.FCM_GCM_SENDER_ID)
            .build()

        return FirebaseApp.initializeApp(context, options)
    }
}
