import { Router } from "express";
import { detectLanguage, responseLanguageInstruction } from "../utils/language.js";
import { authRequired } from "../middleware/auth.js";

const router = Router();

router.post("/detect", authRequired, (req, res) => {
  const text = String(req.body.text || "");
  const detection = detectLanguage(text);
  res.json({
    detection,
    instruction: responseLanguageInstruction(
      detection,
      req.user.language_pref || "auto"
    ),
  });
});

export default router;
