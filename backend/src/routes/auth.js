import { Router } from "express";
import multer from "multer";
import path from "node:path";
import fs from "node:fs";
import { config } from "../config.js";
import { query, queryOne, run, now } from "../db.js";
import {
  hashPassword,
  verifyPassword,
  signAccessToken,
  randomId,
  randomToken,
  sha256,
  sixDigitCode,
  publicUser,
} from "../utils/crypto.js";
import { authRequired } from "../middleware/auth.js";
import { sendPasswordResetCode } from "../services/mail.js";

const router = Router();
fs.mkdirSync(config.uploadsDir, { recursive: true });

const upload = multer({
  storage: multer.diskStorage({
    destination: (_req, _file, cb) => cb(null, config.uploadsDir),
    filename: (req, file, cb) => {
      const ext = path.extname(file.originalname || "").toLowerCase() || ".jpg";
      cb(null, `${req.user.id}-${Date.now()}${ext}`);
    },
  }),
  limits: { fileSize: 2 * 1024 * 1024 },
  fileFilter: (_req, file, cb) => {
    if (!/^image\/(jpeg|png|webp|gif)$/.test(file.mimetype)) {
      return cb(new Error("Only image uploads are allowed"));
    }
    cb(null, true);
  },
});

function validEmail(email) {
  return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email);
}

function issueTokens(user) {
  const accessToken = signAccessToken(user);
  const refresh = randomToken();
  const id = randomId();
  const expires = now() + config.refreshExpiresDays * 24 * 60 * 60 * 1000;
  run(
    "INSERT INTO refresh_tokens (id, user_id, token_hash, expires_at, created_at) VALUES (?, ?, ?, ?, ?)",
    [id, user.id, sha256(refresh), expires, now()]
  );
  return { accessToken, refreshToken: refresh, expiresIn: config.jwtExpiresIn };
}

function getConfigValue(key, fallback) {
  const row = queryOne("SELECT value FROM app_config WHERE key = ?", [key]);
  return row ? row.value : fallback;
}

router.post("/signup", async (req, res) => {
  try {
    if (getConfigValue("allow_signups", "true") !== "true") {
      return res.status(403).json({ error: "Signups are currently disabled" });
    }
    const email = String(req.body.email || "").trim().toLowerCase();
    const password = String(req.body.password || "");
    const displayName = String(req.body.displayName || "").trim() || email.split("@")[0];
    if (!validEmail(email)) {
      return res.status(400).json({ error: "Enter a valid email address" });
    }
    if (password.length < 8) {
      return res.status(400).json({ error: "Password must be at least 8 characters" });
    }
    if (queryOne("SELECT id FROM users WHERE email = ?", [email])) {
      return res.status(409).json({ error: "An account with this email already exists" });
    }
    const id = randomId();
    const t = now();
    run(
      `INSERT INTO users (id, email, password_hash, display_name, role, status, language_pref, created_at, last_active)
       VALUES (?, ?, ?, ?, 'user', 'active', 'auto', ?, ?)`,
      [id, email, await hashPassword(password), displayName, t, t]
    );
    const user = queryOne("SELECT * FROM users WHERE id = ?", [id]);
    const tokens = issueTokens(user);
    res.status(201).json({ user: publicUser(user), ...tokens });
  } catch (err) {
    console.error(err);
    res.status(500).json({ error: "Signup failed" });
  }
});

router.post("/login", async (req, res) => {
  try {
    const email = String(req.body.email || "").trim().toLowerCase();
    const password = String(req.body.password || "");
    const user = queryOne("SELECT * FROM users WHERE email = ?", [email]);
    if (!user || !(await verifyPassword(password, user.password_hash))) {
      return res.status(401).json({ error: "Invalid email or password" });
    }
    if (user.status === "suspended") {
      return res.status(403).json({ error: "This account has been suspended" });
    }
    run("UPDATE users SET last_login = ?, last_active = ? WHERE id = ?", [
      now(),
      now(),
      user.id,
    ]);
    const tokens = issueTokens(user);
    res.json({ user: publicUser(user), ...tokens });
  } catch (err) {
    console.error(err);
    res.status(500).json({ error: "Login failed" });
  }
});

router.post("/refresh", (req, res) => {
  const refreshToken = String(req.body.refreshToken || "");
  if (!refreshToken) return res.status(400).json({ error: "refreshToken required" });
  const row = queryOne(
    "SELECT * FROM refresh_tokens WHERE token_hash = ?",
    [sha256(refreshToken)]
  );
  if (!row || row.expires_at < now()) {
    return res.status(401).json({ error: "Refresh token invalid or expired" });
  }
  const user = queryOne("SELECT * FROM users WHERE id = ?", [row.user_id]);
  if (!user || user.status === "suspended") {
    return res.status(401).json({ error: "Account unavailable" });
  }
  run("DELETE FROM refresh_tokens WHERE id = ?", [row.id]);
  const tokens = issueTokens(user);
  res.json({ user: publicUser(user), ...tokens });
});

