package app.aurorasecurity.security

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.util.Base64

class TelegramBackendApi {
    suspend fun registerUser(
        settings: AlertContactSettings,
        userId: String,
        bindCode: String,
    ): Boolean = withContext(Dispatchers.IO) {
        if (settings.apiUrl.isBlank()) return@withContext false

        val payload = JSONObject()
            .put("userId", userId)
            .put("bindCode", bindCode)
            .put("deviceName", settings.deviceName)
            .toString()

        val baseUrl = resolveBackendBaseUrl(settings.apiUrl)

        executeJsonRequest(
            url = "$baseUrl/users/register",
            token = settings.apiToken,
            method = "POST",
            body = payload,
            wrapNetworkErrors = true,
        )
        return@withContext true
    }

    suspend fun fetchContacts(
        settings: AlertContactSettings,
        userId: String,
    ): List<TelegramContact> = withContext(Dispatchers.IO) {
        if (settings.apiUrl.isBlank()) return@withContext emptyList()

        val baseUrl = resolveBackendBaseUrl(settings.apiUrl)

        val response = executeJsonRequest(
            url = "$baseUrl/users/$userId/contacts",
            token = settings.apiToken,
            method = "GET",
            body = null,
            wrapNetworkErrors = true,
        )

        val contactsJson = JSONObject(response).optJSONArray("contacts") ?: JSONArray()
        buildList {
            for (index in 0 until contactsJson.length()) {
                val item = contactsJson.getJSONObject(index)
                add(
                    TelegramContact(
                        id = item.optString("id"),
                        name = item.optString("name"),
                        telegramHandle = item.optString("telegramHandle"),
                        status = TelegramBindingStatus.valueOf(item.optString("status", "Pending")),
                    ),
                )
            }
        }
    }

