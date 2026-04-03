package com.omersusin.sealora.data.repository

import android.util.Log
import com.omersusin.sealora.data.local.AiConfigDao
import com.omersusin.sealora.data.local.ChatSessionDao
import com.omersusin.sealora.data.local.entity.AiConfigEntity
import com.omersusin.sealora.data.local.entity.ChatSessionEntity
import com.omersusin.sealora.data.remote.ai.AiService
import com.omersusin.sealora.domain.model.*
import com.omersusin.sealora.domain.repository.AiRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AiRepositoryImpl @Inject constructor(
    private val aiConfigDao: AiConfigDao,
    private val chatSessionDao: ChatSessionDao,
    private val aiService: AiService
) : AiRepository {

    companion object {
        private const val TAG = "AiRepo"
        private val json = Json { ignoreUnknownKeys = true }
    }

    override fun getActiveAiConfig(): Flow<AiConfig?> {
        return aiConfigDao.getActiveAiConfig().map { it?.toDomain() }
    }

    override fun getAllAiConfigs(): Flow<List<AiConfig>> {
        return aiConfigDao.getAllAiConfigs().map { entities -> entities.map { it.toDomain() } }
    }

    override suspend fun saveAiConfig(config: AiConfig) {
        aiConfigDao.insertAiConfig(config.toEntity())
    }

    override suspend fun activateConfig(provider: AiProvider) {
        aiConfigDao.deactivateAll()
        aiConfigDao.activateProvider(provider.name)
    }

    override suspend fun deleteConfig(provider: AiProvider) {
        aiConfigDao.deleteAiConfig(AiConfigEntity(provider = provider.name, apiKey = "", model = "", baseUrl = "", isActive = false))
    }

    override suspend fun generateReport(city: String, weatherData: List<WeatherData>): Result<WeatherReport> {
        return try {
            val config = aiConfigDao.getActiveAiConfig().first()?.toDomain()
                ?: return Result.failure(Exception("No active AI configuration found."))
            val aiResponse = aiService.generateWeatherReport(config, city, weatherData).getOrElse { return Result.failure(it) }
            val avgTemp = if (weatherData.isNotEmpty()) weatherData.map { it.temperature }.average() else 0.0
            val avgFeelsLike = if (weatherData.isNotEmpty()) weatherData.map { it.feelsLike }.average() else avgTemp
            val consistency = if (weatherData.size < 2) ConsistencyLevel.HIGH else {
                val maxDiff = weatherData.map { it.temperature }.maxOrNull()!! - weatherData.map { it.temperature }.minOrNull()!!
                when { maxDiff <= 2.0 -> ConsistencyLevel.HIGH; maxDiff <= 5.0 -> ConsistencyLevel.MODERATE; maxDiff <= 8.0 -> ConsistencyLevel.LOW; else -> ConsistencyLevel.CONFLICTING }
            }
            val commonCondition = weatherData.groupingBy { it.condition }.eachCount().maxByOrNull { it.value }?.key ?: "Unknown"
            val commonIcon = weatherData.groupingBy { it.conditionIcon }.eachCount().maxByOrNull { it.value }?.key ?: "🌡️"
            Result.success(WeatherReport(city = city, summary = aiResponse, currentTemp = avgTemp, feelsLike = avgFeelsLike, condition = commonCondition, conditionIcon = commonIcon, providerCount = weatherData.size, providerNames = weatherData.map { it.providerName }, consistency = consistency, hourlySummary = "", dailySummary = "", recommendations = emptyList(), alerts = emptyList(), aiModel = config.model))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun chat(message: String, city: String, weatherContext: List<WeatherData>?): Result<String> {
        return try {
            val config = aiConfigDao.getActiveAiConfig().first()?.toDomain()
                ?: return Result.failure(Exception("No active AI configuration found."))
            val history = getChatHistoryList(city)
            aiService.chat(config, message, city, weatherContext, history)
        } catch (e: Exception) { Result.failure(e) }
    }

    override suspend fun discoverUrlPattern(url: String, city: String): Result<ProviderTemplate> {
        return try {
            val config = aiConfigDao.getActiveAiConfig().first()?.toDomain()
                ?: return Result.failure(Exception("No active AI configuration found."))
            val response = aiService.discoverUrlPattern(config, url, city).getOrElse { return Result.failure(it) }
            Result.success(ProviderTemplate(providerId = UUID.randomUUID().toString(), urlPattern = url, selectors = ScrapingSelectors(), discoveredBy = "ai", confidence = 0.5))
        } catch (e: Exception) { Result.failure(e) }
    }

    override fun getChatHistory(city: String): Flow<List<AiChatMessage>> {
        return chatSessionDao.getAllSessions().map { sessions ->
            val session = sessions.find { it.city == city }
            session?.let { try { json.decodeFromString<List<AiChatMessage>>(it.messagesJson) } catch (e: Exception) { emptyList() } } ?: emptyList()
        }
    }

    override suspend fun saveChatMessage(city: String, message: AiChatMessage) {
        val existing = chatSessionDao.getLatestSessionForCity(city)
        val messages = existing?.let { try { json.decodeFromString<List<AiChatMessage>>(it.messagesJson).toMutableList() } catch (e: Exception) { mutableListOf() } } ?: mutableListOf()
        messages.add(message)
        chatSessionDao.insertSession(ChatSessionEntity(id = existing?.id ?: UUID.randomUUID().toString(), city = city, messagesJson = json.encodeToString(messages), createdAt = existing?.createdAt ?: System.currentTimeMillis()))
    }

    override suspend fun clearChatHistory(city: String) {
        chatSessionDao.getLatestSessionForCity(city)?.let { chatSessionDao.deleteSession(it.id) }
    }

    private suspend fun getChatHistoryList(city: String): List<AiChatMessage> {
        return try { getChatHistory(city).first() } catch (e: Exception) { emptyList() }
    }

    private fun AiConfig.toEntity(): AiConfigEntity = AiConfigEntity(provider = provider.name, apiKey = apiKey, model = model, baseUrl = baseUrl, isActive = isActive)

    private fun AiConfigEntity.toDomain(): AiConfig {
        val aiProvider = try { AiProvider.valueOf(provider) } catch (e: Exception) { AiProvider.OPENROUTER }
        return AiConfig(provider = aiProvider, apiKey = apiKey, model = model, baseUrl = baseUrl.ifBlank { aiProvider.defaultBaseUrl }, isActive = isActive)
    }
}
