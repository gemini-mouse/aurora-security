package app.aurorasecurity.security

import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

internal fun resolveBackendBaseUrl(apiUrl: String): String {
    val cleanUrl = apiUrl.trim().trimEnd('/')
    return if (cleanUrl.isNotEmpty() && !cleanUrl.startsWith("http")) {
        "https://$cleanUrl"
    } else {
        cleanUrl
    }
}

internal fun executeJsonRequest(
    url: String,
    token: String,
    method: String,
    body: String?,
    wrapNetworkErrors: Boolean = false,
): String {
    val connection = (URL(url.trim()).openConnection() as HttpURLConnection).apply {
        requestMethod = method
        connectTimeout = 10_000
        readTimeout = 10_000
        doInput = true
        if (body != null) {
            doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
        }
        if (token.trim().isNotBlank()) {
            setRequestProperty("Authorization", "Bearer ${token.trim()}")
        }
    }

    try {
        if (body != null) {
            OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use { writer ->
                writer.write(body)
            }
        }
        val responseCode = connection.responseCode
        if (responseCode !in 200..299) {
            val errorMessage = connection.errorStream
                ?.bufferedReader(Charsets.UTF_8)
                ?.use { it.readText() }
                .orEmpty()
            throw RuntimeException("HTTP $responseCode: $errorMessage")
        }
        return connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
    } catch (error: Exception) {
        if (error is RuntimeException && error.message?.startsWith("HTTP") == true) {
            throw error
        }
        if (wrapNetworkErrors) {
            throw RuntimeException("Network Error: ${error.message}", error)
        }
        throw error
    } finally {
        connection.disconnect()
    }
}
