package com.jarvis.ai.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jarvis.ai.ui.theme.JarvisBlack
import com.jarvis.ai.ui.theme.JarvisCyan
import com.jarvis.ai.ui.theme.JarvisMuted
import com.jarvis.ai.ui.theme.JarvisNavy
import com.jarvis.ai.ui.theme.JarvisNavyLight
import com.jarvis.ai.ui.theme.JarvisText

val FieldColors @Composable get() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = JarvisCyan,
    unfocusedBorderColor = JarvisCyan.copy(alpha = 0.35f),
    focusedLabelColor = JarvisCyan,
    unfocusedLabelColor = JarvisMuted,
    cursorColor = JarvisCyan,
    focusedTextColor = JarvisText,
    unfocusedTextColor = JarvisText
)

@Composable
fun JarvisScaffold(
    title: String,
    onBack: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(listOf(JarvisBlack, JarvisNavy, JarvisBlack))
            )
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = JarvisCyan)
                }
            }
            Text(title, color = JarvisCyan, fontSize = 18.sp, letterSpacing = 2.sp, modifier = Modifier.weight(1f))
            trailing?.invoke()
        }
        Spacer(Modifier.height(8.dp))
        content()
    }
}

@Composable
fun GlowButton(
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = enabled && !loading,
        modifier = modifier.fillMaxWidth().height(52.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = JarvisNavyLight,
            contentColor = JarvisCyan,
            disabledContainerColor = JarvisNavy
        ),
        shape = RoundedCornerShape(4.dp),
        contentPadding = PaddingValues(12.dp)
    ) {
        if (loading) CircularProgressIndicator(Modifier.size(18.dp), color = JarvisCyan, strokeWidth = 2.dp)
        else Text(text, letterSpacing = 3.sp)
    }
}

@Composable
fun JarvisField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    password: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
    singleLine: Boolean = true
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = modifier.fillMaxWidth(),
        singleLine = singleLine,
        visualTransformation = if (password) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        colors = FieldColors,
        shape = RoundedCornerShape(4.dp)
    )
}

@Composable
fun FeatureCard(title: String, subtitle: String, icon: ImageVector, onClick: () -> Unit) {
    Column(
        Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(JarvisNavy.copy(alpha = 0.85f))
            .border(1.dp, JarvisCyan.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(14.dp)
    ) {
        Icon(icon, contentDescription = title, tint = JarvisCyan, modifier = Modifier.size(28.dp))
        Spacer(Modifier.height(10.dp))
        Text(title, color = JarvisText, fontSize = 14.sp, letterSpacing = 1.sp)
        Text(subtitle, color = JarvisMuted, fontSize = 11.sp)
    }
}

@Composable
fun StatusChip(text: String, ok: Boolean) {
    Box(
        Modifier
            .border(1.dp, if (ok) JarvisCyan else Color(0xFFFF5C7A), RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(text, color = if (ok) JarvisCyan else Color(0xFFFF5C7A), fontSize = 11.sp, letterSpacing = 1.sp)
    }
}
