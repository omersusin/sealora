package com.omersusin.sealora.data.remote.scraper

import android.util.Log
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.serialization.json.*
import com.omersusin.sealora.domain.model.*

class WeatherScraper(private val client: HttpClient) {

    companion object { private const val TAG = "WeatherScraper" }

    suspend fun scrapeWeather(url: String, selectors: ScrapingSelectors, city: String): Result<WeatherData> = runCatching {
        val fullUrl = url.replace("{city}", city).replace("{lang}", "en")
        val response: HttpResponse = client.get(fullUrl)
        val html = response.bodyAsText()
        val temperature = extractValue(html, selectors.temperature)?.toDoubleOrNull() ?: 0.0
        val humidity = extractValue(html, selectors.humidity)?.toIntOrNull() ?: 0
        val windSpeed = extractValue(html, selectors.windSpeed)?.toDoubleOrNull() ?: 0.0
        val condition = extractValue(html, selectors.condition) ?: "Unknown"
        val feelsLike = extractValue(html, selectors.feelsLike)?.toDoubleOrNull() ?: temperature
        val pressure = extractValue(html, selectors.pressure)?.toIntOrNull() ?: 1013
        WeatherData(providerName = url, providerUrl = fullUrl, city = city, temperature = temperature, feelsLike = feelsLike, humidity = humidity, windSpeed = windSpeed, windDirection = "", pressure = pressure, uvIndex = 0.0, visibility = 0.0, cloudCover = 0, precipitation = 0.0, condition = condition, conditionIcon = mapConditionToIcon(condition))
    }

