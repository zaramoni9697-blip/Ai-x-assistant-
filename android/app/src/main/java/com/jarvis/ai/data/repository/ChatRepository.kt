package com.jarvis.ai.data.repository

import com.jarvis.ai.core.security.SessionStore
import com.jarvis.ai.data.local.JarvisDatabase
import com.jarvis.ai.data.local.entity.ConversationEntity
import com.jarvis.ai.data.local.entity.MessageEntity
import com.jarvis.ai.data.remote.api.JarvisApi
import com.jarvis.ai.data.remote.dto.ChatRequest
import com.jarvis.ai.data.remote.dto.ChatResponse
import com.jarvis.ai.data.remote.dto.CreateConversation
import kotlinx.coroutines.flow.Flow
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

class ChatRepository(
    private val api: JarvisApi,
    private val db: JarvisDatabase,
    private val session: SessionStore
) {
    fun conversations(): Flow<List<ConversationEntity>> =
        db.conversations().observe(session.userId)

    fun messages(cid: String): Flow<List<MessageEntity>> = db.messages().observe(cid)

    suspend fun refreshConversations() {
        val remote = api.conversations().conversations
        db.conversations().upsertAll(
            remote.map {
                ConversationEntity(
                    it.id,
                    session.userId,
                    it.title,
                    it.updatedAt ?: System.currentTimeMillis()
                )
            }
        )
    }

    suspend fun loadMessages(cid: String) {
        val remote = api.messages(cid).messages
        db.messages().upsertAll(
            remote.map {
                MessageEntity(
                    it.id, cid, session.userId, it.role, it.content,
                    it.language, it.createdAt ?: System.currentTimeMillis()
                )
            }
        )
    }

    suspend fun ensureConversation(title: String = "New conversation"): String {
        val created = api.createConversation(CreateConversation(title)).conversation
        db.conversations().upsert(
            ConversationEntity(created.id, session.userId, created.title, System.currentTimeMillis())
        )
        return created.id
    }

    suspend fun send(conversationId: String, text: String, mode: String = "chat"): ChatResponse {
        val res = api.sendMessage(conversationId, ChatRequest(text, conversationId, mode))
        cache(res)
        return res
    }

    suspend fun sendNew(text: String, mode: String = "chat"): ChatResponse {
        val res = api.send(ChatRequest(text, null, mode))
        cache(res)
        return res
    }

    suspend fun sendImage(conversationId: String?, file: File, prompt: String): ChatResponse {
        val image = MultipartBody.Part.createFormData(
            "image",
            file.name,
            file.asRequestBody("image/jpeg".toMediaType())
        )
        val content = prompt.toRequestBody("text/plain".toMediaType())
        val cid = conversationId?.toRequestBody("text/plain".toMediaType())
        val res = api.vision(image, content, cid)
        cache(res)
        return res
    }

    suspend fun deleteConversation(id: String) {
        api.deleteConversation(id)
        db.messages().deleteForConversation(id)
        db.conversations().delete(id)
    }

    suspend fun clearAll() {
        api.clearConversations()
        db.messages().clear(session.userId)
        db.conversations().clear(session.userId)
    }

    private suspend fun cache(res: ChatResponse) {
        val t = System.currentTimeMillis()
        db.conversations().upsert(
            ConversationEntity(res.conversationId, session.userId, res.userMessage.content.take(48), t)
        )
        db.messages().upsert(
            MessageEntity(
                res.userMessage.id, res.conversationId, session.userId, "user",
                res.userMessage.content, res.userMessage.language, res.userMessage.createdAt ?: t
            )
        )
        db.messages().upsert(
            MessageEntity(
                res.assistantMessage.id, res.conversationId, session.userId, "assistant",
                res.assistantMessage.content, res.assistantMessage.language,
                res.assistantMessage.createdAt ?: t
            )
        )
    }
}
