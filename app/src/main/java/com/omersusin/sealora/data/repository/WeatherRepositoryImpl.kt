package com.omersusin.sealora.data.repository

import android.util.Log
import com.omersusin.sealora.data.local.ProviderDao
import com.omersusin.sealora.data.local.entity.ProviderEntity
import com.omersusin.sealora.data.remote.scraper.WeatherScraper
import com.omersusin.sealora.domain.model.*
import com.omersusin.sealora.domain.repository.WeatherRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WeatherRepositoryImpl @Inject constructor(
    private val providerDao: ProviderDao,
    private val scraper: WeatherScraper
) : WeatherRepository {

    companion object {
        private const val TAG = "WeatherRepo"
    }

    override fun getActiveProviders(): Flow<List<WeatherProvider>> {
        return providerDao.getActiveProviders().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getAllProviders(): Flow<List<WeatherProvider>> {
        return providerDao.getAllProviders().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getWeather(
        city: String,
        provider: WeatherProvider
    ): Result<WeatherData> {
        return try {
            Log.d(TAG, "Fetching weather for $city from ${provider.name}")

            when (provider.type) {
                ProviderType.BUILTIN -> {
                    scraper.fetchFromOpenMeteo(city)
                }
                ProviderType.API -> {
                    fetchFromApi(city, provider)
                }
                ProviderType.SCRAPED, ProviderType.AI_DISCOVERED -> {
                    val selectors = parseSelectors(provider.urlTemplate)
                    scraper.scrapeWeather(provider.baseUrl, selectors, city)
                }
            }.map { data ->
                data.copy(
                    providerName = provider.name,
                    providerUrl = provider.baseUrl
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching from ${provider.name}: ${e.message}")
            Result.failure(e)
        }
    }

    override suspend fun getWeatherFromAll(city: String): List<WeatherData> {
        val providers = mutableListOf<WeatherProvider>()
        providerDao.getActiveProviders().collect { list ->
            providers.addAll(list.map { it.toDomain() })
        }

        val results = mutableListOf<WeatherData>()
        for (provider in providers) {
            val result = getWeather(city, provider)
            result.onSuccess { data ->
                results.add(data)
            }.onFailure { error ->
                Log.w(TAG, "Provider ${provider.name} failed: ${error.message}")
            }
        }
        return results
    }

    override suspend fun saveProvider(provider: WeatherProvider) {
        val now = System.currentTimeMillis()
        val entity = provider.toEntity(
            createdAt = now,
            updatedAt = now
        )
        providerDao.insertProvider(entity)
        Log.d(TAG, "Provider saved: ${provider.name}")
    }

    override suspend fun deleteProvider(id: String) {
        val entity = providerDao.getProviderById(id)
        entity?.let {
            if (!it.isBuiltIn) {
                providerDao.deleteProvider(it)
                Log.d(TAG, "Provider deleted: ${it.name}")
            }
        }
    }

    override suspend fun toggleProvider(id: String, active: Boolean) {
        providerDao.setProviderActive(id, active)
        Log.d(TAG, "Provider $id active: $active")
    }

    override suspend fun discoverProviderTemplate(
        url: String,
        city: String
    ): Result<ProviderTemplate> = runCatching {
        val testCity = city.ifBlank { "Istanbul" }
        val formattedUrl = url.replace("{city}", testCity)
            .replace("{lang}", "en")

        val template = ProviderTemplate(
            providerId = UUID.randomUUID().toString(),
            urlPattern = url,
            selectors = ScrapingSelectors(
                temperature = """<span[^>]*class="[^"]*temp[^"]*"[^>]*>([+-]?\d+)""",
                humidity = """<span[^>]*class="[^"]*humidity[^"]*"[^>]*>(\d+)""",
                windSpeed = """<span[^>]*class="[^"]*wind[^"]*"[^>]*>([\d.]+)""",
                condition = """<div[^>]*class="[^"]*condition[^"]*"[^>]*>([^<]+)""",
                feelsLike = """<span[^>]*class="[^"]*feels[^"]*"[^>]*>([+-]?\d+)""",
                pressure = """<span[^>]*class="[^"]*pressure[^"]*"[^>]*>(\d+)"""
            ),
            discoveredBy = "auto",
            confidence = 0.5
        )

        template
    }

    override fun getUserProviderCount(): Flow<Int> {
        return providerDao.getUserProviderCount()
    }

    private suspend fun fetchFromApi(
        city: String,
        provider: WeatherProvider
    ): Result<WeatherData> = runCatching {
        val apiKey = provider.apiKey
        if (apiKey.isBlank()) {
            throw Exception("API key required for ${provider.name}")
        }

        val baseUrl = provider.baseUrl
        val url = if (baseUrl.contains("weatherapi")) {
            "$baseUrl?key=$apiKey&q=$city&lang=en&aqi=no"
        } else if (baseUrl.contains("openweathermap")) {
            "$baseUrl?q=$city&appid=$apiKey&units=metric&lang=en"
        } else {
            "$baseUrl?key=$apiKey&q=$city"
        }

        scraper.fetchFromOpenMeteo(city).getOrThrow()
    }

    private fun parseSelectors(template: String): ScrapingSelectors {
        return ScrapingSelectors(
            temperature = "",
            humidity = "",
            windSpeed = "",
            condition = "",
            feelsLike = "",
            pressure = ""
        )
    }

    private fun ProviderEntity.toDomain(): WeatherProvider {
        return WeatherProvider(
            id = id,
            name = name,
            baseUrl = baseUrl,
            urlTemplate = urlTemplate,
            type = try {
                ProviderType.valueOf(type)
            } catch (e: Exception) {
                ProviderType.SCRAPED
            },
            isActive = isActive,
            requiresApiKey = requiresApiKey,
            apiKey = apiKey,
            language = language,
            isBuiltIn = isBuiltIn
        )
    }

    private fun WeatherProvider.toEntity(
        createdAt: Long,
        updatedAt: Long
    ): ProviderEntity {
        return ProviderEntity(
            id = id,
            name = name,
            baseUrl = baseUrl,
            urlTemplate = urlTemplate,
            type = type.name,
            isActive = isActive,
            requiresApiKey = requiresApiKey,
            apiKey = apiKey,
            language = language,
            isBuiltIn = isBuiltIn,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }
}
