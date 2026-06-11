package app.aurorasecurity.security

import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

internal fun saveTelegramContactsToPreferences(
    preferences: SharedPreferences,
    key: String,
    contacts: List<TelegramContact>,
) {
    val array = JSONArray()
    contacts.forEach { contact ->
        array.put(
            JSONObject().apply {
                put("id", contact.id)
                put("name", contact.name)
                put("handle", contact.telegramHandle)
                put("status", contact.status.name)
            },
        )
    }
    preferences.edit().putString(key, array.toString()).apply()
}

internal fun loadTelegramContactsFromPreferences(
    preferences: SharedPreferences,
    key: String,
): List<TelegramContact> {
    return preferences.readJsonList(key) { item ->
        TelegramContact(
            id = item.getString("id"),
            name = item.getString("name"),
            telegramHandle = item.getString("handle"),
            status = item.optString("status").toBindingStatusOrPending(),
        )
    }
}

internal fun savePushContactsToPreferences(
    preferences: SharedPreferences,
    key: String,
    contacts: List<PushContact>,
) {
    val array = JSONArray()
    contacts.forEach { contact ->
        array.put(
            JSONObject().apply {
                put("id", contact.id)
                put("name", contact.name)
                put("deviceName", contact.deviceName)
                put("status", contact.status.name)
            },
        )
    }
    preferences.edit().putString(key, array.toString()).apply()
}

internal fun loadPushContactsFromPreferences(
    preferences: SharedPreferences,
    key: String,
): List<PushContact> {
    return preferences.readJsonList(key) { item ->
        PushContact(
            id = item.getString("id"),
            name = item.optString("name"),
            deviceName = item.optString("deviceName"),
            status = item.optString("status").toBindingStatusOrPending(),
        )
    }
}

internal fun saveLineContactsToPreferences(
    preferences: SharedPreferences,
    key: String,
    contacts: List<LineContact>,
) {
    val array = JSONArray()
    contacts.forEach { contact ->
        array.put(
            JSONObject().apply {
                put("id", contact.id)
                put("name", contact.name)
                put("status", contact.status.name)
            },
        )
    }
    preferences.edit().putString(key, array.toString()).apply()
}

internal fun loadLineContactsFromPreferences(
    preferences: SharedPreferences,
    key: String,
): List<LineContact> {
    return preferences.readJsonList(key) { item ->
        LineContact(
            id = item.getString("id"),
            name = item.optString("name"),
            status = item.optString("status").toBindingStatusOrPending(),
        )
    }
}

internal fun savePendingAlertToPreferences(
    preferences: SharedPreferences,
    key: String,
    payload: PendingAlertPayload,
) {
    preferences.appendJsonObject(
        key = key,
        item = JSONObject().apply {
            put("userId", payload.userId)
            put("apiUrl", payload.apiUrl)
            put("apiToken", payload.apiToken)
            put("message", payload.message)
        },
    )
}

internal fun loadPendingAlertsFromPreferences(
    preferences: SharedPreferences,
    key: String,
): List<PendingAlertPayload> {
    return preferences.readJsonList(key) { item ->
        PendingAlertPayload(
            userId = item.optString("userId"),
            apiUrl = item.optString("apiUrl"),
            apiToken = item.optString("apiToken"),
            message = item.optString("message"),
        )
    }
}

internal fun savePendingAudioToPreferences(
    preferences: SharedPreferences,
    key: String,
    payload: PendingAudioPayload,
) {
    preferences.appendJsonObject(
        key = key,
        item = JSONObject().apply {
            put("userId", payload.userId)
            put("apiUrl", payload.apiUrl)
            put("apiToken", payload.apiToken)
            put("filePath", payload.filePath)
            put("filename", payload.filename)
            put("caption", payload.caption)
            put("sendToTelegram", payload.sendToTelegram)
            put("sendToLine", payload.sendToLine)
            put("sendToPush", payload.sendToPush)
            put("pushEventId", payload.pushEventId)
            put("pushTitle", payload.pushTitle)
            put("pushMessage", payload.pushMessage)
            put("durationMs", payload.durationMs)
        },
    )
}

