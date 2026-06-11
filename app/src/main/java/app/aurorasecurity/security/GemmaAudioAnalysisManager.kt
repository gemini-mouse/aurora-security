package app.aurorasecurity.security

import android.content.Context
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatDelegate
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Capabilities
import com.google.ai.edge.litertlm.Content
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.Conversation
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.ExperimentalApi
import com.google.ai.edge.litertlm.ExperimentalFlags
import com.google.ai.edge.litertlm.Message
import com.google.ai.edge.litertlm.MessageCallback
import com.google.ai.edge.litertlm.SamplerConfig
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

enum class AiModelStatus {
    NotDownloaded,
    Downloading,
    Downloaded,
    Ready,
    Initializing,
    Error,
}

enum class AiStatusMessageKey {
    NotDownloaded,
    DownloadPrompt,
    Downloading,
    DownloadPreparing,
    Downloaded,
    Ready,
    InitializePrompt,
    Initializing,
    DownloadFailed,
    InitializationFailed,
    Analyzing,
    AnalysisComplete,
    GpuFallbackRecovered,
    CpuFallbackFailed,
    AnalysisFailed,
    ModelDeleted,
    CouldNotComplete,
}

enum class GemmaModelType(
    @param:StringRes val titleRes: Int,
    @param:StringRes val descriptionRes: Int,
    val modelName: String,
    val filename: String,
    val version: String,
    val downloadUrl: String,
    val minValidBytes: Long,
    val displaySize: String
) {
    E2B(
        titleRes = R.string.gemma_model_e2b_title,
        descriptionRes = R.string.gemma_model_e2b_description,
        modelName = "Gemma-4-E2B-it",
        filename = "gemma-4-E2B-it.litertlm",
        version = "b4f4f4df93418ddb4aa7da8bf33b584602a5b9f8",
        downloadUrl = "https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm/resolve/b4f4f4df93418ddb4aa7da8bf33b584602a5b9f8/gemma-4-E2B-it.litertlm?download=true",
        minValidBytes = 2_000_000_000L,
        displaySize = "2.6G"
    ),
    E4B(
        titleRes = R.string.gemma_model_e4b_title,
        descriptionRes = R.string.gemma_model_e4b_description,
        modelName = "Gemma-4-E4B-it",
        filename = "gemma-4-E4B-it.litertlm",
        version = "10848a680ad0ab45b556566152371b38c238d6f0",
        downloadUrl = "https://huggingface.co/litert-community/gemma-4-E4B-it-litert-lm/resolve/10848a680ad0ab45b556566152371b38c238d6f0/gemma-4-E4B-it.litertlm?download=true",
        minValidBytes = 3_000_000_000L,
        displaySize = "3.7G"
    );

    companion object {
        fun fromName(name: String): GemmaModelType {
            return values().find { it.name == name } ?: E4B
        }
    }

    fun title(context: Context): String = context.getString(titleRes)

    fun description(context: Context): String = context.getString(descriptionRes)
}

data class AiAnalysisResult(
    val content: String,
    val timestamp: String,
)

data class AiAudioAnalysisMetadata(
    val triggerSource: EmergencyTriggerSource,
    val soundLevelDb: Float?,
    val durationMs: Int,
)

data class AiFeatureUiState(
    val currentModelType: GemmaModelType = GemmaModelType.E4B,
    val modelStatus: AiModelStatus = AiModelStatus.NotDownloaded,
    val downloadProgress: Int = 0,
    val statusMessageKey: AiStatusMessageKey = AiStatusMessageKey.NotDownloaded,
    val statusMessageDetail: String = "",
    val accelerator: String = "Not initialized",
    val isAnalyzing: Boolean = false,
    val lastAnalysisResult: AiAnalysisResult? = null,
)

object GemmaAudioAnalysisManager {
    private val modelMutex = Mutex()
    private val _uiState = MutableStateFlow(AiFeatureUiState())
    val uiState: StateFlow<AiFeatureUiState> = _uiState.asStateFlow()

    private var engine: Engine? = null
    private var activeBackendLabel: String = "Not initialized"
    private var loadedModelType: GemmaModelType? = null

