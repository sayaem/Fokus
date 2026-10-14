package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.preferences.PreferencesManager
import com.example.data.preferences.ThemeMode
import com.example.data.preferences.UserPreferences
import com.example.data.repository.FokusRepository
import com.example.ui.components.FokusSectionHeader
import com.example.ui.localization.AppLanguage
import com.example.ui.localization.LocalFokusStrings
import com.example.ui.theme.FokusElectricLime
import com.example.ui.theme.FokusOnLime
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    preferencesManager: PreferencesManager,
    repository: FokusRepository,
    onNavigateToBlockerDiagnostics: () -> Unit
) {
    val context = LocalContext.current
    val strings = LocalFokusStrings.current
    val scope = rememberCoroutineScope()

    val prefs by preferencesManager.preferencesFlow.collectAsState(initial = UserPreferences())
    val activeSubjects by repository.allActiveSubjects.collectAsState(initial = emptyList())
    val activeChapters by repository.allActiveChapters.collectAsState(initial = emptyList())

    var showTargetDialog by remember { mutableStateOf(false) }
    var showBackupDialog by remember { mutableStateOf(false) }
    var backupJsonPreview by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = strings.settingsTitle,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Preferences, Localization, and Data Backup",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Appearance & Theme Mode
        item {
            FokusSectionHeader(title = strings.appearanceTheme)
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ThemeMode.values().forEach { mode ->
                            val label = when (mode) {
                                ThemeMode.DARK -> strings.themeDark
                                ThemeMode.LIGHT -> strings.themeLight
                                ThemeMode.SYSTEM -> strings.themeSystem
                            }
                            FilterChip(
                                selected = prefs.themeMode == mode,
                                onClick = {
                                    scope.launch { preferencesManager.setThemeMode(mode) }
                                },
                                label = { Text(label, fontSize = 12.sp) },
                                modifier = Modifier.weight(1f),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = FokusElectricLime,
                                    selectedLabelColor = FokusOnLime
                                )
                            )
                        }
                    }
                }
            }
        }

        // Language & Localization
        item {
            FokusSectionHeader(title = strings.languageSetting)
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    AppLanguage.values().forEach { lang ->
                        FilterChip(
                            selected = prefs.language == lang,
                            onClick = {
                                scope.launch { preferencesManager.setLanguage(lang) }
                            },
                            label = { Text("${lang.nativeName} (${lang.displayName})", fontSize = 13.sp) },
                            modifier = Modifier.weight(1f),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = FokusElectricLime,
                                selectedLabelColor = FokusOnLime
                            )
                        )
                    }
                }
            }
        }

        // Daily Target Setting
        item {
            FokusSectionHeader(title = strings.dailyTargetSetting)
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showTargetDialog = true }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "${prefs.dailyTargetMinutes / 60} hours ${prefs.dailyTargetMinutes % 60} minutes",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Text(
                            text = "Target study time per day",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    OutlinedButton(
                        onClick = { showTargetDialog = true },
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Change", fontSize = 12.sp)
                    }
                }
            }
        }

        // Data Backup & Portability
        item {
            FokusSectionHeader(title = strings.backupRestore)
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Export CSV
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                scope.launch {
                                    val csvData = repository.exportSyllabusCsv(activeSubjects, activeChapters)
                                    val sendIntent = Intent().apply {
                                        action = Intent.ACTION_SEND
                                        putExtra(Intent.EXTRA_TEXT, csvData)
                                        type = "text/csv"
                                    }
                                    val shareIntent = Intent.createChooser(sendIntent, "Export FOKUS Syllabus CSV")
                                    context.startActivity(shareIntent)
                                }
                            },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Share, contentDescription = null, tint = FokusElectricLime)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(strings.exportCsv, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                Text("Spreadsheet compatible syllabus export", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }

                    // Export JSON Backup
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                scope.launch {
                                    backupJsonPreview = repository.exportJsonBackup()
                                    showBackupDialog = true
                                }
                            },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.FileDownload, contentDescription = null, tint = FokusElectricLime)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(strings.exportJson, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                Text("Full backup of subjects, chapters, and history", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }

        // About FOKUS
        item {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "FOKUS v1.0",
                        fontWeight = FontWeight.Bold,
                        color = FokusElectricLime,
                        fontSize = 15.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Designed for Bangladeshi HSC & University Admission candidates (BUET, Medical, Dhaka University, Engineering). Data is strictly local-first and student-owned.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(40.dp))
        }
    }

    // Target Duration Dialog
    if (showTargetDialog) {
        var hoursSlider by remember { mutableFloatStateOf(prefs.dailyTargetMinutes / 60f) }

        AlertDialog(
            onDismissRequest = { showTargetDialog = false },
            title = { Text(strings.dailyTargetSetting) },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = String.format("%.1f Hours / Day", hoursSlider),
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = FokusElectricLime
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Slider(
                        value = hoursSlider,
                        onValueChange = { hoursSlider = it },
                        valueRange = 1f..14f,
                        steps = 25,
                        colors = SliderDefaults.colors(
                            thumbColor = FokusElectricLime,
                            activeTrackColor = FokusElectricLime
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            preferencesManager.setDailyTargetMinutes((hoursSlider * 60).toInt())
                            showTargetDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = FokusElectricLime,
                        contentColor = FokusOnLime
                    )
                ) {
                    Text(strings.save)
                }
            },
            dismissButton = {
                TextButton(onClick = { showTargetDialog = false }) {
                    Text(strings.cancel)
                }
            }
        )
    }

    // Backup JSON Dialog
    if (showBackupDialog) {
        AlertDialog(
            onDismissRequest = { showBackupDialog = false },
            title = { Text("Backup JSON Generated") },
            text = {
                Column {
                    Text("Your syllabus and progress data has been exported successfully.", fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = backupJsonPreview,
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 10.sp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val sendIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, backupJsonPreview)
                            type = "application/json"
                        }
                        val shareIntent = Intent.createChooser(sendIntent, "Share FOKUS Backup")
                        context.startActivity(shareIntent)
                        showBackupDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = FokusElectricLime,
                        contentColor = FokusOnLime
                    )
                ) {
                    Text("Share / Save File")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBackupDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}
