package com.jarvis.ai.data.remote.api

import com.jarvis.ai.data.remote.dto.*
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*

interface JarvisApi {
    @GET("api/health")
    suspend fun health(): HealthPublic

    @POST("api/auth/signup")
    suspend fun signup(@Body body: AuthRequest): AuthResponse

    @POST("api/auth/login")
    suspend fun login(@Body body: AuthRequest): AuthResponse

    @POST("api/auth/refresh")
    suspend fun refresh(@Body body: RefreshRequest): AuthResponse

    @POST("api/auth/logout")
    suspend fun logout(@Body body: LogoutRequest): OkResponse

    @GET("api/auth/me")
    suspend fun me(): UserWrapper

    @PUT("api/auth/profile")
    suspend fun updateProfile(@Body body: ProfileUpdate): UserWrapper

    @POST("api/auth/change-password")
    suspend fun changePassword(@Body body: PasswordChange): OkResponse

    @POST("api/auth/forgot-password")
    suspend fun forgot(@Body body: ForgotRequest): ForgotResponse

    @POST("api/auth/reset-password")
    suspend fun reset(@Body body: ResetRequest): OkResponse

    @Multipart
    @POST("api/auth/photo")
    suspend fun uploadPhoto(@Part photo: MultipartBody.Part): UserWrapper

    @GET("api/conversations")
    suspend fun conversations(): ConversationsResponse

    @POST("api/conversations")
    suspend fun createConversation(@Body body: CreateConversation): ConversationWrapper

    @DELETE("api/conversations/{id}")
    suspend fun deleteConversation(@Path("id") id: String): OkResponse

    @DELETE("api/conversations")
    suspend fun clearConversations(): OkResponse

    @GET("api/conversations/{id}/messages")
    suspend fun messages(@Path("id") id: String): MessagesResponse

    @POST("api/conversations/{id}/messages")
    suspend fun sendMessage(@Path("id") id: String, @Body body: ChatRequest): ChatResponse

    @POST("api/messages")
    suspend fun send(@Body body: ChatRequest): ChatResponse

    @Multipart
    @POST("api/vision")
    suspend fun vision(
        @Part image: MultipartBody.Part,
        @Part("content") content: RequestBody,
        @Part("conversationId") conversationId: RequestBody?
    ): ChatResponse

    @GET("api/memories")
    suspend fun memories(): MemoriesResponse

    @POST("api/memories")
    suspend fun createMemory(@Body body: MemoryBody): MemoryWrapper

    @PUT("api/memories/{id}")
    suspend fun updateMemory(@Path("id") id: String, @Body body: MemoryBody): MemoryWrapper

    @DELETE("api/memories/{id}")
    suspend fun deleteMemory(@Path("id") id: String): OkResponse

    @POST("api/language/detect")
    suspend fun detect(@Body body: DetectRequest): DetectResponse

    @POST("api/coding/assist")
    suspend fun coding(@Body body: CodingRequest): CodingResponse

    @POST("api/coding/github")
    suspend fun github(@Body body: GithubRequest): GithubResponse

    @GET("api/admin/stats")
    suspend fun adminStats(): AdminStats

    @GET("api/admin/users")
    suspend fun adminUsers(@Query("q") q: String? = null): AdminUsers

    @PUT("api/admin/users/{id}/status")
    suspend fun adminStatus(@Path("id") id: String, @Body body: StatusBody): UserWrapper

    @GET("api/admin/health")
    suspend fun adminHealth(): HealthDto

    @GET("api/admin/config")
    suspend fun adminConfig(): ConfigResponse

    @PUT("api/admin/config")
    suspend fun adminUpdateConfig(@Body body: ConfigUpdate): ConfigResponse

    @GET("api/admin/usage")
    suspend fun adminUsage(): UsageResponse
}
