package com.steptracker.app.data.model

data class WeatherData(
    val temperature: Double,
    val feelsLike: Double,
    val humidity: Int,
    val windSpeed: Double,
    val description: String,
    val icon: String,
    val timestamp: Long = System.currentTimeMillis()
) 