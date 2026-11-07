package com.jarvis.ai.data.repository

import com.jarvis.ai.core.security.SessionStore
import com.jarvis.ai.data.remote.api.JarvisApi
import com.jarvis.ai.data.remote.dto.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.HttpException
import java.io.File

class AuthRepository(
    private val api: JarvisApi,
    private val session: SessionStore
) {
    private fun persist(res: AuthResponse) {
        session.saveAuth(
            res.accessToken,
            res.refreshToken,
            res.user.id,
            res.user.email,
            res.user.displayName,
            res.user.photoUrl,
            res.user.role,
            res.user.languagePref
        )
    }

    suspend fun login(email: String, password: String): UserDto {
        val res = api.login(AuthRequest(email.trim(), password))
        persist(res)
        return res.user
    }

    suspend fun signup(email: String, password: String, name: String): UserDto {
        val res = api.signup(AuthRequest(email.trim(), password, name.trim().ifBlank { null }))
        persist(res)
        return res.user
    }

    suspend fun me(): UserDto {
        val user = api.me().user
        session.displayName = user.displayName
        session.photoUrl = user.photoUrl ?: ""
        session.role = user.role
        session.languagePref = user.languagePref
        return user
    }

    suspend fun updateProfile(name: String, languagePref: String): UserDto {
        val user = api.updateProfile(ProfileUpdate(name, languagePref)).user
        session.displayName = user.displayName
        session.languagePref = user.languagePref
        return user
    }

    suspend fun changePassword(current: String, next: String) {
        api.changePassword(PasswordChange(current, next))
    }

    suspend fun forgot(email: String): ForgotResponse = api.forgot(ForgotRequest(email.trim()))

    suspend fun reset(email: String, code: String, password: String) {
        api.reset(ResetRequest(email.trim(), code.trim(), password))
    }

    suspend fun uploadPhoto(file: File): UserDto {
        val part = MultipartBody.Part.createFormData(
            "photo",
            file.name,
            file.asRequestBody("image/jpeg".toMediaType())
        )
        val user = api.uploadPhoto(part).user
        session.photoUrl = user.photoUrl ?: ""
        return user
    }

    suspend fun logout() {
        try {
            api.logout(LogoutRequest(session.refreshToken))
        } catch (_: Exception) {
        }
        session.clearAuth()
    }

    suspend fun health(): HealthPublic = api.health()
}

fun httpMessage(e: Throwable): String {
    return when (e) {
        is HttpException -> {
            val raw = e.response()?.errorBody()?.string()
            com.jarvis.ai.core.network.parseError(raw)
        }
        else -> e.message ?: "Network error"
    }
}