    suspend fun fetchFromOpenMeteo(city: String): Result<WeatherData> = runCatching {
        Log.d(TAG, "Fetching from Open-Meteo for: $city")
        val geoResponse: HttpResponse = client.get("https://geocoding-api.open-meteo.com/v1/search?name=$city&count=1&language=en")
        val geoBody = geoResponse.bodyAsText()
        val geoJson = try { Json.parseToJsonElement(geoBody).jsonObject } catch (e: Exception) { throw Exception("Geocoding failed for $city: ${geoBody.take(200)}") }
        val results = geoJson["results"]?.jsonArray
        if (results.isNullOrEmpty()) throw Exception("City not found: $city")
        val location = results[0].jsonObject
        val lat = location["latitude"]?.jsonPrimitive?.double ?: throw Exception("Missing latitude")
        val lon = location["longitude"]?.jsonPrimitive?.double ?: throw Exception("Missing longitude")
        val cityName = location["name"]?.jsonPrimitive?.content ?: city
        Log.d(TAG, "Found city: $cityName at $lat,$lon")

        val weatherResponse: HttpResponse = client.get(
            "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon&current=temperature_2m,relative_humidity_2m,apparent_temperature,precipitation,weather_code,cloud_cover,pressure_msl,wind_speed_10m,wind_direction_10m,uv_index&hourly=temperature_2m,relative_humidity_2m,precipitation_probability,weather_code,wind_speed_10m,uv_index,cloud_cover&daily=weather_code,temperature_2m_max,temperature_2m_min,apparent_temperature_max,apparent_temperature_min,sunrise,sunset,uv_index_max,precipitation_sum,precipitation_probability_max,wind_speed_10m_max&timezone=auto&forecast_days=14"
        )
        val weatherBody = weatherResponse.bodyAsText()
        Log.d(TAG, "Open-Meteo response length: ${weatherBody.length}")
        val weatherJson = try { Json.parseToJsonElement(weatherBody).jsonObject } catch (e: Exception) { throw Exception("Invalid weather JSON: ${weatherBody.take(200)}") }

        val current = weatherJson["current"]?.jsonObject ?: throw Exception("No current data")
        val hourly = weatherJson["hourly"]?.jsonObject ?: throw Exception("No hourly data")
        val daily = weatherJson["daily"]?.jsonObject ?: throw Exception("No daily data")

        val hourlyData = mutableListOf<HourlyData>()
        val times = hourly["time"]?.jsonArray ?: JsonArray(emptyList())
        val temps = hourly["temperature_2m"]?.jsonArray ?: JsonArray(emptyList())
        val humidities = hourly["relative_humidity_2m"]?.jsonArray ?: JsonArray(emptyList())
        val precipProb = hourly["precipitation_probability"]?.jsonArray ?: JsonArray(emptyList())
        val windSpeeds = hourly["wind_speed_10m"]?.jsonArray ?: JsonArray(emptyList())
        val weatherCodes = hourly["weather_code"]?.jsonArray ?: JsonArray(emptyList())
        val uvArray = hourly["uv_index"]?.jsonArray
        val cloudArray = hourly["cloud_cover"]?.jsonArray

        for (i in 0 until minOf(times.size, 48, temps.size, humidities.size)) {
            hourlyData.add(HourlyData(
                time = times[i].jsonPrimitive.content,
                temperature = temps[i].jsonPrimitive.double,
                feelsLike = temps[i].jsonPrimitive.double,
                humidity = humidities[i].jsonPrimitive.int,
                windSpeed = windSpeeds.getOrElse(i) { JsonPrimitive(0) }.jsonPrimitive.double,
                precipitation = 0.0,
                precipitationChance = precipProb.getOrElse(i) { JsonPrimitive(0) }.jsonPrimitive.int,
                condition = weatherCodeToCondition(weatherCodes.getOrElse(i) { JsonPrimitive(0) }.jsonPrimitive.int),
                conditionIcon = weatherCodeToIcon(weatherCodes.getOrElse(i) { JsonPrimitive(0) }.jsonPrimitive.int),
                uvIndex = uvArray?.getOrElse(i) { JsonPrimitive(0) }?.jsonPrimitive?.double ?: 0.0,
                cloudCover = cloudArray?.getOrElse(i) { JsonPrimitive(0) }?.jsonPrimitive?.int ?: 0,
                dewPoint = 0.0
            ))
        }

        val dailyData = mutableListOf<DailyData>()
        val dates = daily["time"]?.jsonArray ?: JsonArray(emptyList())
        val maxTemps = daily["temperature_2m_max"]?.jsonArray ?: JsonArray(emptyList())
        val minTemps = daily["temperature_2m_min"]?.jsonArray ?: JsonArray(emptyList())
        val sunrises = daily["sunrise"]?.jsonArray ?: JsonArray(emptyList())
        val sunsets = daily["sunset"]?.jsonArray ?: JsonArray(emptyList())
        val dailyCodes = daily["weather_code"]?.jsonArray ?: JsonArray(emptyList())
        val windMaxArray = daily["wind_speed_10m_max"]?.jsonArray
        val precipSumArray = daily["precipitation_sum"]?.jsonArray
        val precipProbMaxArray = daily["precipitation_probability_max"]?.jsonArray
        val uvMaxArray = daily["uv_index_max"]?.jsonArray
        val appTempMaxArray = daily["apparent_temperature_max"]?.jsonArray
        val appTempMinArray = daily["apparent_temperature_min"]?.jsonArray

        for (i in 0 until minOf(dates.size, maxTemps.size, minTemps.size)) {
            val dayHourly = hourlyData.filter { it.time.startsWith(dates[i].jsonPrimitive.content.substring(0, minOf(10, dates[i].jsonPrimitive.content.length))) }
            dailyData.add(DailyData(
                date = dates[i].jsonPrimitive.content,
                tempMax = maxTemps[i].jsonPrimitive.double,
                tempMin = minTemps[i].jsonPrimitive.double,
                avgTemp = (maxTemps[i].jsonPrimitive.double + minTemps[i].jsonPrimitive.double) / 2,
                feelsLikeMax = appTempMaxArray?.getOrElse(i) { JsonPrimitive(maxTemps[i].jsonPrimitive.double) }?.jsonPrimitive?.double ?: maxTemps[i].jsonPrimitive.double,
                feelsLikeMin = appTempMinArray?.getOrElse(i) { JsonPrimitive(minTemps[i].jsonPrimitive.double) }?.jsonPrimitive?.double ?: minTemps[i].jsonPrimitive.double,
                humidity = 0,
                windSpeed = windMaxArray?.getOrElse(i) { JsonPrimitive(0) }?.jsonPrimitive?.double ?: 0.0,
                windGust = 0.0,
                precipitation = precipSumArray?.getOrElse(i) { JsonPrimitive(0) }?.jsonPrimitive?.double ?: 0.0,
                precipitationChance = precipProbMaxArray?.getOrElse(i) { JsonPrimitive(0) }?.jsonPrimitive?.int ?: 0,
                sunrise = sunrises.getOrElse(i) { JsonPrimitive("") }.jsonPrimitive.content.split("T").getOrElse(1) { "" },
                sunset = sunsets.getOrElse(i) { JsonPrimitive("") }.jsonPrimitive.content.split("T").getOrElse(1) { "" },
                moonPhase = "",
                condition = weatherCodeToCondition(dailyCodes.getOrElse(i) { JsonPrimitive(0) }.jsonPrimitive.int),
                conditionIcon = weatherCodeToIcon(dailyCodes.getOrElse(i) { JsonPrimitive(0) }.jsonPrimitive.int),
                uvIndexMax = uvMaxArray?.getOrElse(i) { JsonPrimitive(0) }?.jsonPrimitive?.double ?: 0.0,
                hourlyDetail = dayHourly
            ))
        }

        WeatherData(
            providerName = "Open-Meteo", providerUrl = "https://open-meteo.com", city = cityName,
            temperature = current["temperature_2m"]?.jsonPrimitive?.double ?: 0.0,
            feelsLike = current["apparent_temperature"]?.jsonPrimitive?.double ?: 0.0,
            humidity = current["relative_humidity_2m"]?.jsonPrimitive?.int ?: 0,
            windSpeed = current["wind_speed_10m"]?.jsonPrimitive?.double ?: 0.0,
            windDirection = "${current["wind_direction_10m"]?.jsonPrimitive?.int ?: 0}deg",
            pressure = current["pressure_msl"]?.jsonPrimitive?.int ?: 1013,
            uvIndex = current["uv_index"]?.jsonPrimitive?.double ?: 0.0,
            visibility = 10.0,
            cloudCover = current["cloud_cover"]?.jsonPrimitive?.int ?: 0,
            precipitation = current["precipitation"]?.jsonPrimitive?.double ?: 0.0,
            condition = weatherCodeToCondition(current["weather_code"]?.jsonPrimitive?.int ?: 0),
            conditionIcon = weatherCodeToIcon(current["weather_code"]?.jsonPrimitive?.int ?: 0),
            hourlyForecast = hourlyData, dailyForecast = dailyData
        )
    }