    fun refreshState(context: Context, modelType: GemmaModelType) {
        val modelFile = modelFile(context, modelType)
        val isLoadedForCurrentModel = engine != null && loadedModelType == modelType
        val currentState = _uiState.value
        val isBusyForSameModel =
            currentState.currentModelType == modelType &&
                (currentState.modelStatus == AiModelStatus.Downloading ||
                    currentState.modelStatus == AiModelStatus.Initializing)

        if (isBusyForSameModel) {
            _uiState.value = currentState.copy(currentModelType = modelType)
            return
        }

        _uiState.value = currentState.copy(
            modelStatus = when {
                isLoadedForCurrentModel -> AiModelStatus.Ready
                modelFile.exists() -> AiModelStatus.Downloaded
                else -> AiModelStatus.NotDownloaded
            },
            downloadProgress = if (modelFile.exists()) 100 else currentState.downloadProgress,
            statusMessageKey = when {
                isLoadedForCurrentModel -> AiStatusMessageKey.Ready
                modelFile.exists() -> AiStatusMessageKey.Downloaded
                else -> AiStatusMessageKey.DownloadPrompt
            },
            statusMessageDetail = "",
            currentModelType = modelType,
            accelerator = if (isLoadedForCurrentModel) activeBackendLabel else "Not initialized",
        )
    }

    suspend fun ensureModelDownloaded(context: Context, modelType: GemmaModelType) {
        modelMutex.withLock {
            val targetFile = modelFile(context, modelType)
            if (targetFile.exists()) {
                _uiState.value = _uiState.value.copy(currentModelType = modelType)
                refreshState(context, modelType)
                return
            }

            _uiState.value = _uiState.value.copy(
                currentModelType = modelType,
                modelStatus = AiModelStatus.Downloading,
                downloadProgress = 0,
                statusMessageKey = AiStatusMessageKey.Downloading,
                statusMessageDetail = "",
            )

            val tempFile = File(targetFile.parentFile, "${targetFile.name}.part")
            try {
                downloadToFile(modelType.downloadUrl, tempFile, modelType.minValidBytes) { progress ->
                    _uiState.value = _uiState.value.copy(downloadProgress = progress)
                }
                if (targetFile.exists()) {
                    targetFile.delete()
                }
                tempFile.renameTo(targetFile)
                deleteSupersededModelCopies(context, modelType)
                _uiState.value = _uiState.value.copy(
                    modelStatus = AiModelStatus.Ready,
                    downloadProgress = 100,
                    statusMessageKey = AiStatusMessageKey.DownloadPreparing,
                    statusMessageDetail = "",
                )
            } catch (error: Exception) {
                tempFile.delete()
                _uiState.value = _uiState.value.copy(
                    modelStatus = AiModelStatus.Error,
                    statusMessageKey = AiStatusMessageKey.DownloadFailed,
                    statusMessageDetail = error.message.orEmpty(),
                )
                throw error
            }
        }

        ensureInitialized(context = context, modelType = modelType)
    }

