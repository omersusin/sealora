package com.omersusin.sealora.data.remote.ai

import android.util.Log
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.json.*
import com.omersusin.sealora.domain.model.*

class AiService(private val client: HttpClient) {

    companion object {
        private const val TAG = "AiService"
    }

    suspend fun generateWeatherReport(config: AiConfig, city: String, weatherDataList: List<WeatherData>): Result<String> = runCatching {
        val prompt = buildReportPrompt(city, weatherDataList)
        val response = callAi(config, prompt)
        if (response.isBlank()) throw Exception("Empty response from ${config.provider.displayName}")
        response
    }

    suspend fun chat(config: AiConfig, message: String, city: String, weatherContext: List<WeatherData>?, chatHistory: List<AiChatMessage>): Result<String> = runCatching {
        val prompt = buildChatPrompt(message, city, weatherContext, chatHistory)
        val response = callAi(config, prompt)
        if (response.isBlank()) throw Exception("Empty response from ${config.provider.displayName}")
        response
    }

    suspend fun discoverUrlPattern(config: AiConfig, url: String, city: String): Result<String> = runCatching {
        val prompt = buildDiscoverPrompt(url, city)
        callAi(config, prompt)
    }

    private suspend fun callAi(config: AiConfig, prompt: String): String {
        if (config.apiKey.isBlank()) throw Exception("API key is empty. Please set your API key in settings.")
        Log.d(TAG, "Calling ${config.provider.name} with model ${config.model}")
        return when (config.provider) {
            AiProvider.OPENROUTER -> callOpenRouter(config, prompt)
            AiProvider.GROQ -> callGroq(config, prompt)
            AiProvider.GEMINI -> callGemini(config, prompt)
            AiProvider.OPENAI -> callOpenAI(config, prompt)
        }
    }

