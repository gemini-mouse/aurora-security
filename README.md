# Aurora Security

Aurora Security is a local-first mobile safety assistant built for the Kaggle Gemma 4 Good Hackathon. It monitors short emergency audio events on-device, uses Gemma 4 through Google AI Edge LiteRT-LM to assess personal-safety risk, and alerts trusted contacts when the situation should escalate.

The project is designed for moments when normal phone interaction may be difficult: a child is scared, an older adult falls, someone is being followed, or a user cannot safely unlock the phone and type a message.

## Kaggle Submission Resources

| Resource | Link | Purpose |
|---|---|---|
| Product video | [YouTube demo](https://youtu.be/PR8oty_1xbw) | Main story and product walkthrough |
| Live demo / website | [aurorasecurity.app](https://aurorasecurity.app) | Public product overview, screenshots, and privacy messaging |
| Android app download | [Google Play](https://play.google.com/store/apps/details?id=app.aurorasecurity.security&pcampaignid=web_share) | Official published Android app for the live mobile experience |
| Code repository | [github.com/gemini-mouse/aurora-security](https://github.com/gemini-mouse/aurora-security) | Source of truth for the app, backend, and website |
| Kaggle notebook | Submitted with the Kaggle writeup | Reproducible Gemma 4 audio-risk inference demo |

## Product and Technical Focus

Aurora Security targets the **Safety & Trust** and **LiteRT** themes of the Gemma 4 Good Hackathon.

- **Impact & Vision:** hands-free emergency support for people who may not be able to call or text during a crisis.
- **Technical Depth:** on-device Gemma 4 LiteRT-LM audio analysis, local signal gating, crisis-clip capture, deterministic SOS decision logic, and multi-channel trusted-contact delivery.
- **Real-world Utility:** a working Android implementation, a Cloudflare backend, and a public product website.

## What Aurora Does

- Monitors microphone audio locally while protection mode is enabled.
- Detects loud or suspicious trigger candidates with configurable sensitivity.
- Captures a bounded 5-second crisis clip: 2 seconds before the trigger and 3 seconds after it.
- Runs Gemma 4 locally through LiteRT-LM to classify acoustic safety risk.
- Parses a stable `Danger Level` field from the model response.
- Sends SOS alerts to trusted contacts through Push, Telegram, SMS, and phone-call fallback based on user settings.
- Stores local history with alert status, audio evidence, and AI summaries.
- Supports Silent SOS and Loud SOS manual flows that bypass AI verification.

## How Gemma 4 Is Used

Gemma 4 is used as a local verifier, not as an uncontrolled emergency dispatcher.

The mobile app sends the crisis clip to Gemma 4 with a structured prompt that asks for:

- `Acoustic Environment`
- `Disruptive Sounds`
- `Vocal Stress & Emotion`
- `Speech Content`
- `Situation Assessment`
- `Danger Level: Danger / High / Medium / Low`

Only `Danger` and `High` danger levels trigger automatic escalation in verified mode. The rest of the response is kept as human-readable context for history records and trusted-contact summaries.

Decision policy:

| Trigger path | Decision |
|---|---|
| Manual Silent SOS / Loud SOS | Always send SOS |
| Instant mode | Send SOS after countdown |
| Verified mode + `Danger` or `High` | Send SOS |
| Verified mode + `Medium` or `Low` | Save history, suppress automatic escalation |
| AI unavailable or missing result | Keep user-initiated SOS available and avoid blocking emergency action |

This keeps the AI layer auditable: Gemma 4 interprets the audio, while the app owns the safety policy.

## Runtime Pipeline

1. `NoiseMonitor` captures mono 16 kHz PCM frames and estimates live sound level.
2. A fast signal gate starts an emergency countdown when configured trigger conditions are met.
3. `CrisisAudioRecorder` preserves pre-trigger and post-trigger context as a short WAV clip.
4. `GemmaAudioAnalysisManager` loads Gemma 4 LiteRT-LM and runs local audio inference.
5. `NoiseAlarmService` parses `Danger Level` and decides whether to dispatch.
6. `NoiseAlarmAlertDispatcher` sends enabled contact alerts and optional evidence.
7. `HistoryManager` records the event, AI result, and delivery status.

## Repository Map

| Path | Description |
|---|---|
| `app/` | Android Kotlin + Jetpack Compose app |
| `app/src/main/java/app/aurorasecurity/security/GemmaAudioAnalysisManager.kt` | Gemma 4 model download, initialization, prompt construction, and audio inference |
| `app/src/main/java/app/aurorasecurity/security/NoiseAlarmService.kt` | Foreground monitoring service and SOS decision flow |
| `app/src/main/java/app/aurorasecurity/security/CrisisAudioRecorder.kt` | Five-second crisis audio capture |
| `app/src/main/java/app/aurorasecurity/security/NoiseMonitor.kt` | Microphone PCM capture and dB estimation |
| `cf-backend/` | Cloudflare Worker backend for trusted-contact binding and alert delivery |
| `website/` | Public product website |
| `icons/` | App icon assets |

## Android Setup

Prerequisites:

- Android Studio
- JDK 17
- Android SDK 36
- A physical Android device is recommended for microphone, SMS, phone, and on-device AI testing

Open and run:

```powershell
git clone https://github.com/gemini-mouse/aurora-security.git
cd aurora-security
.\gradlew.bat :app:assembleDebug
```

Then open the project in Android Studio and run the `app` configuration on a device.

## Local Configuration

Create `local.properties` for backend and messaging settings:

```properties
telegram.alert.apiUrl=https://your-worker.example.workers.dev
telegram.alert.apiToken=your-shared-secret
telegram.bot.name=your_telegram_bot
telegram.deviceName=Phone

fcm.projectId=your-firebase-project-id
fcm.applicationId=your-firebase-application-id
fcm.apiKey=your-firebase-api-key
fcm.gcmSenderId=your-firebase-sender-id
```

`local.properties` is intentionally not committed.

## Backend Setup

The backend is a Cloudflare Worker using Hono and D1.

```powershell
cd cf-backend
npm install
npm run db:init
npm run dev
```

Main backend responsibilities:

- Register app users and trusted contacts.
- Bind Telegram contacts through setup codes.
- Register Push recipients.
- Dispatch SOS messages and optional AI summaries.
- Store pending delivery work when the network is unavailable.

## Privacy and Safety Model

Aurora Security is built around bounded data movement:

- Routine microphone analysis stays on the device.
- Gemma 4 audio analysis runs locally.
- Crisis audio is short and event-bounded.
- Audio evidence and AI summaries are shared only when enabled by the user.
- Optional training audio upload is opt-in.
- Manual SOS always bypasses model verification.
- If AI fails during an emergency path, the app favors safety instead of silently dropping the alert.

## Important Safety Notice

Aurora Security is a personal safety assistance prototype. It is not an official emergency dispatch system, police reporting system, medical service, or guaranteed rescue solution.

Alerts, audio analysis, location sharing, phone calls, SMS, Push, Telegram delivery, and backend services can fail or be delayed because of permissions, device limits, network issues, operating-system restrictions, third-party service outages, or model error. In immediate danger, contact local emergency services as soon as possible.
