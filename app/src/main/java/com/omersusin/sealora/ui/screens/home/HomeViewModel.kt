package com.omersusin.sealora.ui.screens.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.omersusin.sealora.domain.model.*
import com.omersusin.sealora.domain.usecase.GenerateReportUseCase
import com.omersusin.sealora.domain.usecase.GetWeatherUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val city: String = "",
    val weatherDataList: List<WeatherData> = emptyList(),
    val report: WeatherReport? = null,
    val isLoading: Boolean = false,
    val isGeneratingReport: Boolean = false,
    val error: String? = null,
    val showCityDialog: Boolean = false,
    val selectedHourlyIndex: Int = -1,
    val selectedDailyIndex: Int = -1,
    val chatMessages: List<AiChatMessage> = emptyList(),
    val chatInput: String = "",
    val isChatLoading: Boolean = false,
    val showChatSheet: Boolean = false
)

sealed class HomeEvent {
    data class LoadWeather(val city: String) : HomeEvent()
    data class UpdateCity(val city: String) : HomeEvent()
    data class SelectHourlyIndex(val index: Int) : HomeEvent()
    data class SelectDailyIndex(val index: Int) : HomeEvent()
    data class SendChatMessage(val message: String) : HomeEvent()
    object GenerateReport : HomeEvent()
    object RefreshWeather : HomeEvent()
    object ToggleCityDialog : HomeEvent()
    object ToggleChatSheet : HomeEvent()
    object ClearError : HomeEvent()
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getWeatherUseCase: GetWeatherUseCase,
    private val generateReportUseCase: GenerateReportUseCase
) : ViewModel() {

    companion object {
        private const val TAG = "HomeViewModel"
    }

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    val activeProviders: StateFlow<List<WeatherProvider>> =
        getWeatherUseCase.getActiveProviders()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun onEvent(event: HomeEvent) {
        when (event) {
            is HomeEvent.LoadWeather -> loadWeather(event.city)
            is HomeEvent.UpdateCity -> updateCity(event.city)
            is HomeEvent.SelectHourlyIndex -> selectHourly(event.index)
            is HomeEvent.SelectDailyIndex -> selectDaily(event.index)
            is HomeEvent.SendChatMessage -> sendChat(event.message)
            is HomeEvent.GenerateReport -> generateReport()
            is HomeEvent.RefreshWeather -> refreshWeather()
            is HomeEvent.ToggleCityDialog -> toggleCityDialog()
            is HomeEvent.ToggleChatSheet -> toggleChatSheet()
            is HomeEvent.ClearError -> clearError()
        }
    }

    private fun loadWeather(city: String) {
        if (city.isBlank()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, city = city, error = null) }

            try {
                val results = getWeatherUseCase.getWeatherFromAll(city)

                if (results.isEmpty()) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = "No weather data found for $city"
                        )
                    }
                    return@launch
                }

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        weatherDataList = results,
                        selectedHourlyIndex = -1,
                        selectedDailyIndex = -1,
                        showCityDialog = false
                    )
                }

                Log.d(TAG, "Weather loaded for $city: ${results.size} providers")

                generateReport()
            } catch (e: Exception) {
                Log.e(TAG, "Error loading weather: ${e.message}")
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Unknown error occurred"
                    )
                }
            }
        }
    }

    private fun generateReport() {
        val city = _uiState.value.city
        val data = _uiState.value.weatherDataList

        if (data.isEmpty()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isGeneratingReport = true) }

            generateReportUseCase.generate(city, data)
                .onSuccess { report ->
                    _uiState.update {
                        it.copy(isGeneratingReport = false, report = report)
                    }
                    Log.d(TAG, "Report generated successfully")
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isGeneratingReport = false,
                            error = "Report generation failed: ${error.message}"
                        )
                    }
                    Log.e(TAG, "Report failed: ${error.message}")
                }
        }
    }

    private fun sendChat(message: String) {
        if (message.isBlank()) return

        viewModelScope.launch {
            val userMsg = AiChatMessage(role = "user", content = message)
            generateReportUseCase.saveChatMessage(_uiState.value.city, userMsg)

            _uiState.update {
                it.copy(
                    chatMessages = it.chatMessages + userMsg,
                    chatInput = "",
                    isChatLoading = true
                )
            }

            generateReportUseCase.chat(
                message = message,
                city = _uiState.value.city,
                weatherContext = _uiState.value.weatherDataList
            ).onSuccess { response ->
                val aiMsg = AiChatMessage(role = "assistant", content = response)
                generateReportUseCase.saveChatMessage(_uiState.value.city, aiMsg)
                _uiState.update {
                    it.copy(
                        chatMessages = it.chatMessages + aiMsg,
                        isChatLoading = false
                    )
                }
            }.onFailure { error ->
                val errorMsg = AiChatMessage(
                    role = "assistant",
                    content = "Üzgünüm, bir hata oluştu: ${error.message}"
                )
                _uiState.update {
                    it.copy(
                        chatMessages = it.chatMessages + errorMsg,
                        isChatLoading = false
                    )
                }
            }
        }
    }

    private fun refreshWeather() {
        val city = _uiState.value.city
        if (city.isNotBlank()) {
            loadWeather(city)
        }
    }

    private fun updateCity(city: String) {
        _uiState.update { it.copy(city = city) }
    }

    private fun selectHourly(index: Int) {
        _uiState.update {
            it.copy(selectedHourlyIndex = if (it.selectedHourlyIndex == index) -1 else index)
        }
    }

    private fun selectDaily(index: Int) {
        _uiState.update {
            it.copy(selectedDailyIndex = if (it.selectedDailyIndex == index) -1 else index)
        }
    }

    private fun toggleCityDialog() {
        _uiState.update { it.copy(showCityDialog = !it.showCityDialog) }
    }

    private fun toggleChatSheet() {
        _uiState.update { it.copy(showChatSheet = !it.showChatSheet) }
    }

    private fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
