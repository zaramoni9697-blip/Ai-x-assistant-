import { Router } from "express";
import multer from "multer";
import { authRequired } from "../middleware/auth.js";
import { query, queryOne, run, now } from "../db.js";
import { randomId } from "../utils/crypto.js";
import { chatWithJarvis } from "../services/gemini.js";
import { detectLanguage } from "../utils/language.js";

const router = Router();
const memoryUpload = multer({
  storage: multer.memoryStorage(),
  limits: { fileSize: 6 * 1024 * 1024 },
});

function titleFrom(text) {
  const t = String(text || "New conversation").replace(/\s+/g, " ").trim();
  return t.slice(0, 48) || "New conversation";
}

router.get("/conversations", authRequired, (req, res) => {
  const rows = query(
    "SELECT id, title, created_at as createdAt, updated_at as updatedAt FROM conversations WHERE user_id = ? ORDER BY updated_at DESC",
    [req.user.id]
  );
  res.json({ conversations: rows });
});

router.post("/conversations", authRequired, (req, res) => {
  const id = randomId();
  const t = now();
  const title = titleFrom(req.body.title || "New conversation");
  run(
    "INSERT INTO conversations (id, user_id, title, created_at, updated_at) VALUES (?, ?, ?, ?, ?)",
    [id, req.user.id, title, t, t]
  );
  res.status(201).json({ conversation: { id, title, createdAt: t, updatedAt: t } });
});

router.delete("/conversations/:id", authRequired, (req, res) => {
  const conv = queryOne(
    "SELECT * FROM conversations WHERE id = ? AND user_id = ?",
    [req.params.id, req.user.id]
  );
  if (!conv) return res.status(404).json({ error: "Conversation not found" });
  run("DELETE FROM messages WHERE conversation_id = ?", [conv.id]);
  run("DELETE FROM conversations WHERE id = ?", [conv.id]);
  res.json({ ok: true });
});

router.delete("/conversations", authRequired, (req, res) => {
  const convos = query("SELECT id FROM conversations WHERE user_id = ?", [
    req.user.id,
  ]);
  for (const c of convos) {
    run("DELETE FROM messages WHERE conversation_id = ?", [c.id]);
  }
  run("DELETE FROM conversations WHERE user_id = ?", [req.user.id]);
  res.json({ ok: true, cleared: convos.length });
});

router.get("/conversations/:id/messages", authRequired, (req, res) => {
  const conv = queryOne(
    "SELECT * FROM conversations WHERE id = ? AND user_id = ?",
    [req.params.id, req.user.id]
  );
  if (!conv) return res.status(404).json({ error: "Conversation not found" });
  const messages = query(
    `SELECT id, role, content, language, created_at as createdAt
     FROM messages WHERE conversation_id = ? ORDER BY created_at ASC`,
    [conv.id]
  );
  res.json({ conversation: conv, messages });
});

async function handleMessage(req, res, { imageBase64, imageMime }) {
  let conversationId = req.params.id || req.body.conversationId;
  const text = String(req.body.content || req.body.text || "").trim();
  if (!text && !imageBase64) {
    return res.status(400).json({ error: "Message content required" });
  }
  if (!conversationId) {
    conversationId = randomId();
    const t = now();
    run(
      "INSERT INTO conversations (id, user_id, title, created_at, updated_at) VALUES (?, ?, ?, ?, ?)",
      [conversationId, req.user.id, titleFrom(text), t, t]
    );
  } else {
    const conv = queryOne(
      "SELECT * FROM conversations WHERE id = ? AND user_id = ?",
      [conversationId, req.user.id]
    );
    if (!conv) return res.status(404).json({ error: "Conversation not found" });
  }

  const detection = detectLanguage(text);
  const userMsgId = randomId();
  const t = now();
  run(
    `INSERT INTO messages (id, conversation_id, user_id, role, content, language, created_at)
     VALUES (?, ?, ?, 'user', ?, ?, ?)`,
    [userMsgId, conversationId, req.user.id, text || "[image]", detection.language, t]
  );

  const result = await chatWithJarvis({
    user: req.user,
    conversationId,
    userText: text || "Please analyze this image.",
    imageBase64,
    imageMime,
    mode: req.body.mode || "chat",
  });

  const asstId = randomId();
  const t2 = now();
  run(
    `INSERT INTO messages (id, conversation_id, user_id, role, content, language, created_at)
     VALUES (?, ?, ?, 'assistant', ?, ?, ?)`,
    [
      asstId,
      conversationId,
      req.user.id,
      result.text,
      result.detection?.dominant || detection.dominant,
      t2,
    ]
  );
  run("UPDATE conversations SET updated_at = ?, title = CASE WHEN title = 'New conversation' THEN ? ELSE title END WHERE id = ?", [
    t2,
    titleFrom(text),
    conversationId,
  ]);
  run(
    "INSERT INTO usage_events (id, user_id, type, tokens, meta, created_at) VALUES (?, ?, ?, ?, ?, ?)",
    [
      randomId(),
      req.user.id,
      imageBase64 ? "vision" : "chat",
      result.tokens || 0,
      JSON.stringify({ model: result.model, lang: result.detection?.language }),
      t2,
    ]
  );

  res.json({
    conversationId,
    userMessage: {
      id: userMsgId,
      role: "user",
      content: text || "[image]",
      language: detection.language,
      createdAt: t,
    },
    assistantMessage: {
      id: asstId,
      role: "assistant",
      content: result.text,
      language: result.detection?.dominant,
      createdAt: t2,
    },
    detection: result.detection,
  });
}

router.post("/conversations/:id/messages", authRequired, async (req, res) => {
  try {
    await handleMessage(req, res, {});
  } catch (err) {
    console.error(err);
    res.status(500).json({ error: "Failed to send message" });
  }
});

router.post("/messages", authRequired, async (req, res) => {
  try {
    await handleMessage(req, res, {});
  } catch (err) {
    console.error(err);
    res.status(500).json({ error: "Failed to send message" });
  }
});

router.post(
  "/vision",
  authRequired,
  memoryUpload.single("image"),
  async (req, res) => {
    try {
      if (!req.file) return res.status(400).json({ error: "image required" });
      req.body.content = req.body.content || req.body.prompt || "";
      await handleMessage(req, res, {
        imageBase64: req.file.buffer.toString("base64"),
        imageMime: req.file.mimetype,
      });
    } catch (err) {
      console.error(err);
      res.status(500).json({ error: "Vision request failed" });
    }
  }
);

export default router;
