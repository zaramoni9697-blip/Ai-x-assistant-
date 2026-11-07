import { Router } from "express";
import os from "node:os";
import { adminRequired } from "../middleware/auth.js";
import { query, queryOne, run, now } from "../db.js";
import { geminiConfigured } from "../services/gemini.js";
import { config } from "../config.js";
import { publicUser } from "../utils/crypto.js";

const router = Router();

router.use(adminRequired);

router.get("/stats", (req, res) => {
  const users = queryOne("SELECT COUNT(*) as c FROM users")?.c || 0;
  const active24h =
    queryOne(
      "SELECT COUNT(*) as c FROM users WHERE last_active >= ?",
      [now() - 24 * 60 * 60 * 1000]
    )?.c || 0;
  const active7d =
    queryOne(
      "SELECT COUNT(*) as c FROM users WHERE last_active >= ?",
      [now() - 7 * 24 * 60 * 60 * 1000]
    )?.c || 0;
  const suspended =
    queryOne("SELECT COUNT(*) as c FROM users WHERE status = 'suspended'")?.c || 0;
  const messages =
    queryOne("SELECT COUNT(*) as c FROM messages")?.c || 0;
  const tokens =
    queryOne("SELECT COALESCE(SUM(tokens),0) as c FROM usage_events")?.c || 0;
  const chatsToday =
    queryOne(
      "SELECT COUNT(*) as c FROM usage_events WHERE type IN ('chat','vision','coding') AND created_at >= ?",
      [now() - 24 * 60 * 60 * 1000]
    )?.c || 0;
  res.json({
    users,
    active24h,
    active7d,
    suspended,
    messages,
    tokens,
    chatsToday,
    // Private conversations and memories are intentionally omitted.
    privacy:
      "User conversations, passwords, API keys and private memories are not exposed in this dashboard.",
  });
});

router.get("/users", (req, res) => {
  const q = String(req.query.q || "").trim().toLowerCase();
  const rows = query(
    `SELECT id, email, display_name, role, status, language_pref, created_at, last_active, last_login
     FROM users ORDER BY created_at DESC`
  );
  const filtered = q
    ? rows.filter(
        (u) =>
          u.email.toLowerCase().includes(q) ||
          String(u.display_name).toLowerCase().includes(q)
      )
    : rows;
  res.json({
    users: filtered.map((u) => ({
      ...publicUser(u),
      lastLogin: u.last_login,
    })),
  });
});

router.put("/users/:id/status", (req, res) => {
  const status = String(req.body.status || "");
  if (!["active", "suspended"].includes(status)) {
    return res.status(400).json({ error: "status must be active or suspended" });
  }
  const user = queryOne("SELECT * FROM users WHERE id = ?", [req.params.id]);
  if (!user) return res.status(404).json({ error: "User not found" });
  if (user.id === req.user.id) {
    return res.status(400).json({ error: "You cannot change your own status" });
  }
  if (user.role === "admin" && status === "suspended") {
    return res.status(400).json({ error: "Refuse to suspend another admin from this panel" });
  }
  run("UPDATE users SET status = ? WHERE id = ?", [status, user.id]);
  if (status === "suspended") {
    run("DELETE FROM refresh_tokens WHERE user_id = ?", [user.id]);
  }
  res.json({ user: publicUser({ ...user, status }) });
});

router.put("/users/:id/role", (req, res) => {
  // Role changes are server-side only and cannot be performed by a normal user
  // because this route is behind adminRequired. Still, prevent self-demote accidents
  // and require an explicit confirmation field.
  const role = String(req.body.role || "");
  if (!["user", "admin"].includes(role)) {
    return res.status(400).json({ error: "role must be user or admin" });
  }
  if (req.body.confirm !== "ASSIGN_ROLE") {
    return res.status(400).json({
      error: "Pass confirm=ASSIGN_ROLE. Admin roles cannot be granted from the Android client settings.",
    });
  }
  const user = queryOne("SELECT * FROM users WHERE id = ?", [req.params.id]);
  if (!user) return res.status(404).json({ error: "User not found" });
  if (user.id === req.user.id) {
    return res.status(400).json({ error: "You cannot change your own role" });
  }
  run("UPDATE users SET role = ? WHERE id = ?", [role, user.id]);
  res.json({ user: publicUser({ ...user, role }) });
});

router.get("/health", (_req, res) => {
  const mem = process.memoryUsage();
  res.json({
    ok: true,
    uptimeSec: Math.round(process.uptime()),
    node: process.version,
    platform: `${os.type()} ${os.release()}`,
    load: os.loadavg(),
    memory: {
      rss: mem.rss,
      heapUsed: mem.heapUsed,
    },
    gemini: geminiConfigured() ? "configured" : "missing_api_key",
    env: config.env,
    time: new Date().toISOString(),
  });
});

router.get("/config", (_req, res) => {
  const rows = query("SELECT key, value, updated_at as updatedAt FROM app_config");
  const map = {};
  for (const r of rows) map[r.key] = r.value;
  res.json({
    config: map,
    rows,
    // Never return GEMINI_API_KEY or JWT_SECRET
    secrets: {
      geminiConfigured: geminiConfigured(),
      jwtConfigured: Boolean(process.env.JWT_SECRET),
      smtpConfigured: Boolean(config.smtp.host),
    },
  });
});

router.put("/config", (req, res) => {
  const allowed = new Set([
    "maintenance_mode",
    "allow_signups",
    "max_message_length",
    "wake_word_enabled_default",
  ]);
  const entries = req.body.config || req.body;
  const t = now();
  for (const [key, value] of Object.entries(entries)) {
    if (!allowed.has(key)) continue;
    const exists = queryOne("SELECT key FROM app_config WHERE key = ?", [key]);
    if (exists) {
      run("UPDATE app_config SET value = ?, updated_at = ? WHERE key = ?", [
        String(value),
        t,
        key,
      ]);
    } else {
      run("INSERT INTO app_config (key, value, updated_at) VALUES (?, ?, ?)", [
        key,
        String(value),
        t,
      ]);
    }
  }
  const rows = query("SELECT key, value FROM app_config");
  const map = {};
  for (const r of rows) map[r.key] = r.value;
  res.json({ config: map });
});

router.get("/usage", (_req, res) => {
  const byType = query(
    `SELECT type, COUNT(*) as count, COALESCE(SUM(tokens),0) as tokens
     FROM usage_events GROUP BY type`
  );
  const recent = query(
    `SELECT type, tokens, created_at as createdAt
     FROM usage_events ORDER BY created_at DESC LIMIT 50`
  );
  res.json({
    byType,
    recent,
    note: "Usage counters only — no prompt or response text is included.",
  });
});

export default router;
