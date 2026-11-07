package com.jarvis.ai.feature.chat

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.jarvis.ai.data.local.entity.MessageEntity
import com.jarvis.ai.data.repository.httpMessage
import com.jarvis.ai.di.Graph
import com.jarvis.ai.ui.components.JarvisField
import com.jarvis.ai.ui.components.JarvisScaffold
import com.jarvis.ai.ui.components.VoiceWaveform
import com.jarvis.ai.ui.theme.JarvisCyan
import com.jarvis.ai.ui.theme.JarvisMuted
import com.jarvis.ai.ui.theme.JarvisNavy
import com.jarvis.ai.ui.theme.JarvisNavyLight
import com.jarvis.ai.ui.theme.JarvisText
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun ChatScreen(onBack: () -> Unit, onVoice: () -> Unit) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var conversationId by remember { mutableStateOf<String?>(null) }
    var input by remember { mutableStateOf("") }
    var thinking by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var listening by remember { mutableStateOf(false) }
    var speaking by remember { mutableStateOf(false) }
    var detected by remember { mutableStateOf("AUTO") }
    val speech = remember { SpeechManager(context, Graph.session) }
    val listState = rememberLazyListState()

    LaunchedEffect(Unit) {
        Graph.tts.onSpeaking { speaking = it }
        try {
            Graph.chatRepo.refreshConversations()
        } catch (_: Exception) {
        }
    }

    val cid = conversationId.orEmpty()
    val messages by remember(cid) {
        if (cid.isBlank()) flowOf(emptyList()) else Graph.chatRepo.messages(cid)
    }.collectAsState(initial = emptyList())

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.lastIndex)
    }

    fun sendText(text: String, image: File? = null) {
        val body = text.trim()
        if (body.isBlank() && image == null) return
        val det = LanguageDetector.detect(body)
        detected = det.label
        thinking = true
        error = null
        input = ""
        scope.launch {
            try {
                val res = if (image != null) {
                    Graph.chatRepo.sendImage(conversationId, image, body.ifBlank { "Describe this image." })
                } else if (conversationId == null) {
                    Graph.chatRepo.sendNew(body)
                } else {
                    Graph.chatRepo.send(conversationId!!, body)
                }
                conversationId = res.conversationId
                detected = res.detection?.label ?: det.label
                val replyLang = LanguageDetector.effectiveReply(
                    Graph.session.languagePref,
                    LanguageDetector.detect(res.assistantMessage.content)
                )
                if (Graph.session.ttsEnabled) {
                    Graph.tts.speak(res.assistantMessage.content, replyLang)
                }
            } catch (e: Exception) {
                error = httpMessage(e)
            } finally {
                thinking = false
            }
        }
    }

    val cameraFile = remember {
        File(context.cacheDir, "vision.jpg").also {
            it.parentFile?.mkdirs()
            if (!it.exists()) it.createNewFile()
        }
    }
    val cameraUri = remember {
        FileProvider.getUriForFile(context, context.packageName + ".fileprovider", cameraFile)
    }
    val photo = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        if (ok && cameraFile.exists()) {
            sendText(input.ifBlank { "What do you see?" }, cameraFile)
        }
    }

    JarvisScaffold(title = "CHAT", onBack = onBack, trailing = {
        IconButton(onClick = onVoice) {
            Icon(Icons.Default.Mic, contentDescription = "Voice", tint = JarvisCyan)
        }
    }) {
        Text("Language: $detected  •  Pref: ${Graph.session.languagePref.uppercase()}", color = JarvisMuted, fontSize = 11.sp)
        VoiceWaveform(active = listening || speaking || thinking)
        if (thinking) Text("JARVIS is thinking…", color = JarvisCyan, fontSize = 12.sp)
        if (error != null) Text(error!!, color = com.jarvis.ai.ui.theme.JarvisDanger, fontSize = 12.sp)
        Spacer(Modifier.height(8.dp))
        LazyColumn(Modifier.weight(1f), state = listState, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(messages, key = { it.id }) { m -> Bubble(m) }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { photo.launch(cameraUri) }) {
                Icon(Icons.Default.CameraAlt, contentDescription = "Image", tint = JarvisCyan)
            }
            Box(Modifier.weight(1f)) {
                JarvisField(input, { input = it }, "Message JARVIS")
            }
            IconButton(onClick = {
                if (speaking) {
                    Graph.tts.stop()
                } else if (listening) {
                    speech.stop()
                } else {
                    speech.onListening = { listening = it }
                    speech.onPartial = { input = it }
                    speech.onResult = {
                        input = it
                        sendText(it)
                    }
                    speech.onError = { error = it }
                    speech.start()
                }
            }) {
                Icon(
                    if (speaking || listening) Icons.Default.Stop else Icons.Default.Mic,
                    contentDescription = "Mic",
                    tint = JarvisCyan
                )
            }
            IconButton(onClick = { sendText(input) }, enabled = !thinking) {
                Icon(Icons.Default.Send, contentDescription = "Send", tint = JarvisCyan)
            }
        }
    }
}

@Composable
private fun Bubble(m: MessageEntity) {
    val mine = m.role == "user"
    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start) {
        Column(
            Modifier
                .widthIn(max = 320.dp)
                .background(if (mine) JarvisNavyLight else JarvisNavy, RoundedCornerShape(10.dp))
                .border(1.dp, JarvisCyan.copy(alpha = 0.25f), RoundedCornerShape(10.dp))
                .padding(10.dp)
        ) {
            Text(if (mine) "YOU" else "JARVIS", color = JarvisCyan, fontSize = 10.sp, letterSpacing = 1.sp)
            Text(m.content, color = JarvisText, fontSize = 14.sp)
        }
    }
}
