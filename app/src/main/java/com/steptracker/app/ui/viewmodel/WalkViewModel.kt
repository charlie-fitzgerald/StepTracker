package com.steptracker.app.ui.viewmodel

import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.steptracker.app.data.model.WalkMode
import com.steptracker.app.data.model.WalkSession
import com.steptracker.app.data.preferences.UserPreferences
import com.steptracker.app.data.repository.WalkRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import javax.inject.Inject

@HiltViewModel
class WalkViewModel @Inject constructor(
    private val walkRepository: WalkRepository,
    private val userPreferences: UserPreferences
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(WalkUiState())
    val uiState: StateFlow<WalkUiState> = _uiState.asStateFlow()
    
    private var walkStartTime: LocalDateTime? = null
    private var currentWalkSession: WalkSession? = null
    
    init {
        observeUserPreferences()
    }
    
    private fun observeUserPreferences() {
        viewModelScope.launch {
            userPreferences.unitsSystem.collect { unitsSystem ->
                _uiState.update { it.copy(unitsSystem = unitsSystem) }
            }
        }
    }
    
    fun startWalk(walkMode: WalkMode) {
        walkStartTime = LocalDateTime.now()
        currentWalkSession = WalkSession(
            startTime = walkStartTime!!,
            duration = 0,
            distance = 0.0,
            steps = 0,
            averagePace = 0.0,
            maxElevation = 0.0,
            elevationGain = 0.0,
            walkMode = walkMode
        )
        
        _uiState.update { it.copy(
            isWalking = true,
            walkMode = walkMode,
            walkStartTime = walkStartTime!!
        ) }
    }
    
    fun stopWalk() {
        walkStartTime?.let { startTime ->
            val endTime = LocalDateTime.now()
            val duration = java.time.Duration.between(startTime, endTime).seconds
            
            currentWalkSession?.let { session ->
                val updatedSession = session.copy(
                    endTime = endTime,
                    duration = duration
                )
                
                viewModelScope.launch {
                    walkRepository.insertWalkSession(updatedSession)
                }
            }
        }
        
        resetWalkState()
    }
    
    fun pauseWalk() {
        _uiState.update { it.copy(isPaused = true) }
    }
    
    fun resumeWalk() {
        _uiState.update { it.copy(isPaused = false) }
    }
    
    fun updateLocation(location: Location) {
        _uiState.update { it.copy(currentLocation = location) }
    }
    
    fun updateDistance(distance: Double) {
        _uiState.update { it.copy(totalDistance = distance) }
        
        currentWalkSession?.let { session ->
            currentWalkSession = session.copy(distance = distance)
        }
    }
    
    fun updateSteps(steps: Int) {
        _uiState.update { it.copy(totalSteps = steps) }
        
        currentWalkSession?.let { session ->
            currentWalkSession = session.copy(steps = steps)
        }
    }
    
    fun updateElevation(maxElevation: Double, elevationGain: Double) {
        _uiState.update { it.copy(
            maxElevation = maxElevation,
            elevationGain = elevationGain
        ) }
        
        currentWalkSession?.let { session ->
            currentWalkSession = session.copy(
                maxElevation = maxElevation,
                elevationGain = elevationGain
            )
        }
    }
    
    fun updateRoutePoints(routePoints: List<Location>) {
        _uiState.update { it.copy(routePoints = routePoints) }
    }
    
    private fun resetWalkState() {
        walkStartTime = null
        currentWalkSession = null
        
        _uiState.update { it.copy(
            isWalking = false,
            isPaused = false,
            walkStartTime = null,
            currentLocation = null,
            totalDistance = 0.0,
            totalSteps = 0,
            maxElevation = 0.0,
            elevationGain = 0.0,
            routePoints = emptyList()
        ) }
    }
    
    fun getWalkDuration(): Long {
        return walkStartTime?.let { startTime ->
            val endTime = if (_uiState.value.isWalking && !_uiState.value.isPaused) {
                LocalDateTime.now()
            } else {
                _uiState.value.walkStartTime
            }
            java.time.Duration.between(startTime, endTime).seconds
        } ?: 0L
    }
    
    fun calculatePace(): Double {
        val distance = _uiState.value.totalDistance
        val duration = getWalkDuration()
        
        return if (distance > 0 && duration > 0) {
            // Convert to minutes per kilometer
            (duration / 60.0) / (distance / 1000.0)
        } else {
            0.0
        }
    }
}

data class WalkUiState(
    val isWalking: Boolean = false,
    val isPaused: Boolean = false,
    val walkMode: WalkMode? = null,
    val walkStartTime: LocalDateTime? = null,
    val currentLocation: Location? = null,
    val totalDistance: Double = 0.0,
    val totalSteps: Int = 0,
    val maxElevation: Double = 0.0,
    val elevationGain: Double = 0.0,
    val routePoints: List<Location> = emptyList(),
    val unitsSystem: com.steptracker.app.data.preferences.UnitsSystem = com.steptracker.app.data.preferences.UnitsSystem.METRIC
) 