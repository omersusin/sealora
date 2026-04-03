package com.omersusin.sealora.data.remote.ai

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.json.*
import com.omersusin.sealora.domain.model.*

class AiService(private val client: HttpClient) {

    suspend fun generateWeatherReport(
        config: AiConfig,
        city: String,
        weatherDataList: List<WeatherData>
    ): Result<String> = runCatching {
        val prompt = buildReportPrompt(city, weatherDataList)
        callAi(config, prompt)
    }

    suspend fun chat(
        config: AiConfig,
        message: String,
        city: String,
        weatherContext: List<WeatherData>?,
        chatHistory: List<AiChatMessage>
    ): Result<String> = runCatching {
        val prompt = buildChatPrompt(message, city, weatherContext, chatHistory)
        callAi(config, prompt)
    }

    suspend fun discoverUrlPattern(
        config: AiConfig,
        url: String,
        city: String
    ): Result<String> = runCatching {
        val prompt = buildDiscoverPrompt(url, city)
        callAi(config, prompt)
    }

    private suspend fun callAi(config: AiConfig, prompt: String): String {
        return when (config.provider) {
            AiProvider.OPENROUTER -> callOpenRouter(config, prompt)
            AiProvider.GROQ -> callGroq(config, prompt)
            AiProvider.GEMINI -> callGemini(config, prompt)
            AiProvider.OPENAI -> callOpenAI(config, prompt)
        }
    }

    private suspend fun callOpenRouter(config: AiConfig, prompt: String): String {
        val response: HttpResponse = client.post("${config.baseUrl}/chat/completions") {
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer ${config.apiKey}")
            header("HTTP-Referer", "https://sealora.app")
            setBody(buildJsonObject {
                put("model", config.model.ifBlank { "meta-llama/llama-3.1-8b-instruct:free" })
                putJsonArray("messages") {
                    addJsonObject {
                        put("role", "user")
                        put("content", prompt)
                    }
                }
                put("temperature", 0.7)
                put("max_tokens", 2048)
            }.toString())
        }

        val json = Json.parseToJsonElement(response.bodyAsText()).jsonObject
        return json["choices"]?.jsonArray?.firstOrNull()
            ?.jsonObject?.get("message")?.jsonObject?.get("content")?.jsonPrimitive?.content
            ?: throw Exception("Empty response from OpenRouter")
    }

    private suspend fun callGroq(config: AiConfig, prompt: String): String {
        val response: HttpResponse = client.post("${config.baseUrl}/chat/completions") {
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer ${config.apiKey}")
            setBody(buildJsonObject {
                put("model", config.model.ifBlank { "llama-3.1-8b-instant" })
                putJsonArray("messages") {
                    addJsonObject {
                        put("role", "user")
                        put("content", prompt)
                    }
                }
                put("temperature", 0.7)
                put("max_tokens", 2048)
            }.toString())
        }

        val json = Json.parseToJsonElement(response.bodyAsText()).jsonObject
        return json["choices"]?.jsonArray?.firstOrNull()
            ?.jsonObject?.get("message")?.jsonObject?.get("content")?.jsonPrimitive?.content
            ?: throw Exception("Empty response from Groq")
    }

    private suspend fun callGemini(config: AiConfig, prompt: String): String {
        val url = "${config.baseUrl}/models/${config.model.ifBlank { "gemini-1.5-flash" }}:generateContent?key=${config.apiKey}"
        val response: HttpResponse = client.post(url) {
            contentType(ContentType.Application.Json)
            setBody(buildJsonObject {
                putJsonArray("contents") {
                    addJsonObject {
                        putJsonArray("parts") {
                            addJsonObject {
                                put("text", prompt)
                            }
                        }
                    }
                }
                putJsonObject("generationConfig") {
                    put("temperature", 0.7)
                    put("maxOutputTokens", 2048)
                }
            }.toString())
        }

        val json = Json.parseToJsonElement(response.bodyAsText()).jsonObject
        return json["candidates"]?.jsonArray?.firstOrNull()
            ?.jsonObject?.get("content")?.jsonObject?.get("parts")?.jsonArray?.firstOrNull()
            ?.jsonObject?.get("text")?.jsonPrimitive?.content
            ?: throw Exception("Empty response from Gemini")
    }

    private suspend fun callOpenAI(config: AiConfig, prompt: String): String {
        val response: HttpResponse = client.post("${config.baseUrl}/chat/completions") {
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer ${config.apiKey}")
            setBody(buildJsonObject {
                put("model", config.model.ifBlank { "gpt-4o-mini" })
                putJsonArray("messages") {
                    addJsonObject {
                        put("role", "user")
                        put("content", prompt)
                    }
                }
                put("temperature", 0.7)
                put("max_tokens", 2048)
            }.toString())
        }

        val json = Json.parseToJsonElement(response.bodyAsText()).jsonObject
        return json["choices"]?.jsonArray?.firstOrNull()
            ?.jsonObject?.get("message")?.jsonObject?.get("content")?.jsonPrimitive?.content
            ?: throw Exception("Empty response from OpenAI")
    }

