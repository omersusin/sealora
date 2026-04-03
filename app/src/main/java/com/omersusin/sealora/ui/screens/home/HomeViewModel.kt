package com.omersusin.sealora.ui.screens.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.omersusin.sealora.domain.model.*
import com.omersusin.sealora.domain.usecase.GenerateReportUseCase
import com.omersusin.sealora.domain.usecase.GetWeatherUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val city: String = "", val weatherDataList: List<WeatherData> = emptyList(),
    val report: WeatherReport? = null, val isLoading: Boolean = false,
    val isGeneratingReport: Boolean = false, val error: String? = null,
    val selectedHourlyIndex: Int = -1, val selectedDailyIndex: Int = -1,
    val chatMessages: List<AiChatMessage> = emptyList(), val chatInput: String = "",
    val isChatLoading: Boolean = false, val showChatSheet: Boolean = false
)

sealed class HomeEvent {
    data class LoadWeather(val city: String) : HomeEvent()
    data class UpdateCity(val city: String) : HomeEvent()
    data class SelectHourlyIndex(val index: Int) : HomeEvent()
    data class SelectDailyIndex(val index: Int) : HomeEvent()
    data class SendChatMessage(val message: String) : HomeEvent()
    object GenerateReport : HomeEvent()
    object RefreshWeather : HomeEvent()
    object ToggleChatSheet : HomeEvent()
    object ClearError : HomeEvent()
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getWeatherUseCase: GetWeatherUseCase,
    private val generateReportUseCase: GenerateReportUseCase
) : ViewModel() {
    companion object { private const val TAG = "HomeViewModel" }
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    fun onEvent(event: HomeEvent) {
        when (event) {
            is HomeEvent.LoadWeather -> loadWeather(event.city)
            is HomeEvent.UpdateCity -> _uiState.update { it.copy(city = event.city) }
            is HomeEvent.SelectHourlyIndex -> _uiState.update { it.copy(selectedHourlyIndex = if (it.selectedHourlyIndex == event.index) -1 else event.index) }
            is HomeEvent.SelectDailyIndex -> _uiState.update { it.copy(selectedDailyIndex = if (it.selectedDailyIndex == event.index) -1 else event.index) }
            is HomeEvent.SendChatMessage -> sendChat(event.message)
            is HomeEvent.GenerateReport -> generateReport()
            is HomeEvent.RefreshWeather -> { val c = _uiState.value.city; if (c.isNotBlank()) loadWeather(c) }
            is HomeEvent.ToggleChatSheet -> _uiState.update { it.copy(showChatSheet = !it.showChatSheet) }
            is HomeEvent.ClearError -> _uiState.update { it.copy(error = null) }
        }
    }

    private fun loadWeather(city: String) {
        if (city.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, city = city, error = null) }
            try {
                val results = getWeatherUseCase.getWeatherFromAll(city)
                if (results.isEmpty()) {
                    _uiState.update { it.copy(isLoading = false, error = "No weather data found for $city. Check your providers.") }
                    return@launch
                }
                _uiState.update { it.copy(isLoading = false, weatherDataList = results, selectedHourlyIndex = -1, selectedDailyIndex = -1) }
                Log.d(TAG, "Loaded ${results.size} providers for $city")
                generateReport()
            } catch (e: Exception) {
                Log.e(TAG, "Load error: ${e.message}", e)
                _uiState.update { it.copy(isLoading = false, error = e.message ?: "Unknown error") }
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
                .onSuccess { r -> _uiState.update { it.copy(isGeneratingReport = false, report = r) }; Log.d(TAG, "Report OK") }
                .onFailure { e -> _uiState.update { it.copy(isGeneratingReport = false, error = "Report: ${e.message}") }; Log.e(TAG, "Report fail: ${e.message}") }
        }
    }

    private fun sendChat(message: String) {
        if (message.isBlank()) return
        viewModelScope.launch {
            val userMsg = AiChatMessage(role = "user", content = message)
            generateReportUseCase.saveChatMessage(_uiState.value.city, userMsg)
            _uiState.update { it.copy(chatMessages = it.chatMessages + userMsg, chatInput = "", isChatLoading = true) }
            generateReportUseCase.chat(message, _uiState.value.city, _uiState.value.weatherDataList)
                .onSuccess { resp ->
                    val aiMsg = AiChatMessage(role = "assistant", content = resp)
                    generateReportUseCase.saveChatMessage(_uiState.value.city, aiMsg)
                    _uiState.update { it.copy(chatMessages = it.chatMessages + aiMsg, isChatLoading = false) }
                }
                .onFailure { e ->
                    val errMsg = AiChatMessage(role = "assistant", content = "Hata: ${e.message}")
                    _uiState.update { it.copy(chatMessages = it.chatMessages + errMsg, isChatLoading = false) }
                }
        }
    }
}
