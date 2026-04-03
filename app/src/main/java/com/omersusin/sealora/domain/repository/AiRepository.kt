package com.omersusin.sealora.domain.repository

import com.omersusin.sealora.domain.model.*
import kotlinx.coroutines.flow.Flow

interface AiRepository {

    fun getActiveAiConfig(): Flow<AiConfig?>

    fun getAllAiConfigs(): Flow<List<AiConfig>>

    suspend fun saveAiConfig(config: AiConfig)

    suspend fun activateConfig(provider: AiProvider)

    suspend fun deleteConfig(provider: AiProvider)

    suspend fun generateReport(city: String, weatherData: List<WeatherData>): Result<WeatherReport>

    suspend fun chat(message: String, city: String, weatherContext: List<WeatherData>?): Result<String>

    suspend fun discoverUrlPattern(url: String, city: String): Result<ProviderTemplate>

    fun getChatHistory(city: String): Flow<List<AiChatMessage>>

    suspend fun saveChatMessage(city: String, message: AiChatMessage)

    suspend fun clearChatHistory(city: String)
}
