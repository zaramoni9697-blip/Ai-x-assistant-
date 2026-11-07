/**
 * Automatic Bangla / English / mixed-language detection.
 * Used for both typed messages and speech transcripts.
 * No mandatory language screen — Auto is the default.
 */

const BANGLA_RE = /[\u0980-\u09FF]/g;
const LATIN_RE = /[A-Za-z]/g;

// Common romanized Bangla tokens (Banglish) so "ami kemon acho" is detected.
const ROMAN_BANGLA = new Set(
  `
  ami tumi apni apnar amra tader se she tara
  kemon kemonacho acho aso achen bhalo valo kharap
  dhonnobad dhonnobad thanks shukriya
  kothay kothai keno ki kisher koto kobe
  koro korun korcho korchi korbe
  jabo jacchi jacchi gelo
  khabo khacchi khai khawa
  bhai bon abbu ammu baba ma maa
  na ji hoi hobe hobe na thik ache
  please ektu ekhon ekhane okhane
  bangla banglae banglay
  parbo parben pari parina
  chai chao chao na
  suno shuno bolun bolen bolo
  nam name nam ki
  valo bhalobashi
  kalke ajke agamikal
  raina raat din shokal
  `.trim().split(/\s+/).filter(Boolean)
);

export function detectLanguage(text = "") {
  const raw = String(text || "");
  const trimmed = raw.trim();
  if (!trimmed) {
    return {
      language: "en",
      label: "English",
      dominant: "en",
      mixed: false,
      confidence: 0.2,
      banglaChars: 0,
      latinChars: 0,
    };
  }

  const banglaChars = (trimmed.match(BANGLA_RE) || []).length;
  const latinChars = (trimmed.match(LATIN_RE) || []).length;
  const letterTotal = banglaChars + latinChars || 1;
  const banglaRatio = banglaChars / letterTotal;
  const latinRatio = latinChars / letterTotal;

  const tokens = trimmed
    .toLowerCase()
    .replace(/[^\p{L}\p{N}\s]/gu, " ")
    .split(/\s+/)
    .filter(Boolean);
  const romanHits = tokens.filter((t) => ROMAN_BANGLA.has(t)).length;
  const romanRatio = tokens.length ? romanHits / tokens.length : 0;

  const hasBanglaScript = banglaChars > 0;
  const hasLatin = latinChars > 0;
  const looksRomanBangla = !hasBanglaScript && romanRatio >= 0.25 && romanHits >= 2;

  let mixed = false;
  let dominant = "en";
  let confidence = 0.55;

  if (hasBanglaScript && hasLatin && banglaRatio >= 0.12 && latinRatio >= 0.18) {
    mixed = true;
    dominant = banglaRatio >= latinRatio ? "bn" : "en";
    confidence = 0.8;
  } else if (hasBanglaScript && banglaRatio >= 0.12) {
    dominant = "bn";
    mixed = false;
    confidence = Math.min(0.99, 0.55 + banglaRatio);
  } else if (looksRomanBangla) {
    dominant = "bn";
    mixed = true;
    confidence = Math.min(0.85, 0.45 + romanRatio);
  } else {
    dominant = "en";
    mixed = false;
    confidence = Math.min(0.99, 0.55 + latinRatio);
  }

  const language = mixed ? "mixed" : dominant;
  const label =
    language === "mixed"
      ? dominant === "bn"
        ? "Mixed (Bangla-dominant)"
        : "Mixed (English-dominant)"
      : dominant === "bn"
        ? "Bangla"
        : "English";

  return {
    language,
    label,
    dominant,
    mixed,
    confidence: Number(confidence.toFixed(3)),
    banglaChars,
    latinChars,
  };
}

export function responseLanguageInstruction(detection, userPref = "auto") {
  const pref = (userPref || "auto").toLowerCase();
  if (pref === "bn" || pref === "bangla" || pref === "বাংলা") {
    return "Respond in natural, fluent Bangla (বাংলা লিপিতে). Do not switch to English unless the user used an untranslatable technical term.";
  }
  if (pref === "en" || pref === "english") {
    return "Respond in natural, fluent English.";
  }
  // Auto-detect (default)
  if (detection.dominant === "bn") {
    if (detection.mixed) {
      return "The user mixed Bangla and English. Understand the full sentence. Respond in natural Bangla (বাংলা লিপিতে) and keep untranslatable technical terms in English.";
    }
    return "The user wrote in Bangla. Respond in natural, fluent Bangla (বাংলা লিপিতে).";
  }
  if (detection.mixed) {
    return "The user mixed Bangla and English. Understand the complete sentence and respond in natural English, keeping any Bangla names or phrases intact.";
  }
  return "The user wrote in English. Respond in natural, fluent English.";
}
