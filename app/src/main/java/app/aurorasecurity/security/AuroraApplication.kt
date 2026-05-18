package app.aurorasecurity.security

import android.app.Application

class AuroraApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseInitializer.ensureInitialized(this)
        PushNotificationHelper.createNotificationChannel(this)
    }
}
