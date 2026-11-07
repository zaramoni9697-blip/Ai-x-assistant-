package com.jarvis.ai.feature.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jarvis.ai.data.repository.httpMessage
import com.jarvis.ai.di.Graph
import com.jarvis.ai.ui.components.GlowButton
import com.jarvis.ai.ui.components.JarvisCore
import com.jarvis.ai.ui.components.JarvisField
import com.jarvis.ai.ui.theme.JarvisBlack
import com.jarvis.ai.ui.theme.JarvisCyan
import com.jarvis.ai.ui.theme.JarvisDanger
import com.jarvis.ai.ui.theme.JarvisMuted
import com.jarvis.ai.ui.theme.JarvisNavy
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    onLoggedIn: () -> Unit,
    onSignup: () -> Unit,
    onForgot: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var url by remember { mutableStateOf(Graph.session.apiBaseUrl) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }

    AuthShell {
        JarvisCore(size = 120.dp)
        Spacer(Modifier.height(8.dp))
        Text("JARVIS", color = JarvisCyan, fontSize = 28.sp, letterSpacing = 8.sp)
        Text("SECURE ACCESS", color = JarvisMuted, fontSize = 12.sp, letterSpacing = 4.sp)
        Spacer(Modifier.height(16.dp))
        JarvisField(url, { url = it }, "Backend URL")
        JarvisField(email, { email = it }, "Email", keyboardType = KeyboardType.Email)
        JarvisField(password, { password = it }, "Password", password = true)
        if (error != null) Text(error!!, color = JarvisDanger, fontSize = 13.sp)
        Spacer(Modifier.height(8.dp))
        GlowButton("AUTHENTICATE", loading = loading) {
            error = null
            if (email.isBlank() || password.length < 8) {
                error = "Email and 8+ character password required"
                return@GlowButton
            }
            loading = true
            Graph.session.apiBaseUrl = url
            scope.launch {
                try {
                    Graph.authRepo.login(email, password)
                    onLoggedIn()
                } catch (e: Exception) {
                    error = httpMessage(e)
                } finally {
                    loading = false
                }
            }
        }
        TextButton(onClick = onSignup) { Text("CREATE ACCOUNT", color = JarvisCyan) }
        TextButton(onClick = onForgot) { Text("FORGOT PASSWORD", color = JarvisMuted) }
        Text(
            "Passwords are hashed on the server. Never stored in the app as plain text.",
            color = JarvisMuted,
            fontSize = 11.sp
        )
    }
}

@Composable
fun SignupScreen(onLoggedIn: () -> Unit, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    var url by remember { mutableStateOf(Graph.session.apiBaseUrl) }
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    AuthShell {
        Text("CREATE IDENTITY", color = JarvisCyan, fontSize = 20.sp, letterSpacing = 4.sp)
        Spacer(Modifier.height(12.dp))
        JarvisField(url, { url = it }, "Backend URL")
        JarvisField(name, { name = it }, "Display name")
        JarvisField(email, { email = it }, "Email", keyboardType = KeyboardType.Email)
        JarvisField(password, { password = it }, "Password (min 8)", password = true)
        if (error != null) Text(error!!, color = JarvisDanger)
        GlowButton("SIGN UP", loading = loading) {
            error = null
            loading = true
            Graph.session.apiBaseUrl = url
            scope.launch {
                try {
                    Graph.authRepo.signup(email, password, name)
                    onLoggedIn()
                } catch (e: Exception) {
                    error = httpMessage(e)
                } finally {
                    loading = false
                }
            }
        }
        TextButton(onClick = onBack) { Text("BACK TO LOGIN", color = JarvisMuted) }
    }
}

@Composable
fun ForgotScreen(onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    var email by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var info by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var stage by remember { mutableStateOf(0) }
    var loading by remember { mutableStateOf(false) }
    AuthShell {
        Text("RESET ACCESS", color = JarvisCyan, fontSize = 20.sp, letterSpacing = 4.sp)
        Spacer(Modifier.height(12.dp))
        JarvisField(email, { email = it }, "Email", keyboardType = KeyboardType.Email)
        if (stage == 1) {
            JarvisField(code, { code = it }, "6-digit code")
            JarvisField(password, { password = it }, "New password", password = true)
        }
        if (info != null) Text(info!!, color = JarvisCyan, fontSize = 13.sp)
        if (error != null) Text(error!!, color = JarvisDanger, fontSize = 13.sp)
        GlowButton(if (stage == 0) "SEND CODE" else "RESET PASSWORD", loading = loading) {
            error = null
            loading = true
            scope.launch {
                try {
                    if (stage == 0) {
                        val res = Graph.authRepo.forgot(email)
                        info = res.message + (res.devCode?.let { " Dev code: $it" } ?: "")
                        stage = 1
                    } else {
                        Graph.authRepo.reset(email, code, password)
                        info = "Password updated. You can log in."
                    }
                } catch (e: Exception) {
                    error = httpMessage(e)
                } finally {
                    loading = false
                }
            }
        }
        TextButton(onClick = onBack) { Text("BACK", color = JarvisMuted) }
    }
}

@Composable
private fun AuthShell(content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(JarvisBlack, JarvisNavy, JarvisBlack)))
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        content()
    }
}