internal fun loadPendingAudiosFromPreferences(
    preferences: SharedPreferences,
    key: String,
): List<PendingAudioPayload> {
    return preferences.readJsonList(key) { item ->
        PendingAudioPayload(
            userId = item.optString("userId"),
            apiUrl = item.optString("apiUrl"),
            apiToken = item.optString("apiToken"),
            filePath = item.optString("filePath"),
            filename = item.optString("filename"),
            caption = item.optString("caption"),
            sendToTelegram = item.optBoolean("sendToTelegram", true),
            sendToLine = item.optBoolean("sendToLine", false),
            sendToPush = item.optBoolean("sendToPush", false),
            pushEventId = item.optString("pushEventId"),
            pushTitle = item.optString("pushTitle"),
            pushMessage = item.optString("pushMessage"),
            durationMs = item.optInt("durationMs", CrisisAudioConfig.TOTAL_DURATION_MS),
        )
    }
}

internal fun savePendingAnalysisToPreferences(
    preferences: SharedPreferences,
    key: String,
    payload: PendingAnalysisPayload,
) {
    preferences.appendJsonObject(
        key = key,
        item = JSONObject().apply {
            put("userId", payload.userId)
            put("apiUrl", payload.apiUrl)
            put("apiToken", payload.apiToken)
            put("analysisText", payload.analysisText)
        },
    )
}

internal fun loadPendingAnalysesFromPreferences(
    preferences: SharedPreferences,
    key: String,
): List<PendingAnalysisPayload> {
    return preferences.readJsonList(key) { item ->
        PendingAnalysisPayload(
            userId = item.optString("userId"),
            apiUrl = item.optString("apiUrl"),
            apiToken = item.optString("apiToken"),
            analysisText = item.optString("analysisText"),
        )
    }
}

internal fun savePendingTrainingAudioToPreferences(
    preferences: SharedPreferences,
    key: String,
    payload: PendingTrainingAudioPayload,
) {
    preferences.appendJsonObject(
        key = key,
        item = JSONObject().apply {
            put("userId", payload.userId)
            put("apiUrl", payload.apiUrl)
            put("apiToken", payload.apiToken)
            put("wavFilePath", payload.wavFilePath)
            put("filename", payload.filename)
            put("triggerSource", payload.triggerSource)
            put("dangerLevel", payload.dangerLevel ?: JSONObject.NULL)
            put("capturedAtEpochMs", payload.capturedAtEpochMs)
            put("sampleRateHz", payload.sampleRateHz)
            put("durationMs", payload.durationMs)
        },
    )
}

internal fun loadPendingTrainingAudiosFromPreferences(
    preferences: SharedPreferences,
    key: String,
): List<PendingTrainingAudioPayload> {
    return preferences.readJsonList(key) { item ->
        PendingTrainingAudioPayload(
            userId = item.optString("userId"),
            apiUrl = item.optString("apiUrl"),
            apiToken = item.optString("apiToken"),
            wavFilePath = item.optString("wavFilePath"),
            filename = item.optString("filename"),
            triggerSource = item.optString("triggerSource"),
            dangerLevel = item.optString("dangerLevel").ifBlank { null },
            capturedAtEpochMs = item.optLong("capturedAtEpochMs", 0L),
            sampleRateHz = item.optInt("sampleRateHz", CrisisAudioConfig.SAMPLE_RATE_HZ),
            durationMs = item.optInt("durationMs", CrisisAudioConfig.TOTAL_DURATION_MS),
        )
    }
}

internal fun savePendingPushAlertToPreferences(
    preferences: SharedPreferences,
    key: String,
    payload: PendingPushAlertPayload,
) {
    preferences.appendJsonObject(
        key = key,
        item = JSONObject().apply {
            put("userId", payload.userId)
            put("apiUrl", payload.apiUrl)
            put("apiToken", payload.apiToken)
            put("eventId", payload.eventId)
            put("messageType", payload.messageType)
            put("title", payload.title)
            put("message", payload.message)
            put("sosMessage", payload.sosMessage)
            put("sosDate", payload.sosDate)
            put("sosTime", payload.sosTime)
            put("sosDeviceName", payload.sosDeviceName)
            put("sosMobileNumber", payload.sosMobileNumber)
            put("sosCurrentSoundLevel", payload.sosCurrentSoundLevel)
            put("sosLocationLabel", payload.sosLocationLabel)
            put("sosLocationLink", payload.sosLocationLink)
        },
    )
}

