package com.jarvis.ai.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.jarvis.ai.data.local.dao.ConversationDao
import com.jarvis.ai.data.local.dao.MemoryDao
import com.jarvis.ai.data.local.dao.MessageDao
import com.jarvis.ai.data.local.entity.ConversationEntity
import com.jarvis.ai.data.local.entity.MemoryEntity
import com.jarvis.ai.data.local.entity.MessageEntity

@Database(
    entities = [ConversationEntity::class, MessageEntity::class, MemoryEntity::class],
    version = 1,
    exportSchema = false
)
abstract class JarvisDatabase : RoomDatabase() {
    abstract fun conversations(): ConversationDao
    abstract fun messages(): MessageDao
    abstract fun memories(): MemoryDao

    companion object {
        fun create(context: Context): JarvisDatabase =
            Room.databaseBuilder(context, JarvisDatabase::class.java, "jarvis.db")
                .fallbackToDestructiveMigration()
                .build()
    }
}
