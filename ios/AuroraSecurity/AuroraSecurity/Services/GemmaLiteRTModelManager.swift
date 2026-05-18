import Combine
import Foundation

@MainActor
final class GemmaLiteRTModelManager: ObservableObject {
    @Published private(set) var state = AiFeatureState()

    private var downloadTask: URLSessionDownloadTask?
    private var progressObservation: NSKeyValueObservation?
    private let runtime: LiteRTAudioAnalysisRuntime

    init(runtime: LiteRTAudioAnalysisRuntime = PlaceholderLiteRTRuntime()) {
        self.runtime = runtime
    }

    func refresh(selectedModel: GemmaLiteRTModel) {
        let fileURL = modelFileURL(for: selectedModel)
        let status: AiModelStatus
        let message: String

        if runtime.loadedModel == selectedModel {
            status = .ready
            message = "Model ready for on-device LiteRT-LM audio analysis."
        } else if FileManager.default.fileExists(atPath: fileURL.path) {
            status = .downloaded
            message = "Model downloaded. Initialize LiteRT-LM before enabling verified SOS."
        } else {
            status = .notDownloaded
            message = "Download \(selectedModel.modelName) to enable local audio analysis."
        }

        state.currentModel = selectedModel
        state.modelStatus = status
        state.downloadProgress = status == .notDownloaded ? 0 : 1
        state.statusMessage = message
        state.accelerator = runtime.acceleratorLabel
    }

    func downloadSelectedModel() {
        let model = state.currentModel
        guard let downloadURL = model.downloadURL else {
            state.modelStatus = .error
            state.statusMessage = "Model download URL is invalid."
            return
        }
        let destination = modelFileURL(for: model)
        if FileManager.default.fileExists(atPath: destination.path) {
            refresh(selectedModel: model)
            return
        }

        state.modelStatus = .downloading
        state.downloadProgress = 0
        state.statusMessage = "Downloading \(model.modelName) from Hugging Face..."

        let task = URLSession.shared.downloadTask(with: downloadURL) { [weak self] temporaryURL, _, error in
            Task { @MainActor in
                guard let self else { return }
                self.progressObservation = nil
                self.downloadTask = nil

                if let error {
                    self.state.modelStatus = .error
                    self.state.statusMessage = "Model download failed: \(error.localizedDescription)"
                    return
                }

                guard let temporaryURL else {
                    self.state.modelStatus = .error
                    self.state.statusMessage = "Model download failed: no file was returned."
                    return
                }

                do {
                    try FileManager.default.createDirectory(
                        at: destination.deletingLastPathComponent(),
                        withIntermediateDirectories: true,
                        attributes: nil
                    )
                    if FileManager.default.fileExists(atPath: destination.path) {
                        try FileManager.default.removeItem(at: destination)
                    }
                    try FileManager.default.moveItem(at: temporaryURL, to: destination)
                    try self.validateModelFile(destination, model: model)
                    self.deleteOtherModelCopies(keeping: model)
                    self.refresh(selectedModel: model)
                } catch {
                    try? FileManager.default.removeItem(at: destination)
                    self.state.modelStatus = .error
                    self.state.statusMessage = "Downloaded model is invalid: \(error.localizedDescription)"
                }
            }
        }

        progressObservation = task.progress.observe(\.fractionCompleted, options: [.new]) { [weak self] progress, _ in
            Task { @MainActor in
                self?.state.downloadProgress = progress.fractionCompleted
            }
        }
        downloadTask = task
        task.resume()
    }

    func initializeSelectedModel() async {
        let model = state.currentModel
        let fileURL = modelFileURL(for: model)
        guard FileManager.default.fileExists(atPath: fileURL.path) else {
            state.modelStatus = .notDownloaded
            state.statusMessage = "Download \(model.modelName) before initializing LiteRT-LM."
            return
        }

        state.modelStatus = .initializing
        state.statusMessage = "Initializing \(model.modelName) with LiteRT-LM..."
        do {
            try await runtime.load(model: model, fileURL: fileURL)
            state.modelStatus = .ready
            state.accelerator = runtime.acceleratorLabel
            state.statusMessage = "Model ready. Running on \(runtime.acceleratorLabel)."
        } catch {
            state.modelStatus = .error
            state.accelerator = runtime.acceleratorLabel
            state.statusMessage = "LiteRT-LM initialization failed: \(error.localizedDescription)"
        }
    }

    func analyze(clip: CrisisAudioClip) async -> String? {
        let model = state.currentModel
        let fileURL = modelFileURL(for: model)

        state.isAnalyzing = true
        state.statusMessage = "Analyzing emergency audio with \(model.modelName)..."
        defer { state.isAnalyzing = false }

        do {
            if runtime.loadedModel != model {
                try await runtime.load(model: model, fileURL: fileURL)
            }
            let result = try await runtime.analyzeAudio(wavData: clip.wavData, prompt: Self.analysisPrompt)
            state.modelStatus = .ready
            state.lastAnalysisResult = result
            state.statusMessage = "Analysis complete."
            return result
        } catch {
            state.modelStatus = .error
            state.statusMessage = "Audio analysis failed: \(error.localizedDescription)"
            return nil
        }
    }

    func deleteSelectedModel() {
        let model = state.currentModel
        runtime.unload()
        let fileURL = modelFileURL(for: model)
        try? FileManager.default.removeItem(at: fileURL)
        refresh(selectedModel: model)
    }

    func selectModel(_ model: GemmaLiteRTModel) {
        refresh(selectedModel: model)
    }

    private func validateModelFile(_ fileURL: URL, model: GemmaLiteRTModel) throws {
        let values = try fileURL.resourceValues(forKeys: [.fileSizeKey])
        let size = Int64(values.fileSize ?? 0)
        if size < model.minimumValidBytes {
            throw GemmaModelDownloadError.fileTooSmall(
                expectedSize: model.displaySize,
                actualBytes: size
            )
        }
    }

    private func deleteOtherModelCopies(keeping selectedModel: GemmaLiteRTModel) {
        for model in GemmaLiteRTModel.allCases where model != selectedModel {
            try? FileManager.default.removeItem(at: modelFileURL(for: model))
        }
    }

    private func modelFileURL(for model: GemmaLiteRTModel) -> URL {
        Self.modelDirectory.appendingPathComponent(model.filename)
    }

    private static var modelDirectory: URL {
        let applicationSupport = FileManager.default.urls(for: .applicationSupportDirectory, in: .userDomainMask).first
            ?? FileManager.default.temporaryDirectory
        return applicationSupport.appendingPathComponent("GemmaLiteRT", isDirectory: true)
    }

    private static let analysisPrompt = """
    You are Aurora Security's on-device emergency audio reviewer.
    Analyze the attached 5-second WAV clip. Return exactly:
    Danger Level: Low, Medium, High, or Danger
    Evidence: short observations from the audio
    Action: whether Aurora should send SOS immediately
    """
}

private enum GemmaModelDownloadError: LocalizedError {
    case fileTooSmall(expectedSize: String, actualBytes: Int64)

    var errorDescription: String? {
        switch self {
        case .fileTooSmall(let expectedSize, let actualBytes):
            "The downloaded file is too small (\(actualBytes) bytes). Expected about \(expectedSize)."
        }
    }
}
