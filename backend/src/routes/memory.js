import { Router } from "express";
import { authRequired } from "../middleware/auth.js";
import { query, queryOne, run, now } from "../db.js";
import { randomId } from "../utils/crypto.js";

const router = Router();

router.get("/", authRequired, (req, res) => {
  const rows = query(
    `SELECT id, title, content, created_at as createdAt, updated_at as updatedAt
     FROM memories WHERE user_id = ? ORDER BY updated_at DESC`,
    [req.user.id]
  );
  res.json({ memories: rows });
});

router.post("/", authRequired, (req, res) => {
  const title = String(req.body.title || "").trim();
  const content = String(req.body.content || "").trim();
  if (!title || !content) {
    return res.status(400).json({ error: "title and content are required" });
  }
  const id = randomId();
  const t = now();
  run(
    "INSERT INTO memories (id, user_id, title, content, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?)",
    [id, req.user.id, title.slice(0, 120), content.slice(0, 4000), t, t]
  );
  res.status(201).json({
    memory: { id, title, content, createdAt: t, updatedAt: t },
  });
});

router.put("/:id", authRequired, (req, res) => {
  const row = queryOne("SELECT * FROM memories WHERE id = ? AND user_id = ?", [
    req.params.id,
    req.user.id,
  ]);
  if (!row) return res.status(404).json({ error: "Memory not found" });
  const title = String(req.body.title || row.title).trim();
  const content = String(req.body.content || row.content).trim();
  const t = now();
  run("UPDATE memories SET title = ?, content = ?, updated_at = ? WHERE id = ?", [
    title.slice(0, 120),
    content.slice(0, 4000),
    t,
    row.id,
  ]);
  res.json({
    memory: { id: row.id, title, content, createdAt: row.created_at, updatedAt: t },
  });
});

router.delete("/:id", authRequired, (req, res) => {
  const row = queryOne("SELECT * FROM memories WHERE id = ? AND user_id = ?", [
    req.params.id,
    req.user.id,
  ]);
  if (!row) return res.status(404).json({ error: "Memory not found" });
  run("DELETE FROM memories WHERE id = ?", [row.id]);
  res.json({ ok: true });
});

router.delete("/", authRequired, (req, res) => {
  run("DELETE FROM memories WHERE user_id = ?", [req.user.id]);
  res.json({ ok: true });
});

export default router;
