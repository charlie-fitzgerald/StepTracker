package com.steptracker.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.steptracker.app.data.preferences.UserPreferences
import com.steptracker.app.data.repository.StepRepository
import com.steptracker.app.data.repository.WalkRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class GoalsViewModel @Inject constructor(
    private val stepRepository: StepRepository,
    private val walkRepository: WalkRepository,
    private val userPreferences: UserPreferences
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(GoalsUiState())
    val uiState: StateFlow<GoalsUiState> = _uiState.asStateFlow()
    
    init {
        loadGoalsData()
        observeUserPreferences()
    }
    
    private fun loadGoalsData() {
        viewModelScope.launch {
            // Load today's steps
            stepRepository.getStepDataForDate(LocalDate.now())
                .collect { stepData ->
                    _uiState.update { it.copy(todaySteps = stepData?.steps ?: 0) }
                }
        }
        
        viewModelScope.launch {
            // Load weekly steps
            val endDate = LocalDate.now()
            val startDate = endDate.minusDays(6)
            val weeklySteps = stepRepository.getTotalStepsForDateRange(startDate, endDate)
            _uiState.update { it.copy(weeklySteps = weeklySteps) }
        }
        
        viewModelScope.launch {
            // Load walk sessions count
            walkRepository.getAllWalkSessions()
                .collect { walkSessions ->
                    _uiState.update { it.copy(walkSessionsCount = walkSessions.size) }
                }
        }
        
        // Calculate achievements
        calculateAchievements()
    }
    
    private fun observeUserPreferences() {
        viewModelScope.launch {
            combine(
                userPreferences.dailyGoal,
                userPreferences.weeklyGoal
            ) { dailyGoal, weeklyGoal ->
                _uiState.update { it.copy(
                    dailyGoal = dailyGoal,
                    weeklyGoal = weeklyGoal
                ) }
            }.collect()
        }
    }
    
    private fun calculateAchievements() {
        viewModelScope.launch {
            // Calculate weekly goal achievement
            val endDate = LocalDate.now()
            val startDate = endDate.minusDays(6)
            val weeklySteps = stepRepository.getTotalStepsForDateRange(startDate, endDate)
            val dailyGoal = userPreferences.dailyGoal.first()
            val weeklyGoalAchieved = weeklySteps >= dailyGoal * 7
            
            _uiState.update { it.copy(weeklyGoalAchieved = weeklyGoalAchieved) }
        }
        
        // Calculate monthly streak (simplified)
        _uiState.update { it.copy(monthlyStreak = 15) } // Placeholder
    }
    
    fun showDailyGoalDialog() {
        _uiState.update { it.copy(showDailyGoalDialog = true) }
    }
    
    fun hideDailyGoalDialog() {
        _uiState.update { it.copy(showDailyGoalDialog = false) }
    }
    
    fun showWeeklyGoalDialog() {
        _uiState.update { it.copy(showWeeklyGoalDialog = true) }
    }
    
    fun hideWeeklyGoalDialog() {
        _uiState.update { it.copy(showWeeklyGoalDialog = false) }
    }
    
    fun updateDailyGoal(newGoal: Int) {
        viewModelScope.launch {
            userPreferences.setDailyGoal(newGoal)
        }
    }
    
    fun updateWeeklyGoal(newGoal: Int) {
        viewModelScope.launch {
            userPreferences.setWeeklyGoal(newGoal)
        }
    }
}

data class GoalsUiState(
    val todaySteps: Int = 0,
    val weeklySteps: Int = 0,
    val dailyGoal: Int = 10000,
    val weeklyGoal: Int = 70000,
    val walkSessionsCount: Int = 0,
    val weeklyGoalAchieved: Boolean = false,
    val monthlyStreak: Int = 0,
    val showDailyGoalDialog: Boolean = false,
    val showWeeklyGoalDialog: Boolean = false
) 