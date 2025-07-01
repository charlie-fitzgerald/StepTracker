package com.steptracker.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.steptracker.app.R
import com.steptracker.app.ui.viewmodel.GoalsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalsScreen(
    viewModel: GoalsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header
        Text(
            text = stringResource(R.string.nav_goals),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Daily Goal Card
        GoalCard(
            title = stringResource(R.string.daily_goal),
            currentValue = uiState.todaySteps,
            targetValue = uiState.dailyGoal,
            icon = Icons.Default.Today,
            onEditClick = { viewModel.showDailyGoalDialog() }
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Weekly Goal Card
        GoalCard(
            title = stringResource(R.string.weekly_goal),
            currentValue = uiState.weeklySteps,
            targetValue = uiState.weeklyGoal,
            icon = Icons.Default.DateRange,
            onEditClick = { viewModel.showWeeklyGoalDialog() }
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Achievements Section
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Achievements",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Achievement items
                AchievementItem(
                    icon = Icons.Default.EmojiEvents,
                    title = "First Walk",
                    description = "Complete your first walk",
                    isAchieved = uiState.walkSessionsCount > 0
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                AchievementItem(
                    icon = Icons.Default.Star,
                    title = "Goal Crusher",
                    description = "Meet your daily goal 7 days in a row",
                    isAchieved = uiState.weeklyGoalAchieved
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                AchievementItem(
                    icon = Icons.Default.TrendingUp,
                    title = "Consistency",
                    description = "Walk for 30 days straight",
                    isAchieved = uiState.monthlyStreak >= 30
                )
            }
        }
    }
    
    // Goal editing dialogs
    if (uiState.showDailyGoalDialog) {
        GoalEditDialog(
            title = stringResource(R.string.daily_goal),
            currentValue = uiState.dailyGoal,
            onConfirm = { newValue -> viewModel.updateDailyGoal(newValue) },
            onDismiss = { viewModel.hideDailyGoalDialog() }
        )
    }
    
    if (uiState.showWeeklyGoalDialog) {
        GoalEditDialog(
            title = stringResource(R.string.weekly_goal),
            currentValue = uiState.weeklyGoal,
            onConfirm = { newValue -> viewModel.updateWeeklyGoal(newValue) },
            onDismiss = { viewModel.hideWeeklyGoalDialog() }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalCard(
    title: String,
    currentValue: Int,
    targetValue: Int,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onEditClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                IconButton(onClick = onEditClick) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Progress indicator
            LinearProgressIndicator(
                progress = (currentValue.toFloat() / targetValue.toFloat()).coerceIn(0f, 1f),
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "$currentValue / $targetValue",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                
                val percentage = ((currentValue.toFloat() / targetValue.toFloat()) * 100).toInt()
                Text(
                    text = "$percentage%",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            if (currentValue >= targetValue) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = stringResource(R.string.goal_achieved),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun AchievementItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    isAchieved: Boolean
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isAchieved) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = if (isAchieved) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        if (isAchieved) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalEditDialog(
    title: String,
    currentValue: Int,
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var newValue by remember { mutableStateOf(currentValue.toString()) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Edit $title") },
        text = {
            OutlinedTextField(
                value = newValue,
                onValueChange = { newValue = it },
                label = { Text("Steps") },
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                )
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    newValue.toIntOrNull()?.let { onConfirm(it) }
                    onDismiss()
                }
            ) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
} 