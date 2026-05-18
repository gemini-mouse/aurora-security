package app.aurorasecurity.security

internal data class AlertDeliverySummary(
    val recordId: String,
    val timestampMs: Long,
    val sosAlertStatus: TextAlertDeliveryStatus,
    val audioEvidenceStatus: TextAlertDeliveryStatus,
    val aiSummaryAnalysisStatus: TextAlertDeliveryStatus,
)

internal fun HistoryRecord.toAlertDeliverySummaryOrNull(): AlertDeliverySummary? {
    if (recordType() != HistoryRecordType.LocalAlert || isTest) return null

    val sosStatus = parseDeliveryStatus(sosAlertStatus.ifBlank { textAlertDeliveryStatus })
        ?: return null
    val audioStatus = parseDeliveryStatus(audioEvidenceStatus) ?: return null
    val aiSummaryStatus = parseDeliveryStatus(aiSummaryAnalysisStatus) ?: return null

    return AlertDeliverySummary(
        recordId = id,
        timestampMs = timestampMs,
        sosAlertStatus = sosStatus,
        audioEvidenceStatus = audioStatus,
        aiSummaryAnalysisStatus = aiSummaryStatus,
    )
}

internal fun parseDeliveryStatus(value: String): TextAlertDeliveryStatus? {
    return TextAlertDeliveryStatus.values().firstOrNull {
        it.name.equals(value.trim(), ignoreCase = true)
    }
}