    suspend fun deleteContact(
        settings: AlertContactSettings,
        userId: String,
        contactId: String,
    ): Boolean = withContext(Dispatchers.IO) {
        if (settings.apiUrl.isBlank() || contactId.isBlank()) return@withContext false

        val baseUrl = resolveBackendBaseUrl(settings.apiUrl)

        return@withContext try {
            executeJsonRequest(
                url = "$baseUrl/users/$userId/contacts/${java.net.URLEncoder.encode(contactId, Charsets.UTF_8.name())}",
                token = settings.apiToken,
                method = "DELETE",
                body = null,
                wrapNetworkErrors = true,
            )
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun sendAlert(
        settings: AlertContactSettings,
        userId: String,
        currentDb: Float,
        location: AlertLocation?,
    ): Boolean = withContext(Dispatchers.IO) {
        if (settings.apiUrl.isBlank()) return@withContext false

        val payload = JSONObject()
            .put("userId", userId)
            .put("message", AlertMessageFormatter.format(settings, currentDb, location))
            .toString()

        val baseUrl = resolveBackendBaseUrl(settings.apiUrl)

        return@withContext try {
            executeJsonRequest(
                url = "$baseUrl/alerts",
                token = settings.apiToken,
                method = "POST",
                body = payload,
                wrapNetworkErrors = true,
            )
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun sendPendingAlert(payload: PendingAlertPayload): Boolean = withContext(Dispatchers.IO) {
        if (payload.apiUrl.isBlank()) return@withContext false
        val baseUrl = resolveBackendBaseUrl(payload.apiUrl)

        val body = JSONObject()
            .put("userId", payload.userId)
            .put("message", payload.message)
            .toString()

        return@withContext try {
            executeJsonRequest(
                url = "$baseUrl/alerts",
                token = payload.apiToken,
                method = "POST",
                body = body,
                wrapNetworkErrors = true,
            )
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun sendAudioAlert(
        settings: AlertContactSettings,
        userId: String,
        clip: CrisisAudioClip,
    ): Boolean = withContext(Dispatchers.IO) {
        if (settings.apiUrl.isBlank()) return@withContext false

        val m4aFilename = clip.file.name.let { name ->
            if (name.endsWith(".m4a", ignoreCase = true)) name
            else name.substringBeforeLast('.') + ".m4a"
        }

        val payload = JSONObject()
            .put("userId", userId)
            .put("caption", "5-second crisis audio clip captured by Aurora Security.")
            .put("filename", m4aFilename)
            .put("audioBase64", Base64.getEncoder().encodeToString(clip.m4aBytes))
            .toString()

        val baseUrl = resolveBackendBaseUrl(settings.apiUrl)

        return@withContext try {
            executeJsonRequest(
                url = "$baseUrl/alerts/audio",
                token = settings.apiToken,
                method = "POST",
                body = payload,
                wrapNetworkErrors = true,
            )
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun sendAnalysisResult(
        settings: AlertContactSettings,
        userId: String,
        analysisText: String,
    ): Boolean = withContext(Dispatchers.IO) {
        if (settings.apiUrl.isBlank()) return@withContext false

        val payload = JSONObject()
            .put("userId", userId)
            .put("message", "AI audio analysis result:\n$analysisText")
            .toString()

        val baseUrl = resolveBackendBaseUrl(settings.apiUrl)

        return@withContext try {
            executeJsonRequest(
                url = "$baseUrl/alerts/analysis",
                token = settings.apiToken,
                method = "POST",
                body = payload,
                wrapNetworkErrors = true,
            )
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun sendPendingAudio(payload: PendingAudioPayload): Boolean = withContext(Dispatchers.IO) {
        if (payload.apiUrl.isBlank()) return@withContext false
        val file = java.io.File(payload.filePath)
        if (!file.exists()) return@withContext true // file gone ??skip silently

        val baseUrl = resolveBackendBaseUrl(payload.apiUrl)

        val body = JSONObject()
            .put("userId", payload.userId)
            .put("caption", payload.caption)
            .put("filename", payload.filename)
            .put("audioBase64", Base64.getEncoder().encodeToString(file.readBytes()))
            .toString()

        return@withContext try {
            executeJsonRequest(
                url = "$baseUrl/alerts/audio",
                token = payload.apiToken,
                method = "POST",
                body = body,
                wrapNetworkErrors = true,
            )
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun sendPendingAnalysis(payload: PendingAnalysisPayload): Boolean = withContext(Dispatchers.IO) {
        if (payload.apiUrl.isBlank()) return@withContext false
        val baseUrl = resolveBackendBaseUrl(payload.apiUrl)

        val body = JSONObject()
            .put("userId", payload.userId)
            .put("message", "AI audio analysis result:\n${payload.analysisText}")
            .toString()

        return@withContext try {
            executeJsonRequest(
                url = "$baseUrl/alerts/analysis",
                token = payload.apiToken,
                method = "POST",
                body = body,
                wrapNetworkErrors = true,
            )
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun sendPendingTrainingAudio(payload: PendingTrainingAudioPayload): Boolean = withContext(Dispatchers.IO) {
        if (payload.apiUrl.isBlank()) return@withContext false
        val file = java.io.File(payload.wavFilePath)
        if (!file.exists()) return@withContext true

        val baseUrl = resolveBackendBaseUrl(payload.apiUrl)

        val body = JSONObject()
            .put("userId", payload.userId)
            .put("filename", payload.filename)
            .put("audioBase64", Base64.getEncoder().encodeToString(file.readBytes()))
            .put(
                "metadata",
                JSONObject()
                    .put("triggerSource", payload.triggerSource)
                    .put("dangerLevel", payload.dangerLevel ?: JSONObject.NULL)
                    .put("capturedAtEpochMs", payload.capturedAtEpochMs)
                    .put("sampleRateHz", payload.sampleRateHz)
                    .put("durationMs", payload.durationMs),
            )
            .toString()

        return@withContext try {
            executeJsonRequest(
                url = "$baseUrl/training/audio",
                token = payload.apiToken,
                method = "POST",
                body = body,
                wrapNetworkErrors = true,
            )
            file.delete()
            true
        } catch (e: Exception) {
            false
        }
    }
}
