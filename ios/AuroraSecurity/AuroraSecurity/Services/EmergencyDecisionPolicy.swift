import Foundation

enum EmergencyDecisionPolicy {
    static func shouldBypassAiVerification(_ triggerSource: EmergencyTriggerSource?) -> Bool {
        triggerSource == .silentSosButton || triggerSource == .loudSosButton
    }

    static func shouldDispatchImmediately(triggerSource: EmergencyTriggerSource?, aiSettings: AiSettings) -> Bool {
        shouldBypassAiVerification(triggerSource) ||
            aiSettings.sosDispatchMode == .immediate ||
            !aiSettings.isAiEnabled
    }

    static func shouldDispatchAfterAnalysis(
        aiSettings: AiSettings,
        triggerSource: EmergencyTriggerSource?,
        analysisText: String?
    ) -> Bool {
        if shouldBypassAiVerification(triggerSource) { return true }
        if aiSettings.sosDispatchMode == .immediate || !aiSettings.isAiEnabled { return true }
        let normalized = parseDangerLevel(from: analysisText)?.lowercased().trimmingCharacters(in: .whitespacesAndNewlines)
        return normalized == nil || normalized?.hasPrefix("danger") == true || normalized?.hasPrefix("high") == true
    }

    static func parseDangerLevel(from analysisText: String?) -> String? {
        guard let analysisText, !analysisText.isEmpty else { return nil }
        return analysisText
            .split(separator: "\n")
            .map { $0.trimmingCharacters(in: .whitespacesAndNewlines).replacingOccurrences(of: "^-\\s*", with: "", options: .regularExpression) }
            .first { $0.lowercased().hasPrefix("danger level:") }?
            .components(separatedBy: ":")
            .dropFirst()
            .joined(separator: ":")
            .trimmingCharacters(in: .whitespacesAndNewlines)
    }
}
