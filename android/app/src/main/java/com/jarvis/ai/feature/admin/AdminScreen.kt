package com.jarvis.ai.feature.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jarvis.ai.data.remote.dto.AdminStats
import com.jarvis.ai.data.remote.dto.StatusBody
import com.jarvis.ai.data.remote.dto.UserDto
import com.jarvis.ai.data.repository.httpMessage
import com.jarvis.ai.di.Graph
import com.jarvis.ai.ui.components.JarvisScaffold
import com.jarvis.ai.ui.theme.JarvisCyan
import com.jarvis.ai.ui.theme.JarvisMuted
import com.jarvis.ai.ui.theme.JarvisNavy
import com.jarvis.ai.ui.theme.JarvisText
import kotlinx.coroutines.launch

@Composable
fun AdminScreen(onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    var stats by remember { mutableStateOf<AdminStats?>(null) }
    var users by remember { mutableStateOf<List<UserDto>>(emptyList()) }
    var health by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    fun refresh() {
        scope.launch {
            try {
                stats = Graph.api.adminStats()
                users = Graph.api.adminUsers().users
                health = Graph.api.adminHealth().let { "ok=${it.ok} gemini=${it.gemini} uptime=${it.uptimeSec}s" }
            } catch (e: Exception) {
                error = httpMessage(e)
            }
        }
    }
    LaunchedEffect(Unit) { refresh() }

    JarvisScaffold(title = "ADMIN", onBack = onBack) {
        Text("Server-assigned role only. Private chats and memories are hidden.", color = JarvisMuted, fontSize = 12.sp)
        if (error != null) Text(error!!, color = com.jarvis.ai.ui.theme.JarvisDanger)
        val s = stats
        if (s != null) {
            Text("Users ${s.users}  •  24h ${s.active24h}  •  7d ${s.active7d}", color = JarvisCyan)
            Text("Suspended ${s.suspended}  •  Messages ${s.messages}  •  AI 24h ${s.chatsToday}", color = JarvisText, fontSize = 13.sp)
        }
        Text(health, color = JarvisMuted, fontSize = 12.sp)
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(users, key = { it.id }) { u ->
                Column(
                    Modifier
                        .fillMaxWidth()
                        .background(JarvisNavy, RoundedCornerShape(8.dp))
                        .border(1.dp, JarvisCyan.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                        .padding(10.dp)
                ) {
                    Text(u.email, color = JarvisCyan, fontSize = 13.sp)
                    Text("${u.displayName}  •  ${u.role}  •  ${u.status}", color = JarvisText, fontSize = 12.sp)
                    if (u.id != Graph.session.userId && u.role != "admin") {
                        Row {
                            TextButton(onClick = {
                                scope.launch {
                                    try {
                                        val next = if (u.status == "suspended") "active" else "suspended"
                                        Graph.api.adminStatus(u.id, StatusBody(next))
                                        refresh()
                                    } catch (e: Exception) { error = httpMessage(e) }
                                }
                            }) { Text(if (u.status == "suspended") "Reactivate" else "Suspend", color = JarvisCyan) }
                        }
                    }
                }
            }
        }
    }
}
