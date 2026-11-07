package com.jarvis.ai.di

import android.content.Context
import com.jarvis.ai.core.network.createApi
import com.jarvis.ai.core.security.SessionStore
import com.jarvis.ai.core.voice.TtsManager
import com.jarvis.ai.data.local.JarvisDatabase
import com.jarvis.ai.data.remote.api.JarvisApi
import com.jarvis.ai.data.repository.AuthRepository
import com.jarvis.ai.data.repository.ChatRepository
import com.jarvis.ai.data.repository.MemoryRepository

object Graph {
    lateinit var appContext: Context
        private set
    lateinit var session: SessionStore
        private set
    lateinit var db: JarvisDatabase
        private set
    lateinit var api: JarvisApi
        private set
    lateinit var authRepo: AuthRepository
        private set
    lateinit var chatRepo: ChatRepository
        private set
    lateinit var memoryRepo: MemoryRepository
        private set
    lateinit var tts: TtsManager
        private set

    fun init(context: Context) {
        appContext = context.applicationContext
        session = SessionStore(appContext)
        db = JarvisDatabase.create(appContext)
        api = createApi(session)
        authRepo = AuthRepository(api, session)
        chatRepo = ChatRepository(api, db, session)
        memoryRepo = MemoryRepository(api, db, session)
        tts = TtsManager(appContext)
    }
}
