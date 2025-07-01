package com.steptracker.app.data.dao

import androidx.room.*
import com.steptracker.app.data.model.StepData
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface StepDataDao {
    
    @Query("SELECT * FROM step_data WHERE date = :date")
    suspend fun getStepDataForDate(date: LocalDate): StepData?
    
    @Query("SELECT * FROM step_data ORDER BY date DESC LIMIT :limit")
    fun getRecentStepData(limit: Int): Flow<List<StepData>>
    
    @Query("SELECT * FROM step_data WHERE date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    fun getStepDataForDateRange(startDate: LocalDate, endDate: LocalDate): Flow<List<StepData>>
    
    @Query("SELECT SUM(steps) FROM step_data WHERE date BETWEEN :startDate AND :endDate")
    suspend fun getTotalStepsForDateRange(startDate: LocalDate, endDate: LocalDate): Int?
    
    @Query("SELECT AVG(steps) FROM step_data WHERE date BETWEEN :startDate AND :endDate")
    suspend fun getAverageStepsForDateRange(startDate: LocalDate, endDate: LocalDate): Double?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStepData(stepData: StepData)
    
    @Update
    suspend fun updateStepData(stepData: StepData)
    
    @Query("DELETE FROM step_data WHERE date < :date")
    suspend fun deleteStepDataBeforeDate(date: LocalDate)
} 