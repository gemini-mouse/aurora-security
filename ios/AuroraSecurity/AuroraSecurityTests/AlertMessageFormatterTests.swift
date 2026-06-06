import XCTest
@testable import AuroraSecurity

final class AlertMessageFormatterTests: XCTestCase {
    func testFormatterReplacesTemplateFieldsAndLocation() {
        let settings = AlertContactSettings(
            apiUrl: "",
            apiToken: "",
            botName: "",
            lineBotBasicId: "",
            deviceName: "iPhone",
            userName: "Alice",
            mobileNumber: "1234567890",
            alertMessageTemplate: "User=<User Name>\nDevice=<Device Name>\nDB=<Current dB>\nLocation=<Location>",
            useLine: false,
            useTelegram: false,
            usePush: false,
            useSms: false,
            usePhoneCall: false,
            emergencyPhoneNumber: "",
            secondaryPhoneNumber: "",
            phoneCallPhoneNumber: ""
        )
        let location = AlertLocation(latitude: 25.03, longitude: 121.56)

        let payload = AlertMessageFormatter.formatPayload(settings: settings, currentDb: 91.6, location: location)

        XCTAssertTrue(payload.text.contains("User=Alice"))
        XCTAssertTrue(payload.text.contains("Device=iPhone"))
        XCTAssertTrue(payload.text.contains("DB=92"))
        XCTAssertTrue(payload.text.contains("https://maps.google.com/?q=25.03,121.56"))
        XCTAssertEqual(payload.details.currentSoundLevel, "92 dB")
    }
}
