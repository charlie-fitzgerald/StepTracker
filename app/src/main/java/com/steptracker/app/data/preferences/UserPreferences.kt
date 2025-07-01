package com.steptracker.app.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

class UserPreferences(private val context: Context) {
    
    companion object {
        private val UNITS_SYSTEM = stringPreferencesKey("units_system")
        private val TEMPERATURE_UNIT = stringPreferencesKey("temperature_unit")
        private val THEME_MODE = stringPreferencesKey("theme_mode")
        private val MILESTONE_NOTIFICATIONS = booleanPreferencesKey("milestone_notifications")
        private val MILESTONE_DISTANCE = doublePreferencesKey("milestone_distance")
        private val DAILY_GOAL = intPreferencesKey("daily_goal")
        private val WEEKLY_GOAL = intPreferencesKey("weekly_goal")
    }
    
    val unitsSystem: Flow<UnitsSystem> = context.dataStore.data.map { preferences ->
        when (preferences[UNITS_SYSTEM]) {
            "metric" -> UnitsSystem.METRIC
            "imperial" -> UnitsSystem.IMPERIAL
            else -> UnitsSystem.METRIC
        }
    }
    
    val temperatureUnit: Flow<TemperatureUnit> = context.dataStore.data.map { preferences ->
        when (preferences[TEMPERATURE_UNIT]) {
            "fahrenheit" -> TemperatureUnit.FAHRENHEIT
            "celsius" -> TemperatureUnit.CELSIUS
            else -> TemperatureUnit.CELSIUS
        }
    }
    
    val themeMode: Flow<ThemeMode> = context.dataStore.data.map { preferences ->
        when (preferences[THEME_MODE]) {
            "light" -> ThemeMode.LIGHT
            "dark" -> ThemeMode.DARK
            "system" -> ThemeMode.SYSTEM
            else -> ThemeMode.SYSTEM
        }
    }
    
    val milestoneNotifications: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[MILESTONE_NOTIFICATIONS] ?: true
    }
    
    val milestoneDistance: Flow<Double> = context.dataStore.data.map { preferences ->
        preferences[MILESTONE_DISTANCE] ?: 0.25 // Default 0.25 miles/km
    }
    
    val dailyGoal: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[DAILY_GOAL] ?: 10000
    }
    
    val weeklyGoal: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[WEEKLY_GOAL] ?: 70000
    }
    
    suspend fun setUnitsSystem(unitsSystem: UnitsSystem) {
        context.dataStore.edit { preferences ->
            preferences[UNITS_SYSTEM] = when (unitsSystem) {
                UnitsSystem.METRIC -> "metric"
                UnitsSystem.IMPERIAL -> "imperial"
            }
        }
    }
    
    suspend fun setTemperatureUnit(temperatureUnit: TemperatureUnit) {
        context.dataStore.edit { preferences ->
            preferences[TEMPERATURE_UNIT] = when (temperatureUnit) {
                TemperatureUnit.CELSIUS -> "celsius"
                TemperatureUnit.FAHRENHEIT -> "fahrenheit"
            }
        }
    }
    
    suspend fun setThemeMode(themeMode: ThemeMode) {
        context.dataStore.edit { preferences ->
            preferences[THEME_MODE] = when (themeMode) {
                ThemeMode.LIGHT -> "light"
                ThemeMode.DARK -> "dark"
                ThemeMode.SYSTEM -> "system"
            }
        }
    }
    
    suspend fun setMilestoneNotifications(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[MILESTONE_NOTIFICATIONS] = enabled
        }
    }
    
    suspend fun setMilestoneDistance(distance: Double) {
        context.dataStore.edit { preferences ->
            preferences[MILESTONE_DISTANCE] = distance
        }
    }
    
    suspend fun setDailyGoal(goal: Int) {
        context.dataStore.edit { preferences ->
            preferences[DAILY_GOAL] = goal
        }
    }
    
    suspend fun setWeeklyGoal(goal: Int) {
        context.dataStore.edit { preferences ->
            preferences[WEEKLY_GOAL] = goal
        }
    }
}

enum class UnitsSystem {
    METRIC, IMPERIAL
}

enum class TemperatureUnit {
    CELSIUS, FAHRENHEIT
}

enum class ThemeMode {
    LIGHT, DARK, SYSTEM
} 