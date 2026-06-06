# Aurora Security iOS Port

This folder contains a SwiftUI iPhone port of the Android Aurora Security app.

## Project

- Xcode project: `AuroraSecurity.xcodeproj`
- App target: `AuroraSecurity`
- Unit test target: `AuroraSecurityTests`
- Minimum iOS: 17.0
- Bundle ID: `app.aurorasecurity.ios`

## Architecture Map

The iOS structure mirrors the Android app while using native iOS APIs.

| Android | iOS |
| --- | --- |
| `AlarmDomainModels.kt` | `AuroraSecurity/Models/AuroraModels.swift` |
| `AlarmPreferences.kt` | `Services/PreferencesStore.swift` |
| `NoiseAlarmController.kt` | `App/AuroraAppStore.swift` |
| `NoiseAlarmService.kt` | `AuroraAppStore` + `NoiseMonitor` + `CrisisAudioRecorder` |
| `NoiseMonitor.kt` | `Services/NoiseMonitor.swift` |
| `CrisisAudioRecorder.kt` | `Services/CrisisAudioRecorder.swift` |
| `LocationSnapshotProvider.kt` | `Services/LocationSnapshotProvider.swift` |
| `AlertMessageFormatter.kt` | `Services/AlertMessageFormatter.swift` |
| `HistoryManager.kt` | `Services/HistoryStore.swift` |
| `TelegramBackendApi.kt`, `LineBackendApi.kt`, `PushBackendApi.kt` | `Services/BackendClient.swift` |
| `GemmaAudioAnalysisManager.kt` | `Services/AiAnalysisService.swift` placeholder adapter |
| Compose `DetectTab`, `AiTab`, `SettingsTab`, `HistoryTab` | SwiftUI `DetectView`, `AiView`, `SettingsView`, `HistoryView` |

## Implemented Behavior

- Detect / AI / Settings bottom segmented tabs, with History as the header entry.
- Microphone dB monitoring through `AVAudioEngine`.
- Motion confirmation through `CoreMotion`.
- Manual Silent SOS and Loud SOS.
- Three-second emergency countdown.
- Crisis audio capture with two-second pre-roll plus post-trigger samples, saved as WAV.
- Loud SOS local M4A alarm and vibration loop.
- Location snapshot and Google Maps link formatting.
- Android-equivalent alert message template replacement.
- Android-equivalent emergency decision policy:
  - Manual Silent/Loud SOS always dispatches.
  - Instant mode dispatches after countdown.
  - Verified mode dispatches on `Danger` / `High`.
  - Missing or failed AI result favors safety and dispatches.
- Local history records with alert details, audio path, AI text, and delivery statuses.
- Cloudflare backend JSON routes matching the Android path names.
- Unit tests for emergency policy and alert-message formatting.

## Build And Test

Run on macOS with Xcode installed:

```sh
cd ios/AuroraSecurity
xcodebuild \
  -project AuroraSecurity.xcodeproj \
  -scheme AuroraSecurity \
  -destination 'platform=iOS Simulator,name=iPhone 16' \
  test
```

If the simulator name differs, list devices:

```sh
xcrun simctl list devices available
```

## iPhone Alignment Gaps

- iOS cannot silently send SMS. The app can only open an SMS compose flow that the user confirms.
- iOS cannot silently auto-place phone calls. It can open `tel:` and the user/system must proceed.
- Android foreground services do not exist on iOS. The port requests audio background mode, but App Store review and device runtime policy may restrict continuous microphone monitoring.
- Android FCM push registration is not identical to APNs. The iOS port collects APNs tokens and sends them through the existing `/push/register-device` payload field; the backend must support APNs delivery or bridge tokens through Firebase for iOS.
- The Android Gemma LiteRT-LM runtime is not directly available as-is. `AiAnalysisService` is an adapter seam with a safety-favoring placeholder result until an Apple-compatible LiteRT/MediaPipe/Core ML path is added.
- Critical alerts and high-priority interruption levels require Apple entitlements and review; the current port uses standard notifications.
- Associated Domains require `apple-app-site-association` hosting for `aurorasecurity.app` before universal links work outside debug.