internal fun loadPendingPushAlertsFromPreferences(
    preferences: SharedPreferences,
    key: String,
): List<PendingPushAlertPayload> {
    return preferences.readJsonList(key) { item ->
        PendingPushAlertPayload(
            userId = item.optString("userId"),
            apiUrl = item.optString("apiUrl"),
            apiToken = item.optString("apiToken"),
            eventId = item.optString("eventId"),
            messageType = item.optString("messageType", "sos"),
            title = item.optString("title"),
            message = item.optString("message"),
            sosMessage = item.optString("sosMessage"),
            sosDate = item.optString("sosDate"),
            sosTime = item.optString("sosTime"),
            sosDeviceName = item.optString("sosDeviceName"),
            sosMobileNumber = item.optString("sosMobileNumber"),
            sosCurrentSoundLevel = item.optString("sosCurrentSoundLevel"),
            sosLocationLabel = item.optString("sosLocationLabel"),
            sosLocationLink = item.optString("sosLocationLink"),
        )
    }
}

internal fun savePendingLineAlertToPreferences(
    preferences: SharedPreferences,
    key: String,
    payload: PendingLineAlertPayload,
) {
    preferences.appendJsonObject(
        key = key,
        item = JSONObject().apply {
            put("userId", payload.userId)
            put("apiUrl", payload.apiUrl)
            put("apiToken", payload.apiToken)
            put("message", payload.message)
        },
    )
}

internal fun loadPendingLineAlertsFromPreferences(
    preferences: SharedPreferences,
    key: String,
): List<PendingLineAlertPayload> {
    return preferences.readJsonList(key) { item ->
        PendingLineAlertPayload(
            userId = item.optString("userId"),
            apiUrl = item.optString("apiUrl"),
            apiToken = item.optString("apiToken"),
            message = item.optString("message"),
        )
    }
}

internal fun savePendingLineAudioAnalysisToPreferences(
    preferences: SharedPreferences,
    key: String,
    payload: PendingLineAudioAnalysisPayload,
) {
    preferences.appendJsonObject(
        key = key,
        item = JSONObject().apply {
            put("userId", payload.userId)
            put("apiUrl", payload.apiUrl)
            put("apiToken", payload.apiToken)
            put("filePath", payload.filePath)
            put("filename", payload.filename)
            put("caption", payload.caption)
            put("analysisText", payload.analysisText)
            put("durationMs", payload.durationMs)
        },
    )
}

internal fun loadPendingLineAudioAnalysesFromPreferences(
    preferences: SharedPreferences,
    key: String,
): List<PendingLineAudioAnalysisPayload> {
    return preferences.readJsonList(key) { item ->
        PendingLineAudioAnalysisPayload(
            userId = item.optString("userId"),
            apiUrl = item.optString("apiUrl"),
            apiToken = item.optString("apiToken"),
            filePath = item.optString("filePath"),
            filename = item.optString("filename"),
            caption = item.optString("caption"),
            analysisText = item.optString("analysisText"),
            durationMs = item.optInt("durationMs", CrisisAudioConfig.TOTAL_DURATION_MS),
        )
    }
}

internal fun clearPreferenceValue(
    preferences: SharedPreferences,
    key: String,
) {
    preferences.edit().remove(key).apply()
}

private inline fun <T> SharedPreferences.readJsonList(
    key: String,
    mapper: (JSONObject) -> T,
): List<T> {
    return try {
        val array = readJsonArray(key)
        List(array.length()) { index ->
            mapper(array.getJSONObject(index))
        }
    } catch (_: Exception) {
        emptyList()
    }
}

private fun SharedPreferences.appendJsonObject(
    key: String,
    item: JSONObject,
) {
    val existing = readJsonArray(key)
    existing.put(item)
    edit().putString(key, existing.toString()).apply()
}

private fun SharedPreferences.readJsonArray(key: String): JSONArray {
    val raw = getString(key, null) ?: return JSONArray()
    return try {
        JSONArray(raw)
    } catch (_: Exception) {
        JSONArray()
    }
}

private fun String?.toBindingStatusOrPending(): TelegramBindingStatus {
    return runCatching {
        TelegramBindingStatus.valueOf(this.orEmpty())
    }.getOrDefault(TelegramBindingStatus.Pending)
}
