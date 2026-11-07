package com.jarvis.ai.core.network

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.jarvis.ai.core.security.SessionStore
import com.jarvis.ai.data.remote.api.JarvisApi
import com.jarvis.ai.data.remote.dto.RefreshRequest
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.runBlocking

fun createGson(): Gson = GsonBuilder().serializeNulls().create()

fun createApi(session: SessionStore): JarvisApi {
    val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BASIC
    }
    val auth = Interceptor { chain ->
        val req = chain.request()
        val path = req.url.encodedPath
        val skip = path.contains("/api/auth/login") ||
            path.contains("/api/auth/signup") ||
            path.contains("/api/auth/refresh") ||
            path.contains("/api/auth/forgot") ||
            path.contains("/api/auth/reset") ||
            path.contains("/api/health")
        val b = req.newBuilder()
        if (!skip && session.accessToken.isNotBlank()) {
            b.header("Authorization", "Bearer ${session.accessToken}")
        }
        b.header("Accept", "application/json")
        chain.proceed(b.build())
    }

    val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val original = chain.request()
            val current = session.apiBaseUrl
            val uri = runCatching { java.net.URI(current) }.getOrNull()
            if (uri == null || uri.host.isNullOrBlank()) {
                return@addInterceptor chain.proceed(original)
            }
            val host = uri.host
            val port = when {
                uri.port > 0 -> uri.port
                (uri.scheme ?: "http") == "https" -> 443
                else -> 80
            }
            val newUrl = original.url.newBuilder()
                .scheme(uri.scheme ?: "http")
                .host(host)
                .port(port)
                .build()
            chain.proceed(original.newBuilder().url(newUrl).build())
        }
        .addInterceptor(auth)
        .addInterceptor(logging)
        .authenticator { _, response ->
            if (response.request.url.encodedPath.contains("/api/auth/refresh")) return@authenticator null
            if (session.refreshToken.isBlank()) return@authenticator null
            synchronized(session) {
                try {
                    val refreshApi = Retrofit.Builder()
                        .baseUrl(session.apiBaseUrl)
                        .addConverterFactory(GsonConverterFactory.create(createGson()))
                        .client(OkHttpClient.Builder().connectTimeout(20, TimeUnit.SECONDS).build())
                        .build()
                        .create(JarvisApi::class.java)
                    val refreshed = kotlinx.coroutines.runBlocking {
                        refreshApi.refresh(RefreshRequest(session.refreshToken))
                    }
                    session.saveAuth(
                        refreshed.accessToken,
                        refreshed.refreshToken,
                        refreshed.user.id,
                        refreshed.user.email,
                        refreshed.user.displayName,
                        refreshed.user.photoUrl,
                        refreshed.user.role,
                        refreshed.user.languagePref
                    )
                    response.request.newBuilder()
                        .header("Authorization", "Bearer ${refreshed.accessToken}")
                        .build()
                } catch (_: Exception) {
                    null
                }
            }
        }
        .build()

    return Retrofit.Builder()
        .baseUrl(session.apiBaseUrl)
        .client(client)
        .addConverterFactory(GsonConverterFactory.create(createGson()))
        .build()
        .create(JarvisApi::class.java)
}

fun parseError(raw: String?): String {
    if (raw.isNullOrBlank()) return "Something went wrong"
    return try {
        val err = createGson().fromJson(raw, com.jarvis.ai.data.remote.dto.ErrorBody::class.java)
        err.error ?: raw
    } catch (_: Exception) {
        raw
    }
}
