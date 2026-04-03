package com.omersusin.sealora.data.remote.scraper

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.serialization.json.*
import com.omersusin.sealora.domain.model.*

class WeatherScraper(private val client: HttpClient) {

    suspend fun scrapeWeather(
        url: String,
        selectors: ScrapingSelectors,
        city: String
    ): Result<WeatherData> = runCatching {
        val fullUrl = url.replace("{city}", city)
            .replace("{lang}", "en")

        val response: HttpResponse = client.get(fullUrl)
        val html = response.bodyAsText()

        val temperature = extractValue(html, selectors.temperature)?.toDoubleOrNull() ?: 0.0
        val humidity = extractValue(html, selectors.humidity)?.toIntOrNull() ?: 0
        val windSpeed = extractValue(html, selectors.windSpeed)?.toDoubleOrNull() ?: 0.0
        val condition = extractValue(html, selectors.condition) ?: "Unknown"
        val feelsLike = extractValue(html, selectors.feelsLike)?.toDoubleOrNull() ?: temperature
        val pressure = extractValue(html, selectors.pressure)?.toIntOrNull() ?: 1013

        WeatherData(
            providerName = url,
            providerUrl = fullUrl,
            city = city,
            temperature = temperature,
            feelsLike = feelsLike,
            humidity = humidity,
            windSpeed = windSpeed,
            windDirection = "",
            pressure = pressure,
            uvIndex = 0.0,
            visibility = 0.0,
            cloudCover = 0,
            precipitation = 0.0,
            condition = condition,
            conditionIcon = mapConditionToIcon(condition)
        )
    }

    suspend fun fetchFromOpenMeteo(city: String): Result<WeatherData> = runCatching {
        val geoResponse: HttpResponse = client.get(
            "https://geocoding-api.open-meteo.com/v1/search?name=$city&count=1&language=en"
        )
        val geoJson = Json.parseToJsonElement(geoResponse.bodyAsText()).jsonObject
        val results = geoJson["results"]?.jsonArray

        if (results == null || results.isEmpty()) {
            throw Exception("City not found: $city")
        }

        val location = results[0].jsonObject
        val lat = location["latitude"]!!.jsonPrimitive.double
        val lon = location["longitude"]!!.jsonPrimitive.double
        val cityName = location["name"]!!.jsonPrimitive.content

        val weatherResponse: HttpResponse = client.get(
            "https://api.open-meteo.com/v1/forecast?" +
                "latitude=$lat&longitude=$lon" +
                "&current=temperature_2m,relative_humidity_2m,apparent_temperature," +
                "precipitation,weather_code,cloud_cover,pressure_msl,wind_speed_10m," +
                "wind_direction_10m,uv_index" +
                "&hourly=temperature_2m,relative_humidity_2m,precipitation_probability," +
                "weather_code,wind_speed_10m,uv_index,cloud_cover" +
                "&daily=weather_code,temperature_2m_max,temperature_2m_min," +
                "apparent_temperature_max,apparent_temperature_min,sunrise,sunset," +
                "uv_index_max,precipitation_sum,precipitation_probability_max," +
                "wind_speed_10m_max" +
                "&timezone=auto&forecast_days=14"
        )

        val weatherJson = Json.parseToJsonElement(weatherResponse.bodyAsText()).jsonObject

        val current = weatherJson["current"]!!.jsonObject
        val hourly = weatherJson["hourly"]!!.jsonObject
        val daily = weatherJson["daily"]!!.jsonObject

        val hourlyData = mutableListOf<HourlyData>()
        val times = hourly["time"]!!.jsonArray
        val temps = hourly["temperature_2m"]!!.jsonArray
        val humidities = hourly["relative_humidity_2m"]!!.jsonArray
        val precipProb = hourly["precipitation_probability"]!!.jsonArray
        val windSpeeds = hourly["wind_speed_10m"]!!.jsonArray
        val weatherCodes = hourly["weather_code"]!!.jsonArray

        for (i in 0 until minOf(times.size, 48)) {
            hourlyData.add(
                HourlyData(
                    time = times[i].jsonPrimitive.content,
                    temperature = temps[i].jsonPrimitive.double,
                    feelsLike = temps[i].jsonPrimitive.double,
                    humidity = humidities[i].jsonPrimitive.int,
                    windSpeed = windSpeeds[i].jsonPrimitive.double,
                    precipitation = 0.0,
                    precipitationChance = precipProb[i].jsonPrimitive.int,
                    condition = weatherCodeToCondition(weatherCodes[i].jsonPrimitive.int),
                    conditionIcon = weatherCodeToIcon(weatherCodes[i].jsonPrimitive.int),
                    uvIndex = hourly["uv_index"]?.jsonArray?.getOrNull(i)?.jsonPrimitive?.double ?: 0.0,
                    cloudCover = hourly["cloud_cover"]?.jsonArray?.getOrNull(i)?.jsonPrimitive?.int ?: 0,
                    dewPoint = 0.0
                )
            )
        }

        val dailyData = mutableListOf<DailyData>()
        val dates = daily["time"]!!.jsonArray
        val maxTemps = daily["temperature_2m_max"]!!.jsonArray
        val minTemps = daily["temperature_2m_min"]!!.jsonArray
        val sunrises = daily["sunrise"]!!.jsonArray
        val sunsets = daily["sunset"]!!.jsonArray
        val dailyCodes = daily["weather_code"]!!.jsonArray

        for (i in 0 until dates.size) {
            val dayHourly = hourlyData.filter { it.time.startsWith(dates[i].jsonPrimitive.content.substring(0, 10)) }
            dailyData.add(
                DailyData(
                    date = dates[i].jsonPrimitive.content,
                    tempMax = maxTemps[i].jsonPrimitive.double,
                    tempMin = minTemps[i].jsonPrimitive.double,
                    avgTemp = (maxTemps[i].jsonPrimitive.double + minTemps[i].jsonPrimitive.double) / 2,
                    feelsLikeMax = daily["apparent_temperature_max"]?.jsonArray?.getOrNull(i)?.jsonPrimitive?.double ?: maxTemps[i].jsonPrimitive.double,
                    feelsLikeMin = daily["apparent_temperature_min"]?.jsonArray?.getOrNull(i)?.jsonPrimitive?.double ?: minTemps[i].jsonPrimitive.double,
                    humidity = 0,
                    windSpeed = daily["wind_speed_10m_max"]?.jsonArray?.getOrNull(i)?.jsonPrimitive?.double ?: 0.0,
                    windGust = 0.0,
                    precipitation = daily["precipitation_sum"]?.jsonArray?.getOrNull(i)?.jsonPrimitive?.double ?: 0.0,
                    precipitationChance = daily["precipitation_probability_max"]?.jsonArray?.getOrNull(i)?.jsonPrimitive?.int ?: 0,
                    sunrise = sunrises[i].jsonPrimitive.content.split("T").getOrNull(1) ?: "",
                    sunset = sunsets[i].jsonPrimitive.content.split("T").getOrNull(1) ?: "",
                    moonPhase = "",
                    condition = weatherCodeToCondition(dailyCodes[i].jsonPrimitive.int),
                    conditionIcon = weatherCodeToIcon(dailyCodes[i].jsonPrimitive.int),
                    uvIndexMax = daily["uv_index_max"]?.jsonArray?.getOrNull(i)?.jsonPrimitive?.double ?: 0.0,
                    hourlyDetail = dayHourly
                )
            )
        }

        WeatherData(
            providerName = "Open-Meteo",
            providerUrl = "https://open-meteo.com",
            city = cityName,
            temperature = current["temperature_2m"]!!.jsonPrimitive.double,
            feelsLike = current["apparent_temperature"]!!.jsonPrimitive.double,
            humidity = current["relative_humidity_2m"]!!.jsonPrimitive.int,
            windSpeed = current["wind_speed_10m"]!!.jsonPrimitive.double,
            windDirection = "${current["wind_direction_10m"]!!.jsonPrimitive.int}°",
            pressure = current["pressure_msl"]!!.jsonPrimitive.int,
            uvIndex = current["uv_index"]!!.jsonPrimitive.double,
            visibility = 10.0,
            cloudCover = current["cloud_cover"]!!.jsonPrimitive.int,
            precipitation = current["precipitation"]!!.jsonPrimitive.double,
            condition = weatherCodeToCondition(current["weather_code"]!!.jsonPrimitive.int),
            conditionIcon = weatherCodeToIcon(current["weather_code"]!!.jsonPrimitive.int),
            hourlyForecast = hourlyData,
            dailyForecast = dailyData
        )
    }

