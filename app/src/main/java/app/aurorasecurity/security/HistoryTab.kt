package app.aurorasecurity.security

import android.media.AudioAttributes
import android.media.MediaPlayer
import android.util.Log
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.aurorasecurity.security.ui.theme.AuroraBlue
import app.aurorasecurity.security.ui.theme.AuroraTeal
import app.aurorasecurity.security.ui.theme.Ember
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileInputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


@Composable
fun HistoryTab(
    historyManager: HistoryManager,
    requestedFilterKey: String? = null,
    requestedFilterVersion: Int = 0,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val records by historyManager.recordsFlow.collectAsState(initial = emptyList())
    var selectedFilter by rememberSaveable(requestedFilterKey) {
        mutableStateOf(historyFilterFromKey(requestedFilterKey) ?: HistoryFilter.All)
    }
    var playingId by remember { mutableStateOf<String?>(null) }
    var showDeleteDialogForId by remember { mutableStateOf<String?>(null) }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    val coroutineScope = rememberCoroutineScope()
    val filteredRecords = remember(records, selectedFilter) {
        records.filter { record -> record.matchesFilter(selectedFilter) }
    }
    LaunchedEffect(requestedFilterVersion, requestedFilterKey) {
        historyFilterFromKey(requestedFilterKey)?.let { selectedFilter = it }
    }

    DisposableEffect(Unit) {
        onDispose {
            mediaPlayer = releaseMediaPlayer(mediaPlayer)
        }
    }

    fun stopPlayback(clearPlayingId: Boolean = true) {
        mediaPlayer = releaseMediaPlayer(mediaPlayer)
        if (clearPlayingId) {
            playingId = null
        }
    }

    fun playRecord(record: HistoryRecord) {
        val isRemoteAudio = isRemoteAudioUrl(record.audioFilePath)
        val playableFile = if (isRemoteAudio) null else resolvePlayableAudioFile(record.audioFilePath)
        if (!isRemoteAudio && playableFile == null) {
            throw IllegalStateException("Audio file unavailable")
        }
        val targetRecordId = record.id
        val player = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build(),
            )
            if (isRemoteAudio) {
                setDataSource(record.audioFilePath)
            } else {
                FileInputStream(playableFile!!).use { input ->
                    setDataSource(input.fd)
                }
            }
            setVolume(1f, 1f)
            setOnErrorListener { failedPlayer, what, extra ->
                Log.e(
                    HISTORY_TAB_TAG,
                    "MediaPlayer error while playing ${record.audioFilePath}, what=$what, extra=$extra",
                )
                runCatching { failedPlayer.reset() }
                runCatching { failedPlayer.release() }
                if (mediaPlayer == this) mediaPlayer = null
                if (playingId == targetRecordId) playingId = null
                Toast.makeText(
                    context,
                    context.getString(R.string.history_audio_playback_error),
                    Toast.LENGTH_SHORT,
                ).show()
                true
            }
            setOnCompletionListener {
                runCatching { it.release() }
                if (mediaPlayer == this) mediaPlayer = null
                if (playingId == targetRecordId) playingId = null
            }
            if (isRemoteAudio) {
                setOnPreparedListener { preparedPlayer ->
                    if (mediaPlayer == preparedPlayer && playingId == targetRecordId) {
                        preparedPlayer.start()
                    }
                }
                prepareAsync()
            } else {
                prepare()
                start()
            }
        }
        mediaPlayer = player
        playingId = targetRecordId
    }

    showDeleteDialogForId?.let { recordId ->
        AlertDialog(
            onDismissRequest = { showDeleteDialogForId = null },
            icon = {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    tint = Ember,
                    modifier = Modifier.size(28.dp),
                )
            },
            title = {
                Text(
                    text = stringResource(R.string.history_delete_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.history_delete_message),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (playingId == recordId) {
                            stopPlayback()
                        }
                        coroutineScope.launch {
                            historyManager.deleteRecord(recordId)
                        }
                        showDeleteDialogForId = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError,
                    ),
                ) {
                    Text(stringResource(R.string.common_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialogForId = null }) {
                    Text(stringResource(R.string.common_cancel))
                }
            },
        )
    }

    if (records.isEmpty()) {
        EmptyHistoryState(modifier = modifier)
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        item {
            HistoryFilterBar(
                selectedFilter = selectedFilter,
                onFilterSelected = { selectedFilter = it },
            )
        }

        if (filteredRecords.isEmpty()) {
            item {
                FilteredHistoryEmptyState(selectedFilter = selectedFilter)
            }
        } else {
            items(filteredRecords, key = { it.id }) { record ->
                HistoryRecordCard(
                    record = record,
                    isPlaying = playingId == record.id,
                    onPlayPause = {
                        if (playingId == record.id) {
                            stopPlayback()
                        } else {
                            stopPlayback(clearPlayingId = false)
                            try {
                                playRecord(record)
                            } catch (e: Exception) {
                                val resolvedPath = resolvePlayableAudioFile(record.audioFilePath)?.absolutePath
                                Log.e(
                                    HISTORY_TAB_TAG,
                                    "Failed to play history audio. savedPath=${record.audioFilePath}, resolvedPath=$resolvedPath",
                                    e,
                                )
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.history_audio_playback_error),
                                    Toast.LENGTH_SHORT,
                                ).show()
                                playingId = null
                                mediaPlayer = null
                            }
                        }
                    },
                    onDelete = { showDeleteDialogForId = record.id },
                )
            }
        }
    }
}

