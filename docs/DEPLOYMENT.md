# Deploy JARVIS using only an Android phone

You do not need a computer. A browser, GitHub, and a free cloud host are enough.

## A. Deploy the backend (required)

1. Open GitHub in Chrome / Firefox and sign in (create an account if needed).
2. Fork this repository.
3. Open [Render](https://render.com/) or [Railway](https://railway.app/) in the same browser and sign in with GitHub.
4. Create a **Web Service** from your fork.
   - Root directory: `backend`
   - Build: `npm install`
   - Start: `npm start`
   - Instance: the free / starter plan is enough to try the app
5. Set environment variables (secret dashboard, not in source):

```
NODE_ENV=production
JWT_SECRET=   (long random string)
GEMINI_API_KEY=  (from https://aistudio.google.com/app/apikey)
ADMIN_EMAIL=you@example.com
ADMIN_PASSWORD=  (choose a strong password)
PORT=8080
```

6. Deploy. Copy the public URL, e.g. `https://jarvis-xxxx.onrender.com/`
7. Visit that URL — you should see the JARVIS Admin login.
8. Sign in with `ADMIN_EMAIL` / `ADMIN_PASSWORD`.

Free hosts sleep when idle; the first request after sleep can take ~30s.

## B. Build the APK from GitHub (no Android Studio)

1. In your fork: **Actions → Build JARVIS APK → Run workflow**.
2. When it finishes, open the run → **Artifacts → jarvis-ai-apk**.
3. Download the zip on the phone and unpack the `.apk`.

If Actions is disabled: Repo **Settings → Actions → Allow**.

## C. Install on the phone

1. Settings → Security → allow install from the browser / Files app.
2. Open the APK → Install.
3. Launch **JARVIS AI**.
4. On the login screen, set **Backend URL** to the URL from step A.
5. Create a normal user account (Sign up). Do not use the admin password for daily chat if you can avoid it.
6. Grant microphone, camera and notification permissions when asked.

## D. Language

There is **no** language selection gate. Default is **Auto Detect**:

- Speak or type Bangla → JARVIS replies in Bangla
- Speak or type English → JARVIS replies in English
- Mixed sentences are understood as a whole; the reply follows the dominant language
- Optional override in Settings: Auto Detect / বাংলা / English (does not restart the chat)

## E. Gemini key

If chat replies say the key is missing, edit the host’s environment variables and redeploy. Never paste the key into the Android app or a GitHub file.

## F. Optional: Termux backend on the same phone

Advanced. Install Termux, `pkg install nodejs git`, clone the repo, `cd backend && npm install && npm start`. Use `http://127.0.0.1:8080/` as Backend URL. The phone must keep Termux in the foreground / wake-unlocked. A cloud host is more reliable.
