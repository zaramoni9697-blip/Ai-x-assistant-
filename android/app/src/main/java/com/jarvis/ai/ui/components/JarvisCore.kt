package com.jarvis.ai.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jarvis.ai.ui.theme.JarvisBlue
import com.jarvis.ai.ui.theme.JarvisCyan
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun JarvisCore(
    modifier: Modifier = Modifier,
    size: Dp = 180.dp,
    thinking: Boolean = false,
    listening: Boolean = false
) {
    val inf = rememberInfiniteTransition(label = "core")
    val spin by inf.animateFloat(
        0f, 360f,
        infiniteRepeatable(tween(if (thinking) 4000 else 9000, easing = LinearEasing)),
        label = "spin"
    )
    val pulse by inf.animateFloat(
        0.86f, 1.12f,
        infiniteRepeatable(tween(if (listening) 700 else 1800), RepeatMode.Reverse),
        label = "pulse"
    )
    val hue = if (listening) JarvisCyan else if (thinking) JarvisBlue else JarvisCyan

    Canvas(modifier.size(size)) {
        val r = this.size.minDimension / 2f
        val c = Offset(this.size.width / 2f, this.size.height / 2f)
        drawCircle(
            brush = Brush.radialGradient(
                listOf(hue.copy(alpha = 0.35f), Color.Transparent),
                center = c,
                radius = r * pulse
            )
        )
        rotate(spin, c) {
            drawCircle(color = hue.copy(alpha = 0.9f), radius = r * 0.78f, center = c, style = Stroke(3f))
            drawCircle(color = JarvisBlue.copy(alpha = 0.6f), radius = r * 0.62f, center = c, style = Stroke(2f))
            for (i in 0 until 12) {
                val a = Math.toRadians((i * 30).toDouble())
                val inner = r * 0.72f
                val outer = r * 0.86f
                drawLine(
                    hue,
                    Offset(c.x + inner * cos(a).toFloat(), c.y + inner * sin(a).toFloat()),
                    Offset(c.x + outer * cos(a).toFloat(), c.y + outer * sin(a).toFloat()),
                    strokeWidth = 2f
                )
            }
        }
        rotate(-spin * 1.4f, c) {
            drawArc(
                color = hue,
                startAngle = 0f,
                sweepAngle = 80f,
                useCenter = false,
                style = Stroke(width = 4f),
                topLeft = Offset(c.x - r * 0.5f, c.y - r * 0.5f),
                size = androidx.compose.ui.geometry.Size(r, r)
            )
        }
        drawCircle(
            brush = Brush.radialGradient(listOf(Color.White, hue, hue.copy(alpha = 0.2f))),
            radius = r * 0.16f * pulse,
            center = c
        )
    }
}
