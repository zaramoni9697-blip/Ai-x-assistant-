package com.jarvis.ai.feature.voice

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jarvis.ai.core.language.LanguageDetector
import com.jarvis.ai.core.voice.SpeechManager
import com.jarvis.ai.data.repository.httpMessage
import com.jarvis.ai.di.Graph
import com.jarvis.ai.ui.components.JarvisCore
import com.jarvis.ai.ui.components.JarvisScaffold
import com.jarvis.ai.ui.components.VoiceWaveform
import com.jarvis.ai.ui.theme.JarvisBlack
import com.jarvis.ai.ui.theme.JarvisCyan
import com.jarvis.ai.ui.theme.JarvisMuted
import com.jarvis.ai.ui.theme.JarvisText
import kotlinx.coroutines.launch

@Composable
fun VoiceScreen(onBack: () -> Unit, initialQuery: String? = null) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val speech = remember { SpeechManager(context, Graph.session) }
    var listening by remember { mutableStateOf(false) }
    var thinking by remember { mutableStateOf(false) }
    var speaking by remember { mutableStateOf(false) }
    var heard by remember { mutableStateOf("") }
    var reply by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("Tap the mic and speak. Bangla and English are auto-detected.") }
    var rms by remember { mutableFloatStateOf(0f) }
    var convId by remember { mutableStateOf<String?>(null) }

    val ask: (String) -> Unit = remember {
        { raw: String ->
            val cleaned = SpeechManager.stripWake(raw).ifBlank { raw }
            if (cleaned.isNotBlank()) {
                thinking = true
                status = "Thinking…"
                heard = cleaned
                scope.launch {
                    try {
                        val current = convId
                        val res = if (current == null) Graph.chatRepo.sendNew(cleaned)
                        else Graph.chatRepo.send(current, cleaned)
                        convId = res.conversationId
                        reply = res.assistantMessage.content
                        val lang = LanguageDetector.effectiveReply(
                            Graph.session.languagePref,
                            LanguageDetector.detect(res.assistantMessage.content)
                        )
                        status = res.detection?.label ?: LanguageDetector.detect(cleaned).label
                        if (Graph.session.ttsEnabled) Graph.tts.speak(reply, lang)
                    } catch (e: Exception) {
                        status = httpMessage(e)
                    } finally {
                        thinking = false
                    }
                }
            }
        }
    }

    DisposableEffect(Unit) {
        Graph.tts.onSpeaking { speaking = it }
        speech.onListening = { listening = it }
        speech.onRms = { rms = it }
        speech.onPartial = { heard = it }
        speech.onResult = { ask(it) }
        speech.onError = { status = it }
        onDispose { speech.stop() }
    }

    LaunchedEffect(initialQuery) {
        if (!initialQuery.isNullOrBlank()) ask(initialQuery)
    }

    JarvisScaffold(title = "VOICE", onBack = onBack) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(12.dp))
            JarvisCore(size = 200.dp, thinking = thinking, listening = listening)
            Spacer(Modifier.height(8.dp))
            Text(
                when {
                    listening -> "LISTENING"
                    thinking -> "THINKING"
                    speaking -> "SPEAKING"
                    else -> "READY"
                },
                color = JarvisCyan,
                letterSpacing = 4.sp,
                fontSize = 14.sp
            )
            VoiceWaveform(active = listening || speaking, rms = rms)
            Text(status, color = JarvisMuted, fontSize = 12.sp)
            Spacer(Modifier.height(8.dp))
            if (heard.isNotBlank()) Text("You: $heard", color = JarvisText, fontSize = 14.sp)
            if (reply.isNotBlank()) Text("JARVIS: $reply", color = JarvisCyan, fontSize = 14.sp)
            Spacer(Modifier.height(20.dp))
            FloatingActionButton(
                onClick = {
                    when {
                        speaking -> Graph.tts.stop()
                        listening -> speech.stop()
                        else -> speech.start()
                    }
                },
                containerColor = JarvisCyan,
                contentColor = JarvisBlack
            ) {
                Icon(
                    if (listening || speaking) Icons.Default.Stop else Icons.Default.Mic,
                    contentDescription = "Mic"
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(
                "Interrupt anytime with Stop. Wake phrase: Hey JARVIS (enable in Settings).",
                color = JarvisMuted,
                fontSize = 11.sp
            )
        }
    }
}
