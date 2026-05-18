package app.aurorasecurity.security

import android.content.Context
import android.media.MediaPlayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

internal data class EventPalette(
    val headerBrush: Brush,
    val headerTextColor: Color,
    val sectionBorder: Color,
    val sectionBackground: Color,
    val accent: Color,
    val accentSoft: Color,
)

internal data class HistoryPresentation(
    val type: HistoryRecordType,
    val sourceBadge: String,
    val title: String,
    val subtitle: String,
    val alertMessage: String,
    val date: String,
    val time: String,
    val device: String,
    val mobileNumber: String,
    val currentSoundLevel: String,
    val textAlertDeliveryStatusLabel: String?,
    val location: String,
    val locationLink: String?,
    val aiLines: List<String>,
)

internal data class HistoryDangerLevelVisual(
    val label: String,
    val dotColor: Color,
    val textColor: Color,
    val backgroundColor: Color,
)

internal data class HistoryAiAnalysisItem(
    val label: String?,
    val value: String,
    val dangerLevel: String? = null,
)

internal enum class HistoryFilter(val labelRes: Int) {
    All(R.string.history_filter_all),
    Mine(R.string.history_filter_mine),
    Incoming(R.string.history_filter_incoming),
}

internal fun historyFilterFromKey(filterKey: String?): HistoryFilter? {
    return when (filterKey?.trim()?.lowercase(Locale.getDefault())) {
        "all" -> HistoryFilter.All
        "mine" -> HistoryFilter.Mine
        "incoming" -> HistoryFilter.Incoming
        else -> null
    }
}

internal const val HISTORY_TAB_TAG = "HistoryTab"
internal const val HISTORY_AUDIO_FILE_EXTENSION = "m4a"
internal const val HISTORY_DATE_FORMAT = "yyyy-MM-dd"
internal const val HISTORY_TIME_FORMAT = "HH:mm:ss"
internal const val HISTORY_HEADER_FORMAT = "MMM dd, yyyy, HH:mm"

internal fun resolvePlayableAudioFile(audioFilePath: String): File? {
    if (audioFilePath.isBlank()) return null
    val directFile = File(audioFilePath)
    return directFile.takeIf {
        it.exists() && it.extension.equals(HISTORY_AUDIO_FILE_EXTENSION, ignoreCase = true)
    }
}

internal fun releaseMediaPlayer(player: MediaPlayer?): MediaPlayer? {
    runCatching { player?.stop() }
    runCatching { player?.release() }
    return null
}

internal fun formatHistoryDate(timestamp: Date, pattern: String): String {
    return SimpleDateFormat(pattern, Locale.getDefault()).format(timestamp)
}

internal fun parseDangerLevel(aiLine: String): String? {
    val normalized = aiLine.removePrefix("-").trim()
    if (!normalized.startsWith("Danger Level:", ignoreCase = true)) return null
    return normalized.substringAfter(":", "").trim().ifBlank { null }
}

internal fun historyDangerLevelVisual(
    context: Context,
    level: String,
    isDark: Boolean,
): HistoryDangerLevelVisual {
    return when (level.trim().lowercase()) {
        "danger" -> HistoryDangerLevelVisual(
            label = context.getString(R.string.danger_level_danger),
            dotColor = Color(0xFFE24A4A),
            textColor = if (isDark) Color(0xFFFFD8D8) else Color(0xFF5A1F24),
            backgroundColor = if (isDark) Color(0xFF5A1F24) else Color(0xFFFFD8D8),
        )
        "high" -> HistoryDangerLevelVisual(
            label = context.getString(R.string.danger_level_high),
            dotColor = Color(0xFFFF8A4C),
            textColor = if (isDark) Color(0xFFFFE0CF) else Color(0xFF5B3123),
            backgroundColor = if (isDark) Color(0xFF5B3123) else Color(0xFFFFE0CF),
        )
        "medium" -> HistoryDangerLevelVisual(
            label = context.getString(R.string.danger_level_medium),
            dotColor = Color(0xFFF2C14E),
            textColor = if (isDark) Color(0xFFFFF0C9) else Color(0xFF54461F),
            backgroundColor = if (isDark) Color(0xFF54461F) else Color(0xFFFFF0C9),
        )
        else -> HistoryDangerLevelVisual(
            label = context.getString(R.string.danger_level_low),
            dotColor = Color(0xFFFF6E6E),
            textColor = if (isDark) Color(0xFFFFDCDC) else Color(0xFF4B2630),
            backgroundColor = if (isDark) Color(0xFF4B2630) else Color(0xFFFFDCDC),
        )
    }
}

internal fun localizeHistoryAiLine(
    context: Context,
    line: String,
): String {
    return when (line.trim()) {
        "AI analysis unavailable. Download and initialize the model first." ->
            context.getString(R.string.history_ai_unavailable_download_initialize)
        "AI analysis unavailable. The model is downloaded, but the AI engine is not initialized yet." ->
            context.getString(R.string.history_ai_unavailable_model_downloaded)
        "AI analysis unavailable. The model is still downloading." ->
            context.getString(R.string.history_ai_unavailable_model_downloading)
        "AI analysis unavailable. The AI engine is still initializing." ->
            context.getString(R.string.history_ai_unavailable_engine_initializing)
        else -> line
    }
}

