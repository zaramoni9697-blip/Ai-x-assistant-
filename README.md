# JARVIS AI — Advanced Personal Android Assistant

Native Android assistant (Kotlin, Jetpack Compose, MVVM) with a secure Node.js backend, automatic **Bangla / English / mixed** language detection, voice I/O, device actions, coding help, private memory, and a server-side admin dashboard.

This is not a web chatbot. The product is the Android app in `android/` plus the API in `backend/`.

## Architecture

```
Android (com.jarvis.ai)
  UI (Compose)  →  Repositories  →  Retrofit  →  JARVIS backend
                     ↓
                  Room (per-device cache of THAT user's data)
                     ↓
            Encrypted session (tokens, GitHub token, API URL)

Backend (Express)
  JWT auth (bcrypt passwords)   Gemini proxy (key never leaves the server)
  Per-user conversations/memory  Admin role stored in SQLite (sql.js)
  Static admin HUD at GET /
```

Packages in the app:

| Package | Responsibility |
|---|---|
| `feature.*` | Screens (auth, dashboard, chat, voice, coding, memory, device, profile, settings, admin) |
| `data.repository` | Auth / chat / memory repositories |
| `data.local` | Room database |
| `data.remote` | Retrofit DTOs + API |
| `core.language` | On-device Bangla–English detector |
| `core.voice` | SpeechRecognizer, TTS, wake-word foreground service |
| `core.device` | Flashlight, volume, battery, settings, alarms, app launch |
| `core.security` | EncryptedSharedPreferences session |
| `di.Graph` | Composition root |

## Language (no mandatory picker)

Default setting is **Auto Detect**.

- Bangla speech or typing → natural Bangla replies (বাংলা লিপি)
- English → natural English
- Mixed / Banglish → full sentence understood; reply in the dominant language
- Optional Settings override: Auto Detect / বাংলা / English
- Switching language does **not** restart the conversation
- Preference is stored on the account and sent with later turns

## Quick start

### Backend

```bash
cd backend
cp .env.example .env
# set JWT_SECRET, GEMINI_API_KEY, ADMIN_PASSWORD
npm install
npm start
```

Admin HUD: `http://localhost:8080/`  
Health: `GET /api/health`

The first boot creates a server-side **admin** user. A normal user cannot become admin from the Android client.

### Android

1. Open the `android/` folder in Android Studio (Koala / Ladybug or newer, JDK 17).
2. Let Gradle sync.
3. Run on a device or emulator.
4. On Login, set **Backend URL**:
   - emulator: `http://10.0.2.2:8080/`
   - phone on LAN: `http://YOUR_PC_IP:8080/`
   - production: `https://your-service/`

Or build an APK with the GitHub Action `.github/workflows/android-apk.yml`.

## Features

- Email/password signup, login, forgot/reset password, profile photo, display name, logout, persistent JWT session
- Private chat history and memories per account (Room + server)
- Gemini via backend: chat, reasoning, coding, image understanding (camera)
- Voice: STT + TTS, Bangla and English, listening / thinking / speaking states, interrupt
- Wake phrase **Hey JARVIS** (opt-in foreground service + notification). See `docs/WAKE_WORD.md`
- Device assistant: open apps, flashlight, volume, battery, device info, settings, system alarm/reminder — with confirmation
- Coding assistant: generate / explain / fix / review; GitHub repo analysis with a user-supplied token (not stored on the server)
- Admin dashboard (web + in-app): user counts, suspension, usage counters, health, config. **No** private transcripts, passwords, keys or memories

## Documentation

- [API setup](docs/API_SETUP.md)
- [Android permissions](docs/ANDROID_PERMISSIONS.md)
- [Wake word](docs/WAKE_WORD.md)
- [Phone-only deployment](docs/DEPLOYMENT.md)

## Security rules enforced in this repo

- No Gemini key in Kotlin, XML, or Gradle
- Passwords bcrypt-hashed; never logged
- Admin role assigned only in the backend database
- Admin APIs return aggregates, not chat bodies
- Wake-word mic cannot run without a visible notification
- GitHub token lives in EncryptedSharedPreferences and is sent only to `/api/coding/github`

## Project layout

```
android/                 Native app
backend/src/            Express API
backend/public/         Admin HUD
.github/workflows/       APK build
docs/                    Guides
```

## License

MIT — see [LICENSE](LICENSE).