router.post("/logout", authRequired, (req, res) => {
  const refreshToken = String(req.body.refreshToken || "");
  if (refreshToken) {
    run("DELETE FROM refresh_tokens WHERE token_hash = ? AND user_id = ?", [
      sha256(refreshToken),
      req.user.id,
    ]);
  } else {
    run("DELETE FROM refresh_tokens WHERE user_id = ?", [req.user.id]);
  }
  res.json({ ok: true });
});

router.get("/me", authRequired, (req, res) => {
  res.json({ user: publicUser(req.user) });
});

router.put("/profile", authRequired, (req, res) => {
  const displayName = String(req.body.displayName || req.user.display_name).trim();
  const languagePref = String(req.body.languagePref || req.user.language_pref);
  const allowed = ["auto", "bn", "en"];
  if (!allowed.includes(languagePref)) {
    return res.status(400).json({ error: "languagePref must be auto, bn, or en" });
  }
  run("UPDATE users SET display_name = ?, language_pref = ? WHERE id = ?", [
    displayName.slice(0, 80),
    languagePref,
    req.user.id,
  ]);
  const user = queryOne("SELECT * FROM users WHERE id = ?", [req.user.id]);
  res.json({ user: publicUser(user) });
});

router.post("/change-password", authRequired, async (req, res) => {
  const current = String(req.body.currentPassword || "");
  const next = String(req.body.newPassword || "");
  if (next.length < 8) {
    return res.status(400).json({ error: "New password must be at least 8 characters" });
  }
  if (!(await verifyPassword(current, req.user.password_hash))) {
    return res.status(401).json({ error: "Current password is incorrect" });
  }
  run("UPDATE users SET password_hash = ? WHERE id = ?", [
    await hashPassword(next),
    req.user.id,
  ]);
  run("DELETE FROM refresh_tokens WHERE user_id = ?", [req.user.id]);
  res.json({ ok: true });
});

router.post("/forgot-password", async (req, res) => {
  const email = String(req.body.email || "").trim().toLowerCase();
  const generic = {
    ok: true,
    message: "If an account exists, a reset code was issued.",
  };
  const user = queryOne("SELECT * FROM users WHERE email = ?", [email]);
  if (!user) return res.json(generic);

  const code = sixDigitCode();
  run(
    "INSERT INTO password_resets (id, user_id, code_hash, expires_at, used, created_at) VALUES (?, ?, ?, ?, 0, ?)",
    [randomId(), user.id, sha256(code), now() + 15 * 60 * 1000, now()]
  );
  const mail = await sendPasswordResetCode(user.email, code);
  const payload = { ...generic };
  // Development convenience only — never leak codes in production.
  if (!config.isProd && !mail.sent) {
    payload.devCode = code;
    payload.hint = "SMTP is not configured; code returned for local testing only.";
  }
  res.json(payload);
});

router.post("/reset-password", async (req, res) => {
  const email = String(req.body.email || "").trim().toLowerCase();
  const code = String(req.body.code || "").trim();
  const next = String(req.body.newPassword || "");
  if (next.length < 8) {
    return res.status(400).json({ error: "New password must be at least 8 characters" });
  }
  const user = queryOne("SELECT * FROM users WHERE email = ?", [email]);
  if (!user) return res.status(400).json({ error: "Invalid reset request" });
  const row = queryOne(
    `SELECT * FROM password_resets
     WHERE user_id = ? AND code_hash = ? AND used = 0
     ORDER BY created_at DESC LIMIT 1`,
    [user.id, sha256(code)]
  );
  if (!row || row.expires_at < now()) {
    return res.status(400).json({ error: "Invalid or expired reset code" });
  }
  run("UPDATE password_resets SET used = 1 WHERE id = ?", [row.id]);
  run("UPDATE users SET password_hash = ? WHERE id = ?", [
    await hashPassword(next),
    user.id,
  ]);
  run("DELETE FROM refresh_tokens WHERE user_id = ?", [user.id]);
  res.json({ ok: true });
});

router.post("/photo", authRequired, (req, res) => {
  upload.single("photo")(req, res, (err) => {
    if (err) return res.status(400).json({ error: err.message });
    if (!req.file) return res.status(400).json({ error: "photo file required" });
    const photoUrl = `/uploads/${req.file.filename}`;
    run("UPDATE users SET photo_url = ? WHERE id = ?", [photoUrl, req.user.id]);
    const user = queryOne("SELECT * FROM users WHERE id = ?", [req.user.id]);
    res.json({ user: publicUser(user) });
  });
});

export default router;
