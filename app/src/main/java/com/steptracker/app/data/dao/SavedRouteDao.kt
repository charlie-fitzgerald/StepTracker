package com.steptracker.app.data.dao

import androidx.room.*
import com.steptracker.app.data.model.SavedRoute
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedRouteDao {
    
    @Query("SELECT * FROM saved_routes ORDER BY timestamp DESC")
    fun getAllSavedRoutes(): Flow<List<SavedRoute>>
    
    @Query("SELECT * FROM saved_routes WHERE isFavorite = 1 ORDER BY timestamp DESC")
    fun getFavoriteRoutes(): Flow<List<SavedRoute>>
    
    @Query("SELECT * FROM saved_routes WHERE id = :id")
    suspend fun getSavedRouteById(id: Long): SavedRoute?
    
    @Insert
    suspend fun insertSavedRoute(savedRoute: SavedRoute): Long
    
    @Update
    suspend fun updateSavedRoute(savedRoute: SavedRoute)
    
    @Delete
    suspend fun deleteSavedRoute(savedRoute: SavedRoute)
    
    @Query("UPDATE saved_routes SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun updateFavoriteStatus(id: Long, isFavorite: Boolean)
} 