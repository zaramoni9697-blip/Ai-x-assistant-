package com.jarvis.ai.feature.device

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jarvis.ai.core.device.DeviceActionHandler
import com.jarvis.ai.ui.components.GlowButton
import com.jarvis.ai.ui.components.JarvisField
import com.jarvis.ai.ui.components.JarvisScaffold
import com.jarvis.ai.ui.theme.JarvisCyan
import com.jarvis.ai.ui.theme.JarvisMuted
import com.jarvis.ai.ui.theme.JarvisText

@Composable
fun DeviceScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val handler = remember { DeviceActionHandler(context) }
    var command by remember { mutableStateOf("") }
    var log by remember { mutableStateOf("Ask me to open an app, toggle flashlight, check battery, change volume, open settings, or set an alarm.") }
    var pending by remember { mutableStateOf<DeviceActionHandler.Pending?>(null) }
    var confirmText by remember { mutableStateOf("") }

    fun run(raw: String) {
        val r = handler.interpret(raw)
        if (r.needsConfirmation && r.pending != null) {
            pending = r.pending
            confirmText = r.message
        } else {
            log = r.message
        }
    }

    if (pending != null) {
        AlertDialog(
            onDismissRequest = { pending = null },
            title = { Text("Confirm action") },
            text = { Text(confirmText) },
            confirmButton = {
                TextButton(onClick = {
                    log = handler.execute(pending!!)
                    pending = null
                }) { Text("Allow") }
            },
            dismissButton = { TextButton(onClick = { pending = null }) { Text("Cancel") } }
        )
    }

    JarvisScaffold(title = "DEVICE", onBack = onBack) {
        Text(
            "Sensitive actions always ask first. Android does not allow unrestricted control of every setting.",
            color = JarvisMuted,
            fontSize = 12.sp
        )
        JarvisField(command, { command = it }, "Command (Bangla or English)")
        GlowButton("RUN") { run(command) }
        Spacer(Modifier.height(8.dp))
        GlowButton("FLASHLIGHT") { log = handler.toggleFlashlight() }
        GlowButton("BATTERY") { log = handler.battery().message }
        GlowButton("DEVICE INFO") { log = handler.deviceInfo().message }
        GlowButton("VOLUME UP") { log = handler.volume(1).message }
        Spacer(Modifier.height(12.dp))
        Text(log, color = JarvisText, fontSize = 14.sp, modifier = Modifier.verticalScroll(rememberScrollState()))
        Spacer(Modifier.height(8.dp))
        Text("Installed apps (first 12): " + handler.installedApps().take(12).joinToString(), color = JarvisCyan, fontSize = 11.sp)
    }
}
