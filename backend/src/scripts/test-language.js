import { detectLanguage } from "../utils/language.js";

const cases = [
  ["Hello JARVIS, what is the weather?", "en"],
  ["আমি কেমন আছি জানতে চাই", "bn"],
  ["Ami ekhon kothay jabo", "bn"],
  ["Please explain this code এবং এটা fix করো", "mixed"],
];

let failed = 0;
for (const [text, expect] of cases) {
  const d = detectLanguage(text);
  const ok =
    expect === "mixed"
      ? d.mixed || d.language === "mixed"
      : d.dominant === expect;
  console.log(
    `${ok ? "OK" : "FAIL"}  expect=${expect} got=${d.language}/${d.dominant}  :: ${text}`
  );
  if (!ok) failed += 1;
}
if (failed) process.exit(1);
console.log("language detection passed");
