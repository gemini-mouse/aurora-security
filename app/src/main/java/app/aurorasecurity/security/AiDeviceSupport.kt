package app.aurorasecurity.security

import android.content.Context

data class AiDeviceSupport(
    val totalMemoryBytes: Long,
    val supportsOnDeviceAi: Boolean,
    val defaultModelType: GemmaModelType,
)

private const val SIX_GB_IN_BYTES = 6L * 1024 * 1024 * 1024
private const val EIGHT_GB_IN_BYTES = 8L * 1024 * 1024 * 1024

fun Context.getAiDeviceSupport(): AiDeviceSupport {
    val actManager = getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
    val memInfo = android.app.ActivityManager.MemoryInfo()
    actManager.getMemoryInfo(memInfo)
    val totalMemoryBytes = memInfo.totalMem

    return AiDeviceSupport(
        totalMemoryBytes = totalMemoryBytes,
        supportsOnDeviceAi = totalMemoryBytes >= SIX_GB_IN_BYTES,
        defaultModelType = if (totalMemoryBytes >= EIGHT_GB_IN_BYTES) {
            GemmaModelType.E4B
        } else {
            GemmaModelType.E2B
        },
    )
}
