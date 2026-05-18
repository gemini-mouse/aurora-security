package app.aurorasecurity.security

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

data class HistoryRecord(
    val id: String,
    val timestampMs: Long,
    val type: String,
    val eventId: String,
    val sourceUserId: String,
    val messageType: String,
    val isTest: Boolean,
    val notificationTitle: String,
    val sosText: String,
    val audioFilePath: String,
    val originalAudioFilePath: String,
    val aiResultText: String,
    val textAlertDeliveryStatus: String,
    val sosAlertStatus: String,
    val audioEvidenceStatus: String,
    val aiSummaryAnalysisStatus: String,
)

enum class HistoryRecordType {
    LocalAlert,
    IncomingPush,
}

enum class TextAlertDeliveryStatus {
    Sent,
    Failed,
    Skipped,
}

private const val HISTORY_PREFS_NAME = "history_prefs"
private const val HISTORY_AUDIO_EXTENSION = "m4a"

class HistoryManager(private val context: Context) {
    private val preferences = context.getSharedPreferences(HISTORY_PREFS_NAME, Context.MODE_PRIVATE)
    private val historyDir = File(context.filesDir, "history_audio").apply {
        if (!exists()) {
            mkdirs()
        }
    }

    companion object {
        private const val TAG = "HistoryManager"
        private const val KEY_RECORDS = "records"
        private const val JSON_ID = "id"
        private const val JSON_TIMESTAMP_MS = "timestampMs"
        private const val JSON_TYPE = "type"
        private const val JSON_EVENT_ID = "eventId"
        private const val JSON_SOURCE_USER_ID = "sourceUserId"
        private const val JSON_MESSAGE_TYPE = "messageType"
        private const val JSON_IS_TEST = "isTest"
        private const val JSON_NOTIFICATION_TITLE = "notificationTitle"
        private const val JSON_SOS_TEXT = "sosText"
        private const val JSON_AUDIO_FILE_PATH = "audioFilePath"
        private const val JSON_ORIGINAL_AUDIO_FILE_PATH = "originalAudioFilePath"
        private const val JSON_AI_RESULT_TEXT = "aiResultText"
        private const val JSON_TEXT_ALERT_DELIVERY_STATUS = "textAlertDeliveryStatus"
        private const val JSON_SOS_ALERT_STATUS = "sosAlertStatus"
        private const val JSON_AUDIO_EVIDENCE_STATUS = "audioEvidenceStatus"
        private const val JSON_AI_SUMMARY_ANALYSIS_STATUS = "aiSummaryAnalysisStatus"

        @Volatile
        private var _recordsFlow: MutableStateFlow<List<HistoryRecord>>? = null
    }

    val recordsFlow: StateFlow<List<HistoryRecord>>
        get() {
            var flow = _recordsFlow
            if (flow == null) {
                synchronized(HistoryManager::class.java) {
                    flow = _recordsFlow
                    if (flow == null) {
                        flow = MutableStateFlow(getRecords())
                        _recordsFlow = flow
                    }
                }
            }
            return flow!!.asStateFlow()
        }

    fun getRecords(): List<HistoryRecord> {
        val jsonString = preferences.getString(KEY_RECORDS, null) ?: return emptyList()
        return try {
            val array = JSONArray(jsonString)
            val list = mutableListOf<HistoryRecord>()
            for (i in 0 until array.length()) {
                list += array.getJSONObject(i).toHistoryRecord()
            }
            list.sortedByDescending { it.timestampMs }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse history records", e)
            emptyList()
        }
    }

    suspend fun saveRecord(
        isTest: Boolean,
        sosText: String,
        sourceAudioFile: File?,
        aiResultText: String,
        textAlertDeliveryStatus: TextAlertDeliveryStatus? = null,
        sosAlertStatus: TextAlertDeliveryStatus? = textAlertDeliveryStatus,
        audioEvidenceStatus: TextAlertDeliveryStatus? = null,
        aiSummaryAnalysisStatus: TextAlertDeliveryStatus? = null,
    ) = withContext(Dispatchers.IO) {
        val id = UUID.randomUUID().toString()
        val finalAudioPath = copyAudioToHistory(id, sourceAudioFile)

        val record = HistoryRecord(
            id = id,
            timestampMs = System.currentTimeMillis(),
            type = HistoryRecordType.LocalAlert.name,
            eventId = id,
            sourceUserId = "",
            messageType = "local_alert",
            isTest = isTest,
            notificationTitle = "",
            sosText = sosText,
            audioFilePath = finalAudioPath,
            originalAudioFilePath = sourceAudioFile?.absolutePath.orEmpty(),
            aiResultText = aiResultText,
            textAlertDeliveryStatus = textAlertDeliveryStatus?.name.orEmpty(),
            sosAlertStatus = sosAlertStatus?.name.orEmpty(),
            audioEvidenceStatus = audioEvidenceStatus?.name.orEmpty(),
            aiSummaryAnalysisStatus = aiSummaryAnalysisStatus?.name.orEmpty(),
        )

        val currentList = getRecords().toMutableList()
        currentList.add(0, record) // Add to top
        
        saveListToPrefs(currentList)
        id
    }

