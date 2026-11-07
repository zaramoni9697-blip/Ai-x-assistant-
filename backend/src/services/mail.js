import nodemailer from "nodemailer";
import { config } from "../config.js";

let transporter = null;

function getTransporter() {
  if (!config.smtp.host) return null;
  if (!transporter) {
    transporter = nodemailer.createTransport({
      host: config.smtp.host,
      port: config.smtp.port,
      secure: config.smtp.port === 465,
      auth:
        config.smtp.user && config.smtp.pass
          ? { user: config.smtp.user, pass: config.smtp.pass }
          : undefined,
    });
  }
  return transporter;
}

export async function sendPasswordResetCode(email, code) {
  const tx = getTransporter();
  if (!tx) {
    return { sent: false, reason: "smtp_not_configured" };
  }
  await tx.sendMail({
    from: config.smtp.from,
    to: email,
    subject: "JARVIS AI password reset code",
    text: `Your JARVIS AI password reset code is ${code}. It expires in 15 minutes. If you did not request this, ignore this email.`,
    html: `<p>Your JARVIS AI password reset code is <b>${code}</b>.</p><p>It expires in 15 minutes.</p>`,
  });
  return { sent: true };
}