    private fun extractValue(html: String, selector: String): String? {
        if (selector.isBlank()) return null
        return try { Regex(selector).find(html)?.groupValues?.getOrNull(1) } catch (e: Exception) { null }
    }

    private fun weatherCodeToCondition(code: Int): String = when (code) {
        0 -> "A\u00e7\u0131k"; 1 -> "\u00c7o\u011funlukla A\u00e7\u0131k"; 2 -> "Par\u00e7al\u0131 Bulutlu"; 3 -> "Kapal\u0131"
        45, 48 -> "Sisli"; 51, 53, 55 -> "\u00c7isenti"; 56, 57 -> "Dondurucu \u00c7isenti"
        61, 63, 65 -> "Ya\u011fmurlu"; 66, 67 -> "Dondurucu Ya\u011fmur"
        71, 73, 75 -> "Kar Ya\u011f\u0131\u015fl\u0131"; 77 -> "Kar Taneleri"
        80, 81, 82 -> "Sa\u011fanak Ya\u011f\u0131\u015f"; 85, 86 -> "Kar Sa\u011fana\u011f\u0131"
        95 -> "G\u00f6k G\u00fcr\u00fclt\u00fc"; 96, 99 -> "Dolu ile F\u0131rt\u0131na"
        else -> "Bilinmeyen"
    }

    private fun weatherCodeToIcon(code: Int): String = when (code) {
        0 -> "\u2600\uFE0F"; 1 -> "\uD83C\uDF24\uFE0F"; 2 -> "\u26C5"; 3 -> "\u2601\uFE0F"
        45, 48 -> "\uD83C\uDF2B\uFE0F"; 51, 53, 55 -> "\uD83C\uDF26\uFE0F"
        61, 63, 65 -> "\uD83C\uDF27\uFE0F"; 66, 67 -> "\uD83C\uDF28\uFE0F"
        71, 73, 75 -> "\u2744\uFE0F"; 77 -> "\uD83C\uDF28\uFE0F"
        80, 81, 82 -> "\uD83C\uDF27\uFE0F"; 85, 86 -> "\uD83C\uDF28\uFE0F"
        95 -> "\u26C8\uFE0F"; 96, 99 -> "\u26C8\uFE0F"
        else -> "\uD83C\uDF21\uFE0F"
    }

    private fun mapConditionToIcon(condition: String): String {
        val lower = condition.lowercase()
        return when {
            lower.contains("acik") || lower.contains("clear") || lower.contains("sunny") -> "\u2600\uFE0F"
            lower.contains("bulut") || lower.contains("cloud") -> "\u2601\uFE0F"
            lower.contains("yagmur") || lower.contains("rain") -> "\uD83C\uDF27\uFE0F"
            lower.contains("kar") || lower.contains("snow") -> "\u2744\uFE0F"
            lower.contains("firtina") || lower.contains("storm") -> "\u26C8\uFE0F"
            lower.contains("sis") || lower.contains("fog") -> "\uD83C\uDF2B\uFE0F"
            else -> "\uD83C\uDF21\uFE0F"
        }
    }
}
