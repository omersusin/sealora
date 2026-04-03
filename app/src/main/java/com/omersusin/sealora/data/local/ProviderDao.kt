package com.omersusin.sealora.data.local

import androidx.room.*
import com.omersusin.sealora.data.local.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ProviderDao {

    @Query("SELECT * FROM providers WHERE isActive = 1 ORDER BY isBuiltIn DESC, name ASC")
    fun getActiveProviders(): Flow<List<ProviderEntity>>

    @Query("SELECT * FROM providers ORDER BY isBuiltIn DESC, name ASC")
    fun getAllProviders(): Flow<List<ProviderEntity>>

    @Query("SELECT * FROM providers WHERE id = :id")
    suspend fun getProviderById(id: String): ProviderEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProvider(provider: ProviderEntity)

    @Update
    suspend fun updateProvider(provider: ProviderEntity)

    @Delete
    suspend fun deleteProvider(provider: ProviderEntity)

    @Query("SELECT COUNT(*) FROM providers WHERE isBuiltIn = 0")
    fun getUserProviderCount(): Flow<Int>

    @Query("UPDATE providers SET isActive = :active WHERE id = :id")
    suspend fun setProviderActive(id: String, active: Boolean)
}

@Dao
interface AiConfigDao {

    @Query("SELECT * FROM ai_configs WHERE isActive = 1 LIMIT 1")
    fun getActiveAiConfig(): Flow<AiConfigEntity?>

    @Query("SELECT * FROM ai_configs")
    fun getAllAiConfigs(): Flow<List<AiConfigEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAiConfig(config: AiConfigEntity)

    @Query("UPDATE ai_configs SET isActive = 0")
    suspend fun deactivateAll()

    @Query("UPDATE ai_configs SET isActive = 1 WHERE provider = :provider")
    suspend fun activateProvider(provider: String)

    @Delete
    suspend fun deleteAiConfig(config: AiConfigEntity)
}

@Dao
interface ChatSessionDao {

    @Query("SELECT * FROM chat_sessions ORDER BY createdAt DESC")
    fun getAllSessions(): Flow<List<ChatSessionEntity>>

    @Query("SELECT * FROM chat_sessions WHERE city = :city ORDER BY createdAt DESC LIMIT 1")
    suspend fun getLatestSessionForCity(city: String): ChatSessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: ChatSessionEntity)

    @Query("DELETE FROM chat_sessions WHERE id = :id")
    suspend fun deleteSession(id: String)
}

@Dao
interface CachedWeatherDao {

    @Query("SELECT * FROM cached_weather WHERE city = :city")
    suspend fun getCachedForCity(city: String): List<CachedWeatherEntity>

    @Query("DELETE FROM cached_weather WHERE fetchedAt < :timestamp")
    suspend fun clearOldCache(timestamp: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCache(cache: CachedWeatherEntity)

    @Query("DELETE FROM cached_weather WHERE city = :city AND providerId = :providerId")
    suspend fun clearCacheForProvider(city: String, providerId: String)
}
