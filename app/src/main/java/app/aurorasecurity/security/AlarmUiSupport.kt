package app.aurorasecurity.security

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Sms
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import app.aurorasecurity.security.ui.theme.AuroraBlue
import app.aurorasecurity.security.ui.theme.AuroraTeal
import app.aurorasecurity.security.ui.theme.Frost
import app.aurorasecurity.security.ui.theme.Graphite

internal data class PermissionRationale(
    val permission: String,
    val icon: ImageVector,
    @param:StringRes val titleRes: Int,
    @param:StringRes val descriptionRes: Int,
)

internal val PERMISSION_RATIONALES = listOf(
    PermissionRationale(
        permission = Manifest.permission.RECORD_AUDIO,
        icon = Icons.Outlined.Mic,
        titleRes = R.string.permission_microphone_title,
        descriptionRes = R.string.permission_microphone_description,
    ),
    PermissionRationale(
        permission = Manifest.permission.ACCESS_FINE_LOCATION,
        icon = Icons.Outlined.LocationOn,
        titleRes = R.string.permission_location_title,
        descriptionRes = R.string.permission_location_description,
    ),
    PermissionRationale(
        permission = Manifest.permission.CALL_PHONE,
        icon = Icons.Outlined.Phone,
        titleRes = R.string.permission_phone_title,
        descriptionRes = R.string.permission_phone_description,
    ),
    PermissionRationale(
        permission = Manifest.permission.SEND_SMS,
        icon = Icons.Outlined.Sms,
        titleRes = R.string.permission_sms_title,
        descriptionRes = R.string.permission_sms_description,
    ),
    PermissionRationale(
        permission = Manifest.permission.POST_NOTIFICATIONS,
        icon = Icons.Outlined.NotificationsActive,
        titleRes = R.string.permission_notifications_title,
        descriptionRes = R.string.permission_notifications_description,
    ),
)

@Composable
internal fun AuroraCard(
    modifier: Modifier = Modifier,
    accentColors: List<ComposeColor>? = null,
    content: @Composable () -> Unit,
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val borderColors = accentColors ?: if (isDark) {
        listOf(AuroraBlue.copy(alpha = 0.34f), AuroraTeal.copy(alpha = 0.18f))
    } else {
        listOf(AuroraBlue.copy(alpha = 0.18f), AuroraTeal.copy(alpha = 0.10f))
    }
    val backgroundBrush = if (isDark) {
        Brush.verticalGradient(
            colors = listOf(
                ComposeColor(0xFF132232),
                ComposeColor(0xFF0D1824),
            ),
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                ComposeColor(0xFFFBF7F1),
                ComposeColor(0xFFF4EEE6),
            ),
        )
    }
    androidx.compose.foundation.layout.Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(brush = backgroundBrush)
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(colors = borderColors),
                shape = RoundedCornerShape(28.dp),
            )
    ) {
        content()
    }
}

internal val INITIAL_RUNTIME_PERMISSIONS = buildList {
    add(Manifest.permission.RECORD_AUDIO)
    add(Manifest.permission.ACCESS_FINE_LOCATION)
    add(Manifest.permission.CALL_PHONE)
    add(Manifest.permission.SEND_SMS)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        add(Manifest.permission.POST_NOTIFICATIONS)
    }
}

internal const val TERMS_OF_USE_URL = "https://aurorasecurity.app/terms.html"
internal const val PUSH_BIND_BASE_URL = "https://aurorasecurity.app/push-bind"
internal const val PUSH_BIND_HOST = "aurorasecurity.app"
internal const val PUSH_BIND_PATH = "/push-bind"
internal const val SETTINGS_SECTION_CONTACTS = "contacts"
internal const val SETTINGS_SECTION_CONTACTS_TELEGRAM = "contacts_telegram"
internal const val SETTINGS_SECTION_CONTACTS_PUSH = "contacts_push"
internal const val SETTINGS_SECTION_CONTACTS_SMS = "contacts_sms"
internal const val SETTINGS_SECTION_CONTACTS_PHONE = "contacts_phone"
internal const val SETTINGS_SECTION_PUSH_BIND = "push_bind"
internal const val HISTORY_FILTER_ALL = "all"

internal enum class AlarmTab(@param:StringRes val labelRes: Int, val icon: ImageVector) {
    Detect(R.string.tab_detect, Icons.Outlined.Security),
    AI(R.string.tab_ai, Icons.Outlined.AutoAwesome),
    History(R.string.tab_history, Icons.Outlined.History),
    Settings(R.string.tab_settings, Icons.Outlined.Settings),
}

internal fun buildPushSetupLink(bindCode: String): String {
    return Uri.parse(PUSH_BIND_BASE_URL)
        .buildUpon()
        .appendQueryParameter("code", bindCode.trim().uppercase())
        .build()
        .toString()
}