    suspend fun updateTextAlertDeliveryStatus(
        recordId: String,
        status: TextAlertDeliveryStatus,
    ) = withContext(Dispatchers.IO) {
        val currentList = getRecords().toMutableList()
        val recordIndex = currentList.indexOfFirst { it.id == recordId }
        if (recordIndex < 0) return@withContext

        currentList[recordIndex] = currentList[recordIndex].copy(
            textAlertDeliveryStatus = status.name,
            sosAlertStatus = status.name,
        )
        saveListToPrefs(currentList)
    }

    suspend fun updateAlertDeliveryStatuses(
        recordId: String,
        sosAlertStatus: TextAlertDeliveryStatus? = null,
        audioEvidenceStatus: TextAlertDeliveryStatus? = null,
        aiSummaryAnalysisStatus: TextAlertDeliveryStatus? = null,
    ) = withContext(Dispatchers.IO) {
        val currentList = getRecords().toMutableList()
        val recordIndex = currentList.indexOfFirst { it.id == recordId }
        if (recordIndex < 0) return@withContext

        val current = currentList[recordIndex]
        currentList[recordIndex] = current.copy(
            textAlertDeliveryStatus = sosAlertStatus?.name ?: current.textAlertDeliveryStatus,
            sosAlertStatus = sosAlertStatus?.name ?: current.sosAlertStatus,
            audioEvidenceStatus = audioEvidenceStatus?.name ?: current.audioEvidenceStatus,
            aiSummaryAnalysisStatus = aiSummaryAnalysisStatus?.name ?: current.aiSummaryAnalysisStatus,
        )
        saveListToPrefs(currentList)
    }

    suspend fun upsertIncomingPushRecord(
        eventId: String,
        sourceUserId: String,
        messageType: String,
        title: String,
        message: String,
    ) = withContext(Dispatchers.IO) {
        val currentList = getRecords().toMutableList()
        val existingIndex = currentList.indexOfFirst {
            it.type == HistoryRecordType.IncomingPush.name && it.eventId == eventId
        }

        if (existingIndex >= 0) {
            val existing = currentList[existingIndex]
            currentList[existingIndex] = when (messageType) {
                "ai_analysis" -> existing.copy(
                    sourceUserId = sourceUserId.ifBlank { existing.sourceUserId },
                    messageType = messageType,
                    aiResultText = message,
                )
                else -> existing.copy(
                    sourceUserId = sourceUserId.ifBlank { existing.sourceUserId },
                    messageType = messageType,
                    notificationTitle = title,
                    sosText = message,
                )
            }
        } else {
            val record = HistoryRecord(
                id = UUID.randomUUID().toString(),
                timestampMs = System.currentTimeMillis(),
                type = HistoryRecordType.IncomingPush.name,
                eventId = eventId,
                sourceUserId = sourceUserId,
                messageType = messageType,
                isTest = false,
                notificationTitle = if (messageType == "ai_analysis") "SOS Triggered" else title,
                sosText = if (messageType == "ai_analysis") "" else message,
                audioFilePath = "",
                originalAudioFilePath = "",
                aiResultText = if (messageType == "ai_analysis") message else "",
                textAlertDeliveryStatus = "",
                sosAlertStatus = "",
                audioEvidenceStatus = "",
                aiSummaryAnalysisStatus = "",
            )
            currentList.add(0, record)
        }

        currentList.sortByDescending { it.timestampMs }
        saveListToPrefs(currentList)
    }

    suspend fun deleteRecord(recordId: String) = withContext(Dispatchers.IO) {
        val currentList = getRecords().toMutableList()
        val recordToDelete = currentList.find { it.id == recordId }
        
        if (recordToDelete != null) {
            deleteAudioFile(recordToDelete.audioFilePath)
            deleteOriginalAudioFiles(recordToDelete.originalAudioFilePath)
            currentList.remove(recordToDelete)
            saveListToPrefs(currentList)
        }
    }

