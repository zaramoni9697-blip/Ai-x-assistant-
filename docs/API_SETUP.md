# JARVIS AI — API & backend setup

The Android app **never** contains a Gemini API key. All model calls go through `backend/`.

## 1. Environment

```bash
cd backend
cp .env.example .env
```

Required:

| Variable | Purpose |
|---|---|
| `JWT_SECRET` | Long random string (≥24 chars) used to sign access tokens |
| `GEMINI_API_KEY` | Google AI Studio / Gemini key. Server-side only |
| `ADMIN_EMAIL` | First admin account (created on first boot) |
| `ADMIN_PASSWORD` | Admin password. If omitted, a random one is printed **once** in server logs |

Optional:

| Variable | Purpose |
|---|---|
| `GEMINI_MODEL` | Default `gemini-2.0-flash` |
| `SMTP_*` | Email delivery for password-reset codes |
| `PORT` | Default `8080` |
| `CORS_ORIGINS` | Comma-separated allowed web origins |
| `DB_PATH` | SQLite file (sql.js, no native compiler needed) |

Get a Gemini key: [Google AI Studio](https://aistudio.google.com/app/apikey).

## 2. Run locally

```bash
cd backend
npm install
npm start
```

- API health: `GET /api/health`
- Admin dashboard: `http://localhost:8080/`
- Uploads: `/uploads`

On first boot the server creates an **admin** user. Role `admin` cannot be granted from the Android client.

## 3. Endpoints (summary)

Auth: `/api/auth/signup` `login` `refresh` `logout` `me` `profile` `change-password` `forgot-password` `reset-password` `photo`

Chat: `/api/conversations` `/api/conversations/:id/messages` `/api/messages` `/api/vision`

Memory: `/api/memories`

Language: `/api/language/detect`

Coding: `/api/coding/assist` `/api/coding/github`

Admin (Bearer token + `role=admin`): `/api/admin/stats` `users` `health` `config` `usage`

Private conversation bodies, passwords, API keys and memories are **not** returned by admin endpoints.

## 4. Android connection

In the app **Login** or **Settings** set **Backend URL** to your public HTTPS origin, for example:

```
https://your-jarvis-service.onrender.com/
```

Emulator → host machine backend: `http://10.0.2.2:8080/`

Physical phone on the same Wi-Fi: `http://YOUR_LAN_IP:8080/`

## 5. Docker

```bash
cd backend
docker build -t jarvis-backend .
docker run -p 8080:8080 --env-file .env -v jarvis-data:/app/data jarvis-backend
```

## 6. Optional Firebase Authentication

This project ships a self-contained JWT backend (bcrypt passwords, refresh tokens) so the app works without a Firebase project.

To add Firebase later:

1. Create a Firebase project and enable Email/Password.
2. Put `google-services.json` in `android/app/` (already gitignored).
3. Verify Firebase ID tokens on the backend with the Firebase Admin SDK using a **service account JSON stored only in server env** (`FIREBASE_SERVICE_ACCOUNT`).
4. Map `firebase.uid` → `users.id`. Do not trust a client-side `isAdmin` flag; keep `users.role` server-side.

Until then, `/api/auth/*` is the secure authentication backend.

## 7. Security checklist

- Never commit `.env` or `google-services.json`
- Use HTTPS in production
- Rotate `JWT_SECRET` if leaked (all sessions drop)
- GitHub tokens entered in the coding assistant are sent only for that request and are not stored on the server
- Passwords are bcrypt-hashed (cost 12); they are never stored in plain text
