import Foundation

protocol AiAnalysisService {
    func refreshState(modelType: GemmaModelType) async -> AiFeatureUiState
    func analyze(clip: CrisisAudioClip, modelType: GemmaModelType) async throws -> AiAnalysisResult
}

struct PlaceholderAiAnalysisService: AiAnalysisService {
    func refreshState(modelType: GemmaModelType) async -> AiFeatureUiState {
        AiFeatureUiState(
            modelStatus: .error,
            statusMessage: "On-device Gemma analysis is abstracted for iOS. Add an Apple-compatible LiteRT/MediaPipe adapter here.",
            accelerator: "iOS adapter pending",
            currentModelType: modelType,
            lastAnalysisText: ""
        )
    }

    func analyze(clip: CrisisAudioClip, modelType: GemmaModelType) async throws -> AiAnalysisResult {
        let text = """
        Acoustic Environment: iOS local audio clip captured.
        Disruptive Sounds: Analysis runtime is not connected yet.
        Vocal Stress & Emotion: Unavailable.
        Speech Content: Unavailable.
        Situation Assessment: AI adapter unavailable on this iOS build; emergency policy should favor safety.
        Danger Level: Danger
        """
        return AiAnalysisResult(analysisText: text, dangerLevel: "Danger")
    }
}
