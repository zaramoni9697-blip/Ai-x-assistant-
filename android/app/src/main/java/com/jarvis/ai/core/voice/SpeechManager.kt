package com.jarvis.ai.core.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import com.jarvis.ai.core.security.SessionStore
import java.util.Locale

/**
 * Speech-to-text with automatic Bangla/English handling.
 *
 * Auto Detect (default): do not force a language extra so Google's recognizer
 * can return Bangla, English or mixed transcripts. The transcript is then
 * classified on-device and the reply language follows the user.
 *
 * Optional settings: Auto / বাংলা / English — switching does not restart chat.
 */
class SpeechManager(
    private val context: Context,
    private val session: SessionStore
) {
    private var recognizer: SpeechRecognizer? = null
    var onPartial: (String) -> Unit = {}
    var onResult: (String) -> Unit = {}
    var onError: (String) -> Unit = {}
    var onListening: (Boolean) -> Unit = {}
    var onRms: (Float) -> Unit = {}

    fun available(): Boolean = SpeechRecognizer.isRecognitionAvailable(context)

    fun start(prefer: String = session.languagePref) {
        stop()
        if (!available()) {
            onError("Speech recognition is not available on this device")
            return
        }
        val sr = SpeechRecognizer.createSpeechRecognizer(context)
        recognizer = sr
        sr.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                onListening(true)
            }
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {
                onRms(rmsdB)
            }
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {
                onListening(false)
            }
            override fun onError(error: Int) {
                onListening(false)
                onError(errorMessage(error))
            }
            override fun onResults(results: Bundle?) {
                onListening(false)
                val text = results
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()
                    .orEmpty()
                if (text.isNotBlank()) onResult(text) else onError("No speech detected")
            }
            override fun onPartialResults(partialResults: Bundle?) {
                val text = partialResults
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()
                    .orEmpty()
                if (text.isNotBlank()) onPartial(text)
            }
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
        sr.startListening(buildIntent(prefer))
    }

    fun stop() {
        try {
            recognizer?.cancel()
            recognizer?.destroy()
        } catch (_: Exception) {
        }
        recognizer = null
        onListening(false)
    }

    private fun buildIntent(prefer: String): Intent {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        intent.putExtra(
            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
        )
        intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
        intent.putExtra("android.speech.extra.DICTATION_MODE", true)
        when (prefer.lowercase()) {
            "bn", "bangla", "বাংলা" -> {
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "bn-BD")
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "bn-BD")
            }
            "en", "english" -> {
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "en-US")
            }
            else -> {
                // Auto: hint both languages; many Google recognizers will still auto-detect.
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag())
                intent.putExtra(
                    RecognizerIntent.EXTRA_ADDITIONAL_LANGUAGES,
                    arrayOf("bn-BD", "en-US", "bn-IN")
                )
                intent.putExtra("android.speech.extra.EXTRA_ADDITIONAL_LANGUAGES", arrayOf("en-US", "bn-BD"))
            }
        }
        return intent
    }

    private fun errorMessage(code: Int): String = when (code) {
        SpeechRecognizer.ERROR_AUDIO -> "Microphone error"
        SpeechRecognizer.ERROR_CLIENT -> "Speech client error"
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission required"
        SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Speech network error"
        SpeechRecognizer.ERROR_NO_MATCH -> "I didn't catch that"
        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognizer busy"
        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech heard"
        else -> "Speech error ($code)"
    }

    companion object {
        fun looksLikeWakePhrase(text: String): Boolean {
            val n = text.lowercase()
                .replace(Regex("[^a-zA-Z\u0980-\u09FF\\s]"), " ")
                .replace(Regex("\\s+"), " ")
                .trim()
            return n.contains("hey jarvis") ||
                n.contains("hi jarvis") ||
                n.contains("ok jarvis") ||
                n.contains("okay jarvis") ||
                n.contains("হে জার্ভিস") ||
                n.contains("হেই জার্ভিস") ||
                n.contains("ওহে জার্ভিস") ||
                n == "jarvis" ||
                n.startsWith("jarvis ")
        }

        fun stripWake(text: String): String {
            return text.replace(Regex("(?i)(hey|hi|ok|okay)\\s+jarvis[,:]?\\s*"), "")
                .replace(Regex("(হেই?|ওহে)\\s*জার্ভিস[,:]?\\s*"), "")
                .trim()
        }
    }
}
