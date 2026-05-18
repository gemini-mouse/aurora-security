# Aurora Security iOS MVP

This folder contains a native SwiftUI iOS MVP port of the Android app. It intentionally does not use Flutter.

## Scope

- SwiftUI tab shell: Detect, AI, History, Settings.
- Morandi-inspired color system.
- Microphone decibel monitoring with `AVAudioEngine`.
- Five-second WAV emergency audio snapshot.
- Location snapshot with `CoreLocation`.
- Local notification support.
- Telegram / Push backend dispatch using the existing backend paths.
- iOS phone and SMS fallbacks using system-confirmed `tel:` and `sms:` URLs.
- Gemma 4 LiteRT-LM model management for:
  - `litert-community/gemma-4-E2B-it-litert-lm`
  - `litert-community/gemma-4-E4B-it-litert-lm`

## iOS Differences

iOS does not allow third-party apps to send SMS or place calls automatically in the same way as Android `SEND_SMS` and `CALL_PHONE`. This MVP opens the system phone/message flow as a fallback and keeps backend delivery as the automatic path.

LiteRT-LM model downloading, validation, deletion, and alert-flow integration are implemented. Public Swift LiteRT-LM inference APIs are still emerging, so runtime inference is isolated behind `LiteRTAudioAnalysisRuntime`. Replace `PlaceholderLiteRTRuntime` with the official Swift/C++ LiteRT-LM adapter when linking the runtime.

Open `AuroraSecurity.xcodeproj` on macOS with Xcode 16 or newer.