    private suspend fun callOpenRouter(config: AiConfig, prompt: String): String {
        val url = "${config.baseUrl}/chat/completions"
        val model = config.model.ifBlank { "meta-llama/llama-3.1-8b-instruct:free" }
        Log.d(TAG, "OpenRouter URL: $url, Model: $model")
        val response: HttpResponse = client.post(url) {
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer ${config.apiKey}")
            header("HTTP-Referer", "https://sealora.app")
            header("X-Title", "Sealora")
            setBody(buildString {
                append("{")
                append("\"model\":\"$model\",")
                append("\"messages\":[{\"role\":\"user\",\"content\":\"${prompt.replace("\"", "\\\"").replace("\n", "\\n")}\"}],")
                append("\"temperature\":0.7,")
                append("\"max_tokens\":1024")
                append("}")
            })
        }
        val body = response.bodyAsText()
        Log.d(TAG, "OpenRouter response status: ${response.status}, body length: ${body.length}")
        if (!response.status.isSuccess()) {
            throw Exception("OpenRouter API error ${response.status}: ${body.take(200)}")
        }
        val json = try { Json.parseToJsonElement(body).jsonObject } catch (e: Exception) { throw Exception("Invalid JSON response: ${body.take(200)}") }
        val choices = json["choices"]?.jsonArray
        if (choices.isNullOrEmpty()) throw Exception("No choices in response. Full response: ${body.take(300)}")
        val content = choices.firstOrNull()?.jsonObject?.get("message")?.jsonObject?.get("content")?.jsonPrimitive?.content
        if (content.isNullOrBlank()) throw Exception("Empty content in response choice. Response: ${body.take(300)}")
        return content
    }

    private suspend fun callGroq(config: AiConfig, prompt: String): String {
        val model = config.model.ifBlank { "llama-3.1-8b-instant" }
        val response: HttpResponse = client.post("${config.baseUrl}/chat/completions") {
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer ${config.apiKey}")
            setBody(buildString {
                append("{")
                append("\"model\":\"$model\",")
                append("\"messages\":[{\"role\":\"user\",\"content\":\"${prompt.replace("\"", "\\\"").replace("\n", "\\n")}\"}],")
                append("\"temperature\":0.7,")
                append("\"max_tokens\":1024")
                append("}")
            })
        }
        val body = response.bodyAsText()
        if (!response.status.isSuccess()) throw Exception("Groq error ${response.status}: ${body.take(200)}")
        val json = Json.parseToJsonElement(body).jsonObject
        return json["choices"]?.jsonArray?.firstOrNull()?.jsonObject?.get("message")?.jsonObject?.get("content")?.jsonPrimitive?.content ?: throw Exception("Empty Groq response")
    }

    private suspend fun callGemini(config: AiConfig, prompt: String): String {
        val model = config.model.ifBlank { "gemini-1.5-flash" }
        val url = "${config.baseUrl}/models/${model}:generateContent?key=${config.apiKey}"
        val response: HttpResponse = client.post(url) {
            contentType(ContentType.Application.Json)
            setBody(buildString {
                append("{")
                append("\"contents\":[{\"parts\":[{\"text\":\"${prompt.replace("\"", "\\\"").replace("\n", "\\n")}\"}]}],")
                append("\"generationConfig\":{\"temperature\":0.7,\"maxOutputTokens\":1024}")
                append("}")
            })
        }
        val body = response.bodyAsText()
        if (!response.status.isSuccess()) throw Exception("Gemini error ${response.status}: ${body.take(200)}")
        val json = Json.parseToJsonElement(body).jsonObject
        return json["candidates"]?.jsonArray?.firstOrNull()?.jsonObject?.get("content")?.jsonObject?.get("parts")?.jsonArray?.firstOrNull()?.jsonObject?.get("text")?.jsonPrimitive?.content ?: throw Exception("Empty Gemini response")
    }

    private suspend fun callOpenAI(config: AiConfig, prompt: String): String {
        val model = config.model.ifBlank { "gpt-4o-mini" }
        val response: HttpResponse = client.post("${config.baseUrl}/chat/completions") {
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer ${config.apiKey}")
            setBody(buildString {
                append("{")
                append("\"model\":\"$model\",")
                append("\"messages\":[{\"role\":\"user\",\"content\":\"${prompt.replace("\"", "\\\"").replace("\n", "\\n")}\"}],")
                append("\"temperature\":0.7,")
                append("\"max_tokens\":1024")
                append("}")
            })
        }
        val body = response.bodyAsText()
        if (!response.status.isSuccess()) throw Exception("OpenAI error ${response.status}: ${body.take(200)}")
        val json = Json.parseToJsonElement(body).jsonObject
        return json["choices"]?.jsonArray?.firstOrNull()?.jsonObject?.get("message")?.jsonObject?.get("content")?.jsonPrimitive?.content ?: throw Exception("Empty OpenAI response")
    }

    private fun buildReportPrompt(city: String, weatherDataList: List<WeatherData>): String {
        val dataSummary = weatherDataList.joinToString("\n---\n") { data ->
            "Provider: ${data.providerName}\nTemp: ${data.temperature}C, Feels: ${data.feelsLike}C\nHumidity: ${data.humidity}%, Wind: ${data.windSpeed} km/h ${data.windDirection}\nPressure: ${data.pressure} hPa, UV: ${data.uvIndex}\nCloud: ${data.cloudCover}%, Condition: ${data.condition}\nPrecipitation: ${data.precipitation} mm"
        }
        return "You are a weather AI assistant for the Sealora app. Generate a weather report for $city based on data from ${weatherDataList.size} providers.\n\nDATA:\n$dataSummary\n\nProvide a concise report in Turkish with: summary, hourly highlights, daily outlook, recommendations, and alerts if needed. Use emojis."
    }

    private fun buildChatPrompt(message: String, city: String, weatherContext: List<WeatherData>?, chatHistory: List<AiChatMessage>): String {
        val context = if (weatherContext != null && weatherContext.isNotEmpty()) {
            "Current weather for $city:\n" + weatherContext.joinToString("\n") { "${it.providerName}: ${it.temperature}C, ${it.condition}, Humidity: ${it.humidity}%, Wind: ${it.windSpeed} km/h" }
        } else { "No weather data loaded yet." }
        val history = if (chatHistory.isNotEmpty()) "Chat history:\n" + chatHistory.takeLast(6).joinToString("\n") { "${if (it.role == "user") "User" else "Assistant"}: ${it.content}" } else ""
        return "You are Sealora weather AI assistant. Answer in Turkish.\n\n$context\n\n$history\n\nUser question: $message"
    }

    private fun buildDiscoverPrompt(url: String, city: String): String {
        return "Analyze this weather URL and return a JSON template: $url\nTest city: $city\nReturn JSON with urlTemplate (use {city} placeholder), language, and selectors object with CSS selectors for temperature, humidity, windSpeed, condition, feelsLike, pressure. Return only JSON."
    }
}
