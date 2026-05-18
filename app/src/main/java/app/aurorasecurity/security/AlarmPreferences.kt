package app.aurorasecurity.security

import android.content.Context
import android.content.res.Configuration
import android.provider.Settings
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import app.aurorasecurity.security.onboarding.QuickSetupStep
import java.io.File
import java.security.MessageDigest
import java.util.Locale
import java.util.UUID

class AlarmPreferences(context: Context) {
    private val preferences = context.getSharedPreferences("alarm_prefs", Context.MODE_PRIVATE)
    private val appContext = context.applicationContext

    init {
        migrateTemplateForCurrentInstall()
        migrateLegacyAlertTemplateToLocalizedDefault()
        ensureDefaultAppLanguageOnFirstInstall()
    }

    private fun stringPreferenceOrDefault(key: String, defaultValue: String): String {
        return preferences.getString(key, null)
            ?.takeIf { it.isNotBlank() }
            ?: defaultValue
    }

    private fun defaultAlertMessageTemplate(): String {
        return appContext.getString(R.string.default_alert_message_template)
    }

    private fun defaultAlertMessageTemplate(languageTag: String): String {
        val localizedContext = appContext.createConfigurationContext(
            Configuration(appContext.resources.configuration).apply {
                setLocale(Locale.forLanguageTag(languageTag))
            }
        )
        return localizedContext.getString(R.string.default_alert_message_template)
    }

    private fun allDefaultAlertMessageTemplates(): Set<String> {
        return AppLanguageOption.entries
            .map { defaultAlertMessageTemplate(it.languageTag).trim() }
            .toSet()
    }

    private fun resolveAlertMessageTemplate(storedTemplate: String?): String {
        val currentDefaultTemplate = defaultAlertMessageTemplate()
        val normalizedStoredTemplate = storedTemplate
            ?.replace("\r\n", "\n")
            ?.trim()
            .orEmpty()

        if (normalizedStoredTemplate.isBlank()) {
            return currentDefaultTemplate
        }

        if (isDefaultAlertMessageTemplate(normalizedStoredTemplate)) {
            return currentDefaultTemplate
        }

        return storedTemplate.orEmpty()
    }

    fun getUserId(): String {
        val existing = preferences.getString(KEY_USER_ID, null)
        if (!existing.isNullOrBlank()) return existing

        val generated = stableDeviceId()?.let { deviceId ->
            "device-${sha256("$deviceId:${appContext.packageName}").take(24)}"
        } ?: UUID.randomUUID().toString()
        preferences.edit().putString(KEY_USER_ID, generated).apply()
        return generated
    }

    fun getBindCode(): String {
        val existing = preferences.getString(KEY_BIND_CODE, null)
        if (!existing.isNullOrBlank()) return existing

        val generated = stableDeviceId()?.let { deviceId ->
            sha256("bind:$deviceId:${appContext.packageName}")
                .take(BIND_CODE_LENGTH)
                .uppercase(Locale.US)
        } ?: UUID.randomUUID()
            .toString()
            .replace("-", "")
            .take(BIND_CODE_LENGTH)
            .uppercase(Locale.US)
        preferences.edit().putString(KEY_BIND_CODE, generated).apply()
        return generated
    }

    private fun stableDeviceId(): String? {
        val androidId = Settings.Secure.getString(
            appContext.contentResolver,
            Settings.Secure.ANDROID_ID,
        )
        return androidId
            ?.takeIf { it.isNotBlank() && it != INVALID_ANDROID_ID }
    }