    private fun saveListToPrefs(list: List<HistoryRecord>) {
        val array = JSONArray()
        list.forEach { record ->
            array.put(record.toJson())
        }
        preferences.edit().putString(KEY_RECORDS, array.toString()).apply()
        _recordsFlow?.value = list
    }

    private fun copyAudioToHistory(recordId: String, sourceAudioFile: File?): String {
        if (sourceAudioFile == null || !sourceAudioFile.exists()) return ""
        return try {
            val targetFile = File(historyDir, "$recordId.$HISTORY_AUDIO_EXTENSION")
            sourceAudioFile.copyTo(targetFile, overwrite = true)
            targetFile.absolutePath
        } catch (e: Exception) {
            Log.e(TAG, "Failed to copy audio file for history", e)
            ""
        }
    }

    private fun deleteAudioFile(audioFilePath: String) {
        if (audioFilePath.isBlank()) return
        val audioFile = File(audioFilePath)
        if (audioFile.exists()) {
            audioFile.delete()
        }
    }

    private fun deleteOriginalAudioFiles(originalAudioFilePath: String) {
        if (originalAudioFilePath.isBlank()) return

        val originalAudioFile = File(originalAudioFilePath)
        if (originalAudioFile.exists()) {
            originalAudioFile.delete()
        }

        val wavSibling = File(
            originalAudioFile.parentFile,
            "${originalAudioFile.nameWithoutExtension}.wav",
        )
        if (wavSibling.exists()) {
            wavSibling.delete()
        }
    }

    private fun JSONObject.toHistoryRecord(): HistoryRecord {
        val legacyTextStatus = optString(JSON_TEXT_ALERT_DELIVERY_STATUS, "")
        return HistoryRecord(
            id = getString(JSON_ID),
            timestampMs = getLong(JSON_TIMESTAMP_MS),
            type = optString(JSON_TYPE, HistoryRecordType.LocalAlert.name),
            eventId = optString(JSON_EVENT_ID, optString(JSON_ID)),
            sourceUserId = optString(JSON_SOURCE_USER_ID, ""),
            messageType = optString(JSON_MESSAGE_TYPE, if (optString(JSON_TYPE, HistoryRecordType.LocalAlert.name) == HistoryRecordType.IncomingPush.name) "sos" else "local_alert"),
            isTest = optBoolean(JSON_IS_TEST, false),
            notificationTitle = optString(JSON_NOTIFICATION_TITLE, ""),
            sosText = getString(JSON_SOS_TEXT),
            audioFilePath = getString(JSON_AUDIO_FILE_PATH),
            originalAudioFilePath = optString(JSON_ORIGINAL_AUDIO_FILE_PATH, ""),
            aiResultText = getString(JSON_AI_RESULT_TEXT),
            textAlertDeliveryStatus = legacyTextStatus,
            sosAlertStatus = optString(JSON_SOS_ALERT_STATUS, legacyTextStatus),
            audioEvidenceStatus = optString(JSON_AUDIO_EVIDENCE_STATUS, ""),
            aiSummaryAnalysisStatus = optString(JSON_AI_SUMMARY_ANALYSIS_STATUS, ""),
        )
    }

    private fun HistoryRecord.toJson(): JSONObject {
        return JSONObject().apply {
            put(JSON_ID, id)
            put(JSON_TIMESTAMP_MS, timestampMs)
            put(JSON_TYPE, type)
            put(JSON_EVENT_ID, eventId)
            put(JSON_SOURCE_USER_ID, sourceUserId)
            put(JSON_MESSAGE_TYPE, messageType)
            put(JSON_IS_TEST, isTest)
            put(JSON_NOTIFICATION_TITLE, notificationTitle)
            put(JSON_SOS_TEXT, sosText)
            put(JSON_AUDIO_FILE_PATH, audioFilePath)
            put(JSON_ORIGINAL_AUDIO_FILE_PATH, originalAudioFilePath)
            put(JSON_AI_RESULT_TEXT, aiResultText)
            put(JSON_TEXT_ALERT_DELIVERY_STATUS, textAlertDeliveryStatus)
            put(JSON_SOS_ALERT_STATUS, sosAlertStatus)
            put(JSON_AUDIO_EVIDENCE_STATUS, audioEvidenceStatus)
            put(JSON_AI_SUMMARY_ANALYSIS_STATUS, aiSummaryAnalysisStatus)
        }
    }
}
