package com.jarvis.ai.feature.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jarvis.ai.di.Graph
import com.jarvis.ai.ui.components.FeatureCard
import com.jarvis.ai.ui.components.JarvisCore
import com.jarvis.ai.ui.components.JarvisScaffold
import com.jarvis.ai.ui.components.StatusChip
import com.jarvis.ai.ui.theme.JarvisMuted
import com.jarvis.ai.ui.theme.JarvisText

private data class Tile(val title: String, val subtitle: String, val icon: ImageVector, val route: String)

@Composable
fun DashboardScreen(onOpen: (String) -> Unit) {
    val session = Graph.session
    var online by remember { mutableStateOf<Boolean?>(null) }
    LaunchedEffect(Unit) {
        online = try {
            Graph.authRepo.health().ok
        } catch (_: Exception) {
            false
        }
    }
    val tiles = buildList {
        add(Tile("Chat", "Text with JARVIS", Icons.Default.Chat, "chat"))
        add(Tile("Voice", "Talk — auto Bangla/English", Icons.Default.Mic, "voice"))
        add(Tile("Coding", "Generate, explain, fix", Icons.Default.Code, "coding"))
        add(Tile("Memory", "Private saved facts", Icons.Default.Memory, "memory"))
        add(Tile("Device", "Phone assistant", Icons.Default.PhoneAndroid, "device"))
        add(Tile("Profile", "Photo & identity", Icons.Default.Person, "profile"))
        add(Tile("Settings", "Language & wake word", Icons.Default.Settings, "settings"))
        if (session.isAdmin) add(Tile("Admin", "Control dashboard", Icons.Default.AdminPanelSettings, "admin"))
    }
    JarvisScaffold(title = "JARVIS") {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            JarvisCore(size = 150.dp)
            Spacer(Modifier.height(8.dp))
            Text("Hello, I'm JARVIS", color = JarvisText, fontSize = 20.sp, letterSpacing = 1.sp)
            Text(
                session.displayName.ifBlank { session.email },
                color = JarvisMuted,
                fontSize = 13.sp
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatusChip(
                    if (online == true) "ONLINE" else if (online == false) "OFFLINE" else "…",
                    online != false
                )
                StatusChip(
                    when (session.languagePref) {
                        "bn" -> "বাংলা"
                        "en" -> "ENGLISH"
                        else -> "AUTO DETECT"
                    },
                    true
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            items(tiles) { t ->
                FeatureCard(t.title, t.subtitle, t.icon) { onOpen(t.route) }
            }
        }
    }
}
