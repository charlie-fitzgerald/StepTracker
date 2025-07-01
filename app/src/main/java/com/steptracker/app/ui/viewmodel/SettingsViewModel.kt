package com.steptracker.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.steptracker.app.data.preferences.UserPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userPreferences: UserPreferences
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()
    
    init {
        observeUserPreferences()
    }
    
    private fun observeUserPreferences() {
        viewModelScope.launch {
            combine(
                userPreferences.unitsSystem,
                userPreferences.temperatureUnit,
                userPreferences.themeMode,
                userPreferences.milestoneNotifications,
                userPreferences.milestoneDistance
            ) { unitsSystem, temperatureUnit, themeMode, milestoneNotifications, milestoneDistance ->
                _uiState.update { it.copy(
                    unitsSystem = unitsSystem,
                    temperatureUnit = temperatureUnit,
                    themeMode = themeMode,
                    milestoneNotifications = milestoneNotifications,
                    milestoneDistance = milestoneDistance
                ) }
            }.collect()
        }
    }
    
    fun toggleUnitsSystem() {
        viewModelScope.launch {
            val newSystem = if (uiState.value.unitsSystem == com.steptracker.app.data.preferences.UnitsSystem.METRIC) {
                com.steptracker.app.data.preferences.UnitsSystem.IMPERIAL
            } else {
                com.steptracker.app.data.preferences.UnitsSystem.METRIC
            }
            userPreferences.setUnitsSystem(newSystem)
        }
    }
    
    fun toggleTemperatureUnit() {
        viewModelScope.launch {
            val newUnit = if (uiState.value.temperatureUnit == com.steptracker.app.data.preferences.TemperatureUnit.CELSIUS) {
                com.steptracker.app.data.preferences.TemperatureUnit.FAHRENHEIT
            } else {
                com.steptracker.app.data.preferences.TemperatureUnit.CELSIUS
            }
            userPreferences.setTemperatureUnit(newUnit)
        }
    }
    
    fun showThemeDialog() {
        _uiState.update { it.copy(showThemeDialog = true) }
    }
    
    fun hideThemeDialog() {
        _uiState.update { it.copy(showThemeDialog = false) }
    }
    
    fun setThemeMode(themeMode: com.steptracker.app.data.preferences.ThemeMode) {
        viewModelScope.launch {
            userPreferences.setThemeMode(themeMode)
        }
    }
    
    fun toggleMilestoneNotifications() {
        viewModelScope.launch {
            val newValue = !uiState.value.milestoneNotifications
            userPreferences.setMilestoneNotifications(newValue)
        }
    }
    
    fun setMilestoneNotifications(enabled: Boolean) {
        viewModelScope.launch {
            userPreferences.setMilestoneNotifications(enabled)
        }
    }
    
    fun showMilestoneDistanceDialog() {
        _uiState.update { it.copy(showMilestoneDistanceDialog = true) }
    }
    
    fun hideMilestoneDistanceDialog() {
        _uiState.update { it.copy(showMilestoneDistanceDialog = false) }
    }
    
    fun setMilestoneDistance(distance: Double) {
        viewModelScope.launch {
            userPreferences.setMilestoneDistance(distance)
        }
    }
    
    fun openPrivacyPolicy() {
        // In a real app, this would open a web view or external browser
        // For now, just a placeholder
    }
    
    fun openHelpSupport() {
        // In a real app, this would open help/support screen or contact options
        // For now, just a placeholder
    }
}

data class SettingsUiState(
    val unitsSystem: com.steptracker.app.data.preferences.UnitsSystem = com.steptracker.app.data.preferences.UnitsSystem.METRIC,
    val temperatureUnit: com.steptracker.app.data.preferences.TemperatureUnit = com.steptracker.app.data.preferences.TemperatureUnit.CELSIUS,
    val themeMode: com.steptracker.app.data.preferences.ThemeMode = com.steptracker.app.data.preferences.ThemeMode.SYSTEM,
    val milestoneNotifications: Boolean = true,
    val milestoneDistance: Double = 0.25,
    val showThemeDialog: Boolean = false,
    val showMilestoneDistanceDialog: Boolean = false
) 