    suspend fun ensureInitialized(
        context: Context,
        modelType: GemmaModelType,
        preferredBackends: List<String> = DEFAULT_BACKEND_ORDER,
        forceReinitialize: Boolean = false,
    ) {
        modelMutex.withLock {
            if (engine != null && loadedModelType == modelType && !forceReinitialize) {
                _uiState.value = _uiState.value.copy(
                    modelStatus = AiModelStatus.Ready,
                    accelerator = activeBackendLabel,
                    statusMessageKey = AiStatusMessageKey.Ready,
                    statusMessageDetail = "",
                    currentModelType = modelType
                )
                return
            }

            closeEngineLocked()

            val targetFile = modelFile(context, modelType)
            if (!targetFile.exists()) {
                _uiState.value = _uiState.value.copy(
                    modelStatus = AiModelStatus.NotDownloaded,
                    statusMessageKey = AiStatusMessageKey.InitializePrompt,
                    statusMessageDetail = "",
                    currentModelType = modelType
                )
                return
            }

            _uiState.value = _uiState.value.copy(
                modelStatus = AiModelStatus.Initializing,
                statusMessageKey = AiStatusMessageKey.Initializing,
                statusMessageDetail = "",
                currentModelType = modelType
            )

            val initAttempts = buildBackendAttempts(preferredBackends)
            val enableSpeculativeDecoding = modelSupportsSpeculativeDecoding(targetFile)

            var lastError: Throwable? = null
            for ((backendLabel, backend) in initAttempts) {
                val candidateEngine = runCatching {
                    withContext(Dispatchers.IO) {
                        createEngine(
                            context = context,
                            modelFile = targetFile,
                            backend = backend,
                            enableSpeculativeDecoding = enableSpeculativeDecoding,
                        )
                    }
                }.getOrElse { error ->
                    lastError = error
                    null
                } ?: continue

                engine = candidateEngine
                activeBackendLabel = backendLabel
                loadedModelType = modelType
                _uiState.value = _uiState.value.copy(
                    modelStatus = AiModelStatus.Ready,
                    accelerator = activeBackendLabel,
                    statusMessageKey = AiStatusMessageKey.Ready,
                    statusMessageDetail = activeBackendLabel,
                    currentModelType = modelType
                )
                return
            }

            _uiState.value = _uiState.value.copy(
                modelStatus = AiModelStatus.Error,
                statusMessageKey = AiStatusMessageKey.InitializationFailed,
                statusMessageDetail = lastError?.message.orEmpty(),
                accelerator = "Unavailable",
            )
            throw IllegalStateException(lastError?.message ?: "Unable to initialize Gemma model", lastError)
        }
    }

    suspend fun analyzeClip(
        context: Context,
        clip: CrisisAudioClip,
        modelType: GemmaModelType,
        metadata: AiAudioAnalysisMetadata = AiAudioAnalysisMetadata(
            triggerSource = clip.triggerSource,
            soundLevelDb = null,
            durationMs = clip.durationMs,
        ),
    ): String? {
        ensureInitialized(context = context, modelType = modelType)
        var engineInstance = modelMutex.withLock { engine } ?: return null

        _uiState.value = _uiState.value.copy(
            isAnalyzing = true,
            statusMessageKey = AiStatusMessageKey.Analyzing,
            statusMessageDetail = "",
        )

        return try {
            val result = runAnalysis(context, engineInstance, clip.analysisAudioBytes, modelType, metadata)
            _uiState.value = _uiState.value.copy(
                isAnalyzing = false,
                statusMessageKey = AiStatusMessageKey.AnalysisComplete,
                statusMessageDetail = "",
                accelerator = activeBackendLabel,
                lastAnalysisResult = AiAnalysisResult(
                    content = result,
                    timestamp = ZonedDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                ),
            )
            result
        } catch (error: Exception) {
            if (shouldFallbackToCpu(error) && activeBackendLabel != CPU_BACKEND_LABEL) {
                return runCatching {
                    ensureInitialized(
                        context = context,
                        modelType = modelType,
                        preferredBackends = listOf(CPU_BACKEND_LABEL),
                        forceReinitialize = true,
                    )
                    engineInstance = modelMutex.withLock { engine } ?: return@runCatching null
                    val recoveredResult = runAnalysis(context, engineInstance, clip.analysisAudioBytes, modelType, metadata)
                    _uiState.value = _uiState.value.copy(
                        isAnalyzing = false,
                        modelStatus = AiModelStatus.Ready,
                        accelerator = activeBackendLabel,
                        statusMessageKey = AiStatusMessageKey.GpuFallbackRecovered,
                        statusMessageDetail = "",
                        lastAnalysisResult = AiAnalysisResult(
                            content = recoveredResult,
                            timestamp = ZonedDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                        ),
                    )
                    recoveredResult
                }.getOrElse { recoveryError ->
                    _uiState.value = _uiState.value.copy(
                        isAnalyzing = false,
                        modelStatus = AiModelStatus.Error,
                        accelerator = activeBackendLabel,
                        statusMessageKey = AiStatusMessageKey.CpuFallbackFailed,
                        statusMessageDetail = recoveryError.message.orEmpty(),
                    )
                    null
                }
            }

            _uiState.value = _uiState.value.copy(
                isAnalyzing = false,
                modelStatus = AiModelStatus.Error,
                accelerator = activeBackendLabel,
                statusMessageKey = AiStatusMessageKey.AnalysisFailed,
                statusMessageDetail = error.message.orEmpty(),
            )
            null
        }
    }

