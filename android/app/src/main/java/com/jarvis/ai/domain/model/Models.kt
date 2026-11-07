package com.jarvis.ai.domain.model

data class UserProfile(
    val id: String,
    val email: String,
    val displayName: String,
    val photoUrl: String?,
    val role: String,
    val languagePref: String
)

data class ChatTurn(
    val id: String,
    val role: String,
    val content: String,
    val language: String?,
    val createdAt: Long
)

data class MemoryItem(
    val id: String,
    val title: String,
    val content: String,
    val updatedAt: Long
)
