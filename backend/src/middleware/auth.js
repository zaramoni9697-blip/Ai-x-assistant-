import { queryOne, run, now } from "../db.js";
import { verifyAccessToken, publicUser } from "../utils/crypto.js";

export function authRequired(req, res, next) {
  const header = req.headers.authorization || "";
  const token = header.startsWith("Bearer ") ? header.slice(7) : null;
  if (!token) {
    return res.status(401).json({ error: "Authentication required" });
  }
  try {
    const payload = verifyAccessToken(token);
    const user = queryOne("SELECT * FROM users WHERE id = ?", [payload.sub]);
    if (!user) {
      return res.status(401).json({ error: "Account not found" });
    }
    if (user.status === "suspended") {
      return res.status(403).json({ error: "Account is suspended" });
    }
    run("UPDATE users SET last_active = ? WHERE id = ?", [now(), user.id]);
    req.user = user;
    req.auth = payload;
    next();
  } catch {
    return res.status(401).json({ error: "Invalid or expired token" });
  }
}

export function adminRequired(req, res, next) {
  authRequired(req, res, () => {
    if (req.user.role !== "admin") {
      return res.status(403).json({
        error: "Admin access denied. Role is assigned only on the server.",
      });
    }
    next();
  });
}

export function optionalAuth(req, _res, next) {
  const header = req.headers.authorization || "";
  const token = header.startsWith("Bearer ") ? header.slice(7) : null;
  if (!token) return next();
  try {
    const payload = verifyAccessToken(token);
    const user = queryOne("SELECT * FROM users WHERE id = ?", [payload.sub]);
    if (user && user.status !== "suspended") {
      req.user = user;
    }
  } catch {
    /* ignore */
  }
  next();
}

export { publicUser };
