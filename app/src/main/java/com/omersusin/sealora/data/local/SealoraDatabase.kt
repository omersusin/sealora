package com.omersusin.sealora.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.omersusin.sealora.data.local.entity.*

@Database(
    entities = [
        ProviderEntity::class,
        AiConfigEntity::class,
        ChatSessionEntity::class,
        CachedWeatherEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class SealoraDatabase : RoomDatabase() {
    abstract fun providerDao(): ProviderDao
    abstract fun aiConfigDao(): AiConfigDao
    abstract fun chatSessionDao(): ChatSessionDao
    abstract fun cachedWeatherDao(): CachedWeatherDao

    companion object {
        const val DATABASE_NAME = "sealora_db"
    }
}
