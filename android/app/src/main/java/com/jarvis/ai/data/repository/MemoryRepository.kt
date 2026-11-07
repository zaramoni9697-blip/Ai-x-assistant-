package com.jarvis.ai.data.repository

import com.jarvis.ai.core.security.SessionStore
import com.jarvis.ai.data.local.JarvisDatabase
import com.jarvis.ai.data.local.entity.MemoryEntity
import com.jarvis.ai.data.remote.api.JarvisApi
import com.jarvis.ai.data.remote.dto.MemoryBody
import kotlinx.coroutines.flow.Flow

class MemoryRepository(
    private val api: JarvisApi,
    private val db: JarvisDatabase,
    private val session: SessionStore
) {
    fun observe(): Flow<List<MemoryEntity>> = db.memories().observe(session.userId)

    suspend fun refresh() {
        val remote = api.memories().memories
        db.memories().upsertAll(
            remote.map {
                MemoryEntity(
                    it.id, session.userId, it.title, it.content,
                    it.updatedAt ?: System.currentTimeMillis()
                )
            }
        )
    }

    suspend fun create(title: String, content: String) {
        val m = api.createMemory(MemoryBody(title, content)).memory
        db.memories().upsert(
            MemoryEntity(m.id, session.userId, m.title, m.content, m.updatedAt ?: System.currentTimeMillis())
        )
    }

    suspend fun update(id: String, title: String, content: String) {
        val m = api.updateMemory(id, MemoryBody(title, content)).memory
        db.memories().upsert(
            MemoryEntity(m.id, session.userId, m.title, m.content, m.updatedAt ?: System.currentTimeMillis())
        )
    }

    suspend fun delete(id: String) {
        api.deleteMemory(id)
        db.memories().delete(id)
    }
}