    private fun sha256(value: String): String {
        return MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray())
            .joinToString(separator = "") { byte -> "%02x".format(byte) }
    }

    fun getThresholdDb(): Float {
        return preferences.getFloat(KEY_THRESHOLD_DB, COMMUTE_THRESHOLD_DB)
    }

    fun setThresholdDb(value: Float) {
        preferences.edit().putFloat(KEY_THRESHOLD_DB, value).apply()
    }

    fun getAlertContactSettings(): AlertContactSettings {
        return AlertContactSettings(
            apiUrl = DEFAULT_TELEGRAM_API_URL,
            apiToken = DEFAULT_TELEGRAM_API_TOKEN,
            botName = stringPreferenceOrDefault(KEY_TELEGRAM_BOT_NAME, DEFAULT_TELEGRAM_BOT_NAME),
            deviceName = stringPreferenceOrDefault(KEY_TELEGRAM_DEVICE_NAME, BuildConfig.TELEGRAM_DEVICE_NAME),
            userName = stringPreferenceOrDefault(KEY_USER_NAME, "User"),
            mobileNumber = preferences.getString(KEY_MOBILE_NUMBER, "").orEmpty(),
            alertMessageTemplate = resolveAlertMessageTemplate(
                preferences.getString(KEY_ALERT_MESSAGE_TEMPLATE, null),
            ),
            useTelegram = preferences.getBoolean(KEY_USE_TELEGRAM, true),
            usePush = preferences.getBoolean(KEY_USE_PUSH, true),
            useSms = preferences.getBoolean(KEY_USE_SMS, false),
            usePhoneCall = preferences.getBoolean(KEY_USE_PHONE_CALL, false),
            emergencyPhoneNumber = preferences.getString(KEY_EMERGENCY_PHONE_NUMBER, "").orEmpty(),
            secondaryPhoneNumber = preferences.getString(KEY_SECONDARY_PHONE_NUMBER, "").orEmpty(),
        )
    }

    fun saveAlertContactSettings(settings: AlertContactSettings) {
        preferences.edit()
            .putString(KEY_TELEGRAM_BOT_NAME, settings.botName)
            .putString(KEY_TELEGRAM_DEVICE_NAME, settings.deviceName)
            .putString(KEY_USER_NAME, settings.userName)
            .putString(KEY_MOBILE_NUMBER, settings.mobileNumber)
            .putString(KEY_ALERT_MESSAGE_TEMPLATE, settings.alertMessageTemplate)
            .putBoolean(KEY_USE_TELEGRAM, settings.useTelegram)
            .putBoolean(KEY_USE_PUSH, settings.usePush)
            .putBoolean(KEY_USE_SMS, settings.useSms)
            .putBoolean(KEY_USE_PHONE_CALL, settings.usePhoneCall)
            .putString(KEY_EMERGENCY_PHONE_NUMBER, settings.emergencyPhoneNumber)
            .putString(KEY_SECONDARY_PHONE_NUMBER, settings.secondaryPhoneNumber)
            .apply()
    }

    fun getAiSettings(): AiSettings {
        val deviceSupport = appContext.getAiDeviceSupport()
        val isAiEnabled = deviceSupport.supportsOnDeviceAi &&
            preferences.getBoolean(KEY_IS_AI_ENABLED, true)
        return AiSettings(
            isAiEnabled = isAiEnabled,
            sendAudioToContacts = preferences.getBoolean(KEY_SEND_AUDIO_TO_CONTACTS, true),
            enableAiAnalysis = isAiEnabled,
            preloadAiInBackground = deviceSupport.supportsOnDeviceAi &&
                preferences.getBoolean(KEY_PRELOAD_AI_IN_BACKGROUND, COMMUTE_PRELOAD_AI),
            sosDispatchMode = runCatching {
                SosDispatchMode.valueOf(
                    preferences.getString(KEY_SOS_DISPATCH_MODE, SosDispatchMode.Immediate.name)
                        ?: SosDispatchMode.Immediate.name,
                )
            }.getOrDefault(SosDispatchMode.Immediate).let { mode ->
                if (isAiEnabled) mode else SosDispatchMode.Immediate
            },
            modelType = preferences.getString(KEY_GEMMA_MODEL_TYPE, null)
                ?.takeIf { candidate -> GemmaModelType.entries.any { it.name == candidate } }
                ?: getDefaultGemmaModel(),
            uploadTrainingTriggerAudio = preferences.getBoolean(KEY_UPLOAD_TRAINING_TRIGGER_AUDIO, false),
        )
    }

    private fun getDefaultGemmaModel(): String {
        return appContext.getAiDeviceSupport().defaultModelType.name
    }

    fun saveAiSettings(settings: AiSettings) {
        val deviceSupport = appContext.getAiDeviceSupport()
        val isAiEnabled = deviceSupport.supportsOnDeviceAi && settings.isAiEnabled
        preferences.edit()
            .putBoolean(KEY_IS_AI_ENABLED, isAiEnabled)
            .putBoolean(KEY_SEND_AUDIO_TO_CONTACTS, settings.sendAudioToContacts)
            .putBoolean(KEY_ENABLE_AI_ANALYSIS, isAiEnabled)
            .putBoolean(KEY_PRELOAD_AI_IN_BACKGROUND, settings.preloadAiInBackground)
            .putString(
                KEY_SOS_DISPATCH_MODE,
                if (isAiEnabled) settings.sosDispatchMode.name else SosDispatchMode.Immediate.name,
            )
            .putString(KEY_GEMMA_MODEL_TYPE, settings.modelType)
            .putBoolean(KEY_UPLOAD_TRAINING_TRIGGER_AUDIO, settings.uploadTrainingTriggerAudio)
            .apply()
    }

    fun getMovementSettings(): MovementSettings {
        return MovementSettings(
            useShakeDetection = preferences.getBoolean(KEY_USE_SHAKE_DETECTION, COMMUTE_USE_SHAKE_DETECTION),
            shakeThresholdG = preferences.getFloat(KEY_SHAKE_THRESHOLD_G, COMMUTE_SHAKE_THRESHOLD_G),
        )
    }

    fun getMonitoringPaused(): Boolean {
        return preferences.getBoolean(KEY_MONITORING_PAUSED, false)
    }

    fun setMonitoringPaused(paused: Boolean) {
        preferences.edit().putBoolean(KEY_MONITORING_PAUSED, paused).apply()
    }

    fun saveMovementSettings(settings: MovementSettings) {
        preferences.edit()
            .putBoolean(KEY_USE_SHAKE_DETECTION, settings.useShakeDetection)
            .putFloat(KEY_SHAKE_THRESHOLD_G, settings.shakeThresholdG)
            .apply()
    }

    fun getThemeMode(): AppThemeMode {
        return when (preferences.getString(KEY_THEME_MODE, AppThemeMode.Light.name)) {
            AppThemeMode.Dark.name -> AppThemeMode.Dark
            else -> AppThemeMode.Light
        }
    }

    fun setThemeMode(themeMode: AppThemeMode) {
        preferences.edit()
            .putString(KEY_THEME_MODE, themeMode.name)
            .apply()
    }

    fun getAppLanguageOption(): AppLanguageOption {
        val appLanguageTags = AppCompatDelegate.getApplicationLocales().toLanguageTags()
        val resolvedLanguageTags = appLanguageTags.ifBlank {
            LocaleListCompat.getAdjustedDefault().toLanguageTags()
        }
        return AppLanguageOption.fromLanguageTags(resolvedLanguageTags)
    }

    fun setAppLanguageOption(languageOption: AppLanguageOption) {
        AppCompatDelegate.setApplicationLocales(
            LocaleListCompat.forLanguageTags(languageOption.languageTag),
        )
    }

    fun hasSeenOnboarding(): Boolean {
        return preferences.getBoolean(KEY_HAS_SEEN_ONBOARDING, false)
    }

    fun setSeenOnboarding(seen: Boolean) {
        preferences.edit()
            .putBoolean(KEY_HAS_SEEN_ONBOARDING, seen)
            .apply()
    }

    fun hasCompletedQuickSetup(): Boolean {
        return preferences.getBoolean(KEY_HAS_COMPLETED_QUICK_SETUP, false)
    }

    fun setCompletedQuickSetup(completed: Boolean) {
        preferences.edit()
            .putBoolean(KEY_HAS_COMPLETED_QUICK_SETUP, completed)
            .apply()
    }

    internal fun getQuickSetupCurrentStep(): QuickSetupStep {
        val storedValue = preferences.getString(
            KEY_QUICK_SETUP_CURRENT_STEP,
            QuickSetupStep.Permissions.name,
        ) ?: QuickSetupStep.Permissions.name
        return runCatching { QuickSetupStep.valueOf(storedValue) }
            .getOrDefault(QuickSetupStep.Permissions)
    }

    internal fun setQuickSetupCurrentStep(step: QuickSetupStep) {
        preferences.edit()
            .putString(KEY_QUICK_SETUP_CURRENT_STEP, step.name)
            .apply()
    }

    fun hasCompletedSetupTest(): Boolean {
        return preferences.getBoolean(KEY_HAS_COMPLETED_SETUP_TEST, false)
    }

    fun setCompletedSetupTest(completed: Boolean) {
        preferences.edit()
            .putBoolean(KEY_HAS_COMPLETED_SETUP_TEST, completed)
            .apply()
    }

    fun hasAcceptedTermsOfUse(): Boolean {
        val marker = termsAcceptanceMarkerFile()
        if (marker.exists()) return true

        val acceptedInPreferences = preferences.getBoolean(KEY_ACCEPTED_TERMS_OF_USE, false)
        if (acceptedInPreferences && isAppUpdateInstall()) {
            persistTermsAcceptanceMarker(marker)
            return true
        }

        return false
    }

    fun setAcceptedTermsOfUse(accepted: Boolean) {
        preferences.edit()
            .putBoolean(KEY_ACCEPTED_TERMS_OF_USE, accepted)
            .apply()

        val marker = termsAcceptanceMarkerFile()
        if (accepted) {
            persistTermsAcceptanceMarker(marker)
        } else if (marker.exists()) {
            marker.delete()
        }
    }

    fun hasRequestedPermission(permission: String): Boolean {
        return preferences.getBoolean(permissionRequestKey(permission), false)
    }

    fun markPermissionRequested(permission: String) {
        preferences.edit()
            .putBoolean(permissionRequestKey(permission), true)
            .apply()
    }

    fun markPermissionsRequested(permissions: Collection<String>) {
        preferences.edit().apply {
            permissions.forEach { permission ->
                putBoolean(permissionRequestKey(permission), true)
            }
        }.apply()
    }

    private fun permissionRequestKey(permission: String): String {
        return "${KEY_PERMISSION_REQUESTED_PREFIX}${permission.replace('.', '_')}"
    }

    fun saveTelegramContacts(contacts: List<TelegramContact>) {
        saveTelegramContactsToPreferences(
            preferences = preferences,
            key = KEY_TELEGRAM_CONTACTS,
            contacts = contacts,
        )
    }

    fun getTelegramContacts(): List<TelegramContact> {
        return loadTelegramContactsFromPreferences(
            preferences = preferences,
            key = KEY_TELEGRAM_CONTACTS,
        )
    }

    fun savePushContacts(contacts: List<PushContact>) {
        savePushContactsToPreferences(
            preferences = preferences,
            key = KEY_PUSH_CONTACTS,
            contacts = contacts,
        )
    }

    fun getPushContacts(): List<PushContact> {
        return loadPushContactsFromPreferences(
            preferences = preferences,
            key = KEY_PUSH_CONTACTS,
        )
    }

    // --- Pending Alert Queue (offline retry) ---

    @Synchronized
    fun savePendingAlert(payload: PendingAlertPayload) {
        savePendingAlertToPreferences(
            preferences = preferences,
            key = KEY_PENDING_ALERTS,
            payload = payload,
        )
    }

    fun getPendingAlerts(): List<PendingAlertPayload> {
        return loadPendingAlertsFromPreferences(
            preferences = preferences,
            key = KEY_PENDING_ALERTS,
        )
    }

    fun clearPendingAlerts() {
        clearPreferenceValue(preferences, KEY_PENDING_ALERTS)
    }

    @Synchronized
    fun savePendingAudio(payload: PendingAudioPayload) {
        savePendingAudioToPreferences(
            preferences = preferences,
            key = KEY_PENDING_AUDIO,
            payload = payload,
        )
    }

    fun getPendingAudios(): List<PendingAudioPayload> {
        return loadPendingAudiosFromPreferences(
            preferences = preferences,
            key = KEY_PENDING_AUDIO,
        )
    }

    fun clearPendingAudios() {
        clearPreferenceValue(preferences, KEY_PENDING_AUDIO)
    }

    @Synchronized
    fun savePendingAnalysis(payload: PendingAnalysisPayload) {
        savePendingAnalysisToPreferences(
            preferences = preferences,
            key = KEY_PENDING_ANALYSIS,
            payload = payload,
        )
    }

    fun getPendingAnalyses(): List<PendingAnalysisPayload> {
        return loadPendingAnalysesFromPreferences(
            preferences = preferences,
            key = KEY_PENDING_ANALYSIS,
        )
    }

    fun clearPendingAnalyses() {
        clearPreferenceValue(preferences, KEY_PENDING_ANALYSIS)
    }

    @Synchronized
    fun savePendingTrainingAudio(payload: PendingTrainingAudioPayload) {
        savePendingTrainingAudioToPreferences(
            preferences = preferences,
            key = KEY_PENDING_TRAINING_AUDIO,
            payload = payload,
        )
    }

    fun getPendingTrainingAudios(): List<PendingTrainingAudioPayload> {
        return loadPendingTrainingAudiosFromPreferences(
            preferences = preferences,
            key = KEY_PENDING_TRAINING_AUDIO,
        )
    }

    fun clearPendingTrainingAudios() {
        clearPreferenceValue(preferences, KEY_PENDING_TRAINING_AUDIO)
    }

    @Synchronized
    fun savePendingPushAlert(payload: PendingPushAlertPayload) {
        savePendingPushAlertToPreferences(
            preferences = preferences,
            key = KEY_PENDING_PUSH_ALERTS,
            payload = payload,
        )
    }

    fun getPendingPushAlerts(): List<PendingPushAlertPayload> {
        return loadPendingPushAlertsFromPreferences(
            preferences = preferences,
            key = KEY_PENDING_PUSH_ALERTS,
        )
    }

    fun clearPendingPushAlerts() {
        clearPreferenceValue(preferences, KEY_PENDING_PUSH_ALERTS)
    }

    private fun migrateTemplateForCurrentInstall() {
        val installMarker = appContext.getFileStreamPath(INSTALL_MARKER_FILE)
        if (installMarker.exists()) return

        preferences.edit()
            .putString(KEY_ALERT_MESSAGE_TEMPLATE, defaultAlertMessageTemplate())
            .apply()

        installMarker.parentFile?.mkdirs()
        installMarker.writeText("installed")
    }

    private fun migrateLegacyAlertTemplateToLocalizedDefault() {
        val existingTemplate = preferences.getString(KEY_ALERT_MESSAGE_TEMPLATE, null)?.trim()
        if (existingTemplate.isNullOrBlank()) return

        if (!isDefaultAlertMessageTemplate(existingTemplate)) return

        preferences.edit()
            .putString(KEY_ALERT_MESSAGE_TEMPLATE, defaultAlertMessageTemplate())
            .apply()
    }

    private fun isDefaultAlertMessageTemplate(template: String): Boolean {
        val normalized = template.replace("\r\n", "\n").trim()
        return normalized == LEGACY_DEFAULT_ALERT_MESSAGE_TEMPLATE ||
            normalized == LEGACY_DEFAULT_ALERT_MESSAGE_TEMPLATE_V2 ||
            normalized in allDefaultAlertMessageTemplates()
    }

    fun isAlertMessageTemplateDefault(template: String): Boolean {
        return isDefaultAlertMessageTemplate(template)
    }

    private fun ensureDefaultAppLanguageOnFirstInstall() {
        val marker = appLanguageMarkerFile()
        if (marker.exists()) return

        val currentLanguageTags = AppCompatDelegate.getApplicationLocales().toLanguageTags()
        if (currentLanguageTags.isBlank()) {
            val systemLanguage = AppLanguageOption.fromLanguageTags(
                LocaleListCompat.getAdjustedDefault().toLanguageTags(),
            )
            setAppLanguageOption(systemLanguage)
        }

        marker.parentFile?.mkdirs()
        marker.writeText("initialized")
    }

    private fun termsAcceptanceMarkerFile(): File {
        return File(appContext.noBackupFilesDir, TERMS_ACCEPTED_MARKER_FILE)
    }

    private fun appLanguageMarkerFile(): File {
        return File(appContext.noBackupFilesDir, APP_LANGUAGE_MARKER_FILE)
    }

    private fun persistTermsAcceptanceMarker(marker: File = termsAcceptanceMarkerFile()) {
        marker.parentFile?.mkdirs()
        marker.writeText("accepted")
    }

    private fun isAppUpdateInstall(): Boolean {
        return runCatching {
            val packageInfo = appContext.packageManager.getPackageInfo(appContext.packageName, 0)
            packageInfo.lastUpdateTime > packageInfo.firstInstallTime
        }.getOrDefault(false)
    }

    companion object {
        const val DEFAULT_TELEGRAM_BOT_NAME = "aurora_security_alarm_bot"
        const val DEFAULT_TELEGRAM_API_URL = "https://alarm-telegram-backend.aurora-security.workers.dev/"
        const val DEFAULT_TELEGRAM_API_TOKEN = "aurora"
        const val LEGACY_DEFAULT_ALERT_MESSAGE_TEMPLATE =
            "<User Name> may be experiencing an emergency. Please try to contact them immediately. " +
                "If you cannot reach them promptly, check on their safety in person and seek professional emergency assistance if needed.\n\n" +
                "Date: <Date>\nTime: <Time>\nMobile number: <Mobile Number>\nDevice: <Device Name>\nCurrent sound level: <Current dB> dB\n<Location>"
        const val LEGACY_DEFAULT_ALERT_MESSAGE_TEMPLATE_V2 =
            "<User Name> may be experiencing an emergency. Please try to contact them immediately. " +
                "If you cannot reach them promptly, check on their safety in person and seek professional emergency assistance if needed.\n\n" +
                "Date: <Date>\nTime: <Time>\nMobile number: <Mobile>\nDevice: <Device Name>\nCurrent sound level: <Current dB> dB\n<Location>"

        private const val KEY_USER_ID = "user_id"
        private const val KEY_BIND_CODE = "bind_code"
        private const val BIND_CODE_LENGTH = 12
        private const val KEY_THRESHOLD_DB = "threshold_db"
        private const val KEY_TELEGRAM_BOT_NAME = "telegram_bot_name"
        private const val KEY_TELEGRAM_DEVICE_NAME = "telegram_device_name"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_MOBILE_NUMBER = "mobile_number"
        private const val KEY_ALERT_MESSAGE_TEMPLATE = "alert_message_template"
        private const val KEY_USE_TELEGRAM = "use_telegram"
        private const val KEY_USE_PUSH = "use_push"
        private const val KEY_USE_SMS = "use_sms"
        private const val KEY_USE_PHONE_CALL = "use_phone_call"
        private const val KEY_EMERGENCY_PHONE_NUMBER = "emergency_phone_number"
        private const val KEY_SECONDARY_PHONE_NUMBER = "secondary_phone_number"
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_ACCEPTED_TERMS_OF_USE = "accepted_terms_of_use"
        private const val KEY_HAS_SEEN_ONBOARDING = "has_seen_onboarding"
        private const val KEY_HAS_COMPLETED_QUICK_SETUP = "has_completed_quick_setup"
        private const val KEY_QUICK_SETUP_CURRENT_STEP = "quick_setup_current_step"
        private const val KEY_HAS_COMPLETED_SETUP_TEST = "has_completed_setup_test"
    private const val KEY_SEND_AUDIO_TO_CONTACTS = "send_audio_to_contacts"
    private const val KEY_UPLOAD_TRAINING_TRIGGER_AUDIO = "upload_training_trigger_audio"
    private const val KEY_ENABLE_AI_ANALYSIS = "enable_ai_analysis"
        private const val KEY_PRELOAD_AI_IN_BACKGROUND = "preload_ai_in_background"
        private const val KEY_SOS_DISPATCH_MODE = "sos_dispatch_mode"
        private const val KEY_IS_AI_ENABLED = "is_ai_enabled"
        private const val KEY_GEMMA_MODEL_TYPE = "gemma_model_type"
        private const val KEY_MONITORING_PAUSED = "monitoring_paused"
        private const val KEY_USE_SHAKE_DETECTION = "use_shake_detection"
        private const val KEY_SHAKE_THRESHOLD_G = "shake_threshold_g"
        private const val KEY_TELEGRAM_CONTACTS = "telegram_contacts"
        private const val KEY_PUSH_CONTACTS = "push_contacts"
        private const val KEY_PENDING_ALERTS = "pending_alerts_queue"
    private const val KEY_PENDING_AUDIO = "pending_audio_queue"
    private const val KEY_PENDING_ANALYSIS = "pending_analysis_queue"
    private const val KEY_PENDING_TRAINING_AUDIO = "pending_training_audio_queue"
    private const val KEY_PENDING_PUSH_ALERTS = "pending_push_alerts_queue"
        private const val KEY_PERMISSION_REQUESTED_PREFIX = "permission_requested_"
        private const val INVALID_ANDROID_ID = "9774d56d682e549c"
        private const val INSTALL_MARKER_FILE = "install_marker_v1"
        private const val APP_LANGUAGE_MARKER_FILE = "app_language_initialized_v1"
        private const val TERMS_ACCEPTED_MARKER_FILE = "terms_accepted_v1"
        private const val COMMUTE_THRESHOLD_DB = 90f
        private const val COMMUTE_USE_SHAKE_DETECTION = true
        private const val COMMUTE_SHAKE_THRESHOLD_G = 3.0f
        private const val COMMUTE_PRELOAD_AI = false
    }
}
