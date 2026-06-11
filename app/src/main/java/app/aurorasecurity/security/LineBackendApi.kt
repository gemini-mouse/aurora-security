package app.aurorasecurity.security

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.util.Base64

class LineBackendApi {
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

        executeJsonRequest(
            url = "${resolveBackendBaseUrl(settings.apiUrl)}/users/register",
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
    ): List<LineContact> = withContext(Dispatchers.IO) {
        if (settings.apiUrl.isBlank()) return@withContext emptyList()

        val response = executeJsonRequest(
            url = "${resolveBackendBaseUrl(settings.apiUrl)}/users/$userId/line-contacts",
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
                    LineContact(
                        id = item.optString("id"),
                        name = item.optString("name"),
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

        return@withContext try {
            executeJsonRequest(
                url = "${resolveBackendBaseUrl(settings.apiUrl)}/users/$userId/line-contacts/${java.net.URLEncoder.encode(contactId, Charsets.UTF_8.name())}",
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

    suspend fun sendPendingAlert(payload: PendingLineAlertPayload): Boolean = withContext(Dispatchers.IO) {
        if (payload.apiUrl.isBlank()) return@withContext false

        val body = JSONObject()
            .put("userId", payload.userId)
            .put("message", payload.message)
            .toString()

        return@withContext try {
            executeJsonRequest(
                url = "${resolveBackendBaseUrl(payload.apiUrl)}/line/alerts",
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

    suspend fun sendPendingAudioAnalysis(payload: PendingLineAudioAnalysisPayload): Boolean = withContext(Dispatchers.IO) {
        if (payload.apiUrl.isBlank()) return@withContext false
        val file = java.io.File(payload.filePath)
        if (!file.exists()) return@withContext true

        val body = JSONObject()
            .put("userId", payload.userId)
            .put("caption", payload.caption)
            .put("filename", payload.filename)
            .put("analysisMessage", payload.analysisText)
            .put("audioBase64", Base64.getEncoder().encodeToString(file.readBytes()))
            .put("durationMs", payload.durationMs)
            .toString()

        return@withContext try {
            executeJsonRequest(
                url = "${resolveBackendBaseUrl(payload.apiUrl)}/line/alerts/audio-analysis",
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
}
