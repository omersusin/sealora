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
import javax.inject.Inject

data class SettingsUiState(
    val providers: List<WeatherProvider> = emptyList(),
    val aiConfigs: List<AiConfig> = emptyList(),
    val activeAiProvider: AiProvider? = null,
    val showAddProviderDialog: Boolean = false,
    val showAiConfigDialog: Boolean = false,
    val showProviderUrlDialog: Boolean = false,
    val newProviderUrl: String = "",
    val newProviderName: String = "",
    val newAiApiKey: String = "",
    val newAiModel: String = "",
    val newAiBaseUrl: String = "",
    val selectedAiProvider: AiProvider = AiProvider.OPENROUTER,
    val isDiscoveringUrl: Boolean = false,
    val discoveredTemplate: ProviderTemplate? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val manageProvidersUseCase: ManageProvidersUseCase,
    private val generateReportUseCase: GenerateReportUseCase
) : ViewModel() {

    companion object {
        private const val TAG = "SettingsViewModel"
    }

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            combine(
                manageProvidersUseCase.getAllProviders(),
                generateReportUseCase.getAllAiConfigs()
            ) { providers, aiConfigs ->
                Pair(providers, aiConfigs)
            }.collect { (providers, aiConfigs) ->
                _uiState.update {
                    it.copy(
                        providers = providers,
                        aiConfigs = aiConfigs,
                        activeAiProvider = aiConfigs.find { c -> c.isActive }?.provider
                    )
                }
            }
        }
    }

    fun toggleProvider(id: String, active: Boolean) {
        viewModelScope.launch {
            manageProvidersUseCase.toggleProvider(id, active)
            val msg = if (active) "Sağlayıcı etkinleştirildi" else "Sağlayıcı devre dışı"
            _uiState.update { it.copy(successMessage = msg) }
        }
    }

    fun deleteProvider(id: String) {
        viewModelScope.launch {
            manageProvidersUseCase.removeProvider(id)
            _uiState.update { it.copy(successMessage = "Sağlayıcı silindi") }
        }
    }

    fun addProviderFromUrl() {
        val url = _uiState.value.newProviderUrl
        val name = _uiState.value.newProviderName

        if (url.isBlank()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isDiscoveringUrl = true) }

            manageProvidersUseCase.addProvider(
                name = name.ifBlank { url },
                baseUrl = url,
                urlTemplate = url,
                language = "en"
            ).onSuccess { provider ->
                _uiState.update {
                    it.copy(
                        isDiscoveringUrl = false,
                        showProviderUrlDialog = false,
                        newProviderUrl = "",
                        newProviderName = "",
                        successMessage = "${provider.name} eklendi"
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isDiscoveringUrl = false,
                        error = error.message
                    )
                }
            }
        }
    }

    fun discoverUrlWithAi() {
        val url = _uiState.value.newProviderUrl

        if (url.isBlank()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isDiscoveringUrl = true) }

            generateReportUseCase.discoverUrl(url, "Istanbul")
                .onSuccess { template ->
                    _uiState.update {
                        it.copy(
                            isDiscoveringUrl = false,
                            discoveredTemplate = template
                        )
                    }
                    Log.d(TAG, "URL pattern discovered: ${template.urlPattern}")
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isDiscoveringUrl = false,
                            error = "AI URL keşfi başarısız: ${error.message}"
                        )
                    }
                }
        }
    }

    fun addProviderFromTemplate() {
        val template = _uiState.value.discoveredTemplate ?: return

        viewModelScope.launch {
            manageProvidersUseCase.addFromTemplate(template)
                .onSuccess { provider ->
                    _uiState.update {
                        it.copy(
                            showProviderUrlDialog = false,
                            discoveredTemplate = null,
                            newProviderUrl = "",
                            newProviderName = "",
                            successMessage = "${provider.name} eklendi"
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(error = error.message) }
                }
        }
    }

    fun saveAiConfig() {
        val state = _uiState.value

        viewModelScope.launch {
            val config = AiConfig(
                provider = state.selectedAiProvider,
                apiKey = state.newAiApiKey,
                model = state.newAiModel.ifBlank {
                    state.selectedAiProvider.defaultModels().firstOrNull() ?: ""
                },
                baseUrl = state.newAiBaseUrl.ifBlank { state.selectedAiProvider.defaultBaseUrl },
                isActive = true
            )

            generateReportUseCase.saveAiConfig(config)
            generateReportUseCase.activateConfig(state.selectedAiProvider)

            _uiState.update {
                it.copy(
                    showAiConfigDialog = false,
                    newAiApiKey = "",
                    newAiModel = "",
                    newAiBaseUrl = "",
                    successMessage = "${state.selectedAiProvider.displayName} yapılandırıldı"
                )
            }
        }
    }

    fun activateAiProvider(provider: AiProvider) {
        viewModelScope.launch {
            generateReportUseCase.activateConfig(provider)
            _uiState.update {
                it.copy(successMessage = "${provider.displayName} aktif edildi")
            }
        }
    }

    fun deleteAiConfig(provider: AiProvider) {
        viewModelScope.launch {
            generateReportUseCase.deleteConfig(provider)
            _uiState.update { it.copy(successMessage = "${provider.displayName} silindi") }
        }
    }

    fun updateField(field: SettingsField, value: String) {
        _uiState.update { state ->
            when (field) {
                SettingsField.NEW_PROVIDER_URL -> state.copy(newProviderUrl = value)
                SettingsField.NEW_PROVIDER_NAME -> state.copy(newProviderName = value)
                SettingsField.AI_API_KEY -> state.copy(newAiApiKey = value)
                SettingsField.AI_MODEL -> state.copy(newAiModel = value)
                SettingsField.AI_BASE_URL -> state.copy(newAiBaseUrl = value)
            }
        }
    }

    fun selectAiProvider(provider: AiProvider) {
        _uiState.update {
            it.copy(
                selectedAiProvider = provider,
                newAiModel = "",
                newAiBaseUrl = provider.defaultBaseUrl
            )
        }
    }

    fun showAddProviderDialog(show: Boolean) {
        _uiState.update { it.copy(showProviderUrlDialog = show) }
    }

    fun showAiConfigDialog(show: Boolean) {
        _uiState.update { it.copy(showAiConfigDialog = show) }
    }

    fun clearMessages() {
        _uiState.update { it.copy(error = null, successMessage = null) }
    }
}

enum class SettingsField {
    NEW_PROVIDER_URL,
    NEW_PROVIDER_NAME,
    AI_API_KEY,
    AI_MODEL,
    AI_BASE_URL
}
