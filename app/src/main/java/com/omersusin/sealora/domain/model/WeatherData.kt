package com.omersusin.sealora.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class WeatherData(
    val providerName: String,
    val providerUrl: String,
    val city: String,
    val temperature: Double,
    val feelsLike: Double,
    val humidity: Int,
    val windSpeed: Double,
    val windDirection: String,
    val pressure: Int,
    val uvIndex: Double,
    val visibility: Double,
    val cloudCover: Int,
    val precipitation: Double,
    val condition: String,
    val conditionIcon: String,
    val hourlyForecast: List<HourlyData> = emptyList(),
    val dailyForecast: List<DailyData> = emptyList(),
    val lastUpdated: Long = System.currentTimeMillis()
)

@Serializable
data class HourlyData(
    val time: String,
    val temperature: Double,
    val feelsLike: Double,
    val humidity: Int,
    val windSpeed: Double,
    val precipitation: Double,
    val precipitationChance: Int,
    val condition: String,
    val conditionIcon: String,
    val uvIndex: Double,
    val cloudCover: Int,
    val dewPoint: Double
)

@Serializable
data class DailyData(
    val date: String,
    val tempMax: Double,
    val tempMin: Double,
    val avgTemp: Double,
    val feelsLikeMax: Double,
    val feelsLikeMin: Double,
    val humidity: Int,
    val windSpeed: Double,
    val windGust: Double,
    val precipitation: Double,
    val precipitationChance: Int,
    val sunrise: String,
    val sunset: String,
    val moonPhase: String,
    val condition: String,
    val conditionIcon: String,
    val uvIndexMax: Double,
    val hourlyDetail: List<HourlyData> = emptyList()
)
