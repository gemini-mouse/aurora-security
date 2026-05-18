package app.aurorasecurity.security

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

class AlarmVibrator(context: Context) {
    private var heartbeatActive = false

    private val vibrator: Vibrator? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            manager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }

    fun startHeartbeatPattern() {
        val target = vibrator ?: return
        if (!target.hasVibrator()) return

        heartbeatActive = true
        val timings = longArrayOf(
            0L,
            90L,
            120L,
            150L,
            520L,
            90L,
            120L,
            150L,
            920L,
        )
        val amplitudes = intArrayOf(
            0,
            180,
            0,
            255,
            0,
            180,
            0,
            255,
            0,
        )
        val effect = VibrationEffect.createWaveform(timings, amplitudes, 0)
        target.vibrate(effect)
    }

    fun stopHeartbeat() {
        if (!heartbeatActive) return
        heartbeatActive = false
        vibrator?.cancel()
    }

    fun stopAll() {
        heartbeatActive = false
        vibrator?.cancel()
    }
}