    suspend fun downloadIfNeeded(context: Context, modelType: GemmaModelType) {
        val targetFile = modelFile(context, modelType)
        if (!targetFile.exists()) {
            ensureModelDownloaded(context, modelType)
        } else {
            ensureInitialized(context = context, modelType = modelType)
        }
    }

    suspend fun retryInitialization(context: Context, modelType: GemmaModelType) {
        ensureInitialized(
            context = context,
            modelType = modelType,
            preferredBackends = DEFAULT_BACKEND_ORDER,
            forceReinitialize = true,
        )
    }

    /**
     * Deletes the specified model file from disk to free up space.
     * If the model is currently loaded in the engine, it will be closed first.
     */
    suspend fun deleteModel(context: Context, modelType: GemmaModelType) {
        modelMutex.withLock {
            // 1. Close engine if it's the model we're deleting
            if (loadedModelType == modelType) {
                closeEngineLocked()
            }
            // 2. Delete the file
            val targetFile = modelFile(context, modelType)
            if (targetFile.exists()) {
                targetFile.delete()
            }
            // 3. Clean up the version directory if empty
            targetFile.parentFile?.let { versionDir ->
                if (versionDir.exists() && versionDir.isDirectory && versionDir.list()?.isEmpty() == true) {
                    versionDir.delete()
                }
            }
            // 4. Update UI state if this is the current model
            if (_uiState.value.currentModelType == modelType) {
                _uiState.value = _uiState.value.copy(
                    modelStatus = AiModelStatus.NotDownloaded,
                    statusMessageKey = AiStatusMessageKey.ModelDeleted,
                    statusMessageDetail = "",
                    downloadProgress = 0,
                    accelerator = "Not initialized"
                )
            }
        }
    }

    fun isModelDownloaded(context: Context, modelType: GemmaModelType): Boolean {
        return modelFile(context, modelType).exists()
    }

    fun isModelReady(modelType: GemmaModelType): Boolean {
        return engine != null && loadedModelType == modelType && _uiState.value.modelStatus == AiModelStatus.Ready
    }

    private fun buildBackendAttempts(preferredBackends: List<String>): List<Pair<String, Backend>> {
        val attempts = mutableListOf<Pair<String, Backend>>()
        preferredBackends.forEach { label ->
            when (label) {
                GPU_BACKEND_LABEL -> attempts += GPU_BACKEND_LABEL to Backend.GPU()
                CPU_BACKEND_LABEL -> attempts += CPU_BACKEND_LABEL to Backend.CPU()
            }
        }
        return attempts
    }

    @OptIn(ExperimentalApi::class)
    private fun closeEngineLocked() {
        runCatching { engine?.close() }
        ExperimentalFlags.enableSpeculativeDecoding = null
        engine = null
        activeBackendLabel = "Not initialized"
        loadedModelType = null
    }

    private fun shouldFallbackToCpu(error: Throwable): Boolean {
        val message = error.message.orEmpty().lowercase()
        return message.contains("opencl") ||
            message.contains("status code: 2") ||
            message.contains("unknown: can not find opencl library")
    }

    private suspend fun downloadToFile(
        sourceUrl: String,
        outputFile: File,
        minValidBytes: Long,
        onProgress: (Int) -> Unit,
    ) = withContext(Dispatchers.IO) {
        outputFile.parentFile?.mkdirs()
        val connection = (URL(sourceUrl).openConnection() as HttpURLConnection).apply {
            connectTimeout = 30_000
            readTimeout = 120_000
            requestMethod = "GET"
        }

        try {
            val responseCode = connection.responseCode
            if (responseCode !in 200..299) {
                throw IllegalStateException("Download failed with HTTP $responseCode from $sourceUrl")
            }
            val totalBytes = connection.contentLengthLong
            FileOutputStream(outputFile).use { output ->
                connection.inputStream.use { input ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    var downloaded = 0L
                    while (true) {
                        val read = input.read(buffer)
                        if (read <= 0) break
                        output.write(buffer, 0, read)
                        downloaded += read
                        if (totalBytes > 0) {
                            onProgress(((downloaded * 100) / totalBytes).toInt().coerceIn(0, 100))
                        }
                    }
                }
            }
            val downloadedSize = outputFile.length()
            if (downloadedSize < minValidBytes) {
                throw IllegalStateException(
                    "Downloaded model is too small (${downloadedSize} bytes). Expected at least $minValidBytes bytes.",
                )
            }
        } finally {
            connection.disconnect()
        }
    }

