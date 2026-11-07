# Android permissions

Declared in `android/app/src/main/AndroidManifest.xml`. Runtime prompts are shown in `MainActivity` and Settings.

| Permission | Why | Runtime |
|---|---|---|
| `INTERNET` | Talk to the JARVIS backend | Install |
| `ACCESS_NETWORK_STATE` | Connection status on the dashboard | Install |
| `RECORD_AUDIO` | Speech-to-text and optional wake phrase | Yes |
| `CAMERA` | Visual understanding / flashlight camera id | Yes |
| `FLASHLIGHT` | Torch toggle | Install |
| `POST_NOTIFICATIONS` | Assistant, service, reminders (Android 13+) | Yes |
| `FOREGROUND_SERVICE` + `FOREGROUND_SERVICE_MICROPHONE` | Visible wake-word service | Install |
| `VIBRATE` / `WAKE_LOCK` | Reminder delivery | Install |
| `RECEIVE_BOOT_COMPLETED` | Restart wake service if the user enabled it | Install |
| `SCHEDULE_EXACT_ALARM` / `USE_EXACT_ALARM` | User-approved reminders | Settings grant |
| `MODIFY_AUDIO_SETTINGS` | Volume / media keys | Install |

The app does **not** request `QUERY_ALL_PACKAGES`. Installed apps are listed through the standard launcher intent query.

## Wake-word / background microphone

Background listening is **opt-in** in Settings → Hey JARVIS.

When enabled:

1. User grants microphone (and notification) permission.
2. `WakeWordService` starts as a **foreground service**.
3. A persistent notification stays visible for as long as the mic is in use.
4. Stopping the notification action or toggling Settings off stops listening.

There is no hidden recording.

## Device actions

Flashlight, volume, battery, settings, alarms and app launch run on-device. Sensitive actions show a confirmation dialog first. Alarms use the system Clock app (`AlarmClock.ACTION_SET_ALARM`) because Android does not allow silent unrestricted alarm injection on all devices.
