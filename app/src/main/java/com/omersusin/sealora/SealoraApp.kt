package com.omersusin.sealora

import android.app.Application
import android.util.Log
import com.omersusin.sealora.data.local.ProviderDao
import com.omersusin.sealora.data.local.SealoraDatabase
import com.omersusin.sealora.data.local.entity.ProviderEntity
import com.omersusin.sealora.domain.model.ProviderType
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class SealoraApp : Application() {

    @Inject
    lateinit var database: SealoraDatabase

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        Log.d("SealoraApp", "Application started")
        seedBuiltInProviders()
    }

    private fun seedBuiltInProviders() {
        appScope.launch {
            val dao = database.providerDao()
            val existing = dao.getAllProviders().first()

            if (existing.isEmpty()) {
                Log.d("SealoraApp", "Seeding built-in providers...")

                val builtInProviders = listOf(
                    ProviderEntity(
                        id = "open-meteo",
                        name = "Open-Meteo",
                        baseUrl = "https://api.open-meteo.com",
                        urlTemplate = "https://api.open-meteo.com/v1/forecast",
                        type = ProviderType.BUILTIN.name,
                        isActive = true,
                        requiresApiKey = false,
                        apiKey = "",
                        language = "en",
                        isBuiltIn = true,
                        createdAt = System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis()
                    ),
                    ProviderEntity(
                        id = "weatherapi",
                        name = "WeatherAPI.com",
                        baseUrl = "https://api.weatherapi.com/v1",
                        urlTemplate = "https://api.weatherapi.com/v1/forecast.json",
                        type = ProviderType.API.name,
                        isActive = false,
                        requiresApiKey = true,
                        apiKey = "",
                        language = "en",
                        isBuiltIn = true,
                        createdAt = System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis()
                    ),
                    ProviderEntity(
                        id = "openweathermap",
                        name = "OpenWeatherMap",
                        baseUrl = "https://api.openweathermap.org/data/2.5",
                        urlTemplate = "https://api.openweathermap.org/data/2.5/weather",
                        type = ProviderType.API.name,
                        isActive = false,
                        requiresApiKey = true,
                        apiKey = "",
                        language = "en",
                        isBuiltIn = true,
                        createdAt = System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis()
                    ),
                    ProviderEntity(
                        id = "yr-no",
                        name = "Yr.no (Norveç)",
                        baseUrl = "https://api.met.no/weatherapi",
                        urlTemplate = "https://api.met.no/weatherapi/locationforecast/2.0/compact",
                        type = ProviderType.API.name,
                        isActive = false,
                        requiresApiKey = false,
                        apiKey = "",
                        language = "en",
                        isBuiltIn = true,
                        createdAt = System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis()
                    ),
                    ProviderEntity(
                        id = "wttr-in",
                        name = "wttr.in",
                        baseUrl = "https://wttr.in",
                        urlTemplate = "https://wttr.in/{city}?format=j1",
                        type = ProviderType.SCRAPED.name,
                        isActive = false,
                        requiresApiKey = false,
                        apiKey = "",
                        language = "en",
                        isBuiltIn = true,
                        createdAt = System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis()
                    )
                )

                builtInProviders.forEach { provider ->
                    dao.insertProvider(provider)
                }

                Log.d("SealoraApp", "Built-in providers seeded: ${builtInProviders.size}")
            } else {
                Log.d("SealoraApp", "Providers already exist: ${existing.size}")
            }
        }
    }
}
