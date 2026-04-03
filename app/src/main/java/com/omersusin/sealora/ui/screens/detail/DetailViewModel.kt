package com.omersusin.sealora.ui.screens.detail

import androidx.lifecycle.ViewModel
import com.omersusin.sealora.domain.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

data class DetailUiState(
    val weatherData: WeatherData? = null,
    val report: WeatherReport? = null,
    val selectedTabIndex: Int = 0,
    val expandedDailyIndex: Int = -1,
    val showHourlyDetail: Boolean = false,
    val selectedHourlyData: HourlyData? = null
)

@HiltViewModel
class DetailViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(DetailUiState())
    val uiState: StateFlow<DetailUiState> = _uiState.asStateFlow()

    fun loadData(weatherData: WeatherData, report: WeatherReport?) {
        _uiState.update {
            it.copy(
                weatherData = weatherData,
                report = report
            )
        }
    }

    fun selectTab(index: Int) {
        _uiState.update { it.copy(selectedTabIndex = index) }
    }

    fun expandDaily(index: Int) {
        _uiState.update {
            it.copy(expandedDailyIndex = if (it.expandedDailyIndex == index) -1 else index)
        }
    }

    fun showHourlyDetail(data: HourlyData) {
        _uiState.update {
            it.copy(showHourlyDetail = true, selectedHourlyData = data)
        }
    }

    fun dismissHourlyDetail() {
        _uiState.update {
            it.copy(showHourlyDetail = false, selectedHourlyData = null)
        }
    }
}
