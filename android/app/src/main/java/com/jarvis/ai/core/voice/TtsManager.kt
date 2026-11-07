package com.jarvis.ai.core.voice

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.jarvis.ai.core.language.SpeechLang
import java.util.Locale
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean

class TtsManager(context: Context) {

    private val ready = AtomicBoolean(false)

    private var speakingCb: ((Boolean) -> Unit)? = null

    // Explicit type prevents recursive type checking.
    private lateinit var tts: TextToSpeech

    init {

        tts = TextToSpeech(context.applicationContext) { status ->

            val success = status == TextToSpeech.SUCCESS
            ready.set(success)

            if (success && ::tts.isInitialized) {
                tts.setSpeechRate(0.98f)
                tts.setPitch(1.02f)
            }
        }

        tts.setOnUtteranceProgressListener(
            object : UtteranceProgressListener() {

                override fun onStart(utteranceId: String?) {
                    speakingCb?.invoke(true)
                }

                override fun onDone(utteranceId: String?) {
                    speakingCb?.invoke(false)
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    speakingCb?.invoke(false)
                }
            }
        )
    }

    fun onSpeaking(cb: (Boolean) -> Unit) {
        speakingCb = cb
    }

    fun speak(text: String, lang: SpeechLang) {

        if (!ready.get() || text.isBlank()) return

        val locale = pickLocale(lang)

        val result = tts.setLanguage(locale)

        if (result == TextToSpeech.LANG_MISSING_DATA ||
            result == TextToSpeech.LANG_NOT_SUPPORTED
        ) {
            return
        }

        val id = UUID.randomUUID().toString()

        tts.speak(
            text,
            TextToSpeech.QUEUE_FLUSH,
            null,
            id
        )
    }

    fun stop() {
        if (::tts.isInitialized) {
            tts.stop()
        }

        speakingCb?.invoke(false)
    }

    fun isSpeaking(): Boolean {
        return ::tts.isInitialized && tts.isSpeaking
    }

    fun shutdown() {
        ready.set(false)

        if (::tts.isInitialized) {
            tts.stop()
            tts.shutdown()
        }
    }

    private fun pickLocale(lang: SpeechLang): Locale {

        val wanted = if (lang == SpeechLang.BN) {

            listOf(
                Locale.forLanguageTag("bn-BD"),
                Locale.forLanguageTag("bn-IN"),
                Locale("bn")
            )

        } else {

            listOf(
                Locale.US,
                Locale.UK,
                Locale.ENGLISH
            )
        }

        for (loc in wanted) {

            val result = tts.isLanguageAvailable(loc)

            if (result >= TextToSpeech.LANG_AVAILABLE) {
                return loc
            }
        }

        return Locale.getDefault()
    }
}