internal fun extractPushBindCodeFromUri(uri: Uri?): String? {
    if (uri == null) return null
    if (!uri.scheme.equals("https", ignoreCase = true)) return null
    if (!uri.host.equals(PUSH_BIND_HOST, ignoreCase = true)) return null
    if (uri.path?.trimEnd('/') != PUSH_BIND_PATH) return null

    val sanitizedCode = uri.getQueryParameter("code")
        ?.uppercase()
        ?.filter { it.isLetterOrDigit() }
        .orEmpty()

    return sanitizedCode.ifBlank { null }
}

internal enum class SecurityModePreset(
    @param:StringRes val labelRes: Int,
    @param:StringRes val descriptionRes: Int,
    val alarmThresholdDb: Float,
    val shakeDetectionEnabled: Boolean,
    val movementThresholdG: Float?,
    val preloadAiEngine: Boolean,
    val sosDispatchMode: SosDispatchMode,
) {
    Commute(
        labelRes = R.string.preset_commute,
        descriptionRes = R.string.preset_commute_description,
        alarmThresholdDb = 90f,
        shakeDetectionEnabled = true,
        movementThresholdG = 3.0f,
        preloadAiEngine = false,
        sosDispatchMode = SosDispatchMode.AiFirst,
    ),
    Travel(
        labelRes = R.string.preset_travel,
        descriptionRes = R.string.preset_travel_description,
        alarmThresholdDb = 85f,
        shakeDetectionEnabled = true,
        movementThresholdG = 2.0f,
        preloadAiEngine = true,
        sosDispatchMode = SosDispatchMode.Immediate,
    ),
    Workout(
        labelRes = R.string.preset_workout,
        descriptionRes = R.string.preset_workout_description,
        alarmThresholdDb = 100f,
        shakeDetectionEnabled = true,
        movementThresholdG = 4.5f,
        preloadAiEngine = false,
        sosDispatchMode = SosDispatchMode.AiFirst,
    ),
    NightWalk(
        labelRes = R.string.preset_night_walk,
        descriptionRes = R.string.preset_night_walk_description,
        alarmThresholdDb = 80f,
        shakeDetectionEnabled = false,
        movementThresholdG = null,
        preloadAiEngine = true,
        sosDispatchMode = SosDispatchMode.Immediate,
    ),
    Custom(
        labelRes = R.string.preset_custom,
        descriptionRes = R.string.preset_custom_description,
        alarmThresholdDb = 0f,
        shakeDetectionEnabled = false,
        movementThresholdG = null,
        preloadAiEngine = false,
        sosDispatchMode = SosDispatchMode.Immediate,
    ),
}

internal fun defaultContactMethodMessage(context: Context): String {
    return context.getString(R.string.contact_method_default_message)
}

internal fun contactMethodRequiredMessage(context: Context): String {
    return context.getString(R.string.contact_method_required_message)
}

internal fun AlarmTab.label(context: Context): String {
    return context.getString(labelRes)
}

internal fun SecurityModePreset.label(context: Context): String {
    return context.getString(labelRes)
}

internal fun SecurityModePreset.description(context: Context): String {
    return context.getString(descriptionRes)
}

internal fun localizeAiStatusLabel(
    context: Context,
    aiState: AiFeatureUiState,
): String {
    return when {
        aiState.modelStatus == AiModelStatus.Downloaded -> context.getString(R.string.ai_status_downloaded)
        aiState.modelStatus == AiModelStatus.Ready -> context.getString(R.string.ai_status_ready)
        aiState.modelStatus == AiModelStatus.Error -> context.getString(R.string.ai_status_unavailable)
        else -> context.getString(R.string.ai_status_pending)
    }
}

internal fun localizeAiStatusMessage(
    context: Context,
    message: String,
): String {
    return when {
        message == "Not initialized" -> context.getString(R.string.ai_backend_not_initialized)
        message == "Model not downloaded yet." -> context.getString(R.string.ai_status_message_not_downloaded)
        message.startsWith("Download ") && message.endsWith(" to enable local audio analysis.") ->
            context.getString(R.string.ai_status_message_download_prompt)
        message == "Model downloaded. Initialize the AI engine before enabling AI analysis." ->
            context.getString(R.string.ai_status_message_downloaded)
        message == "Model ready for on-device audio analysis." ->
            context.getString(R.string.ai_status_message_ready)
        message == "Initializing Gemma audio engine..." ->
            context.getString(R.string.ai_status_message_initializing)
        message == "Model downloaded. Preparing LiteRT-LM engine..." ->
            context.getString(R.string.ai_status_message_download_preparing)
        else -> message
    }
}

internal fun localizeAiAccelerator(
    context: Context,
    accelerator: String,
): String {
    return when (accelerator) {
        "Not initialized" -> context.getString(R.string.ai_backend_not_initialized)
        else -> accelerator
    }
}

internal fun TelegramBindingStatus.label(context: Context): String {
    return when (this) {
        TelegramBindingStatus.Pending -> context.getString(R.string.status_bound_pending)
        TelegramBindingStatus.Bound -> context.getString(R.string.status_bound_complete)
    }
}

