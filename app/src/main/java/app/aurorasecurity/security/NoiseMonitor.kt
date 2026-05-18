package app.aurorasecurity.security

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.sqrt

class NoiseMonitor {
    private val scope = CoroutineScope(Dispatchers.Default)
    private var monitorJob: Job? = null
    private var audioRecord: AudioRecord? = null

    fun start(
        onLevelChanged: (Float) -> Unit,
        onPcmFrame: ((ShortArray, Int) -> Unit)? = null,
    ) {
        if (monitorJob?.isActive == true) return

        // Align microphone capture with Gemma 4 audio guidance: mono, 16 kHz, 32 ms frames.
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
                val read = recorder.read(buffer, 0, buffer.size)
                if (read > 0) {
                    onPcmFrame?.invoke(buffer, read)
                    val db = calculateDb(buffer, read)
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
        if (activeJob != null) {
            scope.launch {
                activeJob.cancelAndJoin()
                releaseRecorder()
            }
        } else {
            releaseRecorder()
        }
    }

    private fun releaseRecorder() {
        audioRecord?.runCatching {
            if (recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                stop()
            }
        }
        audioRecord?.release()
        audioRecord = null
    }

    private fun calculateDb(buffer: ShortArray, read: Int): Float {
        var sum = 0.0
        for (index in 0 until read) {
            sum += buffer[index].toDouble().pow(2.0)
        }
        val rms = sqrt(sum / read).coerceAtLeast(1.0)
        val normalizedDb = 20 * log10(rms / Short.MAX_VALUE)
        return (normalizedDb + 90).toFloat().coerceIn(0f, 120f)
    }

    companion object {
        private const val SAMPLE_RATE_HZ = 16_000
        private const val FRAME_SAMPLES = 512 // 32 ms at 16 kHz
    }
}
