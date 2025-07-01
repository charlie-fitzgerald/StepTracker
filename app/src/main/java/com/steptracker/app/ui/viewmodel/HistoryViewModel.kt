package com.steptracker.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.steptracker.app.data.model.WalkSession
import com.steptracker.app.data.preferences.UserPreferences
import com.steptracker.app.data.repository.WalkRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val walkRepository: WalkRepository,
    private val userPreferences: UserPreferences
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()
    
    init {
        loadWalkSessions()
        observeUserPreferences()
    }
    
    private fun loadWalkSessions() {
        viewModelScope.launch {
            walkRepository.getAllWalkSessions()
                .collect { walkSessions ->
                    _uiState.update { it.copy(walkSessions = walkSessions) }
                }
        }
    }
    
    private fun observeUserPreferences() {
        viewModelScope.launch {
            userPreferences.unitsSystem.collect { unitsSystem ->
                _uiState.update { it.copy(unitsSystem = unitsSystem) }
            }
        }
    }
    
    fun saveRoute(walkSession: WalkSession) {
        viewModelScope.launch {
            val updatedSession = walkSession.copy(isSaved = true)
            walkRepository.updateWalkSession(updatedSession)
        }
    }
}

data class HistoryUiState(
    val walkSessions: List<WalkSession> = emptyList(),
    val unitsSystem: com.steptracker.app.data.preferences.UnitsSystem = com.steptracker.app.data.preferences.UnitsSystem.METRIC
) 