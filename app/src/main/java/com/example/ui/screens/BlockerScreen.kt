package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
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
import com.example.blocker.AppInfoItem
import com.example.blocker.BlockerManager
import com.example.blocker.PermissionDiagnostics
import com.example.data.model.BlockerConfigEntity
import com.example.data.model.BlockerMode
import com.example.data.repository.FokusRepository
import com.example.ui.components.FokusMetricCard
import com.example.ui.components.FokusSectionHeader
import com.example.ui.localization.LocalFokusStrings
import com.example.ui.theme.FokusAmber
import com.example.ui.theme.FokusBlue
import com.example.ui.theme.FokusCoral
import com.example.ui.theme.FokusElectricLime
import com.example.ui.theme.FokusOnLime
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun BlockerScreen(
    repository: FokusRepository
) {
    val context = LocalContext.current
    val strings = LocalFokusStrings.current
    val scope = rememberCoroutineScope()

    val blockerConfig by repository.blockerConfigFlow.collectAsState(initial = null)
    val totalInterruptions by repository.totalInterruptionsCount.collectAsState(initial = 0)
    val recentInterruptions by repository.recentInterruptions.collectAsState(initial = emptyList())

    var diagnostics by remember { mutableStateOf(BlockerManager.checkDiagnostics(context)) }
    var installedApps by remember { mutableStateOf<List<AppInfoItem>>(emptyList()) }
    var appSearchQuery by remember { mutableStateOf("") }
    var selectedPackageSet by remember { mutableStateOf<Set<String>>(emptySet()) }
    var showEmergencyDialog by remember { mutableStateOf(false) }

    // Recheck diagnostics when screen resumes or loads
    LaunchedEffect(Unit) {
        diagnostics = BlockerManager.checkDiagnostics(context)
        val savedPackages = blockerConfig?.selectedPackagesJson?.let {
            BlockerManager.parsePackagesJson(it)
        } ?: emptySet()
        selectedPackageSet = savedPackages
        installedApps = BlockerManager.getInstalledUserApps(context, savedPackages)
    }

    LaunchedEffect(blockerConfig) {
        blockerConfig?.let { cfg ->
            val set = BlockerManager.parsePackagesJson(cfg.selectedPackagesJson)
            selectedPackageSet = set
            installedApps = BlockerManager.getInstalledUserApps(context, set)
        }
    }

    val filteredApps = installedApps.filter {
        appSearchQuery.isBlank() || it.appName.contains(appSearchQuery, ignoreCase = true)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = strings.blockerDashboard,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Real-time Android Distraction Blocker",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(onClick = { showEmergencyDialog = true }) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Emergency Unblock",
                        tint = FokusCoral
                    )
                }
            }
        }

        // Status Card
        item {
            val isOperational = diagnostics.hasAccessibility || (diagnostics.hasUsageStats && diagnostics.hasOverlay)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                color = if (isOperational) Color(0xFF1B2C0D) else MaterialTheme.colorScheme.surfaceVariant,
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isOperational) FokusElectricLime else MaterialTheme.colorScheme.outlineVariant
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isOperational) Icons.Default.CheckCircle else Icons.Default.Warning,
                                contentDescription = null,
                                tint = if (isOperational) FokusElectricLime else FokusAmber,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isOperational) "Protection Ready" else "Setup Required",
                                fontWeight = FontWeight.Bold,
                                color = if (isOperational) Color.White else MaterialTheme.colorScheme.onSurface,
                                fontSize = 16.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (isOperational)
                                "${selectedPackageSet.size} apps configured for strict focus enforcement"
                            else
                                "Grant Accessibility or Usage permissions below to enable blocking",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Switch(
                        checked = blockerConfig?.isEnabled ?: false,
                        onCheckedChange = { enabled ->
                            scope.launch {
                                val current = blockerConfig ?: BlockerConfigEntity()
                                repository.saveBlockerConfig(current.copy(isEnabled = enabled))
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = FokusOnLime,
                            checkedTrackColor = FokusElectricLime
                        )
                    )
                }
            }
        }

        // Metrics: Blocked Attempts & Selected Apps Count
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FokusMetricCard(
                    title = strings.interruptionsBlocked,
                    value = "${blockerConfig?.totalBlockedAttempts ?: totalInterruptions}",
                    modifier = Modifier.weight(1f),
                    accentColor = FokusCoral,
                    icon = Icons.Default.Block
                )
                FokusMetricCard(
                    title = "Apps Guarded",
                    value = "${selectedPackageSet.size}",
                    modifier = Modifier.weight(1f),
                    accentColor = FokusElectricLime,
                    icon = Icons.Default.Shield
                )
            }
        }

        // Permission Diagnostics Section
        item {
            FokusSectionHeader(title = strings.permissionsSetup)
        }

        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Accessibility Service
                    PermissionRow(
                        title = strings.accessibilityService,
                        isGranted = diagnostics.hasAccessibility,
                        onGrant = {
                            runCatching {
                                context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                            }
                        }
                    )

                    // Usage Access
                    PermissionRow(
                        title = strings.usageStatsAccess,
                        isGranted = diagnostics.hasUsageStats,
                        onGrant = {
                            runCatching {
                                context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                            }
                        }
                    )

                    // Overlay
                    PermissionRow(
                        title = strings.overlayPermission,
                        isGranted = diagnostics.hasOverlay,
                        onGrant = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                runCatching {
                                    val intent = Intent(
                                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                        Uri.parse("package:${context.packageName}")
                                    )
                                    context.startActivity(intent)
                                }
                            }
                        }
                    )
                }
            }
        }

        // Application Selection Section
        item {
            FokusSectionHeader(title = strings.selectedAppsToBlock)
        }

        item {
            OutlinedTextField(
                value = appSearchQuery,
                onValueChange = { appSearchQuery = it },
                placeholder = { Text("Search installed applications...") },
                leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = FokusElectricLime,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                ),
                singleLine = true
            )
        }

        if (filteredApps.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "No user-installed launcher apps found. (System dialer and settings are protected from being blocked).",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        } else {
            items(filteredApps, key = { it.packageName }) { app ->
                val isSelected = selectedPackageSet.contains(app.packageName)
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            val newSet = if (isSelected) {
                                selectedPackageSet - app.packageName
                            } else {
                                selectedPackageSet + app.packageName
                            }
                            selectedPackageSet = newSet
                            scope.launch {
                                val current = blockerConfig ?: BlockerConfigEntity()
                                repository.saveBlockerConfig(
                                    current.copy(
                                        selectedPackagesJson = BlockerManager.packagesToJson(newSet)
                                    )
                                )
                            }
                        },
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isSelected,
                            onCheckedChange = { checked ->
                                val newSet = if (checked) {
                                    selectedPackageSet + app.packageName
                                } else {
                                    selectedPackageSet - app.packageName
                                }
                                selectedPackageSet = newSet
                                scope.launch {
                                    val current = blockerConfig ?: BlockerConfigEntity()
                                    repository.saveBlockerConfig(
                                        current.copy(
                                            selectedPackagesJson = BlockerManager.packagesToJson(newSet)
                                        )
                                    )
                                }
                            },
                            colors = CheckboxDefaults.colors(
                                checkedColor = FokusElectricLime,
                                checkmarkColor = FokusOnLime
                            )
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = app.appName,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = app.packageName,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Recent Blocked Interruption Logs
        if (recentInterruptions.isNotEmpty()) {
            item {
                FokusSectionHeader(title = "Recent Blocked Attempts")
            }
            val logDateFormat = SimpleDateFormat("dd MMM, hh:mm:ss a", Locale.getDefault())
            items(recentInterruptions.take(10), key = { it.id }) { log ->
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(text = log.appName, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text(
                                text = logDateFormat.format(Date(log.timestamp)),
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = "Blocked",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = FokusCoral
                        )
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(40.dp))
        }
    }

    // Emergency Recovery Dialog
    if (showEmergencyDialog) {
        AlertDialog(
            onDismissRequest = { showEmergencyDialog = false },
            title = { Text(strings.emergencyUnblock, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    text = strings.emergencyUnblockNotice + "\n\nIf you ever need to disarm the blocker immediately, tapping 'Disarm All' will instantly release all restrictions.",
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        BlockerManager.emergencyUnblock()
                        showEmergencyDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = FokusCoral)
                ) {
                    Text("Disarm All")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEmergencyDialog = false }) {
                    Text(strings.cancel)
                }
            }
        )
    }
}

@Composable
private fun PermissionRow(
    title: String,
    isGranted: Boolean,
    onGrant: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            Text(
                text = if (isGranted) "Operational" else "Permission missing",
                fontSize = 11.sp,
                color = if (isGranted) FokusElectricLime else FokusAmber
            )
        }

        if (!isGranted) {
            OutlinedButton(
                onClick = onGrant,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Enable", fontSize = 11.sp)
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Default.OpenInNew,
                    contentDescription = null,
                    modifier = Modifier.size(12.dp)
                )
            }
        } else {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = FokusElectricLime,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
