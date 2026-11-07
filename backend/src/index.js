import fs from "node:fs";
import path from "node:path";
import express from "express";
import cors from "cors";
import helmet from "helmet";
import rateLimit from "express-rate-limit";
import { config } from "./config.js";
import { initDb, closeDb, queryOne, run, now } from "./db.js";
import { hashPassword, randomId } from "./utils/crypto.js";
import authRoutes from "./routes/auth.js";
import chatRoutes from "./routes/chat.js";
import memoryRoutes from "./routes/memory.js";
import codingRoutes from "./routes/coding.js";
import adminRoutes from "./routes/admin.js";
import languageRoutes from "./routes/language.js";
import { geminiConfigured } from "./services/gemini.js";

async function seedAdmin() {
  const existing = queryOne("SELECT id FROM users WHERE role = 'admin' LIMIT 1");
  if (existing) return;
  const email = config.adminEmail;
  let password = config.adminPassword;
  let generated = false;
  if (!password || password.length < 8) {
    password = randomId().replace(/-/g, "").slice(0, 16);
    generated = true;
  }
  const id = randomId();
  const t = now();
  run(
    `INSERT INTO users (id, email, password_hash, display_name, role, status, language_pref, created_at, last_active)
     VALUES (?, ?, ?, 'Administrator', 'admin', 'active', 'auto', ?, ?)`,
    [id, email, await hashPassword(password), t, t]
  );
  console.log("╔══════════════════════════════════════════════════════╗");
  console.log("║  JARVIS admin account created (server-side only)    ║");
  console.log(`║  email:    ${email.padEnd(41)}║`);
  if (generated) {
    console.log(`║  password: ${password.padEnd(41)}║`);
    console.log("║  Save this password. It will not be shown again.    ║");
  } else {
    console.log("║  password: (from ADMIN_PASSWORD env)                ║");
  }
  console.log("╚══════════════════════════════════════════════════════╝");
}

async function main() {
  fs.mkdirSync(path.dirname(config.dbPath), { recursive: true });
  fs.mkdirSync(config.uploadsDir, { recursive: true });
  await initDb();
  await seedAdmin();

  const app = express();
  app.set("trust proxy", 1);
  app.use(
    helmet({
      // Allow embedding the admin HUD in a dashboard preview / iframe.
      frameguard: false,
      crossOriginEmbedderPolicy: false,
      crossOriginResourcePolicy: { policy: "cross-origin" },
      contentSecurityPolicy: {
        useDefaults: true,
        directives: {
          "script-src": ["'self'", "'unsafe-inline'"],
          "style-src": ["'self'", "'unsafe-inline'", "https://fonts.googleapis.com"],
          "font-src": ["'self'", "https://fonts.gstatic.com", "data:"],
          "img-src": ["'self'", "data:", "blob:"],
          "connect-src": ["'self'"],
          "frame-ancestors": ["'self'", "https:", "http:"],
        },
      },
    })
  );
  app.use(
    cors({
      origin: (origin, cb) => {
        if (!origin) return cb(null, true);
        if (config.corsOrigins.length === 0) return cb(null, true);
        if (config.corsOrigins.includes(origin)) return cb(null, true);
        return cb(null, false);
      },
      credentials: true,
    })
  );
  app.use(express.json({ limit: "2mb" }));
  app.use(express.urlencoded({ extended: true }));

  const authLimiter = rateLimit({
    windowMs: 15 * 60 * 1000,
    max: 40,
    standardHeaders: true,
    legacyHeaders: false,
  });
  const apiLimiter = rateLimit({
    windowMs: 60 * 1000,
    max: 120,
    standardHeaders: true,
    legacyHeaders: false,
  });

  app.get("/api/health", (_req, res) => {
    res.json({
      ok: true,
      name: "JARVIS AI",
      gemini: geminiConfigured() ? "configured" : "missing_api_key",
      time: new Date().toISOString(),
    });
  });

  app.use("/api/auth", authLimiter, authRoutes);
  app.use("/api", apiLimiter, chatRoutes);
  app.use("/api/memories", apiLimiter, memoryRoutes);
  app.use("/api/coding", apiLimiter, codingRoutes);
  app.use("/api/admin", adminRoutes);
  app.use("/api/language", languageRoutes);

  app.use("/uploads", express.static(config.uploadsDir, { maxAge: "7d" }));
  app.use(express.static(config.publicDir));
  app.get("/admin", (_req, res) => {
    res.sendFile(path.join(config.publicDir, "index.html"));
  });
  app.get("*", (req, res, next) => {
    if (req.path.startsWith("/api")) {
      return res.status(404).json({ error: "Not found" });
    }
    next();
  });

  app.use((err, _req, res, _next) => {
    console.error("[error]", err);
    res.status(500).json({ error: "Internal server error" });
  });

  const server = app.listen(config.port, config.host, () => {
    console.log(`JARVIS backend listening on http://${config.host}:${config.port}`);
    console.log(`Admin dashboard: http://localhost:${config.port}/`);
    if (!geminiConfigured()) {
      console.warn("GEMINI_API_KEY is not set — chat will return a configuration message.");
    }
  });

  const shutdown = () => {
    server.close(() => {
      closeDb();
      process.exit(0);
    });
  };
  process.on("SIGINT", shutdown);
  process.on("SIGTERM", shutdown);
}

main().catch((err) => {
  console.error("Fatal startup error", err);
  process.exit(1);
});
