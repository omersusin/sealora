package com.omersusin.sealora.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class WeatherReport(
    val city: String,
    val summary: String,
    val currentTemp: Double,
    val feelsLike: Double,
    val condition: String,
    val conditionIcon: String,
    val providerCount: Int,
    val providerNames: List<String>,
    val consistency: ConsistencyLevel,
    val hourlySummary: String,
    val dailySummary: String,
    val recommendations: List<String>,
    val alerts: List<WeatherAlert> = emptyList(),
    val generatedAt: Long = System.currentTimeMillis(),
    val aiModel: String = ""
)

@Serializable
data class WeatherAlert(
    val type: AlertType,
    val title: String,
    val description: String,
    val severity: Int // 1-5
)

@Serializable
enum class AlertType {
    TEMPERATURE, WIND, RAIN, UV, STORM, FOG, SNOW, HAIL
}

@Serializable
enum class ConsistencyLevel(val label: String, val emoji: String) {
    HIGH("Çok Tutarlı", "✅"),
    MODERATE("Tutarlı", "🟡"),
    LOW("Az Tutarlı", "🟠"),
    CONFLICTING("Çelişkili", "🔴")
}

@Serializable
data class CityWeatherState(
    val city: String,
    val weatherDataList: List<WeatherData>,
    val report: WeatherReport? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)
