package com.steptracker.app.data.repository

import com.steptracker.app.data.dao.WalkSessionDao
import com.steptracker.app.data.model.WalkSession
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WalkRepository @Inject constructor(
    private val walkSessionDao: WalkSessionDao
) {
    
    fun getAllWalkSessions(): Flow<List<WalkSession>> {
        return walkSessionDao.getAllWalkSessions()
    }
    
    fun getWalkSessionsForDateRange(startDate: LocalDateTime, endDate: LocalDateTime): Flow<List<WalkSession>> {
        return walkSessionDao.getWalkSessionsForDateRange(startDate, endDate)
    }
    
    suspend fun getWalkSessionById(id: Long): WalkSession? {
        return walkSessionDao.getWalkSessionById(id)
    }
    
    fun getSavedWalkSessions(): Flow<List<WalkSession>> {
        return walkSessionDao.getSavedWalkSessions()
    }
    
    suspend fun insertWalkSession(walkSession: WalkSession): Long {
        return walkSessionDao.insertWalkSession(walkSession)
    }
    
    suspend fun updateWalkSession(walkSession: WalkSession) {
        walkSessionDao.updateWalkSession(walkSession)
    }
    
    suspend fun deleteWalkSession(walkSession: WalkSession) {
        walkSessionDao.deleteWalkSession(walkSession)
    }
    
    suspend fun getTotalDistanceForDateRange(startDate: LocalDateTime, endDate: LocalDateTime): Double {
        return walkSessionDao.getTotalDistanceForDateRange(startDate, endDate) ?: 0.0
    }
    
    suspend fun getAveragePaceForDateRange(startDate: LocalDateTime, endDate: LocalDateTime): Double {
        return walkSessionDao.getAveragePaceForDateRange(startDate, endDate) ?: 0.0
    }
} 