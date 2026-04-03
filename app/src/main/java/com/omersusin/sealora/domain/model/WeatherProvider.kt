package com.omersusin.sealora.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class WeatherProvider(
    val id: String,
    val name: String,
    val baseUrl: String,
    val urlTemplate: String,
    val type: ProviderType,
    val isActive: Boolean = true,
    val requiresApiKey: Boolean = false,
    val apiKey: String = "",
    val language: String = "en",
    val isBuiltIn: Boolean = false
)

@Serializable
enum class ProviderType {
    BUILTIN,      // Open-Meteo gibi
    API,          // WeatherAPI gibi
    SCRAPED,      // Kullanıcı eklediği link
    AI_DISCOVERED // AI'ın keşfettiği şablon
}

@Serializable
data class ProviderTemplate(
    val providerId: String,
    val urlPattern: String,
    val selectors: ScrapingSelectors,
    val discoveredBy: String = "user",
    val confidence: Double = 0.0
)

@Serializable
data class ScrapingSelectors(
    val temperature: String = "",
    val humidity: String = "",
    val windSpeed: String = "",
    val condition: String = "",
    val feelsLike: String = "",
    val pressure: String = "",
    val hourlyContainer: String = "",
    val dailyContainer: String = ""
)
