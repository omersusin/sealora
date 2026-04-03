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
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
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
    }

    override fun getActiveAiConfig(): Flow<AiConfig?> {
        return aiConfigDao.getActiveAiConfig().map { entity ->
            entity?.toDomain()
        }
    }

    override fun getAllAiConfigs(): Flow<List<AiConfig>> {
        return aiConfigDao.getAllAiConfigs().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun saveAiConfig(config: AiConfig) {
        val entity = config.toEntity()
        aiConfigDao.insertAiConfig(entity)
        Log.d(TAG, "AI config saved: ${config.provider.name}")
    }

    override suspend fun activateConfig(provider: AiProvider) {
        aiConfigDao.deactivateAll()
        aiConfigDao.activateProvider(provider.name)
        Log.d(TAG, "AI provider activated: ${provider.name}")
    }

    override suspend fun deleteConfig(provider: AiProvider) {
        val configs = mutableListOf<AiConfig>()
        aiConfigDao.getAllAiConfigs().collect { list ->
            configs.addAll(list.map { it.toDomain() })
        }
        val config = configs.find { it.provider == provider }
        config?.let {
            aiConfigDao.deleteAiConfig(it.toEntity())
            Log.d(TAG, "AI config deleted: ${provider.name}")
        }
    }

    override suspend fun generateReport(
        city: String,
        weatherData: List<WeatherData>
    ): Result<WeatherReport> {
        return try {
            val config = getActiveConfig()
                ?: return Result.failure(Exception("No active AI configuration found. Please set up an AI provider in settings."))

            val aiResponse = aiService.generateWeatherReport(config, city, weatherData)
                .getOrElse { return Result.failure(it) }

            val report = parseReportResponse(aiResponse, city, weatherData, config)
            Result.success(report)
        } catch (e: Exception) {
            Log.e(TAG, "Error generating report: ${e.message}")
            Result.failure(e)
        }
    }

    override suspend fun chat(
        message: String,
        city: String,
        weatherContext: List<WeatherData>?
    ): Result<String> {
        return try {
            val config = getActiveConfig()
                ?: return Result.failure(Exception("No active AI configuration found."))

            val chatHistory = getChatHistoryList(city)
            aiService.chat(config, message, city, weatherContext, chatHistory)
        } catch (e: Exception) {
            Log.e(TAG, "Error in chat: ${e.message}")
            Result.failure(e)
        }
    }

    override suspend fun discoverUrlPattern(
        url: String,
        city: String
    ): Result<ProviderTemplate> {
        return try {
            val config = getActiveConfig()
                ?: return Result.failure(Exception("No active AI configuration found."))

            val aiResponse = aiService.discoverUrlPattern(config, url, city)
                .getOrElse { return Result.failure(it) }

            val template = parseTemplateResponse(aiResponse, url)
            Result.success(template)
        } catch (e: Exception) {
            Log.e(TAG, "Error discovering URL: ${e.message}")
            Result.failure(e)
        }
    }

    override fun getChatHistory(city: String): Flow<List<AiChatMessage>> {
        return chatSessionDao.getAllSessions().map { sessions ->
            val session = sessions.find { it.city == city }
            session?.let {
                try {
                    Json.decodeFromString<List<AiChatMessage>>(it.messagesJson)
                } catch (e: Exception) {
                    emptyList()
                }
            } ?: emptyList()
        }
    }

    override suspend fun saveChatMessage(city: String, message: AiChatMessage) {
        val existing = chatSessionDao.getLatestSessionForCity(city)
        val messages = existing?.let {
            try {
                Json.decodeFromString<List<AiChatMessage>>(it.messagesJson).toMutableList()
            } catch (e: Exception) {
                mutableListOf()
            }
        } ?: mutableListOf()

        messages.add(message)

        val session = ChatSessionEntity(
            id = existing?.id ?: UUID.randomUUID().toString(),
            city = city,
            messagesJson = Json.encodeToString(messages),
            createdAt = existing?.createdAt ?: System.currentTimeMillis()
        )
        chatSessionDao.insertSession(session)
    }

    override suspend fun clearChatHistory(city: String) {
        val existing = chatSessionDao.getLatestSessionForCity(city)
        existing?.let {
            chatSessionDao.deleteSession(it.id)
        }
    }

    private suspend fun getActiveConfig(): AiConfig? {
        var config: AiConfig? = null
        aiConfigDao.getActiveAiConfig().collect { entity ->
            config = entity?.toDomain()
        }
        return config
    }

    private suspend fun getChatHistoryList(city: String): List<AiChatMessage> {
        var history = listOf<AiChatMessage>()
        getChatHistory(city).collect {
            history = it
        }
        return history
    }

    private fun parseReportResponse(
        response: String,
        city: String,
        weatherData: List<WeatherData>,
        config: AiConfig
    ): WeatherReport {
        val avgTemp = if (weatherData.isNotEmpty()) {
            weatherData.map { it.temperature }.average()
        } else 0.0

        val avgFeelsLike = if (weatherData.isNotEmpty()) {
            weatherData.map { it.feelsLike }.average()
        } else avgTemp

        val consistency = calculateConsistency(weatherData)

        val commonCondition = weatherData
            .groupingBy { it.condition }
            .eachCount()
            .maxByOrNull { it.value }?.key ?: "Unknown"

        val commonIcon = weatherData
            .groupingBy { it.conditionIcon }
            .eachCount()
            .maxByOrNull { it.value }?.key ?: "🌡️"

        return WeatherReport(
            city = city,
            summary = response,
            currentTemp = avgTemp,
            feelsLike = avgFeelsLike,
            condition = commonCondition,
            conditionIcon = commonIcon,
            providerCount = weatherData.size,
            providerNames = weatherData.map { it.providerName },
            consistency = consistency,
            hourlySummary = extractSection(response, "SAATLİK", "GÜNLÜK"),
            dailySummary = extractSection(response, "GÜNLÜK", "ÖNERİLER"),
            recommendations = extractListItems(response, "ÖNERİLER"),
            alerts = extractAlerts(response, weatherData),
            aiModel = config.model
        )
    }

    private fun parseTemplateResponse(response: String, url: String): ProviderTemplate {
        return try {
            val jsonStart = response.indexOf('{')
            val jsonEnd = response.lastIndexOf('}') + 1
            if (jsonStart >= 0 && jsonEnd > jsonStart) {
                val jsonStr = response.substring(jsonStart, jsonEnd)
                val json = Json.parseToJsonElement(jsonStr).jsonObject

                val selectors = json["selectors"]?.jsonObject

                ProviderTemplate(
                    providerId = UUID.randomUUID().toString(),
                    urlPattern = json["urlTemplate"]?.jsonPrimitive?.content ?: url,
                    selectors = ScrapingSelectors(
                        temperature = selectors?.get("temperature")?.jsonPrimitive?.content ?: "",
                        humidity = selectors?.get("humidity")?.jsonPrimitive?.content ?: "",
                        windSpeed = selectors?.get("windSpeed")?.jsonPrimitive?.content ?: "",
                        condition = selectors?.get("condition")?.jsonPrimitive?.content ?: "",
                        feelsLike = selectors?.get("feelsLike")?.jsonPrimitive?.content ?: "",
                        pressure = selectors?.get("pressure")?.jsonPrimitive?.content ?: ""
                    ),
                    discoveredBy = "ai",
                    confidence = json["confidence"]?.jsonPrimitive?.double ?: 0.5
                )
            } else {
                ProviderTemplate(
                    providerId = UUID.randomUUID().toString(),
                    urlPattern = url,
                    selectors = ScrapingSelectors(),
                    discoveredBy = "ai",
                    confidence = 0.3
                )
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse AI template response: ${e.message}")
            ProviderTemplate(
                providerId = UUID.randomUUID().toString(),
                urlPattern = url,
                selectors = ScrapingSelectors(),
                discoveredBy = "ai",
                confidence = 0.2
            )
        }
    }

    private fun calculateConsistency(data: List<WeatherData>): ConsistencyLevel {
        if (data.size < 2) return ConsistencyLevel.HIGH

        val temps = data.map { it.temperature }
        val maxDiff = temps.maxOrNull()!! - temps.minOrNull()!!

        return when {
            maxDiff <= 2.0 -> ConsistencyLevel.HIGH
            maxDiff <= 5.0 -> ConsistencyLevel.MODERATE
            maxDiff <= 8.0 -> ConsistencyLevel.LOW
            else -> ConsistencyLevel.CONFLICTING
        }
    }

    private fun extractSection(text: String, startMarker: String, endMarker: String): String {
        val start = text.indexOf(startMarker, ignoreCase = true)
        if (start < 0) return ""
        val end = text.indexOf(endMarker, start + startMarker.length, ignoreCase = true)
        val section = if (end > start) {
            text.substring(start, end)
        } else {
            text.substring(start)
        }
        return section.trim()
    }

    private fun extractListItems(text: String, sectionName: String): List<String> {
        val section = extractSection(text, sectionName, "")
        val lines = section.split("\n")
        return lines.filter { line ->
            line.trim().startsWith("-") ||
            line.trim().startsWith("•") ||
            line.trim().startsWith("*") ||
            Regex("""^\d+[.)]""").containsMatchIn(line.trim())
        }.map { it.trim().replace(Regex("""^[-•*\d.)\s]+"""), "").trim() }
    }

    private fun extractAlerts(
        text: String,
        weatherData: List<WeatherData>
    ): List<WeatherAlert> {
        val alerts = mutableListOf<WeatherAlert>()

        val avgTemp = weatherData.map { it.temperature }.average()
        if (avgTemp > 35) {
            alerts.add(
                WeatherAlert(
                    type = AlertType.TEMPERATURE,
                    title = "🌡️ Yüksek Sıcaklık Uyarısı",
                    description = "Ortalama sıcaklık ${avgTemp.toInt()}°C. Bol su için ve gölgede kalın.",
                    severity = 4
                )
            )
        }
        if (avgTemp < 0) {
            alerts.add(
                WeatherAlert(
                    type = AlertType.TEMPERATURE,
                    title = "❄️ Dondurucu Soğuk Uyarısı",
                    description = "Sıcaklık ${avgTemp.toInt()}°C. Kalın giyinin.",
                    severity = 4
                )
            )
        }

        val avgWind = weatherData.map { it.windSpeed }.average()
        if (avgWind > 50) {
            alerts.add(
                WeatherAlert(
                    type = AlertType.WIND,
                    title = "💨 Kuvvetli Rüzgar Uyarısı",
                    description = "Rüzgar hızı ${avgWind.toInt()} km/h.",
                    severity = 3
                )
            )
        }

        val avgUv = weatherData.map { it.uvIndex }.average()
        if (avgUv > 8) {
            alerts.add(
                WeatherAlert(
                    type = AlertType.UV,
                    title = "☀️ Yüksek UV İndeksi",
                    description = "UV indeksi ${avgUv.toInt()}. Güneş kremi kullanın.",
                    severity = 3
                )
            )
        }

        return alerts
    }

    private fun AiConfig.toEntity(): AiConfigEntity {
        return AiConfigEntity(
            provider = provider.name,
            apiKey = apiKey,
            model = model,
            baseUrl = baseUrl,
            isActive = isActive
        )
    }

    private fun AiConfigEntity.toDomain(): AiConfig {
        val aiProvider = try {
            AiProvider.valueOf(provider)
        } catch (e: Exception) {
            AiProvider.OPENROUTER
        }
        return AiConfig(
            provider = aiProvider,
            apiKey = apiKey,
            model = model,
            baseUrl = baseUrl.ifBlank { aiProvider.defaultBaseUrl },
            isActive = isActive
        )
    }
}
