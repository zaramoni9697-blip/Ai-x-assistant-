import dotenv from "dotenv";
import path from "node:path";
import { fileURLToPath } from "node:url";
import crypto from "node:crypto";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
dotenv.config({ path: path.resolve(__dirname, "../.env") });

function requiredInProd(name, fallback) {
  const value = process.env[name] || fallback;
  if (process.env.NODE_ENV === "production" && !process.env[name] && !fallback) {
    console.warn(`[config] ${name} is not set`);
  }
  return value;
}

const jwtSecret =
  process.env.JWT_SECRET && process.env.JWT_SECRET.length >= 24
    ? process.env.JWT_SECRET
    : null;

export const config = {
  port: Number(process.env.PORT || 8080),
  host: process.env.HOST || "0.0.0.0",
  env: process.env.NODE_ENV || "development",
  isProd: (process.env.NODE_ENV || "development") === "production",
  jwtSecret: jwtSecret || crypto.randomBytes(48).toString("hex"),
  jwtExpiresIn: process.env.JWT_EXPIRES_IN || "2h",
  refreshExpiresDays: Number(process.env.REFRESH_EXPIRES_DAYS || 30),
  geminiKey: process.env.GEMINI_API_KEY || "",
  geminiModel: process.env.GEMINI_MODEL || "gemini-2.0-flash",
  adminEmail: (process.env.ADMIN_EMAIL || "admin@jarvis.local").toLowerCase(),
  adminPassword: process.env.ADMIN_PASSWORD || "",
  smtp: {
    host: process.env.SMTP_HOST || "",
    port: Number(process.env.SMTP_PORT || 587),
    user: process.env.SMTP_USER || "",
    pass: process.env.SMTP_PASS || "",
    from: process.env.SMTP_FROM || "JARVIS AI <noreply@jarvis.local>",
  },
  corsOrigins: (process.env.CORS_ORIGINS || "")
    .split(",")
    .map((s) => s.trim())
    .filter(Boolean),
  dbPath: process.env.DB_PATH
    ? path.resolve(process.env.DB_PATH)
    : path.resolve(__dirname, "../data/jarvis.sqlite"),
  uploadsDir: path.resolve(__dirname, "../uploads"),
  publicDir: path.resolve(__dirname, "../public"),
  publicUrl: process.env.PUBLIC_URL || "http://localhost:8080",
};

if (!jwtSecret) {
  console.warn(
    "[config] JWT_SECRET missing or too short — a random secret was generated for this process. Sessions will not survive restarts. Set JWT_SECRET in .env."
  );
}
