package com.jarvis.ai.feature.settings

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.jarvis.ai.core.voice.WakeWordService
import com.jarvis.ai.data.repository.httpMessage
import com.jarvis.ai.di.Graph
import com.jarvis.ai.ui.components.GlowButton
import com.jarvis.ai.ui.components.JarvisField
import com.jarvis.ai.ui.components.JarvisScaffold
import com.jarvis.ai.ui.theme.JarvisCyan
import com.jarvis.ai.ui.theme.JarvisMuted
import com.jarvis.ai.ui.theme.JarvisText
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var url by remember { mutableStateOf(Graph.session.apiBaseUrl) }
    var lang by remember { mutableStateOf(Graph.session.languagePref) }
    var wake by remember { mutableStateOf(Graph.session.wakeWordEnabled) }
    var tts by remember { mutableStateOf(Graph.session.ttsEnabled) }
    var notif by remember { mutableStateOf(Graph.session.notificationsEnabled) }
    var info by remember { mutableStateOf<String?>(null) }

    val mic = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { granted ->
        val ok = granted.values.all { it }
        if (ok) {
            Graph.session.wakeWordEnabled = true
            wake = true
            WakeWordService.start(context)
            info = "Wake-word listening started. A persistent notification will stay visible."
        } else {
            info = "Microphone (and notification) permission is required for Hey JARVIS."
        }
    }

    JarvisScaffold(title = "SETTINGS", onBack = onBack) {
        Text("Language — no restart required. Auto Detect is default.", color = JarvisMuted, fontSize = 12.sp)
        Row {
            listOf("auto" to "AUTO DETECT", "bn" to "বাংলা", "en" to "ENGLISH").forEach { (id, label) ->
                FilterChip(
                    selected = lang == id,
                    onClick = {
                        lang = id
                        Graph.session.languagePref = id
                        scope.launch {
                            try {
                                Graph.authRepo.updateProfile(Graph.session.displayName, id)
                            } catch (_: Exception) {
                            }
                        }
                    },
                    label = { Text(label) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        JarvisField(url, { url = it }, "Backend URL")
        GlowButton("SAVE SERVER URL") {
            Graph.session.apiBaseUrl = url
            info = "Saved. New requests will use this server."
        }
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Speak replies (TTS)", color = JarvisText, modifier = Modifier.weight(1f))
            Switch(checked = tts, onCheckedChange = { tts = it; Graph.session.ttsEnabled = it }, colors = SwitchDefaults.colors(checkedThumbColor = JarvisCyan))
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Notifications", color = JarvisText, modifier = Modifier.weight(1f))
            Switch(checked = notif, onCheckedChange = { notif = it; Graph.session.notificationsEnabled = it }, colors = SwitchDefaults.colors(checkedThumbColor = JarvisCyan))
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Hey JARVIS wake phrase", color = JarvisText, modifier = Modifier.weight(1f))
            Switch(
                checked = wake,
                onCheckedChange = { on ->
                    if (on) {
                        val perms = buildList {
                            add(Manifest.permission.RECORD_AUDIO)
                            if (Build.VERSION.SDK_INT >= 33) add(Manifest.permission.POST_NOTIFICATIONS)
                        }.toTypedArray()
                        mic.launch(perms)
                    } else {
                        wake = false
                        Graph.session.wakeWordEnabled = false
                        WakeWordService.stop(context)
                    }
                },
                colors = SwitchDefaults.colors(checkedThumbColor = JarvisCyan)
            )
        }
        Text(
            "Wake listening uses a visible foreground service and microphone permission. It never records secretly. Built-in detection uses SpeechRecognizer; a custom Porcupine model is optional (docs/WAKE_WORD.md).",
            color = JarvisMuted,
            fontSize = 12.sp
        )
        Spacer(Modifier.height(8.dp))
        GlowButton("CLEAR MY CHAT HISTORY") {
            scope.launch {
                try {
                    Graph.chatRepo.clearAll()
                    info = "Conversation history cleared for this account."
                } catch (e: Exception) {
                    info = httpMessage(e)
                }
            }
        }
        if (info != null) Text(info!!, color = JarvisCyan, fontSize = 13.sp)
    }
}