    private fun extractValue(html: String, selector: String): String? {
        if (selector.isBlank()) return null
        val regex = try {
            Regex(selector)
        } catch (e: Exception) {
            return null
        }
        return regex.find(html)?.groupValues?.getOrNull(1)
    }

    private fun weatherCodeToCondition(code: Int): String = when (code) {
        0 -> "Açık"
        1 -> "Çoğunlukla Açık"
        2 -> "Parçalı Bulutlu"
        3 -> "Kapalı"
        45, 48 -> "Sisli"
        51, 53, 55 -> "Çisenti"
        56, 57 -> "Dondurucu Çisenti"
        61, 63, 65 -> "Yağmurlu"
        66, 67 -> "Dondurucu Yağmur"
        71, 73, 75 -> "Kar Yağışlı"
        77 -> "Kar Taneleri"
        80, 81, 82 -> "Sağanak Yağış"
        85, 86 -> "Kar Sağanağı"
        95 -> "Gök Gürültülü Fırtına"
        96, 99 -> "Dolu ile Fırtına"
        else -> "Bilinmeyen"
    }

    private fun weatherCodeToIcon(code: Int): String = when (code) {
        0 -> "☀️"
        1 -> "🌤️"
        2 -> "⛅"
        3 -> "☁️"
        45, 48 -> "🌫️"
        51, 53, 55 -> "🌦️"
        56, 57 -> "🌧️"
        61, 63, 65 -> "🌧️"
        66, 67 -> "🌨️"
        71, 73, 75 -> "❄️"
        77 -> "🌨️"
        80, 81, 82 -> "🌧️"
        85, 86 -> "🌨️"
        95 -> "⛈️"
        96, 99 -> "⛈️"
        else -> "❓"
    }

    private fun mapConditionToIcon(condition: String): String {
        val lower = condition.lowercase()
        return when {
            lower.contains("açık") || lower.contains("clear") || lower.contains("sunny") -> "☀️"
            lower.contains("bulut") || lower.contains("cloud") -> "☁️"
            lower.contains("yağmur") || lower.contains("rain") -> "🌧️"
            lower.contains("kar") || lower.contains("snow") -> "❄️"
            lower.contains("fırtına") || lower.contains("storm") -> "⛈️"
            lower.contains("sis") || lower.contains("fog") -> "🌫️"
            lower.contains("çisenti") || lower.contains("drizzle") -> "🌦️"
            else -> "🌡️"
        }
    }
}
