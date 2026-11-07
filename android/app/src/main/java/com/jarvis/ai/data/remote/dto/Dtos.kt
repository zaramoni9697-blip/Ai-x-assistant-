package com.jarvis.ai.data.remote.dto

data class AuthRequest(
    val email: String,
    val password: String,
    val displayName: String? = null
)

data class AuthResponse(
    val user: UserDto,
    val accessToken: String,
    val refreshToken: String,
    val expiresIn: String? = null
)

data class UserDto(
    val id: String,
    val email: String,
    val displayName: String,
    val photoUrl: String? = null,
    val role: String,
    val status: String,
    val languagePref: String,
    val createdAt: Long? = null,
    val lastActive: Long? = null,
    val lastLogin: Long? = null
)

data class UserWrapper(val user: UserDto)
data class RefreshRequest(val refreshToken: String)
data class LogoutRequest(val refreshToken: String? = null)
data class ProfileUpdate(val displayName: String, val languagePref: String)
data class PasswordChange(val currentPassword: String, val newPassword: String)
data class ForgotRequest(val email: String)
data class ForgotResponse(val ok: Boolean, val message: String? = null, val devCode: String? = null)
data class ResetRequest(val email: String, val code: String, val newPassword: String)
data class OkResponse(val ok: Boolean = true)

data class ConversationDto(
    val id: String,
    val title: String,
    val createdAt: Long? = null,
    val updatedAt: Long? = null
)

data class ConversationsResponse(val conversations: List<ConversationDto>)
data class ConversationWrapper(val conversation: ConversationDto)
data class CreateConversation(val title: String? = null)

data class MessageDto(
    val id: String,
    val role: String,
    val content: String,
    val language: String? = null,
    val createdAt: Long? = null
)

data class MessagesResponse(
    val conversation: ConversationDto? = null,
    val messages: List<MessageDto>
)

data class ChatRequest(
    val content: String,
    val conversationId: String? = null,
    val mode: String? = "chat"
)

data class DetectionDto(
    val language: String? = null,
    val label: String? = null,
    val dominant: String? = null,
    val mixed: Boolean? = null,
    val confidence: Double? = null
)

data class ChatResponse(
    val conversationId: String,
    val userMessage: MessageDto,
    val assistantMessage: MessageDto,
    val detection: DetectionDto? = null
)

data class MemoryDto(
    val id: String,
    val title: String,
    val content: String,
    val createdAt: Long? = null,
    val updatedAt: Long? = null
)

data class MemoriesResponse(val memories: List<MemoryDto>)
data class MemoryWrapper(val memory: MemoryDto)
data class MemoryBody(val title: String, val content: String)

data class CodingRequest(
    val prompt: String,
    val code: String = "",
    val task: String = "assist"
)

data class CodingResponse(
    val result: String,
    val detection: DetectionDto? = null,
    val task: String? = null,
    val notice: String? = null
)

data class GithubRequest(
    val repo: String,
    val githubToken: String,
    val prompt: String? = null
)

data class GithubResponse(
    val result: String,
    val notice: String? = null
)

data class DetectRequest(val text: String)
data class DetectResponse(val detection: DetectionDto)

data class AdminStats(
    val users: Int = 0,
    val active24h: Int = 0,
    val active7d: Int = 0,
    val suspended: Int = 0,
    val messages: Int = 0,
    val tokens: Int = 0,
    val chatsToday: Int = 0,
    val privacy: String? = null
)

data class AdminUsers(val users: List<UserDto>)
data class StatusBody(val status: String)
data class RoleBody(val role: String, val confirm: String = "ASSIGN_ROLE")
data class HealthDto(
    val ok: Boolean = false,
    val uptimeSec: Int = 0,
    val gemini: String? = null,
    val node: String? = null,
    val env: String? = null
)
data class ConfigResponse(
    val config: Map<String, String> = emptyMap(),
    val secrets: Map<String, Any?> = emptyMap()
)
data class ConfigUpdate(val config: Map<String, String>)
data class UsageResponse(
    val byType: List<Map<String, Any>> = emptyList(),
    val note: String? = null
)

data class ErrorBody(val error: String? = null)
data class HealthPublic(val ok: Boolean, val name: String? = null, val gemini: String? = null)
