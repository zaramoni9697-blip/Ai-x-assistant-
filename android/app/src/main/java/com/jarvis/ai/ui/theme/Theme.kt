package com.jarvis.ai.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Scheme = darkColorScheme(
    primary = JarvisCyan,
    onPrimary = JarvisBlack,
    secondary = JarvisBlue,
    onSecondary = Color.White,
    background = JarvisBlack,
    onBackground = JarvisText,
    surface = JarvisNavy,
    onSurface = JarvisText,
    surfaceVariant = JarvisNavyLight,
    error = JarvisDanger,
    onError = Color.White,
    outline = JarvisCyanDim
)

@Composable
fun JarvisTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = Scheme,
        typography = JarvisTypography,
        content = content
    )
}
