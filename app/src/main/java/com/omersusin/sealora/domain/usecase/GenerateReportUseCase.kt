package com.omersusin.sealora.domain.usecase

import com.omersusin.sealora.domain.model.AiChatMessage
import com.omersusin.sealora.domain.model.WeatherData
import com.omersusin.sealora.domain.model.WeatherReport
import com.omersusin.sealora.domain.repository.AiRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GenerateReportUseCase @Inject constructor(
    private val aiRepository: AiRepository
) {
    suspend fun generate(city: String, weatherData: List<WeatherData>): Result<WeatherReport> {
        if (weatherData.isEmpty()) {
            return Result.failure(Exception("No weather data available for report generation"))
        }
        return aiRepository.generateReport(city, weatherData)
    }

    suspend fun chat(
        message: String,
        city: String,
        weatherContext: List<WeatherData>?
    ): Result<String> {
        if (message.isBlank()) {
            return Result.failure(IllegalArgumentException("Message cannot be empty"))
        }
        return aiRepository.chat(message.trim(), city, weatherContext)
    }

    suspend fun discoverUrl(url: String, city: String): Result<com.omersusin.sealora.domain.model.ProviderTemplate> {
        if (url.isBlank()) {
            return Result.failure(IllegalArgumentException("URL cannot be empty"))
        }
        return aiRepository.discoverUrlPattern(url.trim(), city.trim())
    }

    fun getChatHistory(city: String): Flow<List<AiChatMessage>> {
        return aiRepository.getChatHistory(city)
    }

    suspend fun saveChatMessage(city: String, message: AiChatMessage) {
        aiRepository.saveChatMessage(city, message)
    }

    suspend fun clearChatHistory(city: String) {
        aiRepository.clearChatHistory(city)
    }

    fun getActiveAiConfig() = aiRepository.getActiveAiConfig()

    fun getAllAiConfigs() = aiRepository.getAllAiConfigs()

    suspend fun saveAiConfig(config: com.omersusin.sealora.domain.model.AiConfig) {
        aiRepository.saveAiConfig(config)
    }

    suspend fun activateConfig(provider: com.omersusin.sealora.domain.model.AiProvider) {
        aiRepository.activateConfig(provider)
    }

    suspend fun deleteConfig(provider: com.omersusin.sealora.domain.model.AiProvider) {
        aiRepository.deleteConfig(provider)
    }
}
