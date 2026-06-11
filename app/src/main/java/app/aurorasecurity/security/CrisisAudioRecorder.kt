package app.aurorasecurity.security

import android.content.Context
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicLong

internal object AudioEncodingUtil {
    private const val CHANNEL_COUNT = 1
    private const val PCM_BYTES_PER_SAMPLE = 2
    private const val WAV_HEADER_BYTES = 44
    private const val AAC_BIT_RATE = 64_000
    private const val AAC_INPUT_CHUNK_SIZE = 16_384
    private const val CODEC_TIMEOUT_US = 10_000L

    fun encodePcm16ToM4aBytes(context: Context, samples: ShortArray): ByteArray? {
        val tempFile = File(context.cacheDir, "crisis_tmp_${System.nanoTime()}.m4a")
        return try {
            val pcmBytes = ByteArray(samples.size * PCM_BYTES_PER_SAMPLE)
            ByteBuffer.wrap(pcmBytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().put(samples)

            val format = MediaFormat.createAudioFormat(
                MediaFormat.MIMETYPE_AUDIO_AAC,
                CrisisAudioConfig.SAMPLE_RATE_HZ,
                CHANNEL_COUNT,
            ).apply {
                setInteger(MediaFormat.KEY_BIT_RATE, AAC_BIT_RATE)
                setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, AAC_INPUT_CHUNK_SIZE)
                setInteger(
                    MediaFormat.KEY_AAC_PROFILE,
                    MediaCodecInfo.CodecProfileLevel.AACObjectLC,
                )
            }

            val codec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC)
            codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            codec.start()

            val muxer = MediaMuxer(
                tempFile.absolutePath,
                MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4,
            )

            var audioTrackIndex = -1
            var muxerStarted = false
            var inputOffset = 0
            var encodingDone = false
            val bufferInfo = MediaCodec.BufferInfo()

            while (!encodingDone) {
                if (inputOffset < pcmBytes.size) {
                    val inputIndex = codec.dequeueInputBuffer(CODEC_TIMEOUT_US)
                    if (inputIndex >= 0) {
                        val inputBuffer = codec.getInputBuffer(inputIndex)!!
                        val chunkSize = (pcmBytes.size - inputOffset).coerceAtMost(inputBuffer.capacity())
                        inputBuffer.clear()
                        inputBuffer.put(pcmBytes, inputOffset, chunkSize)
                        val presentationUs = inputOffset.toLong() * 1_000_000L /
                            (CrisisAudioConfig.SAMPLE_RATE_HZ * PCM_BYTES_PER_SAMPLE)
                        inputOffset += chunkSize
                        val flags = if (inputOffset >= pcmBytes.size) {
                            MediaCodec.BUFFER_FLAG_END_OF_STREAM
                        } else {
                            0
                        }
                        codec.queueInputBuffer(inputIndex, 0, chunkSize, presentationUs, flags)
                    }
                }

                val outputIndex = codec.dequeueOutputBuffer(bufferInfo, CODEC_TIMEOUT_US)
                when {
                    outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        audioTrackIndex = muxer.addTrack(codec.outputFormat)
                        muxer.start()
                        muxerStarted = true
                    }
                    outputIndex >= 0 -> {
                        if (muxerStarted && bufferInfo.size > 0 &&
                            bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG == 0
                        ) {
                            val outputBuffer = codec.getOutputBuffer(outputIndex)!!
                            outputBuffer.position(bufferInfo.offset)
                            outputBuffer.limit(bufferInfo.offset + bufferInfo.size)
                            muxer.writeSampleData(audioTrackIndex, outputBuffer, bufferInfo)
                        }
                        codec.releaseOutputBuffer(outputIndex, false)
                        if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                            encodingDone = true
                        }
                    }
                }
            }

            if (muxerStarted) muxer.stop()
            muxer.release()
            codec.stop()
            codec.release()

            tempFile.takeIf { it.length() > 0L }?.readBytes()
        } catch (_: Exception) {
            null
        } finally {
            tempFile.delete()
        }
    }

    fun pcm16ToWavBytes(samples: ShortArray): ByteArray {
        val pcmByteSize = samples.size * PCM_BYTES_PER_SAMPLE
        val byteRate = CrisisAudioConfig.SAMPLE_RATE_HZ * CHANNEL_COUNT * PCM_BYTES_PER_SAMPLE
        val blockAlign = (CHANNEL_COUNT * PCM_BYTES_PER_SAMPLE).toShort()
        val bitsPerSample = (PCM_BYTES_PER_SAMPLE * 8).toShort()

        val header = ByteArray(WAV_HEADER_BYTES)
        val buffer = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN)
        buffer.put("RIFF".toByteArray())
        buffer.putInt(pcmByteSize + 36)
        buffer.put("WAVE".toByteArray())
        buffer.put("fmt ".toByteArray())
        buffer.putInt(16)
        buffer.putShort(1)
        buffer.putShort(CHANNEL_COUNT.toShort())
        buffer.putInt(CrisisAudioConfig.SAMPLE_RATE_HZ)
        buffer.putInt(byteRate)
        buffer.putShort(blockAlign)
        buffer.putShort(bitsPerSample)
        buffer.put("data".toByteArray())
        buffer.putInt(pcmByteSize)

        val pcmBuffer = ByteBuffer.allocate(pcmByteSize).order(ByteOrder.LITTLE_ENDIAN)
        samples.forEach { sample -> pcmBuffer.putShort(sample) }
        return header + pcmBuffer.array()
    }
}

