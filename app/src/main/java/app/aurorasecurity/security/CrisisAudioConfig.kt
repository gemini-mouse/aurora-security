package app.aurorasecurity.security

object CrisisAudioConfig {
    const val SAMPLE_RATE_HZ = 16_000
    const val PRE_TRIGGER_SECONDS = 2
    const val POST_TRIGGER_SECONDS = 3
    const val TOTAL_DURATION_SECONDS = PRE_TRIGGER_SECONDS + POST_TRIGGER_SECONDS
    const val TOTAL_DURATION_MS = TOTAL_DURATION_SECONDS * 1_000
}
