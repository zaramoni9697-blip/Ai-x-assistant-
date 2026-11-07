package com.jarvis.ai.feature.memory

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jarvis.ai.data.repository.httpMessage
import com.jarvis.ai.di.Graph
import com.jarvis.ai.ui.components.GlowButton
import com.jarvis.ai.ui.components.JarvisField
import com.jarvis.ai.ui.components.JarvisScaffold
import com.jarvis.ai.ui.theme.JarvisCyan
import com.jarvis.ai.ui.theme.JarvisMuted
import com.jarvis.ai.ui.theme.JarvisNavy
import com.jarvis.ai.ui.theme.JarvisText
import kotlinx.coroutines.launch

@Composable
fun MemoryScreen(onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    val items by Graph.memoryRepo.observe().collectAsState(initial = emptyList())
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        try { Graph.memoryRepo.refresh() } catch (e: Exception) { error = httpMessage(e) }
    }
    JarvisScaffold(title = "MEMORY", onBack = onBack) {
        Text("Private to your account. Admins cannot read these by default.", color = JarvisMuted, fontSize = 12.sp)
        JarvisField(title, { title = it }, "Title")
        JarvisField(content, { content = it }, "What should JARVIS remember?", singleLine = false)
        if (error != null) Text(error!!, color = com.jarvis.ai.ui.theme.JarvisDanger, fontSize = 12.sp)
        GlowButton("SAVE MEMORY") {
            scope.launch {
                try {
                    Graph.memoryRepo.create(title, content)
                    title = ""; content = ""
                } catch (e: Exception) { error = httpMessage(e) }
            }
        }
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(items, key = { it.id }) { m ->
                Column(
                    Modifier
                        .fillMaxWidth()
                        .background(JarvisNavy, RoundedCornerShape(8.dp))
                        .border(1.dp, JarvisCyan.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    Row {
                        Text(m.title, color = JarvisCyan, modifier = Modifier.weight(1f))
                        IconButton(onClick = { scope.launch { try { Graph.memoryRepo.delete(m.id) } catch (e: Exception) { error = httpMessage(e) } } }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = JarvisCyan)
                        }
                    }
                    Text(m.content, color = JarvisText, fontSize = 13.sp)
                }
            }
        }
    }
}
