package com.steptracker.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.steptracker.app.ui.theme.StepGreen
import com.steptracker.app.ui.theme.StepBlue
import com.steptracker.app.ui.theme.StepOrange
import com.steptracker.app.ui.theme.StepRed
import com.steptracker.app.ui.theme.StepGray
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.*

@Composable
fun HabitTracker() {
    val today = LocalDate.now()
    val days = generateDaysForCalendar(today)
    
    Column {
        // Month labels
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
                .forEach { month ->
                    Text(
                        text = month,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Calendar grid
        LazyVerticalGrid(
            columns = GridCells.Fixed(7),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            items(days) { day ->
                DayCell(
                    date = day,
                    isToday = day == today,
                    activityLevel = getActivityLevel(day) // This would come from your data
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Legend
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Less",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                DayCell(date = LocalDate.now(), isToday = false, activityLevel = 0)
                DayCell(date = LocalDate.now(), isToday = false, activityLevel = 1)
                DayCell(date = LocalDate.now(), isToday = false, activityLevel = 2)
                DayCell(date = LocalDate.now(), isToday = false, activityLevel = 3)
                DayCell(date = LocalDate.now(), isToday = false, activityLevel = 4)
            }
            
            Text(
                text = "More",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun DayCell(
    date: LocalDate,
    isToday: Boolean,
    activityLevel: Int
) {
    val color = when (activityLevel) {
        0 -> StepGray.copy(alpha = 0.1f)
        1 -> StepGreen.copy(alpha = 0.3f)
        2 -> StepGreen.copy(alpha = 0.5f)
        3 -> StepGreen.copy(alpha = 0.7f)
        4 -> StepGreen
        else -> StepGray.copy(alpha = 0.1f)
    }
    
    val borderColor = if (isToday) MaterialTheme.colorScheme.primary else Color.Transparent
    
    Box(
        modifier = Modifier
            .size(12.dp)
            .background(
                color = color,
                shape = MaterialTheme.shapes.small
            )
            .background(
                color = borderColor,
                shape = MaterialTheme.shapes.small
            ),
        contentAlignment = Alignment.Center
    ) {
        // Empty for now, could add tooltip or click handler
    }
}

private fun generateDaysForCalendar(today: LocalDate): List<LocalDate> {
    val days = mutableListOf<LocalDate>()
    val startDate = today.minusDays(364) // Show last year
    
    var currentDate = startDate
    while (currentDate <= today) {
        days.add(currentDate)
        currentDate = currentDate.plusDays(1)
    }
    
    return days
}

private fun getActivityLevel(date: LocalDate): Int {
    // This would be implemented to get actual step data
    // For now, return random activity level
    return (0..4).random()
} 