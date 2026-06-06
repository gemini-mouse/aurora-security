package app.aurorasecurity.security

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.log10

class NoiseMonitor {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var monitorJob: Job? = null
    private var audioRecord: AudioRecord? = null

    fun start(
        onLevelChanged: (Float) -> Unit,
        onPcmFrame: ((ShortArray, Int) -> Unit)? = null,
    ) {
        if (monitorJob?.isActive == true) return

        // Capture mono 16 kHz PCM in 512 ms frames to reduce wakeups while preserving continuous audio.
        val sampleRate = SAMPLE_RATE_HZ
        val minBufferSize = AudioRecord.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
        )
        if (minBufferSize <= 0) return

        val recorderBufferSize = maxOf(minBufferSize, FRAME_SAMPLES * 4)

        val recorder = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            recorderBufferSize,
        )
        if (recorder.state != AudioRecord.STATE_INITIALIZED) {
            recorder.release()
            return
        }

        audioRecord = recorder
        recorder.startRecording()

        monitorJob = scope.launch {
            val buffer = ShortArray(FRAME_SAMPLES)
            while (isActive) {
                val read = try {
                    recorder.read(buffer, 0, buffer.size)
                } catch (_: IllegalStateException) {
                    break
                }
                if (read > 0) {
                    onPcmFrame?.invoke(buffer, read)
                    val db = calculateDb(buffer, read)
                    if (!isActive) break
                    withContext(Dispatchers.Main) {
                        onLevelChanged(db)
                    }
                }
            }
        }
    }

    fun stop() {
        val activeJob = monitorJob
        monitorJob = null
        activeJob?.cancel()
        releaseRecorder()
    }

    fun close() {
        stop()
        scope.cancel()
    }

    private fun releaseRecorder() {
        val recorder = audioRecord ?: return
        audioRecord = null
        recorder.runCatching {
            if (recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                stop()
            }
        }
        recorder.release()
    }

    private fun calculateDb(buffer: ShortArray, read: Int): Float {
        var sumSquares = 0L
        for (index in 0 until read) {
            val sample = buffer[index].toLong()
            sumSquares += sample * sample
        }
        val meanSquare = (sumSquares.toDouble() / read).coerceAtLeast(1.0)
        val db = 10.0 * log10(meanSquare) + DB_OFFSET
        return db.toFloat().coerceIn(0f, 120f)
    }

    companion object {
        private const val SAMPLE_RATE_HZ = 16_000
        private const val FRAME_SAMPLES = 8_192 // 512 ms at 16 kHz
        private val DB_OFFSET = 90.0 - 20.0 * log10(Short.MAX_VALUE.toDouble())
    }
}
