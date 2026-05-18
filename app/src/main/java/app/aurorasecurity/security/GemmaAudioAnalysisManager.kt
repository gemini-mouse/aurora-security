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
import java.util.Locale

enum class AiModelStatus {
    NotDownloaded,
    Downloading,
    Downloaded,
    Ready,
    Initializing,
    Error,
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

data class AiFeatureUiState(
    val currentModelType: GemmaModelType = GemmaModelType.E4B,
    val modelStatus: AiModelStatus = AiModelStatus.NotDownloaded,
    val downloadProgress: Int = 0,
    val statusMessage: String = "Model not downloaded yet.",
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
            statusMessage = when {
                isLoadedForCurrentModel -> "Model ready for on-device audio analysis."
                modelFile.exists() -> "Model downloaded. Initialize the AI engine before enabling AI analysis."
                else -> "Download ${modelType.modelName} to enable local audio analysis."
            },
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
                statusMessage = "Downloading ${modelType.modelName} for the first time...",
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
                    statusMessage = "MTP-capable model downloaded. Preparing LiteRT-LM engine...",
                )
            } catch (error: Exception) {
                tempFile.delete()
                _uiState.value = _uiState.value.copy(
                    modelStatus = AiModelStatus.Error,
                    statusMessage = "Model download failed: ${error.message ?: "unknown error"}",
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
                    statusMessage = "Model ready for on-device audio analysis.",
                    currentModelType = modelType
                )
                return
            }

            closeEngineLocked()

            val targetFile = modelFile(context, modelType)
            if (!targetFile.exists()) {
                _uiState.value = _uiState.value.copy(
                    modelStatus = AiModelStatus.NotDownloaded,
                    statusMessage = "Download the Gemma model before enabling AI analysis.",
                    currentModelType = modelType
                )
                return
            }

            _uiState.value = _uiState.value.copy(
                modelStatus = AiModelStatus.Initializing,
                statusMessage = "Initializing Gemma audio engine...",
                currentModelType = modelType
            )

            val initAttempts = buildBackendAttempts(context, preferredBackends)
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
                activeBackendLabel = buildAccelerationLabel(backendLabel, enableSpeculativeDecoding)
                loadedModelType = modelType
                _uiState.value = _uiState.value.copy(
                    modelStatus = AiModelStatus.Ready,
                    accelerator = activeBackendLabel,
                    statusMessage = "Model ready. Running on $activeBackendLabel.",
                    currentModelType = modelType
                )
                return
            }

            _uiState.value = _uiState.value.copy(
                modelStatus = AiModelStatus.Error,
                statusMessage = "Model initialization failed: ${lastError?.message ?: "unknown error"}",
                accelerator = "Unavailable",
            )
            throw IllegalStateException(lastError?.message ?: "Unable to initialize Gemma model", lastError)
        }
    }

    suspend fun analyzeClip(
        context: Context,
        clip: CrisisAudioClip,
        modelType: GemmaModelType,
    ): String? {
        ensureInitialized(context = context, modelType = modelType)
        var engineInstance = modelMutex.withLock { engine } ?: return null

        _uiState.value = _uiState.value.copy(
            isAnalyzing = true,
            statusMessage = "Analyzing crisis audio clip...",
        )

        return try {
            val result = runAnalysis(context, engineInstance, clip.analysisAudioBytes, modelType)
            _uiState.value = _uiState.value.copy(
                isAnalyzing = false,
                statusMessage = "Analysis complete.",
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
                    val recoveredResult = runAnalysis(context, engineInstance, clip.analysisAudioBytes, modelType)
                    _uiState.value = _uiState.value.copy(
                        isAnalyzing = false,
                        modelStatus = AiModelStatus.Ready,
                        accelerator = activeBackendLabel,
                        statusMessage = "GPU audio inference failed on this device. Aurora switched to CPU and recovered successfully.",
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
                        statusMessage = "Audio analysis failed after CPU fallback: ${recoveryError.message ?: "unknown error"}",
                    )
                    null
                }
            }

            _uiState.value = _uiState.value.copy(
                isAnalyzing = false,
                modelStatus = AiModelStatus.Error,
                accelerator = activeBackendLabel,
                statusMessage = "Audio analysis failed: ${error.message ?: "unknown error"}",
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
                    statusMessage = "Model deleted to free up space.",
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

    private fun buildBackendAttempts(
        context: Context,
        preferredBackends: List<String>,
    ): List<Pair<String, Backend>> {
        val attempts = mutableListOf<Pair<String, Backend>>()
        preferredBackends.forEach { label ->
            when (label) {
                GPU_BACKEND_LABEL -> attempts += GPU_BACKEND_LABEL to Backend.GPU()
                NPU_BACKEND_LABEL -> {
                    runCatching {
                        attempts += NPU_BACKEND_LABEL to Backend.NPU(
                            nativeLibraryDir = context.applicationInfo.nativeLibraryDir,
                        )
                    }
                }
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
                maxNumTokens = 512,
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

    private fun buildAccelerationLabel(
        backendLabel: String,
        enableSpeculativeDecoding: Boolean,
    ): String {
        return if (enableSpeculativeDecoding) {
            "$backendLabel + MTP"
        } else {
            backendLabel
        }
    }

    private fun deleteSupersededModelCopies(context: Context, modelType: GemmaModelType) {
        val baseDir = context.getExternalFilesDir(null) ?: context.filesDir
        val modelRoot = File(baseDir, "models")
        val legacyVersions = LEGACY_MODEL_VERSIONS[modelType].orEmpty()
        legacyVersions
            .filterNot { it == modelType.version }
            .forEach { legacyVersion ->
                val legacyFile = File(modelRoot, "$legacyVersion/${modelType.filename}")
                if (legacyFile.exists()) {
                    legacyFile.delete()
                }
                legacyFile.parentFile?.let { versionDir ->
                    if (versionDir.exists() && versionDir.isDirectory && versionDir.list()?.isEmpty() == true) {
                        versionDir.delete()
                    }
                }
            }
    }

    @OptIn(ExperimentalApi::class)
    private suspend fun runAnalysis(
        context: Context,
        engine: Engine,
        audioBytes: ByteArray,
        modelType: GemmaModelType,
    ): String = withContext(Dispatchers.IO) {
        val conversation = engine.createConversation(
            ConversationConfig(
                samplerConfig = SamplerConfig(
                    topK = 32,
                    topP = 0.9,
                    temperature = 0.2,
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
                        Content.Text(buildAudioAnalysisPrompt(context, modelType)),
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

    private fun buildAudioAnalysisPrompt(context: Context, modelType: GemmaModelType): String {
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
            appendLine("Focus strictly on emotional distress and disruptive noises.")
            appendLine("Think through the evidence in English internally, but do not reveal your reasoning.")
            appendLine("Write the final field values in $outputLanguage.")
            appendLine("Keep the field keys exactly in English as shown below.")
            appendLine("Keep the Danger Level value exactly one of: Danger / High / Medium / Low.")
            appendLine("RULES:")
            appendLine("1. Normal talking, silence, or music = Low danger.")
            appendLine("2. Screaming, crying, calls for help ('help me', 'stop'), or aggressive yelling = High or Danger.")
            appendLine("3. Sudden loud impacts, thuds, or breaking glass = Medium or High danger.")
            if (modelType == GemmaModelType.E2B) {
                appendLine("4. This fast E2B model must not transcribe speech. Keep Speech Content exactly: unavailable")
                appendLine("5. For Situation Assessment, rely only on acoustic environment, disruptive sounds, and vocal stress.")
                appendLine("6. Do not invent, quote, paraphrase, or infer any spoken words or speaker intent.")
            }
            appendLine()
            appendLine("Return the result in this exact structure:")
            appendLine("- Acoustic Environment: <describe background noise>")
            appendLine("- Disruptive Sounds: <e.g., none, thud, breaking glass, siren>")
            appendLine("- Vocal Stress & Emotion: <e.g., normal, panic, aggressive, screaming>")
            if (modelType == GemmaModelType.E2B) {
                appendLine("- Speech Content: unavailable")
                appendLine("- Situation Assessment: <brief threat summary based only on non-transcribed audio evidence>")
            } else {
                appendLine("- Speech Content: <transcribed clear speech ONLY>")
                appendLine("- Situation Assessment: <brief summary of the threat>")
            }
            append("- Danger Level: Danger / High / Medium / Low")
        }
    }

    private val DEFAULT_BACKEND_ORDER = listOf(GPU_BACKEND_LABEL, CPU_BACKEND_LABEL)
    private val LEGACY_MODEL_VERSIONS = mapOf(
        GemmaModelType.E2B to setOf("7fa1d78473894f7e736a21d920c3aa80f950c0db"),
        GemmaModelType.E4B to setOf("main"),
    )
    private const val GPU_BACKEND_LABEL = "GPU"
    private const val NPU_BACKEND_LABEL = "NPU"
    private const val CPU_BACKEND_LABEL = "CPU"
}
