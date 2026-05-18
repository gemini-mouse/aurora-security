package app.aurorasecurity.security

object AlarmAppVisibility {
    @Volatile
    var isInForeground: Boolean = false
}
