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
import com.steptracker.app.ui.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header
        Text(
            text = stringResource(R.string.nav_settings),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Units Section
        SettingsSection(title = stringResource(R.string.units)) {
            // Units System
            SettingsItem(
                icon = Icons.Default.Straighten,
                title = stringResource(R.string.units),
                subtitle = if (uiState.unitsSystem == com.steptracker.app.data.preferences.UnitsSystem.METRIC) {
                    stringResource(R.string.metric)
                } else {
                    stringResource(R.string.imperial)
                },
                onClick = { viewModel.toggleUnitsSystem() }
            )
            
            // Temperature Unit
            SettingsItem(
                icon = Icons.Default.Thermostat,
                title = stringResource(R.string.temperature_unit),
                subtitle = if (uiState.temperatureUnit == com.steptracker.app.data.preferences.TemperatureUnit.CELSIUS) {
                    stringResource(R.string.celsius)
                } else {
                    stringResource(R.string.fahrenheit)
                },
                onClick = { viewModel.toggleTemperatureUnit() }
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Theme Section
        SettingsSection(title = stringResource(R.string.theme)) {
            SettingsItem(
                icon = Icons.Default.Palette,
                title = stringResource(R.string.theme),
                subtitle = when (uiState.themeMode) {
                    com.steptracker.app.data.preferences.ThemeMode.LIGHT -> stringResource(R.string.light)
                    com.steptracker.app.data.preferences.ThemeMode.DARK -> stringResource(R.string.dark)
                    com.steptracker.app.data.preferences.ThemeMode.SYSTEM -> stringResource(R.string.system)
                },
                onClick = { viewModel.showThemeDialog() }
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Notifications Section
        SettingsSection(title = stringResource(R.string.notifications)) {
            // Milestone Notifications
            SettingsItem(
                icon = Icons.Default.Notifications,
                title = stringResource(R.string.milestone_notifications),
                subtitle = if (uiState.milestoneNotifications) "Enabled" else "Disabled",
                onClick = { viewModel.toggleMilestoneNotifications() },
                trailing = {
                    Switch(
                        checked = uiState.milestoneNotifications,
                        onCheckedChange = { viewModel.setMilestoneNotifications(it) }
                    )
                }
            )
            
            // Milestone Distance
            SettingsItem(
                icon = Icons.Default.Place,
                title = stringResource(R.string.milestone_distance),
                subtitle = "${uiState.milestoneDistance} ${if (uiState.unitsSystem == com.steptracker.app.data.preferences.UnitsSystem.IMPERIAL) "miles" else "km"}",
                onClick = { viewModel.showMilestoneDistanceDialog() }
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // About Section
        SettingsSection(title = "About") {
            SettingsItem(
                icon = Icons.Default.Info,
                title = "Version",
                subtitle = "1.0.0",
                onClick = { }
            )
            
            SettingsItem(
                icon = Icons.Default.PrivacyTip,
                title = "Privacy Policy",
                subtitle = "View our privacy policy",
                onClick = { viewModel.openPrivacyPolicy() }
            )
            
            SettingsItem(
                icon = Icons.Default.Help,
                title = "Help & Support",
                subtitle = "Get help and contact support",
                onClick = { viewModel.openHelpSupport() }
            )
        }
    }
    
    // Theme selection dialog
    if (uiState.showThemeDialog) {
        ThemeSelectionDialog(
            currentTheme = uiState.themeMode,
            onThemeSelected = { theme -> viewModel.setThemeMode(theme) },
            onDismiss = { viewModel.hideThemeDialog() }
        )
    }
    
    // Milestone distance dialog
    if (uiState.showMilestoneDistanceDialog) {
        MilestoneDistanceDialog(
            currentDistance = uiState.milestoneDistance,
            unitsSystem = uiState.unitsSystem,
            onDistanceSelected = { distance -> viewModel.setMilestoneDistance(distance) },
            onDismiss = { viewModel.hideMilestoneDistanceDialog() }
        )
    }
}

@Composable
fun SettingsSection(
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            content()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    trailing: @Composable (() -> Unit)? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            trailing?.invoke() ?: run {
                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeSelectionDialog(
    currentTheme: com.steptracker.app.data.preferences.ThemeMode,
    onThemeSelected: (com.steptracker.app.data.preferences.ThemeMode) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Choose Theme") },
        text = {
            Column {
                listOf(
                    com.steptracker.app.data.preferences.ThemeMode.LIGHT to stringResource(R.string.light),
                    com.steptracker.app.data.preferences.ThemeMode.DARK to stringResource(R.string.dark),
                    com.steptracker.app.data.preferences.ThemeMode.SYSTEM to stringResource(R.string.system)
                ).forEach { (theme, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = currentTheme == theme,
                            onClick = { onThemeSelected(theme) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = label)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.ok))
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MilestoneDistanceDialog(
    currentDistance: Double,
    unitsSystem: com.steptracker.app.data.preferences.UnitsSystem,
    onDistanceSelected: (Double) -> Unit,
    onDismiss: () -> Unit
) {
    var distanceText by remember { mutableStateOf(currentDistance.toString()) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.milestone_distance)) },
        text = {
            OutlinedTextField(
                value = distanceText,
                onValueChange = { distanceText = it },
                label = { Text("Distance") },
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal
                )
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    distanceText.toDoubleOrNull()?.let { onDistanceSelected(it) }
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