@Composable
private fun HistoryFilterBar(
    selectedFilter: HistoryFilter,
    onFilterSelected: (HistoryFilter) -> Unit,
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val containerColor = if (isDark) {
        MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
    } else {
        MaterialTheme.colorScheme.surface
    }
    val borderColor = if (isDark) {
        Color.White.copy(alpha = 0.08f)
    } else {
        Color(0x140F1E2D)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(containerColor)
            .border(
                width = 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(22.dp),
            )
            .padding(6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        HistoryFilter.entries.forEach { filter ->
            val isSelected = filter == selectedFilter
            val selectedBrush = if (isDark) {
                Brush.horizontalGradient(
                    colors = listOf(
                        Color(0xFF335778),
                        Color(0xFF4A7F8A),
                    ),
                )
            } else {
                Brush.horizontalGradient(
                    colors = listOf(
                        Color(0xFFE9EEF4),
                        Color(0xFFDCECEF),
                    ),
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        if (isSelected) selectedBrush else Brush.linearGradient(
                            colors = listOf(Color.Transparent, Color.Transparent),
                        ),
                    )
                    .clickable { onFilterSelected(filter) }
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(filter.labelRes),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isSelected) {
                        if (isDark) Color.White else Color(0xFF294257)
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
        }
    }
}

@Composable
private fun FilteredHistoryEmptyState(
    selectedFilter: HistoryFilter,
) {
    val message = when (selectedFilter) {
        HistoryFilter.All -> stringResource(R.string.history_no_history)
        HistoryFilter.Mine -> stringResource(R.string.history_no_personal_records)
        HistoryFilter.Incoming -> stringResource(R.string.history_no_incoming_records)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f))
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f),
                shape = RoundedCornerShape(24.dp),
            )
            .padding(horizontal = 20.dp, vertical = 28.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun EmptyHistoryState(modifier: Modifier = Modifier) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val titleColor = if (isDark) Color(0xFFF6F0E7) else Color(0xFF263446)
    val bodyColor = if (isDark) Color(0xFFC5CDD7) else Color(0xFF5E6B7E)
    val badgeBorder = if (isDark) {
        Brush.linearGradient(
            colors = listOf(
                Color(0xFF4E657F).copy(alpha = 0.74f),
                AuroraTeal.copy(alpha = 0.24f),
            ),
        )
    } else {
        Brush.linearGradient(
            colors = listOf(
                Color(0xFFE7DCCF),
                Color(0xFFF4EBE0),
            ),
        )
    }
    val badgeBackground = if (isDark) {
        Brush.linearGradient(
            colors = listOf(
                Color(0xFF162232),
                Color(0xFF101926),
            ),
        )
    } else {
        Brush.linearGradient(
            colors = listOf(
                Color(0xFFF8F2EA),
                Color(0xFFF1E7DB),
            ),
        )
    }
    val haloBackground = if (isDark) {
        Brush.radialGradient(
            colors = listOf(
                AuroraBlue.copy(alpha = 0.16f),
                Color.Transparent,
            ),
        )
    } else {
        Brush.radialGradient(
            colors = listOf(
                Color(0xFFEEE4D8),
                Color.Transparent,
            ),
        )
    }
    val iconHalo = if (isDark) {
        Brush.radialGradient(
            colors = listOf(
                AuroraTeal.copy(alpha = 0.24f),
                Color.Transparent,
            ),
        )
    } else {
        Brush.radialGradient(
            colors = listOf(
                AuroraBlue.copy(alpha = 0.10f),
                Color.Transparent,
            ),
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 26.dp, vertical = 32.dp),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier.padding(top = 64.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(28.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(148.dp)
                    .background(brush = haloBackground, shape = RoundedCornerShape(42.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(112.dp)
                        .clip(RoundedCornerShape(34.dp))
                        .background(
                            brush = badgeBackground,
                            shape = RoundedCornerShape(34.dp),
                        )
                        .border(
                            width = 1.dp,
                            brush = badgeBorder,
                            shape = RoundedCornerShape(34.dp),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .background(brush = iconHalo, shape = RoundedCornerShape(22.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.AutoAwesome,
                            contentDescription = null,
                            tint = if (isDark) Color(0xFFF4EEE5) else Color(0xFF5A6D86),
                            modifier = Modifier.size(28.dp),
                        )
                    }
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(28.dp),
            ) {
                Text(
                    text = stringResource(R.string.history_empty_title),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = titleColor,
                    textAlign = TextAlign.Center,
                    lineHeight = 38.sp,
                    modifier = Modifier.fillMaxWidth(0.90f),
                )
                Text(
                    text = stringResource(R.string.history_empty_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = bodyColor,
                    lineHeight = 28.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth(0.72f)
                        .padding(top = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun HistoryRecordCard(
    record: HistoryRecord,
    isPlaying: Boolean,
    onPlayPause: () -> Unit,
    onDelete: () -> Unit,
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val context = LocalContext.current
    val presentation = remember(record, context) { presentRecord(record, context) }
    val palette = remember(presentation.type, record.isTest, isDark) {
        eventPalette(type = presentation.type, isTest = record.isTest, isDark = isDark)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(
                if (isDark) MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
                else MaterialTheme.colorScheme.surface,
            )
            .border(
                width = 1.dp,
                color = palette.sectionBorder.copy(alpha = if (isDark) 0.55f else 0.7f),
                shape = RoundedCornerShape(28.dp),
            )
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        EventHeader(
            sourceBadge = presentation.sourceBadge,
            title = presentation.title,
            subtitle = presentation.subtitle,
            palette = palette,
            type = presentation.type,
            isTest = record.isTest,
            onDelete = onDelete,
        )

        HistoryInfoSection(
            title = stringResource(R.string.history_section_alert_message),
            accent = palette.accent,
            background = palette.sectionBackground,
            border = palette.sectionBorder,
        ) {
            Text(
                text = presentation.alertMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = 22.sp,
            )
        }

        HistoryInfoSection(
            title = stringResource(R.string.history_section_incident_details),
            accent = palette.accent,
            background = palette.sectionBackground,
            border = palette.sectionBorder,
        ) {
            MetadataRow(stringResource(R.string.history_metadata_date), presentation.date)
            MetadataRow(stringResource(R.string.history_metadata_time), presentation.time)
            MetadataRow(stringResource(R.string.history_metadata_device), presentation.device)
            MetadataRow(stringResource(R.string.history_metadata_mobile), presentation.mobileNumber)
            MetadataRow(stringResource(R.string.history_metadata_sound), presentation.currentSoundLevel)
            LocationRow(
                locationLabel = presentation.location,
                locationLink = presentation.locationLink,
                accent = palette.accent,
            )
            presentation.textAlertDeliveryStatusLabel?.let { statusLabel ->
                MetadataRow(stringResource(R.string.history_metadata_status), statusLabel)
            }
        }

        val hasPlayableAudio = isPlayableAudioReference(record.audioFilePath)
        if (presentation.type != HistoryRecordType.IncomingPush) {
            HistoryInfoSection(
                title = stringResource(R.string.history_section_sound_ai),
                accent = palette.accent,
                background = palette.sectionBackground,
                border = palette.sectionBorder,
            ) {
                AudioPlaybackRow(
                    record = record,
                    isPlaying = isPlaying,
                    accent = palette.accent,
                    accentSoft = palette.accentSoft,
                    onPlayPause = onPlayPause,
                )

                HistoryAnalysisContent(
                    aiLines = presentation.aiLines,
                    accent = palette.accent,
                    border = palette.sectionBorder,
                )
            }
        } else if (hasPlayableAudio || presentation.aiLines.isNotEmpty()) {
            HistoryInfoSection(
                title = stringResource(
                    if (hasPlayableAudio) {
                        R.string.history_section_sound_ai
                    } else {
                        R.string.history_section_ai_analysis
                    },
                ),
                accent = palette.accent,
                background = palette.sectionBackground,
                border = palette.sectionBorder,
            ) {
                if (hasPlayableAudio) {
                    AudioPlaybackRow(
                        record = record,
                        isPlaying = isPlaying,
                        accent = palette.accent,
                        accentSoft = palette.accentSoft,
                        onPlayPause = onPlayPause,
                    )
                }
                if (presentation.aiLines.isNotEmpty()) {
                    HistoryAnalysisContent(
                        aiLines = presentation.aiLines,
                        accent = palette.accent,
                        border = palette.sectionBorder,
                    )
                }
            }
        }
    }
}

@Composable
private fun HistoryAnalysisContent(
    aiLines: List<String>,
    accent: Color,
    border: Color,
) {
    val context = LocalContext.current
    if (aiLines.isNotEmpty()) {
        val analysisItems = remember(aiLines, context) {
            aiLines.map { parseHistoryAiAnalysisItem(context, it) }
        }
        HorizontalDivider(
            modifier = Modifier.padding(vertical = 12.dp),
            color = border.copy(alpha = 0.45f),
            thickness = 0.75.dp,
        )
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            analysisItems.forEach { item ->
                if (item.dangerLevel != null) {
                    HistoryDangerLevelBadge(level = item.dangerLevel)
                } else {
                    HistoryAiAnalysisCard(
                        item = item,
                        accent = accent,
                        border = border,
                    )
                }
            }
        }
    } else {
        HorizontalDivider(
            modifier = Modifier.padding(vertical = 12.dp),
            color = border.copy(alpha = 0.45f),
            thickness = 0.75.dp,
        )
        Text(
            text = stringResource(R.string.history_no_ai_analysis),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun HistoryAiAnalysisCard(
    item: HistoryAiAnalysisItem,
    accent: Color,
    border: Color,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.42f))
            .border(
                width = 1.dp,
                color = border.copy(alpha = 0.30f),
                shape = RoundedCornerShape(16.dp),
            )
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        item.label?.let { label ->
            Text(
                text = label.uppercase(Locale.getDefault()),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.9.sp,
                color = accent,
            )
        }
        Text(
            text = item.value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            lineHeight = 22.sp,
        )
    }
}

@Composable
private fun HistoryDangerLevelBadge(level: String) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val context = LocalContext.current
    val visual = remember(level, isDark, context) { historyDangerLevelVisual(context, level, isDark) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.42f))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.danger_level_title),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            lineHeight = 21.sp,
        )
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(visual.backgroundColor)
                .padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(9.dp)
                    .background(visual.dotColor, CircleShape),
            )
            Text(
                text = visual.label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = visual.textColor,
            )
        }
    }
}

@Composable
private fun EventHeader(
    sourceBadge: String,
    title: String,
    subtitle: String,
    palette: EventPalette,
    type: HistoryRecordType,
    isTest: Boolean,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(palette.headerBrush)
            .padding(start = 18.dp, end = 10.dp, top = 16.dp, bottom = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .background(Color.White.copy(alpha = 0.18f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = when {
                    type == HistoryRecordType.IncomingPush -> Icons.Outlined.AutoAwesome
                    isTest -> Icons.Outlined.AutoAwesome
                    else -> Icons.Outlined.ErrorOutline
                },
                contentDescription = null,
                tint = palette.headerTextColor,
                modifier = Modifier.size(20.dp),
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            HistorySourceBadge(
                label = sourceBadge,
                textColor = palette.headerTextColor,
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = palette.headerTextColor,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = palette.headerTextColor.copy(alpha = 0.92f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Surface(
            modifier = Modifier.size(44.dp),
            shape = CircleShape,
            color = Color.White.copy(alpha = 0.18f),
            onClick = onDelete,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = stringResource(R.string.history_delete_record_content_description),
                    tint = palette.headerTextColor,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

@Composable
private fun HistorySourceBadge(
    label: String,
    textColor: Color,
) {
    Text(
        text = label.uppercase(Locale.getDefault()),
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(Color.White.copy(alpha = 0.18f))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.ExtraBold,
        letterSpacing = 0.7.sp,
        color = textColor,
    )
}

@Composable
private fun HistoryInfoSection(
    title: String,
    accent: Color,
    background: Color,
    border: Color,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(background)
            .border(
                width = 1.dp,
                color = border,
                shape = RoundedCornerShape(22.dp),
            )
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(accent, CircleShape),
            )
            Text(
                text = title.uppercase(Locale.getDefault()),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.1.sp,
                color = accent,
            )
        }
        content()
    }
}

@Composable
private fun MetadataRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "$label:",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(86.dp),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun LocationRow(
    locationLabel: String,
    locationLink: String?,
    accent: Color,
) {
    val uriHandler = LocalUriHandler.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.history_location_label),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(86.dp),
        )
        if (locationLink != null) {
            Text(
                text = stringResource(R.string.history_google_maps),
                style = MaterialTheme.typography.bodyMedium,
                color = accent,
                textDecoration = TextDecoration.Underline,
                modifier = Modifier
                    .weight(1f)
                    .clickable { uriHandler.openUri(locationLink) },
            )
        } else {
            Text(
                text = locationLabel,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun AudioPlaybackRow(
    record: HistoryRecord,
    isPlaying: Boolean,
    accent: Color,
    accentSoft: Color,
    onPlayPause: () -> Unit,
) {
    val canPlayAudio = remember(record.audioFilePath) {
        isPlayableAudioReference(record.audioFilePath)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(
                if (isPlaying) accentSoft.copy(alpha = 0.24f)
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
            )
            .let { if (canPlayAudio) it.clickable { onPlayPause() } else it }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .background(
                    if (isPlaying) accent.copy(alpha = 0.20f)
                    else MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = when {
                    !canPlayAudio -> Icons.Outlined.Mic
                    isPlaying -> Icons.Default.Stop
                    else -> Icons.Default.PlayArrow
                },
                contentDescription = null,
                tint = when {
                    !canPlayAudio -> MaterialTheme.colorScheme.error
                    isPlaying -> accent
                    else -> MaterialTheme.colorScheme.primary
                },
                modifier = Modifier.size(18.dp),
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = stringResource(R.string.history_audio_recording),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = when {
                    !canPlayAudio -> stringResource(R.string.history_audio_missing)
                    isPlaying -> stringResource(R.string.history_audio_playing)
                    else -> stringResource(R.string.history_audio_tap_to_listen)
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun presentRecord(
    record: HistoryRecord,
    context: android.content.Context,
): HistoryPresentation {
    val timestamp = Date(record.timestampMs)
    val dateOnly = formatHistoryDate(timestamp, HISTORY_DATE_FORMAT)
    val timeOnly = formatHistoryDate(timestamp, HISTORY_TIME_FORMAT)
    val headerDate = formatHistoryDate(timestamp, HISTORY_HEADER_FORMAT)
    val recordType = record.recordType()

    val alertMessage = record.sosMessage.ifBlank { record.sosText }
    val recordDate = record.sosDate.ifBlank { dateOnly }
    val recordTime = record.sosTime.ifBlank { timeOnly }
    val recordDevice = record.sosDeviceName.ifBlank {
        context.getString(R.string.history_unknown_device)
    }
    val recordMobileNumber = record.sosMobileNumber.ifBlank {
        context.getString(R.string.history_not_provided)
    }
    val recordCurrentSoundLevel = record.sosCurrentSoundLevel.ifBlank {
        context.getString(R.string.history_unavailable)
    }
    val recordLocationLink = record.sosLocationLink
    val recordLocation = record.sosLocationLabel.ifBlank {
        when {
            recordLocationLink.isNotBlank() -> ""
            else -> context.getString(R.string.history_unavailable)
        }
    }

    val fallbackAlert = if (record.isTest) {
        context.getString(R.string.history_fallback_manual_test)
    } else if (recordType == HistoryRecordType.IncomingPush) {
        context.getString(R.string.history_fallback_incoming)
    } else {
        context.getString(R.string.history_fallback_saved)
    }

    val aiLines = record.aiResultText
        .lineSequence()
        .map { it.trim().removePrefix("-").removePrefix("*").trim() }
        .filter { it.isNotBlank() }
        .filterNot { it.isAiAudioAnalysisHeader() }
        .map { localizeHistoryAiLine(context, it) }
        .toList()

    return HistoryPresentation(
        type = recordType,
        sourceBadge = when {
            recordType == HistoryRecordType.IncomingPush -> context.getString(R.string.history_source_incoming)
            record.isTest -> context.getString(R.string.history_source_test)
            else -> context.getString(R.string.history_source_mine)
        },
        title = when {
            recordType == HistoryRecordType.IncomingPush -> context.getString(R.string.history_title_incoming_alert)
            record.isTest -> context.getString(R.string.history_title_audio_test)
            else -> context.getString(R.string.history_title_my_sos)
        },
        subtitle = headerDate,
        alertMessage = alertMessage.ifBlank { fallbackAlert },
        date = recordDate,
        time = recordTime,
        device = recordDevice,
        mobileNumber = recordMobileNumber,
        currentSoundLevel = recordCurrentSoundLevel,
        textAlertDeliveryStatusLabel = record.textAlertDeliveryStatusLabel(context, recordType),
        location = recordLocation.ifBlank { context.getString(R.string.history_unavailable) },
        locationLink = recordLocationLink.ifBlank { null },
        aiLines = aiLines,
    )
}

private fun String.isAiAudioAnalysisHeader(): Boolean {
    val normalized = trim()
        .trimEnd(':', '：')
        .lowercase()
    return normalized in setOf(
        "ai audio analysis result",
        "ai 音訊分析結果",
        "ai 音声分析結果",
        "ai 오디오 분석 결과",
        "resultado del análisis de audio con ia",
    )
}

private fun HistoryRecord.textAlertDeliveryStatusLabel(
    context: android.content.Context,
    recordType: HistoryRecordType,
): String? {
    if (recordType != HistoryRecordType.LocalAlert || isTest) return null
    return when (runCatching { TextAlertDeliveryStatus.valueOf(sosAlertStatus) }.getOrNull()) {
        TextAlertDeliveryStatus.Sent -> context.getString(R.string.history_text_alert_status_sent)
        TextAlertDeliveryStatus.Failed -> context.getString(R.string.history_text_alert_status_failed)
        TextAlertDeliveryStatus.Skipped -> context.getString(R.string.history_text_alert_status_skipped)
        null -> null
    }
}
