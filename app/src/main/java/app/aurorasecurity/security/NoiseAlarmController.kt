package app.aurorasecurity.security

import android.os.SystemClock
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.update
import kotlin.math.sqrt

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
    private val _alertDeliveryResults = MutableSharedFlow<AlertDeliverySummary>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    internal val alertDeliveryResults: SharedFlow<AlertDeliverySummary> = _alertDeliveryResults.asSharedFlow()
    private var latestDetectedDb: Float = 0f
    private var lastUiDbUpdateAtMs: Long = 0L
    private var lastUiGUpdateAtMs: Long = 0L
    private var currentPeakMagnitudeSquared: Float = 0f
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
            val thresholdState = current.evaluateThresholdState(
                detectedDb = latestDetectedDb,
                thresholdDb = value,
                isShaking = current.isShaking,
                canResetSilence = true,
            )
            current.copy(
                thresholdDb = value,
                isSilenced = thresholdState.isSilenced,
                isThresholdExceeded = thresholdState.isThresholdExceeded,
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
            val thresholdState = current.evaluateThresholdState(
                detectedDb = db,
                thresholdDb = current.thresholdDb,
                isShaking = current.isShaking,
                canResetSilence = true,
            )
            
            val timeUpdate = now - lastUiDbUpdateAtMs >= UI_DB_UPDATE_INTERVAL_MS
            val stateUpdate =
                thresholdState.isThresholdExceeded != current.isThresholdExceeded ||
                    thresholdState.isSilenced != current.isSilenced
            
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
                isSilenced = thresholdState.isSilenced,
                isThresholdExceeded = thresholdState.isThresholdExceeded,
            )
        }
    }

    fun updateShakeLevel(gForce: Float, isShaking: Boolean) {
        val magnitude = gForce * GRAVITY_MPS2
        updateShakeMagnitudeSquared(magnitude * magnitude, isShaking)
    }

    fun updateShakeMagnitudeSquared(magnitudeSquared: Float, isShaking: Boolean) {
        val now = SystemClock.elapsedRealtime()
        currentPeakMagnitudeSquared = maxOf(currentPeakMagnitudeSquared, magnitudeSquared.coerceAtLeast(0f))

        _uiState.update { current ->
            val thresholdState = current.evaluateThresholdState(
                detectedDb = latestDetectedDb,
                thresholdDb = current.thresholdDb,
                isShaking = isShaking,
                canResetSilence = false,
            )
            
            val shouldUpdateDisplayedG = now - lastUiGUpdateAtMs >= UI_G_UPDATE_INTERVAL_MS
            val displayG = if (shouldUpdateDisplayedG) {
                val peak = sqrt(currentPeakMagnitudeSquared.toDouble()).toFloat() / GRAVITY_MPS2
                currentPeakMagnitudeSquared = 0f
                lastUiGUpdateAtMs = now
                peak
            } else {
                current.currentG
            }

            current.copy(
                currentG = displayG,
                isShaking = isShaking,
                isThresholdExceeded = thresholdState.isThresholdExceeded
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

    internal fun publishAlertDeliveryResult(summary: AlertDeliverySummary) {
        _alertDeliveryResults.tryEmit(summary)
    }

    private fun NoiseAlarmUiState.evaluateThresholdState(
        detectedDb: Float,
        thresholdDb: Float,
        isShaking: Boolean,
        canResetSilence: Boolean,
    ): ThresholdState {
        val nextSilenced = if (canResetSilence && detectedDb < thresholdDb) {
            false
        } else {
            isSilenced
        }
        val soundThresholdExceeded =
            hasMicPermission &&
                isMonitoring &&
                !nextSilenced &&
                detectedDb >= thresholdDb
        val thresholdExceeded = if (useShakeDetection) {
            soundThresholdExceeded && isShaking
        } else {
            soundThresholdExceeded
        }
        return ThresholdState(
            isSilenced = nextSilenced,
            isThresholdExceeded = thresholdExceeded,
        )
    }

    private data class ThresholdState(
        val isSilenced: Boolean,
        val isThresholdExceeded: Boolean,
    )

    private const val UI_DB_UPDATE_INTERVAL_MS = 1_000L
    private const val UI_G_UPDATE_INTERVAL_MS = 2_000L
    private const val COUNTDOWN_SECONDS = 3
    private const val GRAVITY_MPS2 = 9.81f
}