    @OptIn(ExperimentalApi::class)
    private fun createEngine(
        context: Context,
        modelFile: File,
        backend: Backend,
        enableSpeculativeDecoding: Boolean,
    ): Engine {
        ExperimentalFlags.enableSpeculativeDecoding = enableSpeculativeDecoding
        val engine = Engine(
            EngineConfig(
                modelPath = modelFile.absolutePath,
                backend = backend,
                audioBackend = Backend.CPU(),
                maxNumTokens = 768,
                cacheDir = context.cacheDir.absolutePath,
            ),
        )
        engine.initialize()
        return engine
    }

    private fun modelSupportsSpeculativeDecoding(modelFile: File): Boolean {
        return runCatching {
            Capabilities(modelFile.absolutePath).use { capabilities ->
                capabilities.hasSpeculativeDecodingSupport()
            }
        }.getOrDefault(false)
    }

    private fun deleteSupersededModelCopies(context: Context, modelType: GemmaModelType) {
        val baseDir = context.getExternalFilesDir(null) ?: context.filesDir
        val modelRoot = File(baseDir, "models")
        modelRoot
            .listFiles { file -> file.isDirectory && file.name != modelType.version }
            .orEmpty()
            .forEach { versionDir ->
                val supersededFile = File(versionDir, modelType.filename)
                if (supersededFile.exists()) {
                    supersededFile.delete()
                }
                if (versionDir.list()?.isEmpty() == true) {
                    versionDir.delete()
                }
            }
    }

    @OptIn(ExperimentalApi::class)
    private suspend fun runAnalysis(
        context: Context,
        engine: Engine,
        audioBytes: ByteArray,
        modelType: GemmaModelType,
        metadata: AiAudioAnalysisMetadata,
    ): String = withContext(Dispatchers.IO) {
        val conversation = engine.createConversation(
            ConversationConfig(
                samplerConfig = SamplerConfig(
                    topK = 64,
                    topP = 0.95,
                    temperature = 1.0,
                ),
            ),
        )

        try {
            val result = CompletableDeferred<String>()
            val builder = StringBuilder()
            conversation.sendMessageAsync(
                Contents.of(
                    listOf(
                        Content.AudioBytes(audioBytes),
                        Content.Text(buildAudioAnalysisPrompt(context, modelType, metadata)),
                    ),
                ),
                object : MessageCallback {
                    override fun onMessage(message: Message) {
                        builder.append(message.toString())
                    }

                    override fun onDone() {
                        result.complete(builder.toString().trim())
                    }

                    override fun onError(throwable: Throwable) {
                        if (throwable is CancellationException) {
                            result.complete(builder.toString().trim())
                        } else {
                            result.completeExceptionally(throwable)
                        }
                    }
                },
                emptyMap(),
            )
            result.await()
        } finally {
            runCatching { conversation.close() }
        }
    }

    private fun modelFile(context: Context, modelType: GemmaModelType): File {
        val baseDir = context.getExternalFilesDir(null) ?: context.filesDir
        return File(baseDir, "models/${modelType.version}/${modelType.filename}")
    }

