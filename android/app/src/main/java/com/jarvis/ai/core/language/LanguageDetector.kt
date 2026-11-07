package com.jarvis.ai.core.language

/**
 * On-device Bangla / English / mixed detection for typed and spoken text.
 * Auto Detect is the default — the user never has to pick a language first.
 */
enum class SpeechLang { EN, BN, MIXED }

data class DetectedLanguage(
    val language: SpeechLang,
    val dominant: SpeechLang,
    val mixed: Boolean,
    val confidence: Float,
    val label: String
) {
    val ttsLocaleTag: String
        get() = if (dominant == SpeechLang.BN) "bn-BD" else "en-US"

    val sttLocaleTag: String
        get() = if (dominant == SpeechLang.BN) "bn-BD" else "en-US"
}

object LanguageDetector {
    private val banglaRange = '\u0980'..'\u09FF'
    private val romanBangla = setOf(
        "ami", "tumi", "apni", "apnar", "amra", "tader", "tara",
        "kemon", "acho", "achen", "aso", "bhalo", "valo", "kharap",
        "dhonnobad", "shukriya", "kothay", "kothai", "keno", "kobe",
        "koro", "korcho", "korchi", "jabo", "jacchi", "khabo",
        "bhai", "bon", "abbu", "ammu", "baba", "thik", "ache",
        "hobe", "parbo", "parina", "suno", "shuno", "bolun",
        "ekhon", "ekhane", "okhane", "bangla", "kalke", "ajke"
    )

    fun detect(text: String): DetectedLanguage {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) {
            return DetectedLanguage(SpeechLang.EN, SpeechLang.EN, false, 0.2f, "English")
        }
        val banglaChars = trimmed.count { it in banglaRange }
        val latinChars = trimmed.count { it.isLetter() && it !in banglaRange }
        val total = (banglaChars + latinChars).coerceAtLeast(1)
        val banglaRatio = banglaChars.toFloat() / total
        val latinRatio = latinChars.toFloat() / total
        val tokens = trimmed.lowercase()
            .replace(Regex("[^\\p{L}\\p{N}\\s]"), " ")
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() }
        val romanHits = tokens.count { it in romanBangla }
        val romanRatio = if (tokens.isEmpty()) 0f else romanHits.toFloat() / tokens.size
        val hasBangla = banglaChars > 0
        val hasLatin = latinChars > 0
        val looksRomanBangla = !hasBangla && romanRatio >= 0.25f && romanHits >= 2

        val mixed: Boolean
        val dominant: SpeechLang
        val confidence: Float
        when {
            hasBangla && hasLatin && banglaRatio >= 0.12f && latinRatio >= 0.18f -> {
                mixed = true
                dominant = if (banglaRatio >= latinRatio) SpeechLang.BN else SpeechLang.EN
                confidence = 0.8f
            }
            hasBangla && banglaRatio >= 0.12f -> {
                mixed = false
                dominant = SpeechLang.BN
                confidence = (0.55f + banglaRatio).coerceAtMost(0.99f)
            }
            looksRomanBangla -> {
                mixed = true
                dominant = SpeechLang.BN
                confidence = (0.45f + romanRatio).coerceAtMost(0.85f)
            }
            else -> {
                mixed = false
                dominant = SpeechLang.EN
                confidence = (0.55f + latinRatio).coerceAtMost(0.99f)
            }
        }
        val language = if (mixed) SpeechLang.MIXED else dominant
        val label = when {
            mixed && dominant == SpeechLang.BN -> "Mixed (Bangla)"
            mixed -> "Mixed (English)"
            dominant == SpeechLang.BN -> "বাংলা"
            else -> "English"
        }
        return DetectedLanguage(language, dominant, mixed, confidence, label)
    }

    /**
     * User preference overlay. "auto" uses detection; "bn"/"en" force reply language.
     */
    fun effectiveReply(pref: String, detected: DetectedLanguage): SpeechLang {
        return when (pref.lowercase()) {
            "bn", "bangla", "বাংলা" -> SpeechLang.BN
            "en", "english" -> SpeechLang.EN
            else -> detected.dominant
        }
    }
}
