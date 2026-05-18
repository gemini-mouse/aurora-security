package app.aurorasecurity.security

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class PushBackendApi {
    suspend fun registerDevice(
        settings: AlertContactSettings,
        userId: String,
        fcmToken: String,
    ): Boolean = withContext(Dispatchers.IO) {
        if (settings.apiUrl.isBlank() || fcmToken.isBlank()) return@withContext false

        val payload = JSONObject()
            .put("userId", userId)
            .put("name", settings.userName.ifBlank { "Aurora contact" })
            .put("deviceName", settings.deviceName.ifBlank { "Phone" })
            .put("fcmToken", fcmToken)
            .toString()

        return@withContext try {
            executeJsonRequest(
                url = "${resolveBackendBaseUrl(settings.apiUrl)}/push/register-device",
                token = settings.apiToken,
                method = "POST",
                body = payload,
            )
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun bindCurrentDevice(
        settings: AlertContactSettings,
        bindCode: String,
        contactUserId: String,
    ): Boolean = withContext(Dispatchers.IO) {
        if (settings.apiUrl.isBlank() || bindCode.isBlank() || contactUserId.isBlank()) {
            return@withContext false
        }

        val payload = JSONObject()
            .put("bindCode", bindCode)
            .put("contactUserId", contactUserId)
            .toString()

        return@withContext try {
            executeJsonRequest(
                url = "${resolveBackendBaseUrl(settings.apiUrl)}/push/bind",
                token = settings.apiToken,
                method = "POST",
                body = payload,
            )
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun fetchContacts(
        settings: AlertContactSettings,
        userId: String,
    ): List<PushContact> = withContext(Dispatchers.IO) {
        if (settings.apiUrl.isBlank()) return@withContext emptyList()

        val response = executeJsonRequest(
            url = "${resolveBackendBaseUrl(settings.apiUrl)}/users/$userId/push-contacts",
            token = settings.apiToken,
            method = "GET",
            body = null,
        )

        val contactsJson = JSONObject(response).optJSONArray("contacts") ?: JSONArray()
        buildList {
            for (index in 0 until contactsJson.length()) {
                val item = contactsJson.getJSONObject(index)
                add(
                    PushContact(
                        id = item.optString("id"),
                        name = item.optString("name"),
                        deviceName = item.optString("deviceName"),
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
                url = "${resolveBackendBaseUrl(settings.apiUrl)}/users/$userId/push-contacts/${java.net.URLEncoder.encode(contactId, Charsets.UTF_8.name())}",
                token = settings.apiToken,
                method = "DELETE",
                body = null,
            )
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun sendAlert(
        payload: PendingPushAlertPayload,
    ): Boolean = withContext(Dispatchers.IO) {
        if (payload.apiUrl.isBlank()) return@withContext false

        val body = JSONObject()
            .put("userId", payload.userId)
            .put("eventId", payload.eventId)
            .put("messageType", payload.messageType)
            .put("title", payload.title)
            .put("message", payload.message)
            .toString()

        return@withContext try {
            executeJsonRequest(
                url = "${resolveBackendBaseUrl(payload.apiUrl)}/push/alerts",
                token = payload.apiToken,
                method = "POST",
                body = body,
            )
            true
        } catch (e: Exception) {
            false
        }
    }

}