data class CrisisAudioClip(
    val sessionId: Long,
    val triggerSource: EmergencyTriggerSource,
    val emergencyMode: EmergencyMode,
    val analysisAudioBytes: ByteArray,
    val m4aBytes: ByteArray?,
    val file: File?,
    val wavFile: File,
    val durationMs: Int,
)

class CrisisAudioRecorder(
    private val context: Context,
    private val onClipReady: (CrisisAudioClip) -> Unit,
) {
    private val lock = Any()
    private val encodingExecutor: ExecutorService = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "CrisisAudioRecorder").apply {
            isDaemon = true
        }
    }
    private val ringBuffer = ShortArray(PRE_TRIGGER_SAMPLES)
    private var ringWriteIndex = 0
    private var ringFilledSamples = 0
    private var captureSession: CaptureSession? = null
    @Volatile
    private var closed = false

    fun appendFrame(frame: ShortArray, samplesRead: Int) {
        if (closed || samplesRead <= 0) return

        var pcmDataToEncode: Pair<ShortArray, CaptureSession>? = null
        synchronized(lock) {
            if (closed) return@synchronized
            appendToRingBufferLocked(frame, samplesRead)

            val activeSession = captureSession ?: return@synchronized
            val boundedSamplesRead = samplesRead.coerceAtMost(frame.size)
            activeSession.append(frame, boundedSamplesRead)
            if (activeSession.isComplete()) {
                val pcmData = buildPcmDataLocked(activeSession)
                pcmDataToEncode = pcmData to activeSession
                captureSession = null
            }
        }

        pcmDataToEncode?.let { (pcmData, session) ->
            runCatching {
                encodingExecutor.execute {
                    if (closed) return@execute
                    val clip = finalizeClip(pcmData, session)
                    if (!closed) {
                        onClipReady(clip)
                    }
                }
            }
        }
    }

    fun startCapture(
        triggerSource: EmergencyTriggerSource,
        emergencyMode: EmergencyMode,
    ): Long {
        synchronized(lock) {
            val existing = captureSession
            if (existing != null) return existing.sessionId

            val preTriggerSamples = snapshotRingBufferLocked()
            val session = CaptureSession(
                sessionId = sessionCounter.incrementAndGet(),
                triggerSource = triggerSource,
                emergencyMode = emergencyMode,
                preTriggerSamples = preTriggerSamples,
            )
            captureSession = session
            return session.sessionId
        }
    }

    fun cancelCapture(sessionId: Long? = null) {
        synchronized(lock) {
            if (sessionId == null || captureSession?.sessionId == sessionId) {
                captureSession = null
            }
        }
    }

    fun close() {
        closed = true
        cancelCapture()
        encodingExecutor.shutdownNow()
    }

    private fun appendToRingBufferLocked(frame: ShortArray, samplesRead: Int) {
        for (index in 0 until samplesRead) {
            ringBuffer[ringWriteIndex] = frame[index]
            ringWriteIndex = (ringWriteIndex + 1) % ringBuffer.size
            ringFilledSamples = (ringFilledSamples + 1).coerceAtMost(ringBuffer.size)
        }
    }

    private fun snapshotRingBufferLocked(): ShortArray {
        if (ringFilledSamples == 0) return ShortArray(0)

        val result = ShortArray(ringFilledSamples)
        val startIndex = if (ringFilledSamples == ringBuffer.size) ringWriteIndex else 0
        for (index in 0 until ringFilledSamples) {
            result[index] = ringBuffer[(startIndex + index) % ringBuffer.size]
        }
        return result
    }

    private fun buildPcmDataLocked(session: CaptureSession): ShortArray {
        val pcmData = ShortArray(session.preTriggerSamples.size + session.postTriggerSamples.size)
        System.arraycopy(
            session.preTriggerSamples,
            0,
            pcmData,
            0,
            session.preTriggerSamples.size,
        )
        System.arraycopy(
            session.postTriggerSamples,
            0,
            pcmData,
            session.preTriggerSamples.size,
            session.postTriggerSamples.size,
        )
        return pcmData
    }

    private fun finalizeClip(pcmData: ShortArray, session: CaptureSession): CrisisAudioClip {
        val analysisAudioBytes = AudioEncodingUtil.pcm16ToWavBytes(pcmData)
        val m4aBytes = encodeToM4aBytes(pcmData)

        val outputDir = File(context.filesDir, "crisis-audio").apply { mkdirs() }
        val outputBaseName = nextOutputBaseName(outputDir)
        val outputFile = m4aBytes?.let { encodedAudio ->
            File(outputDir, "$outputBaseName.m4a").also { file ->
                FileOutputStream(file).use { stream ->
                    stream.write(encodedAudio)
                }
            }
        }
        val wavFile = File(outputDir, "$outputBaseName.wav")
        FileOutputStream(wavFile).use { stream ->
            stream.write(analysisAudioBytes)
        }

        return CrisisAudioClip(
            sessionId = session.sessionId,
            triggerSource = session.triggerSource,
            emergencyMode = session.emergencyMode,
            analysisAudioBytes = analysisAudioBytes,
            m4aBytes = m4aBytes,
            file = outputFile,
            wavFile = wavFile,
            durationMs = CrisisAudioConfig.TOTAL_DURATION_MS,
        )
    }

    private fun encodeToM4aBytes(samples: ShortArray): ByteArray? {
        return AudioEncodingUtil.encodePcm16ToM4aBytes(context, samples)
    }

    private fun nextOutputBaseName(outputDir: File): String {
        val timestamp = LocalDateTime.now().format(FILE_NAME_FORMATTER)
        var candidate = "crisis_${timestamp}"
        var suffix = 1
        while (
            File(outputDir, "$candidate.m4a").exists() ||
            File(outputDir, "$candidate.wav").exists()
        ) {
            candidate = "crisis_${timestamp}_$suffix"
            suffix += 1
        }
        return candidate
    }

    private data class CaptureSession(
        val sessionId: Long,
        val triggerSource: EmergencyTriggerSource,
        val emergencyMode: EmergencyMode,
        val preTriggerSamples: ShortArray,
        val postTriggerSamples: ShortArray = ShortArray(POST_TRIGGER_SAMPLES),
        var postSamplesWritten: Int = 0,
    ) {
        fun append(frame: ShortArray, samplesRead: Int) {
            val writable = minOf(samplesRead, postTriggerSamples.size - postSamplesWritten)
            if (writable <= 0) return
            System.arraycopy(frame, 0, postTriggerSamples, postSamplesWritten, writable)
            postSamplesWritten += writable
        }

        fun isComplete(): Boolean = postSamplesWritten >= postTriggerSamples.size
    }

    companion object {
        private const val PRE_TRIGGER_SAMPLES =
            CrisisAudioConfig.SAMPLE_RATE_HZ * CrisisAudioConfig.PRE_TRIGGER_SECONDS
        private const val POST_TRIGGER_SAMPLES =
            CrisisAudioConfig.SAMPLE_RATE_HZ * CrisisAudioConfig.POST_TRIGGER_SECONDS
        private val sessionCounter = AtomicLong(0)
        private val FILE_NAME_FORMATTER: DateTimeFormatter =
            DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")
    }
}