internal fun SecurityModePreset.matches(
    thresholdDb: Float,
    movementSettings: MovementSettings,
    aiSettings: AiSettings,
): Boolean {
    if (this == SecurityModePreset.Custom) return false
    val thresholdMatches = kotlin.math.abs(thresholdDb - alarmThresholdDb) < 0.05f
    val shakeMatches = movementSettings.useShakeDetection == shakeDetectionEnabled
    val movementMatches = if (shakeDetectionEnabled) {
        movementSettings.useShakeDetection &&
            movementThresholdG != null &&
            kotlin.math.abs(movementSettings.shakeThresholdG - movementThresholdG) < 0.05f
    } else {
        true
    }
    val preloadMatches = !aiSettings.enableAiAnalysis ||
        aiSettings.preloadAiInBackground == preloadAiEngine
    val dispatchMatches = aiSettings.sosDispatchMode == sosDispatchMode
    return thresholdMatches &&
        shakeMatches &&
        movementMatches &&
        preloadMatches &&
        dispatchMatches
}

internal fun currentSecurityModePreset(
    thresholdDb: Float,
    movementSettings: MovementSettings,
    aiSettings: AiSettings,
): SecurityModePreset {
    return SecurityModePreset.entries.firstOrNull { preset ->
        preset.matches(thresholdDb, movementSettings, aiSettings)
    } ?: SecurityModePreset.Custom
}

internal data class HeroAiBadgeColors(
    val container: ComposeColor,
    val content: ComposeColor,
)

internal fun heroAiStatusLabel(
    context: Context,
    aiSettings: AiSettings,
    aiState: AiFeatureUiState,
): String? {
    return when {
        !aiSettings.enableAiAnalysis -> null
        !aiSettings.preloadAiInBackground -> context.getString(R.string.ai_status_pending)
        aiState.modelStatus == AiModelStatus.Downloading ||
            aiState.modelStatus == AiModelStatus.Initializing -> context.getString(R.string.ai_status_pending)
        else -> localizeAiStatusLabel(context, aiState)
    }
}

internal fun isAiAnalysisReady(
    aiSettings: AiSettings,
    aiState: AiFeatureUiState,
): Boolean {
    return aiState.currentModelType.name == aiSettings.modelType &&
        aiState.modelStatus == AiModelStatus.Ready
}

internal fun clampAiSettingsForDevice(
    aiSettings: AiSettings,
    aiDeviceSupport: AiDeviceSupport,
): AiSettings {
    val fallbackModel = aiDeviceSupport.defaultModelType.name
    val resolvedModelType = aiSettings.modelType
        .takeIf { candidate ->
            GemmaModelType.entries.any { it.name == candidate }
        }
        ?: fallbackModel

    return if (!aiDeviceSupport.supportsOnDeviceAi) {
        aiSettings.copy(
            isAiEnabled = false,
            enableAiAnalysis = false,
            preloadAiInBackground = false,
            sosDispatchMode = SosDispatchMode.Immediate,
            modelType = fallbackModel,
        )
    } else {
        aiSettings.copy(
            enableAiAnalysis = aiSettings.isAiEnabled,
            sosDispatchMode = if (aiSettings.isAiEnabled) {
                aiSettings.sosDispatchMode
            } else {
                SosDispatchMode.Immediate
            },
            modelType = resolvedModelType,
        )
    }
}

internal suspend fun prepareAiEngineIfNeeded(
    context: Context,
    aiSettings: AiSettings,
) {
    val currentModel = GemmaModelType.fromName(aiSettings.modelType)
    GemmaAudioAnalysisManager.refreshState(context, currentModel)
    if (!aiSettings.enableAiAnalysis) return

    if (GemmaAudioAnalysisManager.isModelDownloaded(context, currentModel)) {
        runCatching {
            GemmaAudioAnalysisManager.ensureInitialized(context, currentModel)
        }
    } else if (aiSettings.preloadAiInBackground) {
        runCatching {
            GemmaAudioAnalysisManager.downloadIfNeeded(context, currentModel)
        }
    }
}

internal val SETTINGS_PRESET_ROWS = listOf(
    listOf(SecurityModePreset.Commute, SecurityModePreset.Travel),
    listOf(SecurityModePreset.Workout, SecurityModePreset.NightWalk),
)

internal fun heroAiBadgeColors(isDark: Boolean): HeroAiBadgeColors {
    return if (isDark) {
        HeroAiBadgeColors(
            container = ComposeColor.White.copy(alpha = 0.18f),
            content = Frost,
        )
    } else {
        HeroAiBadgeColors(
            container = ComposeColor(0xDDE8EFEA),
            content = ComposeColor(0xFF6E7D7B),
        )
    }
}

internal fun launchEmergencyCall(context: Context, phoneNumber: String) {
    val sanitizedNumber = phoneNumber.trim()
    if (sanitizedNumber.isBlank()) return
    val callIntent = Intent(
        Intent.ACTION_CALL,
        Uri.parse("tel:${Uri.encode(sanitizedNumber)}"),
    )
    context.startActivity(callIntent)
}
