package com.steptracker.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.steptracker.app.data.model.StepData
import com.steptracker.app.data.model.WeatherData
import com.steptracker.app.data.preferences.UserPreferences
import com.steptracker.app.data.repository.StepRepository
import com.steptracker.app.data.repository.WeatherRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val stepRepository: StepRepository,
    private val weatherRepository: WeatherRepository,
    private val userPreferences: UserPreferences
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
    
    init {
        loadTodayStepData()
        loadWeeklyStats()
        loadMonthlyStats()
        observeUserPreferences()
    }
    
    private fun loadTodayStepData() {
        viewModelScope.launch {
            stepRepository.getStepDataForDate(LocalDate.now())
                .collect { stepData ->
                    _uiState.update { it.copy(
                        todaySteps = stepData?.steps ?: 0,
                        todayDistance = stepData?.distance ?: 0.0,
                        todayCalories = stepData?.calories ?: 0
                    ) }
                }
        }
    }
    
    private fun loadWeeklyStats() {
        viewModelScope.launch {
            val endDate = LocalDate.now()
            val startDate = endDate.minusDays(6)
            
            stepRepository.getStepDataForDateRange(startDate, endDate)
                .collect { stepDataList ->
                    val totalSteps = stepDataList.sumOf { it.steps }
                    val averageSteps = if (stepDataList.isNotEmpty()) totalSteps / stepDataList.size else 0
                    
                    _uiState.update { it.copy(
                        weeklyTotal = totalSteps,
                        weeklyAverage = averageSteps
                    ) }
                }
        }
    }
    
    private fun loadMonthlyStats() {
        viewModelScope.launch {
            val endDate = LocalDate.now()
            val startDate = endDate.minusDays(29)
            
            val totalSteps = stepRepository.getTotalStepsForDateRange(startDate, endDate)
            _uiState.update { it.copy(monthlyTotal = totalSteps) }
        }
    }
    
    private fun observeUserPreferences() {
        viewModelScope.launch {
            combine(
                userPreferences.unitsSystem,
                userPreferences.temperatureUnit,
                userPreferences.dailyGoal,
                userPreferences.weeklyGoal
            ) { unitsSystem, temperatureUnit, dailyGoal, weeklyGoal ->
                _uiState.update { it.copy(
                    unitsSystem = unitsSystem,
                    temperatureUnit = temperatureUnit,
                    dailyGoal = dailyGoal,
                    weeklyGoal = weeklyGoal
                ) }
            }.collect()
        }
    }
    
    fun loadWeather(lat: Double, lon: Double, apiKey: String) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isWeatherLoading = true) }
                
                userPreferences.temperatureUnit.collect { tempUnit ->
                    val units = if (tempUnit == com.steptracker.app.data.preferences.TemperatureUnit.FAHRENHEIT) "imperial" else "metric"
                    weatherRepository.getCurrentWeather(lat, lon, apiKey, units)
                        .collect { weatherData ->
                            _uiState.update { it.copy(
                                weatherData = weatherData,
                                isWeatherLoading = false
                            ) }
                        }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(
                    weatherError = e.message ?: "Failed to load weather",
                    isWeatherLoading = false
                ) }
            }
        }
    }
    
    fun updateStepCount(steps: Int) {
        viewModelScope.launch {
            val today = LocalDate.now()
            val currentStepData = stepRepository.getOrCreateStepDataForDate(today)
            val updatedStepData = currentStepData.copy(steps = steps)
            stepRepository.updateStepData(updatedStepData)
        }
    }
}

data class HomeUiState(
    val todaySteps: Int = 0,
    val todayDistance: Double = 0.0,
    val todayCalories: Int = 0,
    val weeklyTotal: Int = 0,
    val weeklyAverage: Int = 0,
    val monthlyTotal: Int = 0,
    val weatherData: WeatherData? = null,
    val isWeatherLoading: Boolean = false,
    val weatherError: String? = null,
    val unitsSystem: com.steptracker.app.data.preferences.UnitsSystem = com.steptracker.app.data.preferences.UnitsSystem.METRIC,
    val temperatureUnit: com.steptracker.app.data.preferences.TemperatureUnit = com.steptracker.app.data.preferences.TemperatureUnit.CELSIUS,
    val dailyGoal: Int = 10000,
    val weeklyGoal: Int = 70000
) 