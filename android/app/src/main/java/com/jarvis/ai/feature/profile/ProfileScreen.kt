package com.jarvis.ai.feature.profile

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.jarvis.ai.data.repository.httpMessage
import com.jarvis.ai.di.Graph
import com.jarvis.ai.ui.components.GlowButton
import com.jarvis.ai.ui.components.JarvisField
import com.jarvis.ai.ui.components.JarvisScaffold
import com.jarvis.ai.ui.theme.JarvisCyan
import com.jarvis.ai.ui.theme.JarvisMuted
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun ProfileScreen(onBack: () -> Unit, onLoggedOut: () -> Unit) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var name by remember { mutableStateOf(Graph.session.displayName) }
    var current by remember { mutableStateOf("") }
    var next by remember { mutableStateOf("") }
    var info by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    val photo = Graph.session.photoUrl.let { p ->
        if (p.isBlank()) null else Graph.session.apiBaseUrl.trimEnd('/') + p
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        val file = File(context.cacheDir, "avatar.jpg")
        context.contentResolver.openInputStream(uri)?.use { inn ->
            file.outputStream().use { out -> inn.copyTo(out) }
        }
        scope.launch {
            try {
                Graph.authRepo.uploadPhoto(file)
                info = "Photo updated"
            } catch (e: Exception) { error = httpMessage(e) }
        }
    }

    JarvisScaffold(title = "PROFILE", onBack = onBack) {
        if (photo != null) {
            AsyncImage(photo, contentDescription = "Photo", modifier = Modifier.size(96.dp).clip(CircleShape), contentScale = ContentScale.Crop)
        }
        Text(Graph.session.email, color = JarvisCyan)
        Text("Role: ${Graph.session.role}  (assigned on server)", color = JarvisMuted, fontSize = 12.sp)
        Spacer(Modifier.height(8.dp))
        JarvisField(name, { name = it }, "Display name")
        GlowButton("SAVE PROFILE") {
            scope.launch {
                try {
                    Graph.authRepo.updateProfile(name, Graph.session.languagePref)
                    info = "Saved"
                } catch (e: Exception) { error = httpMessage(e) }
            }
        }
        GlowButton("CHANGE PHOTO") { picker.launch("image/*") }
        Spacer(Modifier.height(12.dp))
        JarvisField(current, { current = it }, "Current password", password = true)
        JarvisField(next, { next = it }, "New password", password = true)
        GlowButton("UPDATE PASSWORD") {
            scope.launch {
                try {
                    Graph.authRepo.changePassword(current, next)
                    info = "Password changed. Please log in again."
                    Graph.authRepo.logout()
                    onLoggedOut()
                } catch (e: Exception) { error = httpMessage(e) }
            }
        }
        if (info != null) Text(info!!, color = JarvisCyan)
        if (error != null) Text(error!!, color = com.jarvis.ai.ui.theme.JarvisDanger)
        Spacer(Modifier.height(16.dp))
        GlowButton("LOG OUT") {
            scope.launch {
                Graph.authRepo.logout()
                onLoggedOut()
            }
        }
    }
}
