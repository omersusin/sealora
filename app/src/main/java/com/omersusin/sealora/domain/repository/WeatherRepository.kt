package com.omersusin.sealora.domain.repository

import com.omersusin.sealora.domain.model.*
import kotlinx.coroutines.flow.Flow

interface WeatherRepository {

    fun getActiveProviders(): Flow<List<WeatherProvider>>

    fun getAllProviders(): Flow<List<WeatherProvider>>

    suspend fun getWeather(city: String, provider: WeatherProvider): Result<WeatherData>

    suspend fun getWeatherFromAll(city: String): List<WeatherData>

    suspend fun saveProvider(provider: WeatherProvider)

    suspend fun deleteProvider(id: String)

    suspend fun toggleProvider(id: String, active: Boolean)

    suspend fun discoverProviderTemplate(url: String, city: String): Result<ProviderTemplate>

    fun getUserProviderCount(): Flow<Int>
}
