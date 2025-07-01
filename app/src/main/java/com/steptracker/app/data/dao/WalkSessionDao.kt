package com.steptracker.app.data.dao

import androidx.room.*
import com.steptracker.app.data.model.WalkSession
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

@Dao
interface WalkSessionDao {
    
    @Query("SELECT * FROM walk_sessions ORDER BY startTime DESC")
    fun getAllWalkSessions(): Flow<List<WalkSession>>
    
    @Query("SELECT * FROM walk_sessions WHERE startTime BETWEEN :startDate AND :endDate ORDER BY startTime DESC")
    fun getWalkSessionsForDateRange(startDate: LocalDateTime, endDate: LocalDateTime): Flow<List<WalkSession>>
    
    @Query("SELECT * FROM walk_sessions WHERE id = :id")
    suspend fun getWalkSessionById(id: Long): WalkSession?
    
    @Query("SELECT * FROM walk_sessions WHERE isSaved = 1 ORDER BY timestamp DESC")
    fun getSavedWalkSessions(): Flow<List<WalkSession>>
    
    @Insert
    suspend fun insertWalkSession(walkSession: WalkSession): Long
    
    @Update
    suspend fun updateWalkSession(walkSession: WalkSession)
    
    @Delete
    suspend fun deleteWalkSession(walkSession: WalkSession)
    
    @Query("SELECT SUM(distance) FROM walk_sessions WHERE startTime BETWEEN :startDate AND :endDate")
    suspend fun getTotalDistanceForDateRange(startDate: LocalDateTime, endDate: LocalDateTime): Double?
    
    @Query("SELECT AVG(averagePace) FROM walk_sessions WHERE startTime BETWEEN :startDate AND :endDate")
    suspend fun getAveragePaceForDateRange(startDate: LocalDateTime, endDate: LocalDateTime): Double?
} 