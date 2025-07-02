package com.steptracker.app.data.repository

import android.content.Context
import com.steptracker.app.R
import com.steptracker.app.data.api.WeatherApi
import com.steptracker.app.data.model.WeatherData
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WeatherRepository @Inject constructor(
    private val weatherApi: WeatherApi,
    @ApplicationContext private val context: Context
) {
    
    fun getCurrentWeather(lat: Double, lon: Double, units: String = "metric"): Flow<WeatherData> {
        return flow {
            try {
                val apiKey = context.getString(R.string.openweathermap_api_key)
                val response = weatherApi.getCurrentWeather(lat, lon, apiKey, units)
                val weatherData = WeatherData(
                    temperature = response.main.temp,
                    feelsLike = response.main.feels_like,
                    humidity = response.main.humidity,
                    windSpeed = response.wind.speed,
                    description = response.weather.firstOrNull()?.description ?: "",
                    icon = response.weather.firstOrNull()?.icon ?: ""
                )
                emit(weatherData)
            } catch (e: Exception) {
                // In a real app, you might want to emit a sealed class with error states
                throw e
            }
        }
    }
} 