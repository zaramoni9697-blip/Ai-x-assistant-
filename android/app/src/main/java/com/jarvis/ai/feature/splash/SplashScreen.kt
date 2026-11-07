package com.jarvis.ai.feature.splash

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jarvis.ai.di.Graph
import com.jarvis.ai.ui.components.JarvisCore
import com.jarvis.ai.ui.theme.JarvisBlack
import com.jarvis.ai.ui.theme.JarvisCyan
import com.jarvis.ai.ui.theme.JarvisMuted
import com.jarvis.ai.ui.theme.JarvisNavy
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onDone: (loggedIn: Boolean) -> Unit) {
    LaunchedEffect(Unit) {
        delay(1400)
        val logged = Graph.session.accessToken.isNotBlank()
        if (logged) {
            try { Graph.authRepo.me() } catch (_: Exception) { /* token may refresh later */ }
        }
        onDone(Graph.session.accessToken.isNotBlank())
    }
    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(JarvisBlack, JarvisNavy, JarvisBlack))),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            JarvisCore(thinking = true)
            Spacer(Modifier.height(24.dp))
            Text("JARVIS", color = JarvisCyan, fontSize = 32.sp, letterSpacing = 10.sp)
            Spacer(Modifier.height(8.dp))
            Text("PERSONAL AI ASSISTANT", color = JarvisMuted, fontSize = 12.sp, letterSpacing = 4.sp)
        }
    }
}
