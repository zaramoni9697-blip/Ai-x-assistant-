package com.jarvis.ai.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import com.jarvis.ai.ui.theme.JarvisCyan
import kotlin.math.sin

@Composable
fun VoiceWaveform(
    active: Boolean,
    rms: Float = 0f,
    modifier: Modifier = Modifier
) {
    val inf = rememberInfiniteTransition(label = "wave")
    val phase by inf.animateFloat(
        0f, (Math.PI * 2).toFloat(),
        infiniteRepeatable(tween(1200, easing = LinearEasing), RepeatMode.Restart),
        label = "phase"
    )
    Canvas(modifier.fillMaxWidth().height(56.dp)) {
        val bars = 28
        val w = size.width / bars
        val mid = size.height / 2f
        for (i in 0 until bars) {
            val amp = if (active) {
                val n = sin(phase + i * 0.45f)
                (0.25f + 0.75f * ((n + 1f) / 2f) * (0.4f + (rms / 10f).coerceIn(0f, 1.6f)))
            } else 0.12f
            val h = size.height * amp * 0.9f
            drawLine(
                color = JarvisCyan.copy(alpha = if (active) 0.95f else 0.35f),
                start = Offset(i * w + w / 2f, mid - h / 2f),
                end = Offset(i * w + w / 2f, mid + h / 2f),
                strokeWidth = w * 0.45f,
                cap = StrokeCap.Round
            )
        }
    }
}
