import fs from "node:fs";
import path from "node:path";
import initSqlJs from "sql.js";
import { config } from "./config.js";

let SQL = null;
let db = null;
let saveTimer = null;

function persist() {
  if (!db) return;
  const dir = path.dirname(config.dbPath);
  fs.mkdirSync(dir, { recursive: true });
  const data = db.export();
  const tmp = `${config.dbPath}.tmp`;
  fs.writeFileSync(tmp, Buffer.from(data));
  fs.renameSync(tmp, config.dbPath);
}

function scheduleSave() {
  if (saveTimer) clearTimeout(saveTimer);
  saveTimer = setTimeout(() => {
    try {
      persist();
    } catch (err) {
      console.error("[db] persist failed", err);
    }
  }, 80);
}

function migrate() {
  db.run(`
    CREATE TABLE IF NOT EXISTS users (
      id TEXT PRIMARY KEY,
      email TEXT UNIQUE NOT NULL,
      password_hash TEXT NOT NULL,
      display_name TEXT NOT NULL,
      photo_url TEXT,
      role TEXT NOT NULL DEFAULT 'user',
      status TEXT NOT NULL DEFAULT 'active',
      language_pref TEXT NOT NULL DEFAULT 'auto',
      created_at INTEGER NOT NULL,
      last_active INTEGER NOT NULL,
      last_login INTEGER
    );

    CREATE TABLE IF NOT EXISTS refresh_tokens (
      id TEXT PRIMARY KEY,
      user_id TEXT NOT NULL,
      token_hash TEXT NOT NULL,
      expires_at INTEGER NOT NULL,
      created_at INTEGER NOT NULL,
      FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
    );

    CREATE TABLE IF NOT EXISTS password_resets (
      id TEXT PRIMARY KEY,
      user_id TEXT NOT NULL,
      code_hash TEXT NOT NULL,
      expires_at INTEGER NOT NULL,
      used INTEGER NOT NULL DEFAULT 0,
      created_at INTEGER NOT NULL,
      FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
    );

    CREATE TABLE IF NOT EXISTS conversations (
      id TEXT PRIMARY KEY,
      user_id TEXT NOT NULL,
      title TEXT NOT NULL,
      created_at INTEGER NOT NULL,
      updated_at INTEGER NOT NULL,
      FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
    );

    CREATE TABLE IF NOT EXISTS messages (
      id TEXT PRIMARY KEY,
      conversation_id TEXT NOT NULL,
      user_id TEXT NOT NULL,
      role TEXT NOT NULL,
      content TEXT NOT NULL,
      language TEXT,
      created_at INTEGER NOT NULL,
      FOREIGN KEY (conversation_id) REFERENCES conversations(id) ON DELETE CASCADE
    );

    CREATE TABLE IF NOT EXISTS memories (
      id TEXT PRIMARY KEY,
      user_id TEXT NOT NULL,
      title TEXT NOT NULL,
      content TEXT NOT NULL,
      created_at INTEGER NOT NULL,
      updated_at INTEGER NOT NULL,
      FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
    );

    CREATE TABLE IF NOT EXISTS usage_events (
      id TEXT PRIMARY KEY,
      user_id TEXT,
      type TEXT NOT NULL,
      tokens INTEGER DEFAULT 0,
      meta TEXT,
      created_at INTEGER NOT NULL
    );

    CREATE TABLE IF NOT EXISTS app_config (
      key TEXT PRIMARY KEY,
      value TEXT NOT NULL,
      updated_at INTEGER NOT NULL
    );

    CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);
    CREATE INDEX IF NOT EXISTS idx_conv_user ON conversations(user_id);
    CREATE INDEX IF NOT EXISTS idx_msg_conv ON messages(conversation_id);
    CREATE INDEX IF NOT EXISTS idx_mem_user ON memories(user_id);
    CREATE INDEX IF NOT EXISTS idx_usage_created ON usage_events(created_at);
  `);

  const defaults = [
    ["maintenance_mode", "false"],
    ["allow_signups", "true"],
    ["max_message_length", "8000"],
    ["wake_word_enabled_default", "false"],
  ];
  const now = Date.now();
  for (const [key, value] of defaults) {
    const exists = queryOne("SELECT key FROM app_config WHERE key = ?", [key]);
    if (!exists) {
      db.run("INSERT INTO app_config (key, value, updated_at) VALUES (?, ?, ?)", [
        key,
        value,
        now,
      ]);
    }
  }
  persist();
}

export async function initDb() {
  SQL = await initSqlJs();
  const dir = path.dirname(config.dbPath);
  fs.mkdirSync(dir, { recursive: true });
  if (fs.existsSync(config.dbPath)) {
    const file = fs.readFileSync(config.dbPath);
    db = new SQL.Database(file);
  } else {
    db = new SQL.Database();
  }
  db.run("PRAGMA foreign_keys = ON;");
  migrate();
  console.log(`[db] ready at ${config.dbPath}`);
}

export function query(sql, params = []) {
  const stmt = db.prepare(sql);
  stmt.bind(params);
  const rows = [];
  while (stmt.step()) {
    rows.push(stmt.getAsObject());
  }
  stmt.free();
  return rows;
}

export function queryOne(sql, params = []) {
  const rows = query(sql, params);
  return rows[0] || null;
}

export function run(sql, params = []) {
  db.run(sql, params);
  scheduleSave();
}

export function now() {
  return Date.now();
}

export function closeDb() {
  if (saveTimer) clearTimeout(saveTimer);
  persist();
  if (db) db.close();
}

process.on("exit", () => {
  try {
    persist();
  } catch {
    /* ignore */
  }
});
