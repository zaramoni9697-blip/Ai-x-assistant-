# Wake phrase: “Hey JARVIS”

## What ships in this project

On-device loop using Android `SpeechRecognizer` inside `WakeWordService`:

- Matches `hey jarvis`, `hi jarvis`, `ok jarvis`, `হে জার্ভিস`, `হেই জার্ভিস`
- Requires a **foreground service**, **microphone permission**, and a **persistent notification**
- Free, no extra SDK, no API key
- Accuracy depends on Google’s recognizer, network/on-device speech pack, and ambient noise
- Not a true always-on DSP wake-word engine (Android may pause recognition)

This is the default because it has **no licensing cost** and no extra secret.

## Production-grade option: Picovoice Porcupine

For always-on, offline, low-power detection of a custom “Hey JARVIS” phrase:

1. Create an account at [Picovoice Console](https://console.picovoice.ai/)
2. Create an AccessKey
3. Train a custom wake-word **Hey JARVIS** (English). Bangla custom words may need Porcupine’s custom wake-word process.
4. Add the Android SDK:

```kotlin
implementation("ai.picovoice:porcupine-android:3.0.2")
```

5. Store the AccessKey in the **backend** or in `local.properties` / CI secrets — **never** hardcode it in Kotlin that is committed.
6. Replace the SpeechRecognizer loop in `WakeWordService` with Porcupine’s `PorcupineManager`. Keep the same foreground notification.

Licensing: Picovoice is free for selected indie/hobby quotas; commercial apps need a paid plan. Read their current terms before shipping.

## Other engines

- **openWakeWord / microWakeWord** — custom models, more integration work, no official Android SDK
- **Snowboy** — unmaintained
- **Google Assistant hotword** — not available to third-party apps

Recommendation: ship SpeechRecognizer for v1; add Porcupine if you need true always-on detection.
