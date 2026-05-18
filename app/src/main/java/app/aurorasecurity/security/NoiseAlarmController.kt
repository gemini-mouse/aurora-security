package app.aurorasecurity.security

import android.os.SystemClock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class NoiseAlarmUiState(
    val currentDb: Float = 0f,
    val thresholdDb: Float = 90f,
    val alarmTriggered: Boolean = false,
    val isSilenced: Boolean = false,
    val hasMicPermission: Boolean = false,
    val isMonitoring: Boolean = false,
    val isThresholdExceeded: Boolean = false,
    val activeEmergencyMode: EmergencyMode? = null,
    val pendingEmergencyMode: EmergencyMode? = null,
    val pendingTriggerSource: EmergencyTriggerSource? = null,
    val countdownSecondsRemaining: Int = 0,
    val useShakeDetection: Boolean = true,
    val shakeThresholdG: Float = 3.0f,
    val currentG: Float = 1.0f,
    val isShaking: Boolean = false,
    val triggerDb: Float? = null,
)

enum class EmergencyMode {
    Silent,
    Loud,
}

enum class EmergencyTriggerSource {
    SoundDetection,
    SilentSosButton,
    LoudSosButton,
}

object NoiseAlarmController {
    private val _uiState = MutableStateFlow(NoiseAlarmUiState())
    val uiState: StateFlow<NoiseAlarmUiState> = _uiState.asStateFlow()
    private var latestDetectedDb: Float = 0f
    private var lastUiDbUpdateAtMs: Long = 0L
    private var lastUiGUpdateAtMs: Long = 0L
    private var currentPeakG: Float = 0f
    private var currentPeakDb: Float = 0f

    fun setMicPermission(granted: Boolean) {
        _uiState.update {
            it.copy(
                hasMicPermission = granted,
                isMonitoring = if (granted) it.isMonitoring else false,
                currentDb = if (granted) it.currentDb else 0f,
                alarmTriggered = if (granted) it.alarmTriggered else false,
                isThresholdExceeded = if (granted) it.isThresholdExceeded else false,
                activeEmergencyMode = if (granted) it.activeEmergencyMode else null,
                pendingEmergencyMode = if (granted) it.pendingEmergencyMode else null,
                pendingTriggerSource = if (granted) it.pendingTriggerSource else null,
                countdownSecondsRemaining = if (granted) it.countdownSecondsRemaining else 0,
            )
        }
        if (!granted) {
            latestDetectedDb = 0f
            lastUiDbUpdateAtMs = 0L
        }
    }

    fun setMonitoring(active: Boolean) {
        _uiState.update {
            it.copy(isMonitoring = active)
        }
    }

    fun setThreshold(value: Float) {
        _uiState.update { current ->
            val silenced = if (latestDetectedDb < value) false else current.isSilenced
            val soundThresholdExceeded = current.hasMicPermission && current.isMonitoring && !silenced && latestDetectedDb >= value
            val thresholdExceeded = if (current.useShakeDetection) {
                soundThresholdExceeded && current.isShaking
            } else {
                soundThresholdExceeded
            }
            current.copy(
                thresholdDb = value,
                isSilenced = silenced,
                isThresholdExceeded = thresholdExceeded,
            )
        }
    }

    fun setMovementSettings(useShake: Boolean, thresholdG: Float) {
        _uiState.update { current ->
            current.copy(
                useShakeDetection = useShake,
                shakeThresholdG = thresholdG
            )
        }
    }

    fun updateLevel(db: Float) {
        latestDetectedDb = db
        currentPeakDb = maxOf(currentPeakDb, db)
        val now = SystemClock.elapsedRealtime()
        _uiState.update { current ->
            val silenced = if (db < current.thresholdDb) false else current.isSilenced
            val soundThresholdExceeded = current.hasMicPermission && current.isMonitoring && !silenced && latestDetectedDb >= current.thresholdDb
            val thresholdExceeded = if (current.useShakeDetection) {
                soundThresholdExceeded && current.isShaking
            } else {
                soundThresholdExceeded
            }
            
            val timeUpdate = now - lastUiDbUpdateAtMs >= UI_DB_UPDATE_INTERVAL_MS
            val stateUpdate = thresholdExceeded != current.isThresholdExceeded || silenced != current.isSilenced
            
            val displayDb = if (timeUpdate) {
                val peak = currentPeakDb
                currentPeakDb = 0f
                lastUiDbUpdateAtMs = now
                peak
            } else if (stateUpdate) {
                maxOf(current.currentDb, db)
            } else {
                current.currentDb
            }

            current.copy(
                currentDb = displayDb,
                isSilenced = silenced,
                isThresholdExceeded = thresholdExceeded,
            )
        }
    }

    fun updateShakeLevel(gForce: Float, isShaking: Boolean) {
        val now = SystemClock.elapsedRealtime()
        currentPeakG = maxOf(currentPeakG, gForce)

        _uiState.update { current ->
            val soundThresholdExceeded = current.hasMicPermission && current.isMonitoring && !current.isSilenced && latestDetectedDb >= current.thresholdDb
            val thresholdExceeded = if (current.useShakeDetection) {
                soundThresholdExceeded && isShaking
            } else {
                soundThresholdExceeded
            }
            
            val shouldUpdateDisplayedG = now - lastUiGUpdateAtMs >= UI_G_UPDATE_INTERVAL_MS
            val displayG = if (shouldUpdateDisplayedG) {
                val peak = currentPeakG
                currentPeakG = 0f
                lastUiGUpdateAtMs = now
                peak
            } else {
                current.currentG
            }

            current.copy(
                currentG = displayG,
                isShaking = isShaking,
                isThresholdExceeded = thresholdExceeded
            )
        }
    }

    fun stopAlarm() {
        _uiState.update { current ->
            current.copy(
                isSilenced = true,
                alarmTriggered = false,
                activeEmergencyMode = null,
                pendingEmergencyMode = null,
                pendingTriggerSource = null,
                countdownSecondsRemaining = 0,
                triggerDb = null,
            )
        }
    }

    fun startCountdown(mode: EmergencyMode, source: EmergencyTriggerSource) {
        _uiState.update { current ->
            current.copy(
                isSilenced = false,
                alarmTriggered = false,
                activeEmergencyMode = null,
                pendingEmergencyMode = mode,
                pendingTriggerSource = source,
                countdownSecondsRemaining = COUNTDOWN_SECONDS,
                triggerDb = if (source == EmergencyTriggerSource.SoundDetection) latestDetectedDb else current.currentDb,
            )
        }
    }

    fun updateCountdown(secondsRemaining: Int) {
        _uiState.update { current ->
            current.copy(
                countdownSecondsRemaining = secondsRemaining.coerceAtLeast(0),
            )
        }
    }

    fun activatePendingEmergency() {
        _uiState.update { current ->
            val mode = current.pendingEmergencyMode ?: return@update current
            current.copy(
                isSilenced = false,
                alarmTriggered = mode == EmergencyMode.Loud,
                activeEmergencyMode = mode,
                pendingEmergencyMode = null,
                pendingTriggerSource = null,
                countdownSecondsRemaining = 0,
            )
        }
    }

    private const val UI_DB_UPDATE_INTERVAL_MS = 1_000L
    private const val UI_G_UPDATE_INTERVAL_MS = 2_000L
    private const val COUNTDOWN_SECONDS = 3
}
