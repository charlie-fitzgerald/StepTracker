package com.steptracker.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.steptracker.app.R
import com.steptracker.app.ui.viewmodel.WalkUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalkStatsCard(uiState: WalkUiState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Walk Statistics",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Stats Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Duration
                StatItem(
                    icon = Icons.Default.Timer,
                    value = formatDuration(uiState.walkStartTime?.let { 
                        java.time.Duration.between(it, java.time.LocalDateTime.now()).seconds 
                    } ?: 0L),
                    label = stringResource(R.string.walk_duration)
                )
                
                // Distance
                StatItem(
                    icon = Icons.Default.Place,
                    value = formatDistance(uiState.totalDistance, uiState.unitsSystem),
                    label = stringResource(R.string.walk_distance)
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Steps
                StatItem(
                    icon = Icons.Default.DirectionsWalk,
                    value = "${uiState.totalSteps}",
                    label = stringResource(R.string.steps)
                )
                
                // Pace
                StatItem(
                    icon = Icons.Default.Speed,
                    value = formatPace(uiState.totalDistance, uiState.walkStartTime?.let { 
                        java.time.Duration.between(it, java.time.LocalDateTime.now()).seconds 
                    } ?: 0L, uiState.unitsSystem),
                    label = stringResource(R.string.walk_pace)
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Max Elevation
                StatItem(
                    icon = Icons.Default.Terrain,
                    value = formatElevation(uiState.maxElevation, uiState.unitsSystem),
                    label = stringResource(R.string.max_elevation)
                )
                
                // Elevation Gain
                StatItem(
                    icon = Icons.Default.TrendingUp,
                    value = formatElevation(uiState.elevationGain, uiState.unitsSystem),
                    label = stringResource(R.string.elevation_gain)
                )
            }
        }
    }
}

@Composable
fun StatItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    label: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.weight(1f)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun formatDuration(seconds: Long): String {
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    val secs = seconds % 60
    return String.format("%02d:%02d:%02d", hours, minutes, secs)
}

private fun formatDistance(meters: Double, unitsSystem: com.steptracker.app.data.preferences.UnitsSystem): String {
    return if (unitsSystem == com.steptracker.app.data.preferences.UnitsSystem.IMPERIAL) {
        val miles = meters * 0.000621371
        String.format("%.2f mi", miles)
    } else {
        val km = meters / 1000.0
        String.format("%.2f km", km)
    }
}

private fun formatPace(meters: Double, seconds: Long, unitsSystem: com.steptracker.app.data.preferences.UnitsSystem): String {
    if (meters <= 0 || seconds <= 0) return "--"
    
    val paceMinutesPerUnit = if (unitsSystem == com.steptracker.app.data.preferences.UnitsSystem.IMPERIAL) {
        val miles = meters * 0.000621371
        val minutes = seconds / 60.0
        minutes / miles
    } else {
        val km = meters / 1000.0
        val minutes = seconds / 60.0
        minutes / km
    }
    
    val unit = if (unitsSystem == com.steptracker.app.data.preferences.UnitsSystem.IMPERIAL) "min/mi" else "min/km"
    return String.format("%.1f %s", paceMinutesPerUnit, unit)
}

private fun formatElevation(meters: Double, unitsSystem: com.steptracker.app.data.preferences.UnitsSystem): String {
    return if (unitsSystem == com.steptracker.app.data.preferences.UnitsSystem.IMPERIAL) {
        val feet = meters * 3.28084
        String.format("%.0f ft", feet)
    } else {
        String.format("%.0f m", meters)
    }
} 