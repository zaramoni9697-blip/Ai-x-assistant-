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
    private val tts = TextToSpeech(context.applicationContext) { status ->
        ready.set(status == TextToSpeech.SUCCESS)
        if (ready.get()) {
            tts.setSpeechRate(0.98f)
            tts.setPitch(1.02f)
        }
    }

    init {
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
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
        })
    }

    fun onSpeaking(cb: (Boolean) -> Unit) {
        speakingCb = cb
    }

    fun speak(text: String, lang: SpeechLang) {
        if (!ready.get() || text.isBlank()) return
        val locale = pickLocale(lang)
        tts.language = locale
        val id = UUID.randomUUID().toString()
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, id)
    }

    fun stop() {
        tts.stop()
        speakingCb?.invoke(false)
    }

    fun isSpeaking(): Boolean = tts.isSpeaking

    fun shutdown() {
        tts.stop()
        tts.shutdown()
    }

    private fun pickLocale(lang: SpeechLang): Locale {
        val wanted = if (lang == SpeechLang.BN) listOf(
            Locale.forLanguageTag("bn-BD"),
            Locale.forLanguageTag("bn-IN"),
            Locale("bn")
        ) else listOf(
            Locale.US,
            Locale.UK,
            Locale.ENGLISH
        )
        for (loc in wanted) {
            val r = tts.isLanguageAvailable(loc)
            if (r >= TextToSpeech.LANG_AVAILABLE) return loc
        }
        return Locale.getDefault()
    }
}
