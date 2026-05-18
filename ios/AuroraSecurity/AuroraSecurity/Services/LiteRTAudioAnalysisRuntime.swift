import Foundation

protocol LiteRTAudioAnalysisRuntime: AnyObject {
    var loadedModel: GemmaLiteRTModel? { get }
    var acceleratorLabel: String { get }

    func load(model: GemmaLiteRTModel, fileURL: URL) async throws
    func analyzeAudio(wavData: Data, prompt: String) async throws -> String
    func unload()
}

enum LiteRTRuntimeError: LocalizedError {
    case swiftRuntimeUnavailable
    case modelFileMissing

    var errorDescription: String? {
        switch self {
        case .swiftRuntimeUnavailable:
            "The public LiteRT-LM Swift runtime is not linked yet. The model can be downloaded now; inference will activate after the native LiteRT-LM iOS adapter is added."
        case .modelFileMissing:
            "The selected .litertlm model file is missing."
        }
    }
}

final class PlaceholderLiteRTRuntime: LiteRTAudioAnalysisRuntime {
    private(set) var loadedModel: GemmaLiteRTModel?
    var acceleratorLabel: String { "LiteRT-LM Swift adapter pending" }

    func load(model: GemmaLiteRTModel, fileURL: URL) async throws {
        guard FileManager.default.fileExists(atPath: fileURL.path) else {
            throw LiteRTRuntimeError.modelFileMissing
        }
        loadedModel = nil
        throw LiteRTRuntimeError.swiftRuntimeUnavailable
    }

    func analyzeAudio(wavData: Data, prompt: String) async throws -> String {
        throw LiteRTRuntimeError.swiftRuntimeUnavailable
    }

    func unload() {
        loadedModel = nil
    }
}
