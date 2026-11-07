package com.jarvis.ai.domain.usecase

import com.jarvis.ai.core.language.DetectedLanguage
import com.jarvis.ai.core.language.LanguageDetector

class DetectLanguageUseCase {
    operator fun invoke(text: String): DetectedLanguage = LanguageDetector.detect(text)
}
