package com.omersusin.sealora.domain.usecase

import com.omersusin.sealora.domain.model.ProviderTemplate
import com.omersusin.sealora.domain.model.WeatherProvider
import com.omersusin.sealora.domain.repository.WeatherRepository
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject

class ManageProvidersUseCase @Inject constructor(
    private val repository: WeatherRepository
) {
    fun getAllProviders(): Flow<List<WeatherProvider>> {
        return repository.getAllProviders()
    }

    fun getActiveProviders(): Flow<List<WeatherProvider>> {
        return repository.getActiveProviders()
    }

    suspend fun addProvider(
        name: String,
        baseUrl: String,
        urlTemplate: String,
        requiresApiKey: Boolean = false,
        apiKey: String = "",
        language: String = "en"
    ): Result<WeatherProvider> = runCatching {
        val provider = WeatherProvider(
            id = UUID.randomUUID().toString(),
            name = name,
            baseUrl = baseUrl,
            urlTemplate = urlTemplate,
            type = com.omersusin.sealora.domain.model.ProviderType.SCRAPED,
            isActive = true,
            requiresApiKey = requiresApiKey,
            apiKey = apiKey,
            language = language,
            isBuiltIn = false
        )
        repository.saveProvider(provider)
        provider
    }

    suspend fun addFromTemplate(template: ProviderTemplate): Result<WeatherProvider> = runCatching {
        val provider = WeatherProvider(
            id = template.providerId,
            name = template.urlPattern,
            baseUrl = template.urlPattern,
            urlTemplate = template.urlPattern,
            type = com.omersusin.sealora.domain.model.ProviderType.AI_DISCOVERED,
            isActive = true,
            requiresApiKey = false,
            apiKey = "",
            language = "en",
            isBuiltIn = false
        )
        repository.saveProvider(provider)
        provider
    }

    suspend fun removeProvider(id: String) {
        repository.deleteProvider(id)
    }

    suspend fun toggleProvider(id: String, active: Boolean) {
        repository.toggleProvider(id, active)
    }

    suspend fun discoverTemplate(url: String, city: String): Result<ProviderTemplate> {
        return repository.discoverProviderTemplate(url, city)
    }
}
