package com.jarvis.ai.core.security

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.jarvis.ai.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SessionStore(context: Context) {
    private val prefs: SharedPreferences = try {
        val master = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "jarvis_secure_session",
            master,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    } catch (_: Exception) {
        context.getSharedPreferences("jarvis_session_fallback", Context.MODE_PRIVATE)
    }

    private val _loggedIn = MutableStateFlow(accessToken.isNotBlank())
    val loggedIn: StateFlow<Boolean> = _loggedIn.asStateFlow()

    var accessToken: String
        get() = prefs.getString("access", "") ?: ""
        set(value) { prefs.edit().putString("access", value).apply(); _loggedIn.value = value.isNotBlank() }

    var refreshToken: String
        get() = prefs.getString("refresh", "") ?: ""
        set(value) { prefs.edit().putString("refresh", value).apply() }

    var userId: String
        get() = prefs.getString("uid", "") ?: ""
        set(value) { prefs.edit().putString("uid", value).apply() }

    var email: String
        get() = prefs.getString("email", "") ?: ""
        set(value) { prefs.edit().putString("email", value).apply() }

    var displayName: String
        get() = prefs.getString("name", "") ?: ""
        set(value) { prefs.edit().putString("name", value).apply() }

    var photoUrl: String
        get() = prefs.getString("photo", "") ?: ""
        set(value) { prefs.edit().putString("photo", value).apply() }

    var role: String
        get() = prefs.getString("role", "user") ?: "user"
        set(value) { prefs.edit().putString("role", value).apply() }

    var languagePref: String
        get() = prefs.getString("lang", "auto") ?: "auto"
        set(value) { prefs.edit().putString("lang", value).apply() }

    var apiBaseUrl: String
        get() {
            val stored = prefs.getString("api", "") ?: ""
            return stored.ifBlank { BuildConfig.API_BASE_URL }.trimEnd('/') + "/"
        }
        set(value) {
            val normalized = value.trim().trimEnd('/') + "/"
            prefs.edit().putString("api", normalized).apply()
        }

    var githubToken: String
        get() = prefs.getString("gh", "") ?: ""
        set(value) { prefs.edit().putString("gh", value).apply() }

    var wakeWordEnabled: Boolean
        get() = prefs.getBoolean("wake", false)
        set(value) { prefs.edit().putBoolean("wake", value).apply() }

    var ttsEnabled: Boolean
        get() = prefs.getBoolean("tts", true)
        set(value) { prefs.edit().putBoolean("tts", value).apply() }

    var notificationsEnabled: Boolean
        get() = prefs.getBoolean("notif", true)
        set(value) { prefs.edit().putBoolean("notif", value).apply() }

    val isAdmin: Boolean get() = role == "admin"

    fun saveAuth(
        access: String,
        refresh: String,
        id: String,
        email: String,
        name: String,
        photo: String?,
        role: String,
        languagePref: String
    ) {
        prefs.edit()
            .putString("access", access)
            .putString("refresh", refresh)
            .putString("uid", id)
            .putString("email", email)
            .putString("name", name)
            .putString("photo", photo ?: "")
            .putString("role", role)
            .putString("lang", languagePref)
            .apply()
        _loggedIn.value = access.isNotBlank()
    }

    fun clearAuth() {
        val url = apiBaseUrl
        val wake = wakeWordEnabled
        val tts = ttsEnabled
        prefs.edit().clear().apply()
        apiBaseUrl = url
        wakeWordEnabled = wake
        ttsEnabled = tts
        _loggedIn.value = false
    }
}
