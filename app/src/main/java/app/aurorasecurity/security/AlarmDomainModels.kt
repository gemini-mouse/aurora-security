package app.aurorasecurity.security

data class AlertContactSettings(
    val apiUrl: String,
    val apiToken: String,
    val botName: String,
    val deviceName: String,
    val userName: String,
    val mobileNumber: String,
    val alertMessageTemplate: String,
    val useTelegram: Boolean,
    val usePush: Boolean,
    val useSms: Boolean,
    val usePhoneCall: Boolean,
    val emergencyPhoneNumber: String,
    val secondaryPhoneNumber: String,
)

data class AiSettings(
    val isAiEnabled: Boolean = true,
    val sendAudioToContacts: Boolean,
    val enableAiAnalysis: Boolean,
    val preloadAiInBackground: Boolean,
    val sosDispatchMode: SosDispatchMode = SosDispatchMode.Immediate,
    val modelType: String = GemmaModelType.E2B.name,
    val uploadTrainingTriggerAudio: Boolean = false,
)

enum class SosDispatchMode {
    Immediate,
    AiFirst,
}

data class PendingAlertPayload(
    val userId: String,
    val apiUrl: String,
    val apiToken: String,
    val message: String,
)

data class PendingAudioPayload(
    val userId: String,
    val apiUrl: String,
    val apiToken: String,
    val filePath: String,
    val filename: String,
    val caption: String,
)

data class PendingAnalysisPayload(
    val userId: String,
    val apiUrl: String,
    val apiToken: String,
    val analysisText: String,
)

data class PendingTrainingAudioPayload(
    val userId: String,
    val apiUrl: String,
    val apiToken: String,
    val wavFilePath: String,
    val filename: String,
    val triggerSource: String,
    val dangerLevel: String?,
    val capturedAtEpochMs: Long,
    val sampleRateHz: Int,
    val durationMs: Int,
)

data class PendingPushAlertPayload(
    val userId: String,
    val apiUrl: String,
    val apiToken: String,
    val eventId: String,
    val messageType: String,
    val title: String,
    val message: String,
)

enum class AppThemeMode {
    Light,
    Dark,
}

enum class AppLanguageOption(
    val languageTag: String,
    val labelRes: Int,
) {
    English("en", R.string.language_option_english),
    TraditionalChinese("zh-TW", R.string.language_option_traditional_chinese),
    Spanish("es", R.string.language_option_spanish),
    Korean("ko", R.string.language_option_korean),
    Japanese("ja", R.string.language_option_japanese);

    companion object {
        fun fromLanguageTags(languageTags: String?): AppLanguageOption {
            val primaryTag = languageTags
                ?.substringBefore(',')
                ?.trim()
                .orEmpty()

            return when {
                primaryTag.equals(TraditionalChinese.languageTag, ignoreCase = true) ||
                    primaryTag.startsWith("zh", ignoreCase = true) -> TraditionalChinese
                primaryTag.equals(Spanish.languageTag, ignoreCase = true) ||
                    primaryTag.startsWith("es", ignoreCase = true) -> Spanish
                primaryTag.equals(Korean.languageTag, ignoreCase = true) ||
                    primaryTag.startsWith("ko", ignoreCase = true) -> Korean
                primaryTag.equals(Japanese.languageTag, ignoreCase = true) ||
                    primaryTag.startsWith("ja", ignoreCase = true) -> Japanese
                else -> English
            }
        }
    }
}

enum class TelegramBindingStatus(val label: String) {
    Pending("Pending"),
    Bound("Bound"),
}

data class TelegramContact(
    val id: String,
    val name: String,
    val telegramHandle: String,
    val status: TelegramBindingStatus,
)

data class PushContact(
    val id: String,
    val name: String,
    val deviceName: String,
    val status: TelegramBindingStatus,
)

data class MovementSettings(
    val useShakeDetection: Boolean = true,
    val shakeThresholdG: Float = 3.0f,
)
