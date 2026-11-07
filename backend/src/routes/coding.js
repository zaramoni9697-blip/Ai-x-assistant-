import { Router } from "express";
import { authRequired } from "../middleware/auth.js";
import { codingAssist } from "../services/gemini.js";
import { run, now } from "../db.js";
import { randomId } from "../utils/crypto.js";

const router = Router();

router.post("/assist", authRequired, async (req, res) => {
  try {
    const prompt = String(req.body.prompt || "").trim();
    const code = String(req.body.code || "");
    const task = String(req.body.task || "assist");
    if (!prompt && !code) {
      return res.status(400).json({ error: "prompt or code required" });
    }
    const allowed = ["generate", "explain", "fix", "review", "assist"];
    if (!allowed.includes(task)) {
      return res.status(400).json({ error: "invalid task" });
    }
    const result = await codingAssist({
      user: req.user,
      prompt,
      code,
      task,
    });
    run(
      "INSERT INTO usage_events (id, user_id, type, tokens, meta, created_at) VALUES (?, ?, ?, ?, ?, ?)",
      [
        randomId(),
        req.user.id,
        "coding",
        result.tokens || 0,
        JSON.stringify({ task }),
        now(),
      ]
    );
    res.json({
      result: result.text,
      detection: result.detection,
      task,
      notice:
        "Proposed changes only. JARVIS will not commit or modify repository files without explicit user approval in the app.",
    });
  } catch (err) {
    console.error(err);
    res.status(500).json({ error: "Coding assistant failed" });
  }
});

router.post("/github", authRequired, async (req, res) => {
  try {
    const repo = String(req.body.repo || "").trim();
    const token = String(req.body.githubToken || req.headers["x-github-token"] || "");
    if (!repo || !/^[\w.-]+\/[\w.-]+$/.test(repo)) {
      return res.status(400).json({ error: "repo must look like owner/name" });
    }
    if (!token) {
      return res.status(400).json({
        error: "A GitHub personal access token is required. It is used only for this request and is not stored.",
      });
    }
    const gh = await fetch(`https://api.github.com/repos/${repo}`, {
      headers: {
        Authorization: `Bearer ${token}`,
        "User-Agent": "JARVIS-AI",
        Accept: "application/vnd.github+json",
      },
    });
    if (!gh.ok) {
      return res.status(gh.status).json({
        error: "GitHub repository access failed. Check the token scopes and repo name.",
      });
    }
    const meta = await gh.json();
    const readmeRes = await fetch(`https://api.github.com/repos/${repo}/readme`, {
      headers: {
        Authorization: `Bearer ${token}`,
        "User-Agent": "JARVIS-AI",
        Accept: "application/vnd.github.raw",
      },
    });
    const readme = readmeRes.ok ? (await readmeRes.text()).slice(0, 12000) : "";
    const prompt = `Analyze this GitHub repository at a high level. Do not invent files.
Repo: ${repo}
Description: ${meta.description || ""}
Language: ${meta.language || ""}
Stars: ${meta.stargazers_count}
Default branch: ${meta.default_branch}
README (truncated):
${readme}

User request: ${req.body.prompt || "Summarize architecture, risks, and suggested improvements."}`;
    const result = await codingAssist({
      user: req.user,
      prompt,
      code: "",
      task: "review",
    });
    run(
      "INSERT INTO usage_events (id, user_id, type, tokens, meta, created_at) VALUES (?, ?, ?, ?, ?, ?)",
      [
        randomId(),
        req.user.id,
        "github_review",
        result.tokens || 0,
        JSON.stringify({ repo }),
        now(),
      ]
    );
    res.json({
      repo: { fullName: meta.full_name, description: meta.description, language: meta.language },
      result: result.text,
      notice: "Analysis only. No files were modified. Any patch must be approved by the user.",
    });
  } catch (err) {
    console.error(err);
    res.status(500).json({ error: "GitHub analysis failed" });
  }
});

export default router;
