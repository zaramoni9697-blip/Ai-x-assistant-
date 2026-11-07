package com.jarvis.ai.feature.coding

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jarvis.ai.data.remote.dto.CodingRequest
import com.jarvis.ai.data.remote.dto.GithubRequest
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
fun CodingScreen(onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    var task by remember { mutableStateOf("generate") }
    var prompt by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var repo by remember { mutableStateOf("") }
    var result by remember { mutableStateOf("") }
    var notice by remember { mutableStateOf("Proposed changes only. Nothing is committed without your approval.") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val tasks = listOf("generate", "explain", "fix", "review")

    JarvisScaffold(title = "CODING", onBack = onBack) {
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            tasks.forEach { t ->
                FilterChip(
                    selected = task == t,
                    onClick = { task = t },
                    label = { Text(t.uppercase()) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = JarvisCyan,
                        selectedLabelColor = JarvisNavy,
                        labelColor = JarvisText
                    )
                )
            }
        }
        JarvisField(prompt, { prompt = it }, "What should JARVIS do?", singleLine = false)
        JarvisField(code, { code = it }, "Paste code (optional)", singleLine = false)
        JarvisField(repo, { repo = it }, "GitHub repo owner/name (optional)")
        if (Graph.session.githubToken.isBlank()) {
            var tok by remember { mutableStateOf("") }
            JarvisField(tok, { tok = it; Graph.session.githubToken = it }, "GitHub token (stored encrypted, never uploaded except for this request)")
        }
        Text(notice, color = JarvisMuted, fontSize = 12.sp)
        if (error != null) Text(error!!, color = com.jarvis.ai.ui.theme.JarvisDanger, fontSize = 12.sp)
        GlowButton("RUN ASSISTANT", loading = loading) {
            loading = true
            error = null
            scope.launch {
                try {
                    result = if (repo.isNotBlank()) {
                        val r = Graph.api.github(
                            GithubRequest(repo.trim(), Graph.session.githubToken, prompt)
                        )
                        notice = r.notice ?: notice
                        r.result
                    } else {
                        val r = Graph.api.coding(CodingRequest(prompt, code, task))
                        notice = r.notice ?: notice
                        r.result
                    }
                } catch (e: Exception) {
                    error = httpMessage(e)
                } finally {
                    loading = false
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            result,
            color = JarvisText,
            fontSize = 13.sp,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
        )
    }
}
