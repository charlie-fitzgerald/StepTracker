package com.steptracker.app.data.repository

import com.steptracker.app.data.dao.StepDataDao
import com.steptracker.app.data.model.StepData
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StepRepository @Inject constructor(
    private val stepDataDao: StepDataDao
) {
    
    fun getStepDataForDate(date: LocalDate): Flow<StepData?> {
        return stepDataDao.getRecentStepData(1).map { list ->
            list.firstOrNull { it.date == date }
        }
    }
    
    fun getRecentStepData(limit: Int): Flow<List<StepData>> {
        return stepDataDao.getRecentStepData(limit)
    }
    
    fun getStepDataForDateRange(startDate: LocalDate, endDate: LocalDate): Flow<List<StepData>> {
        return stepDataDao.getStepDataForDateRange(startDate, endDate)
    }
    
    suspend fun getTotalStepsForDateRange(startDate: LocalDate, endDate: LocalDate): Int {
        return stepDataDao.getTotalStepsForDateRange(startDate, endDate) ?: 0
    }
    
    suspend fun getAverageStepsForDateRange(startDate: LocalDate, endDate: LocalDate): Double {
        return stepDataDao.getAverageStepsForDateRange(startDate, endDate) ?: 0.0
    }
    
    suspend fun insertStepData(stepData: StepData) {
        stepDataDao.insertStepData(stepData)
    }
    
    suspend fun updateStepData(stepData: StepData) {
        stepDataDao.updateStepData(stepData)
    }
    
    suspend fun getOrCreateStepDataForDate(date: LocalDate): StepData {
        val existing = stepDataDao.getStepDataForDate(date)
        return existing ?: StepData(
            date = date,
            steps = 0,
            distance = 0.0,
            calories = 0
        )
    }
} 