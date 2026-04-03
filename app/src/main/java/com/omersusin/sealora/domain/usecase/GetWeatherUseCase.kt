package com.omersusin.sealora.domain.usecase

import com.omersusin.sealora.domain.model.WeatherData
import com.omersusin.sealora.domain.model.WeatherProvider
import com.omersusin.sealora.domain.repository.WeatherRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetWeatherUseCase @Inject constructor(
    private val repository: WeatherRepository
) {
    fun getActiveProviders(): Flow<List<WeatherProvider>> {
        return repository.getActiveProviders()
    }

    suspend fun getWeatherFromProvider(
        city: String,
        provider: WeatherProvider
    ): Result<WeatherData> {
        if (city.isBlank()) {
            return Result.failure(IllegalArgumentException("City name cannot be empty"))
        }
        return repository.getWeather(city.trim(), provider)
    }

    suspend fun getWeatherFromAll(city: String): List<WeatherData> {
        if (city.isBlank()) return emptyList()
        return repository.getWeatherFromAll(city.trim())
    }

    fun getUserProviderCount(): Flow<Int> {
        return repository.getUserProviderCount()
    }
}
