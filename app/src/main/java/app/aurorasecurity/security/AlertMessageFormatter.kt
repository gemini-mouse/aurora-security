package app.aurorasecurity.security

import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

data class AlertMessageDetails(
    val date: String,
    val time: String,
    val deviceName: String,
    val mobileNumber: String,
    val currentSoundLevel: String,
    val locationLabel: String,
    val locationLink: String,
)

data class AlertMessagePayload(
    val text: String,
    val historyMessage: String,
    val details: AlertMessageDetails,
)

object AlertMessageFormatter {
    fun format(
        settings: AlertContactSettings,
        currentDb: Float,
        location: AlertLocation?,
    ): String {
        return formatPayload(settings, currentDb, location).text
    }

    fun formatPayload(
        settings: AlertContactSettings,
        currentDb: Float,
        location: AlertLocation?,
    ): AlertMessagePayload {
        val now = ZonedDateTime.now(ZoneId.systemDefault())
        val locationValue = location?.googleMapsUrl ?: "unavailable"
        val fallbackLocationLine = "Location: $locationValue"
        val currentSoundLevel = "${currentDb.roundToInt()} dB"
        val templateContainsLocation = settings.alertMessageTemplate.contains("<Location>")
        val body = settings.alertMessageTemplate
            .replace("<User Name>", settings.userName.ifBlank { "User" })
            .replace("<Date>", now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")))
            .replace("<Time>", now.format(DateTimeFormatter.ofPattern("HH:mm:ss")))
            .replace("<Mobile Number>", settings.mobileNumber.ifBlank { "Not provided" })
            .replace("<Current dB>", currentDb.roundToInt().toString())
            .replace("<Device Name>", settings.deviceName.ifBlank { "Phone" })
            .replace("<Location>", locationValue)
            .trimEnd()

        val mobileNumberLine = settings.mobileNumber
            .trim()
            .takeIf { it.isNotBlank() }
            ?.let { "Mobile number: $it" }

        val shouldAppendMobileLine = !settings.alertMessageTemplate.contains("<Mobile Number>")

        val text = listOfNotNull(
            body,
            if (shouldAppendMobileLine) mobileNumberLine else null,
            if (!templateContainsLocation) fallbackLocationLine else null,
        ).joinToString(separator = "\n")

        return AlertMessagePayload(
            text = text,
            historyMessage = body.substringBefore("\n\n").trim().ifBlank { body },
            details = AlertMessageDetails(
                date = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                time = now.format(DateTimeFormatter.ofPattern("HH:mm:ss")),
                deviceName = settings.deviceName.ifBlank { "Phone" },
                mobileNumber = settings.mobileNumber.trim(),
                currentSoundLevel = currentSoundLevel,
                locationLabel = if (location == null) "unavailable" else "",
                locationLink = location?.googleMapsUrl.orEmpty(),
            ),
        )
    }
}
