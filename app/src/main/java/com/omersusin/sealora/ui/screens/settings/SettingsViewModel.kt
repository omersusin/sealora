package com.omersusin.sealora.ui.screens.settings

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.omersusin.sealora.domain.model.*
import com.omersusin.sealora.domain.usecase.GenerateReportUseCase
import com.omersusin.sealora.domain.usecase.ManageProvidersUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class SettingsUiState(
    val providers: List<WeatherProvider> = emptyList(),
    val aiConfigs: List<AiConfig> = emptyList(),
    val activeAiProvider: AiProvider? = null,
    val showProviderUrlDialog: Boolean = false,
    val showAiConfigDialog: Boolean = false,
    val newProviderUrl: String = "",
    val newProviderName: String = "",
    val newAiApiKey: String = "",
    val newAiModel: String = "",
    val newAiBaseUrl: String = "",
    val selectedAiProvider: AiProvider = AiProvider.OPENROUTER,
    val isDiscoveringUrl: Boolean = false,
    val discoveredTemplate: String = "",
    val discoveredConfidence: Double = 0.0,
    val isLoading: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val manageProvidersUseCase: ManageProvidersUseCase,
    private val generateReportUseCase: GenerateReportUseCase
) : ViewModel() {
    companion object { private const val TAG = "SettingsVM" }

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init { loadData() }

    private fun loadData() {
        viewModelScope.launch {
            combine(manageProvidersUseCase.getAllProviders(), generateReportUseCase.getAllAiConfigs()) { providers, configs -> Pair(providers, configs) }
                .collect { (providers, configs) ->
                    _uiState.update { it.copy(providers = providers, aiConfigs = configs, activeAiProvider = configs.find { c -> c.isActive }?.provider) }
                }
        }
    }

    fun toggleProvider(id: String, active: Boolean) {
        viewModelScope.launch {
            manageProvidersUseCase.toggleProvider(id, active)
            _uiState.update { it.copy(successMessage = if (active) "Etkinlestirildi" else "Devre disi") }
        }
    }

    fun deleteProvider(id: String) {
        viewModelScope.launch { manageProvidersUseCase.removeProvider(id); _uiState.update { it.copy(successMessage = "Silindi") } }
    }

    fun addProviderFromUrl() {
        val url = _uiState.value.newProviderUrl
        val name = _uiState.value.newProviderName
        if (url.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            manageProvidersUseCase.addProvider(
                name = name.ifBlank { url },
                baseUrl = url,
                urlTemplate = url,
                language = "en"
            ).onSuccess { p ->
                _uiState.update { it.copy(isLoading = false, showProviderUrlDialog = false, newProviderUrl = "", newProviderName = "", successMessage = "${p.name} eklendi") }
            }.onFailure { e ->
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun discoverUrlWithAi() {
        val url = _uiState.value.newProviderUrl
        if (url.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isDiscoveringUrl = true, discoveredTemplate = "", discoveredConfidence = 0.0) }
            generateReportUseCase.discoverUrl(url, "Istanbul")
                .onSuccess { template ->
                    _uiState.update { it.copy(isDiscoveringUrl = false, discoveredTemplate = template.urlPattern, discoveredConfidence = template.confidence) }
                    Log.d(TAG, "Discovered: ${template.urlPattern}")
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isDiscoveringUrl = false, error = "AI kesif basarisiz: ${e.message}") }
                }
        }
    }

    fun addProviderFromTemplate() {
        val url = _uiState.value.discoveredTemplate.ifBlank { _uiState.value.newProviderUrl }
        if (url.isBlank()) return
        viewModelScope.launch {
            manageProvidersUseCase.addProvider(
                name = "AI: $url",
                baseUrl = url,
                urlTemplate = url,
                language = "en"
            ).onSuccess { p ->
                _uiState.update { it.copy(showProviderUrlDialog = false, discoveredTemplate = "", newProviderUrl = "", newProviderName = "", successMessage = "${p.name} eklendi") }
            }.onFailure { e ->
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun saveAiConfig() {
        val state = _uiState.value
        viewModelScope.launch {
            val config = AiConfig(
                provider = state.selectedAiProvider,
                apiKey = state.newAiApiKey,
                model = state.newAiModel.ifBlank { state.selectedAiProvider.defaultModels().firstOrNull() ?: "" },
                baseUrl = state.newAiBaseUrl.ifBlank { state.selectedAiProvider.defaultBaseUrl },
                isActive = true
            )
            generateReportUseCase.saveAiConfig(config)
            generateReportUseCase.activateConfig(state.selectedAiProvider)
            _uiState.update { it.copy(showAiConfigDialog = false, newAiApiKey = "", newAiModel = "", newAiBaseUrl = "", successMessage = "${state.selectedAiProvider.displayName} yapilandirildi") }
        }
    }

    fun activateAiProvider(provider: AiProvider) {
        viewModelScope.launch { generateReportUseCase.activateConfig(provider); _uiState.update { it.copy(successMessage = "${provider.displayName} aktif") } }
    }

    fun deleteAiConfig(provider: AiProvider) {
        viewModelScope.launch { generateReportUseCase.deleteConfig(provider); _uiState.update { it.copy(successMessage = "${provider.displayName} silindi") } }
    }

    fun updateField(field: SettingsField, value: String) {
        _uiState.update { s -> when (field) {
            SettingsField.NEW_PROVIDER_URL -> s.copy(newProviderUrl = value)
            SettingsField.NEW_PROVIDER_NAME -> s.copy(newProviderName = value)
            SettingsField.AI_API_KEY -> s.copy(newAiApiKey = value)
            SettingsField.AI_MODEL -> s.copy(newAiModel = value)
            SettingsField.AI_BASE_URL -> s.copy(newAiBaseUrl = value)
        }}
    }

    fun selectAiProvider(provider: AiProvider) {
        _uiState.update { it.copy(selectedAiProvider = provider, newAiModel = "", newAiBaseUrl = provider.defaultBaseUrl) }
    }

    fun showAddProviderDialog(show: Boolean) { _uiState.update { it.copy(showProviderUrlDialog = show) } }
    fun showAiConfigDialog(show: Boolean) { _uiState.update { it.copy(showAiConfigDialog = show) } }
    fun clearMessages() { _uiState.update { it.copy(error = null, successMessage = null) } }
}

enum class SettingsField { NEW_PROVIDER_URL, NEW_PROVIDER_NAME, AI_API_KEY, AI_MODEL, AI_BASE_URL }