    private fun buildAudioAnalysisPrompt(
        context: Context,
        modelType: GemmaModelType,
        metadata: AiAudioAnalysisMetadata,
    ): String {
        val outputLanguage = when (AppLanguageOption.fromLanguageTags(
            AppCompatDelegate.getApplicationLocales().toLanguageTags(),
        )) {
            AppLanguageOption.TraditionalChinese -> "Traditional Chinese"
            AppLanguageOption.Spanish -> "Spanish"
            AppLanguageOption.Korean -> "Korean"
            AppLanguageOption.Japanese -> "Japanese"
            AppLanguageOption.English -> "English"
        }

        return buildString {
            appendLine("Analyze this audio clip for personal safety risk.")
            appendLine()
            appendLine("Language:")
            if (modelType == GemmaModelType.E2B) {
                appendLine("- Write all field values in $outputLanguage.")
                appendLine("- Do not transcribe, quote, paraphrase, translate, or infer spoken words.")
                appendLine("- Keep Speech Content exactly: unavailable.")
            } else {
                appendLine("- Write all field values in $outputLanguage, except Speech Content.")
                appendLine("- Speech Content must preserve the original spoken language. Do not translate it.")
            }
            appendLine()
            appendLine("Audio metadata:")
            appendLine("- Trigger Source: ${metadata.triggerSource.promptLabel()}")
            appendLine("- Duration: ${metadata.durationMs} ms")
            appendLine("- Sound Level: ${metadata.soundLevelDb?.let { "%.1f dB".format(it) } ?: "unknown"}")
            appendLine()
            appendLine("Definitions:")
            appendLine("- Acoustic Environment: describe the audible scene, such as quiet room, normal conversation, screaming, crying, traffic, music, or low background noise. Use \"none\" only when the clip is silent or unusable.")
            if (modelType == GemmaModelType.E2B) {
                appendLine("- Alert Sounds: safety-relevant audio cues, including screams, crying, panic cries, calls for help, distressed pleading, sirens, or urgent non-transcribed speech cues. Use \"none detected\" only when no safety-relevant cue is heard.")
                appendLine("- Vocal Stress & Emotion: describe the vocal state, such as normal, calm, angry, crying, panic, aggressive, or screaming.")
                appendLine("- Situation Assessment: one short sentence based only on acoustic environment, alert sounds, and vocal stress.")
            } else {
                appendLine("- Alert Sounds: safety-relevant audio cues, including screams, crying, panic cries, calls for help, distressed pleading, sirens, or urgent words such as \"help\", \"stop\", \"don't\", and equivalents in any language. Use \"none detected\" only when no safety-relevant cue is heard.")
                appendLine("- Vocal Stress & Emotion: describe the speaker's vocal state, such as normal, calm, angry, crying, panic, aggressive, or screaming.")
                appendLine("- Speech Content: transcribe meaningful clear speech only, in the original spoken language. For non-word screaming or repeated \"ah\" sounds, use \"non-speech screaming\" instead of long repetition.")
                appendLine("- Situation Assessment: one short sentence summarizing what may be happening.")
            }
            appendLine("- Danger Level: one of Danger, High, Medium, Low.")
            appendLine()
            appendLine("Consistency:")
            appendLine("- If speech, voice, scream, music, or noise is audible, Acoustic Environment must not be \"none\".")
            appendLine("- If screaming, crying, panic, aggressive yelling, or a call for help is present, Alert Sounds must not be \"none detected\".")
            appendLine("- Normal conversation without distress is Low.")
            appendLine("- Screaming, panic, crying, calls for help, or aggressive yelling is High or Danger.")
            appendLine("- Distressed pleading or repeated rescue requests is Medium or High.")
            appendLine()
            appendLine("Output exactly:")
            appendLine("- Acoustic Environment: <under 12 words>")
            appendLine("- Alert Sounds: <under 12 words>")
            appendLine("- Vocal Stress & Emotion: <under 8 words>")
            if (modelType == GemmaModelType.E2B) {
                appendLine("- Speech Content: unavailable")
            } else {
                appendLine("- Speech Content: <clear speech, non-speech screaming, or none detected>")
            }
            appendLine("- Situation Assessment: <one short sentence>")
            append("- Danger Level: Danger / High / Medium / Low")
        }
    }

    private fun EmergencyTriggerSource.promptLabel(): String {
        return when (this) {
            EmergencyTriggerSource.SoundDetection -> "sound detection"
            EmergencyTriggerSource.SilentSosButton -> "manual silent SOS"
            EmergencyTriggerSource.LoudSosButton -> "manual loud SOS"
        }
    }

    private val DEFAULT_BACKEND_ORDER = listOf(GPU_BACKEND_LABEL, CPU_BACKEND_LABEL)
    private const val GPU_BACKEND_LABEL = "GPU"
    private const val CPU_BACKEND_LABEL = "CPU"
}
