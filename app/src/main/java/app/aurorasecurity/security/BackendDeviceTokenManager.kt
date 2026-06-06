package app.aurorasecurity.security

import android.content.Context
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

object BackendDeviceTokenManager {
    private val registrationMutex = Mutex()

    suspend fun ensureRegistered(
        context: Context,
        settings: AlertContactSettings,
        backendApi: TelegramBackendApi = TelegramBackendApi(),
    ): AlertContactSettings {
        if (settings.apiUrl.isBlank()) return settings

        val preferences = AlarmPreferences(context.applicationContext)
        val existingToken = preferences.getBackendDeviceToken()
        if (existingToken.isNotBlank()) {
            return settings.copy(apiToken = existingToken)
        }

        return registrationMutex.withLock {
            val tokenAfterWaiting = preferences.getBackendDeviceToken()
            if (tokenAfterWaiting.isNotBlank()) {
                return@withLock settings.copy(apiToken = tokenAfterWaiting)
            }

            val bootstrapToken = preferences.getBackendBootstrapApiToken()
            if (bootstrapToken.isBlank()) return@withLock settings

            val bootstrapSettings = settings.copy(apiToken = bootstrapToken)
            val deviceToken = backendApi.registerBackendDevice(
                settings = bootstrapSettings,
                userId = preferences.getUserId(),
                bindCode = preferences.getBindCode(),
                installSecret = preferences.getBackendInstallSecret(),
            ) ?: return@withLock settings

            preferences.saveBackendDeviceToken(deviceToken)
            settings.copy(apiToken = deviceToken)
        }
    }
}