internal fun localizeHistoryAiLabel(
    context: Context,
    label: String,
): String {
    return when (label.trim().uppercase(Locale.ROOT)) {
        "DANGER LEVEL" -> context.getString(R.string.danger_level_title)
        "ACOUSTIC ENVIRONMENT" -> context.getString(R.string.history_ai_label_acoustic_environment)
        "DISRUPTIVE SOUNDS" -> context.getString(R.string.history_ai_label_disruptive_sounds)
        "VOCAL STRESS & EMOTION" -> context.getString(R.string.history_ai_label_vocal_stress_emotion)
        "SPEECH CONTENT" -> context.getString(R.string.history_ai_label_speech_content)
        "SITUATION ASSESSMENT" -> context.getString(R.string.history_ai_label_situation_assessment)
        else -> label
    }
}

internal fun localizeHistoryAiValue(
    context: Context,
    value: String,
): String {
    return when (value.trim()) {
        "None" -> context.getString(R.string.history_ai_value_none)
        "Normal" -> context.getString(R.string.history_ai_value_normal)
        "No immediate threat detected." -> context.getString(R.string.history_ai_value_no_immediate_threat)
        else -> value
    }
}

internal fun parseHistoryAiAnalysisItem(
    context: Context,
    line: String,
): HistoryAiAnalysisItem {
    val normalized = line.removePrefix("-").trim()
    val dangerLevel = parseDangerLevel(normalized)
    if (dangerLevel != null) {
        return HistoryAiAnalysisItem(
            label = context.getString(R.string.danger_level_title),
            value = localizeHistoryAiValue(context, dangerLevel),
            dangerLevel = dangerLevel,
        )
    }

    val separatorIndex = normalized.indexOf(':')
    if (separatorIndex <= 0 || separatorIndex >= normalized.lastIndex) {
        return HistoryAiAnalysisItem(
            label = null,
            value = localizeHistoryAiValue(context, normalized),
        )
    }

    return HistoryAiAnalysisItem(
        label = localizeHistoryAiLabel(context, normalized.substring(0, separatorIndex).trim()),
        value = localizeHistoryAiValue(context, normalized.substring(separatorIndex + 1).trim()),
    )
}

internal fun HistoryRecord.recordType(): HistoryRecordType {
    return runCatching { HistoryRecordType.valueOf(type) }
        .getOrDefault(HistoryRecordType.LocalAlert)
}

internal fun HistoryRecord.isIncomingRecord(): Boolean {
    return recordType() == HistoryRecordType.IncomingPush
}

internal fun HistoryRecord.isIncomingAiRecord(): Boolean {
    return isIncomingRecord() && messageType == "ai_analysis"
}

internal fun HistoryRecord.matchesFilter(filter: HistoryFilter): Boolean {
    return when (filter) {
        HistoryFilter.All -> true
        HistoryFilter.Mine -> !isIncomingRecord()
        HistoryFilter.Incoming -> isIncomingRecord()
    }
}

internal fun eventPalette(
    type: HistoryRecordType,
    isTest: Boolean,
    isDark: Boolean,
): EventPalette {
    return if (isTest) {
        EventPalette(
            headerBrush = Brush.horizontalGradient(
                listOf(
                    if (isDark) Color(0xFF4D607A) else Color(0xFF74869D),
                    if (isDark) Color(0xFF6E6375) else Color(0xFF93868A),
                ),
            ),
            headerTextColor = Color.White,
            sectionBorder = if (isDark) Color(0xFF566A84) else Color(0xFF8A9AB0),
            sectionBackground = if (isDark) Color(0xFF27303A) else Color(0xFFF4F1EC),
            accent = if (isDark) Color(0xFFB7C6D9) else Color(0xFF5D708C),
            accentSoft = if (isDark) Color(0xFF73849A) else Color(0xFFB8C4D1),
        )
    } else if (type == HistoryRecordType.IncomingPush) {
        EventPalette(
            headerBrush = Brush.horizontalGradient(
                listOf(
                    if (isDark) Color(0xFF35597B) else Color(0xFF5F86AF),
                    if (isDark) Color(0xFF3E7D85) else Color(0xFF74A8A6),
                ),
            ),
            headerTextColor = Color.White,
            sectionBorder = if (isDark) Color(0xFF517690) else Color(0xFFA6C1D8),
            sectionBackground = if (isDark) Color(0xFF253240) else Color(0xFFF0F6FA),
            accent = if (isDark) Color(0xFF9FD3E0) else Color(0xFF4F84A8),
            accentSoft = if (isDark) Color(0xFF4E6F80) else Color(0xFFBCD7E5),
        )
    } else {
        EventPalette(
            headerBrush = Brush.horizontalGradient(
                listOf(
                    if (isDark) Color(0xFF9E4F46) else Color(0xFFC56A5D),
                    if (isDark) Color(0xFFBC7468) else Color(0xFFD88B7D),
                ),
            ),
            headerTextColor = Color.White,
            sectionBorder = if (isDark) Color(0xFF8E5B54) else Color(0xFFC9948B),
            sectionBackground = if (isDark) Color(0xFF2A2F35) else Color(0xFFF6EFE8),
            accent = if (isDark) Color(0xFFF0B4AA) else Color(0xFFB95F53),
            accentSoft = if (isDark) Color(0xFF9F6A61) else Color(0xFFE0B1A9),
        )
    }
}