    private fun buildReportPrompt(city: String, weatherDataList: List<WeatherData>): String {
        val dataSummary = weatherDataList.joinToString("\n\n") { data ->
            """
            Sağlayıcı: ${data.providerName}
            Sıcaklık: ${data.temperature}°C (Hissedilen: ${data.feelsLike}°C)
            Nem: ${data.humidity}%
            Rüzgar: ${data.windSpeed} km/h ${data.windDirection}
            Basınç: ${data.pressure} hPa
            UV İndeksi: ${data.uvIndex}
            Bulutluluk: ${data.cloudCover}%
            Durum: ${data.condition} ${data.conditionIcon}
            Yağış: ${data.precipitation} mm
            """.trimIndent()
        }

        return """
        Sen Sealora adlı hava durumu uygulamasının yapay zeka asistanısın.
        
        $city şehri için ${weatherDataList.size} farklı hava durumu sağlayıcısından veri toplandı.
        
        SAĞLAYICI VERİLERİ:
        $dataSummary
        
        Lütfen şu formatta kapsamlı bir rapor oluştur:
        
        1. GENEL ÖZET: 2-3 cümlelik hava durumu özeti
        2. SAĞLAYICI TUTARLILIĞI: Sağlayıcılar arasındaki farkları analiz et
        3. SAATLİK ÖZET: Önemli saatlik değişiklikler
        4. GÜNLÜK ÖZET: 3-5 günlük genel görünüm
        5. ÖNERİLER: Kullanıcıya özel öneriler (giyim, aktivite vb.)
        6. UYARILAR: Dikkat edilmesi gereken durumlar
        
        Türkçe, samimi ve bilgilendirici bir dil kullan. Emoji kullanabilirsin.
        """.trimIndent()
    }

    private fun buildChatPrompt(
        message: String,
        city: String,
        weatherContext: List<WeatherData>?,
        chatHistory: List<AiChatMessage>
    ): String {
        val contextPart = if (weatherContext != null && weatherContext.isNotEmpty()) {
            val dataSummary = weatherContext.joinToString("\n") { data ->
                "${data.providerName}: ${data.temperature}°C, ${data.condition} ${data.conditionIcon}, Nem: ${data.humidity}%, Rüzgar: ${data.windSpeed} km/h"
            }
            """
            MEVCUT HAVA DURUMU VERİLERİ ($city):
            $dataSummary
            """
        } else {
            "Henüz hava durumu verisi yüklenmedi."
        }

        val historyPart = if (chatHistory.isNotEmpty()) {
            val history = chatHistory.takeLast(6).joinToString("\n") { msg ->
                "${if (msg.role == "user") "Kullanıcı" else "Asistan"}: ${msg.content}"
            }
            "SOHBET GEÇMİŞİ:\n$history"
        } else {
            ""
        }

        return """
        Sen Sealora adlı hava durumu uygulamasının yapay zeka asistanısın.
        Hava durumu hakkında soruları yanıtlıyorsun.
        Türkçe, samimi ve bilgilendirici bir dil kullan.
        
        $contextPart
        
        $historyPart
        
        KULLANICI SORUSU: $message
        
        Yanıtını hava durumu verilerine dayandır. Eğer veri yoksa, genel hava durumu bilgisi ver.
        """.trimIndent()
    }

    private fun buildDiscoverPrompt(url: String, city: String): String {
        return """
        Bir hava durumu web sitesinin URL yapısını analiz et.
        
        ÖRNEK URL: $url
        TEST ŞEHRİ: $city
        
        Bu URL'nin yapısını analiz et ve şu bilgileri JSON formatında ver:
        
        {
            "urlTemplate": "Şehir adını {city} placeholder ile şablonlaştırılmış URL",
            "language": "Sayfanın dili (en, tr, vb.)",
            "selectors": {
                "temperature": "Sıcaklık için CSS selector veya regex",
                "humidity": "Nem için selector",
                "windSpeed": "Rüzgar hızı için selector",
                "condition": "Hava durumu durumu için selector",
                "feelsLike": "Hissedilen sıcaklık için selector",
                "pressure": "Basınç için selector"
            },
            "confidence": 0.0-1.0 arası güven skoru
        }
        
        Sadece JSON döndür, başka açıklama yapma.
        """.trimIndent()
    }
}
