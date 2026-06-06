import XCTest
@testable import AuroraSecurity

final class EmergencyDecisionPolicyTests: XCTestCase {
    func testManualSosAlwaysBypassesAiVerification() {
        XCTAssertTrue(EmergencyDecisionPolicy.shouldBypassAiVerification(.silentSosButton))
        XCTAssertTrue(EmergencyDecisionPolicy.shouldBypassAiVerification(.loudSosButton))
        XCTAssertFalse(EmergencyDecisionPolicy.shouldBypassAiVerification(.soundDetection))
    }

    func testImmediateModeDispatchesWithoutWaitingForAi() {
        let settings = AiSettings(isAiEnabled: true, sendAudioToContacts: true, preloadAiInBackground: false, sosDispatchMode: .immediate)

        XCTAssertTrue(EmergencyDecisionPolicy.shouldDispatchImmediately(triggerSource: .soundDetection, aiSettings: settings))
    }

    func testAiFirstDispatchesOnlyHighRiskOrMissingAnalysis() {
        let settings = AiSettings(isAiEnabled: true, sendAudioToContacts: true, preloadAiInBackground: false, sosDispatchMode: .aiFirst)

        XCTAssertTrue(EmergencyDecisionPolicy.shouldDispatchAfterAnalysis(aiSettings: settings, triggerSource: .soundDetection, analysisText: "Danger Level: Danger"))
        XCTAssertTrue(EmergencyDecisionPolicy.shouldDispatchAfterAnalysis(aiSettings: settings, triggerSource: .soundDetection, analysisText: "- Danger Level: High"))
        XCTAssertFalse(EmergencyDecisionPolicy.shouldDispatchAfterAnalysis(aiSettings: settings, triggerSource: .soundDetection, analysisText: "Danger Level: Medium"))
        XCTAssertFalse(EmergencyDecisionPolicy.shouldDispatchAfterAnalysis(aiSettings: settings, triggerSource: .soundDetection, analysisText: "Danger Level: Low"))
        XCTAssertTrue(EmergencyDecisionPolicy.shouldDispatchAfterAnalysis(aiSettings: settings, triggerSource: .soundDetection, analysisText: nil))
    }

    func testDangerLevelParserTrimsBulletsAndWhitespace() {
        let analysis = """
        Acoustic Environment: Street
        - Danger Level: High
        """

        XCTAssertEqual(EmergencyDecisionPolicy.parseDangerLevel(from: analysis), "High")
    }
}
