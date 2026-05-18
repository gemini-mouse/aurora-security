package app.aurorasecurity.security

import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

object AlertMessageFormatter {
    fun format(
        settings: AlertContactSettings,
        currentDb: Float,
        location: AlertLocation?,
    ): String {
        val now = ZonedDateTime.now(ZoneId.systemDefault())
        val body = settings.alertMessageTemplate
            .replace("<User Name>", settings.userName.ifBlank { "User" })
            .replace("<Date>", now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")))
            .replace("<Time>", now.format(DateTimeFormatter.ofPattern("HH:mm:ss")))
            .replace("<Mobile Number>", settings.mobileNumber.ifBlank { "Not provided" })
            .replace("<Current dB>", currentDb.roundToInt().toString())
            .replace("<Device Name>", settings.deviceName.ifBlank { "Phone" })
            .replace("<Location>", "")
            .trimEnd()

        val locationLine = when (location) {
            null -> "Location: unavailable"
            else -> "Location: ${location.googleMapsUrl}"
        }
        val mobileNumberLine = settings.mobileNumber
            .trim()
            .takeIf { it.isNotBlank() }
            ?.let { "Mobile number: $it" }

        val shouldAppendMobileLine = !settings.alertMessageTemplate.contains("<Mobile Number>")

        return listOfNotNull(
            body,
            if (shouldAppendMobileLine) mobileNumberLine else null,
            locationLine,
        ).joinToString(separator = "\n")
    }
}
