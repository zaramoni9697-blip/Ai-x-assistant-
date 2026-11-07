import { initDb, closeDb, queryOne, run, now } from "../db.js";
import { config } from "../config.js";
import { hashPassword, randomId } from "../utils/crypto.js";

await initDb();
const email = (process.argv[2] || config.adminEmail).toLowerCase();
const password = process.argv[3] || config.adminPassword;
if (!password || password.length < 8) {
  console.error("Usage: node src/scripts/seed-admin.js admin@email.com StrongPass123");
  process.exit(1);
}
const existing = queryOne("SELECT * FROM users WHERE email = ?", [email]);
if (existing) {
  run("UPDATE users SET role = 'admin', status = 'active', password_hash = ? WHERE id = ?", [
    await hashPassword(password),
    existing.id,
  ]);
  console.log("Updated existing user to admin:", email);
} else {
  const t = now();
  run(
    `INSERT INTO users (id, email, password_hash, display_name, role, status, language_pref, created_at, last_active)
     VALUES (?, ?, ?, 'Administrator', 'admin', 'active', 'auto', ?, ?)`,
    [randomId(), email, await hashPassword(password), t, t]
  );
  console.log("Created admin:", email);
}
closeDb();
