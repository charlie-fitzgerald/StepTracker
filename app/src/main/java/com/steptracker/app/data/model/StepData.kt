package com.steptracker.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(tableName = "step_data")
data class StepData(
    @PrimaryKey val date: LocalDate,
    val steps: Int,
    val distance: Double, // in meters
    val calories: Int,
    val timestamp: Long = System.currentTimeMillis()
) 