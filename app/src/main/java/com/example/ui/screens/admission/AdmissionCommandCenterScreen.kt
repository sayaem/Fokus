package com.example.ui.screens.admission

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.admission.AdmissionCountdownEngine
import com.example.admission.AdmissionNotificationScheduler
import com.example.data.model.AdmissionApplicationEntity
import com.example.data.model.AdmissionChecklistEntity
import com.example.data.model.AdmissionEventEntity
import com.example.data.model.AdmissionEventType
import com.example.data.model.AdmissionReminderEntity
import com.example.data.model.ApplicationStatus
import com.example.data.model.PaymentStatus
import com.example.data.model.UniversityEntity
import com.example.data.repository.AdmissionOverviewStats
import com.example.data.repository.FokusRepository
import com.example.data.preferences.PreferencesManager
import com.example.data.preferences.UserPreferences
import com.example.ui.localization.AppLanguage
import com.example.ui.localization.LocalFokusStrings
import com.example.ui.theme.FokusAmber
import com.example.ui.theme.FokusBlue
import com.example.ui.theme.FokusCoral
import com.example.ui.theme.FokusElectricLime
import com.example.ui.theme.FokusOnLime
import kotlinx.coroutines.launch
import java.util.UUID

enum class AdmissionSubTab(val index: Int) {
    OVERVIEW(0),
    UNIVERSITIES(1),
    CALENDAR(2),
    REMINDERS(3)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdmissionCommandCenterScreen(
    repository: FokusRepository,
    preferencesManager: PreferencesManager,
    initialTab: AdmissionSubTab = AdmissionSubTab.OVERVIEW,
    onBack: () -> Unit
) {
    val strings = LocalFokusStrings.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val userPrefs by preferencesManager.preferencesFlow.collectAsState(initial = UserPreferences())
    val language = userPrefs.language

    val universities by repository.allActiveUniversities.collectAsState(initial = emptyList())
    val applications by repository.allActiveApplications.collectAsState(initial = emptyList())
    val events by repository.allActiveAdmissionEvents.collectAsState(initial = emptyList())
    val stats by repository.admissionOverviewFlow.collectAsState(initial = AdmissionOverviewStats())
    val activeReminders by repository.getActiveFutureReminders().collectAsState(initial = emptyList())

    var currentTab by remember { mutableStateOf(initialTab) }

    // Dialog States
    var showAddUniDialog by remember { mutableStateOf(false) }
    var uniToEdit by remember { mutableStateOf<UniversityEntity?>(null) }
    var uniToDelete by remember { mutableStateOf<UniversityEntity?>(null) }

    var showAddAppDialog by remember { mutableStateOf(false) }
    var appToEdit by remember { mutableStateOf<AdmissionApplicationEntity?>(null) }
    var appToDelete by remember { mutableStateOf<AdmissionApplicationEntity?>(null) }
    var preselectedUniForApp by remember { mutableStateOf<String?>(null) }

    var showAddEventDialog by remember { mutableStateOf(false) }
    var eventToEdit by remember { mutableStateOf<AdmissionEventEntity?>(null) }
    var eventToDelete by remember { mutableStateOf<AdmissionEventEntity?>(null) }
    var preselectedUniForEvent by remember { mutableStateOf<String?>(null) }
    var preselectedDateForEvent by remember { mutableStateOf<Long?>(null) }

    var topActionMenuExpanded by remember { mutableStateOf(false) }

    // Android 13+ Notification Permission Launcher
    var hasNotificationPermission by remember { mutableStateOf(true) }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasNotificationPermission = granted
        if (granted) {
            AdmissionNotificationScheduler.createNotificationChannel(context)
            scope.launch {
                snackbarHostState.showSnackbar("Notifications enabled for admission reminders!")
            }
        }
    }

    LaunchedEffect(Unit) {
        AdmissionNotificationScheduler.createNotificationChannel(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            hasNotificationPermission = androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = strings.admissionCommandCenter,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { topActionMenuExpanded = true }) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add",
                                tint = FokusElectricLime
                            )
                        }

                        DropdownMenu(
                            expanded = topActionMenuExpanded,
                            onDismissRequest = { topActionMenuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(strings.addUniversity) },
                                leadingIcon = { Icon(Icons.Default.School, contentDescription = null) },
                                onClick = {
                                    topActionMenuExpanded = false
                                    uniToEdit = null
                                    showAddUniDialog = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(strings.addApplication) },
                                leadingIcon = { Icon(Icons.Default.HourglassTop, contentDescription = null) },
                                onClick = {
                                    topActionMenuExpanded = false
                                    appToEdit = null
                                    preselectedUniForApp = null
                                    showAddAppDialog = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(strings.addAdmissionEvent) },
                                leadingIcon = { Icon(Icons.Default.Event, contentDescription = null) },
                                onClick = {
                                    topActionMenuExpanded = false
                                    eventToEdit = null
                                    preselectedUniForEvent = null
                                    preselectedDateForEvent = null
                                    showAddEventDialog = true
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Navigation Sub-Tabs
            TabRow(
                selectedTabIndex = currentTab.index,
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .clip(RoundedCornerShape(12.dp))
            ) {
                listOf(
                    AdmissionSubTab.OVERVIEW to strings.admissionOverview,
                    AdmissionSubTab.UNIVERSITIES to strings.admissionUniversities,
                    AdmissionSubTab.CALENDAR to strings.admissionCalendar,
                    AdmissionSubTab.REMINDERS to strings.admissionReminders
                ).forEach { (tab, label) ->
                    Tab(
                        selected = currentTab == tab,
                        onClick = { currentTab = tab },
                        text = {
                            Text(
                                text = label,
                                fontSize = 12.sp,
                                fontWeight = if (currentTab == tab) FontWeight.Bold else FontWeight.Medium,
                                color = if (currentTab == tab) FokusElectricLime else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    )
                }
            }

            // Tab Content
            when (currentTab) {
                AdmissionSubTab.OVERVIEW -> {
                    AdmissionOverviewTab(
                        stats = stats,
                        events = events,
                        universities = universities,
                        applications = applications,
                        language = language,
                        onAddUniversity = {
                            uniToEdit = null
                            showAddUniDialog = true
                        },
                        onAddEvent = {
                            eventToEdit = null
                            preselectedUniForEvent = null
                            showAddEventDialog = true
                        },
                        onOpenCalendar = { currentTab = AdmissionSubTab.CALENDAR },
                        onOpenUniversities = { currentTab = AdmissionSubTab.UNIVERSITIES },
                        onEventClick = { ev ->
                            eventToEdit = ev
                            showAddEventDialog = true
                        }
                    )
                }

                AdmissionSubTab.UNIVERSITIES -> {
                    AdmissionUniversitiesTab(
                        universities = universities,
                        applications = applications,
                        events = events,
                        repository = repository,
                        language = language,
                        onAddUniversity = {
                            uniToEdit = null
                            showAddUniDialog = true
                        },
                        onEditUniversity = { uni ->
                            uniToEdit = uni
                            showAddUniDialog = true
                        },
                        onDeleteUniversity = { uni ->
                            uniToDelete = uni
                        },
                        onAddApplication = { uniId ->
                            appToEdit = null
                            preselectedUniForApp = uniId
                            showAddAppDialog = true
                        },
                        onEditApplication = { app ->
                            appToEdit = app
                            showAddAppDialog = true
                        },
                        onDeleteApplication = { app ->
                            appToDelete = app
                        },
                        onAddEventForUni = { uniId ->
                            eventToEdit = null
                            preselectedUniForEvent = uniId
                            showAddEventDialog = true
                        }
                    )
                }

                AdmissionSubTab.CALENDAR -> {
                    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                        AdmissionCalendarView(
                            events = events,
                            universities = universities,
                            language = language,
                            onAddEventForDate = { dateMillis ->
                                eventToEdit = null
                                preselectedDateForEvent = dateMillis
                                showAddEventDialog = true
                            },
                            onEventClick = { ev ->
                                eventToEdit = ev
                                showAddEventDialog = true
                            }
                        )
                    }
                }

                AdmissionSubTab.REMINDERS -> {
                    AdmissionRemindersTab(
                        activeReminders = activeReminders,
                        events = events,
                        universities = universities,
                        language = language,
                        hasNotificationPermission = hasNotificationPermission,
                        onRequestPermission = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        },
                        onToggleReminder = { reminder ->
                            scope.launch {
                                val updated = reminder.copy(isEnabled = !reminder.isEnabled)
                                repository.updateAdmissionReminder(updated)
                                if (updated.isEnabled) {
                                    AdmissionNotificationScheduler.scheduleReminder(context, updated)
                                } else {
                                    AdmissionNotificationScheduler.cancelReminder(context, updated.id)
                                }
                            }
                        },
                        onDeleteReminder = { reminder ->
                            scope.launch {
                                AdmissionNotificationScheduler.cancelReminder(context, reminder.id)
                                repository.deleteAdmissionReminder(reminder)
                            }
                        }
                    )
                }
            }
        }
    }

    // Dialogs
    if (showAddUniDialog) {
        AddEditUniversityDialog(
            initialUniversity = uniToEdit,
            onDismiss = { showAddUniDialog = false; uniToEdit = null },
            onSave = { uni ->
                scope.launch {
                    if (uniToEdit == null) {
                        repository.insertUniversity(uni)
                        snackbarHostState.showSnackbar("University added!")
                    } else {
                        repository.updateUniversity(uni)
                        snackbarHostState.showSnackbar("University updated!")
                    }
                    showAddUniDialog = false
                    uniToEdit = null
                }
            }
        )
    }

    if (showAddAppDialog) {
        AddEditApplicationDialog(
            universities = universities,
            initialApplication = appToEdit,
            preselectedUniversityId = preselectedUniForApp,
            onDismiss = { showAddAppDialog = false; appToEdit = null },
            onSave = { app ->
                scope.launch {
                    if (appToEdit == null) {
                        repository.insertApplication(app)
                        snackbarHostState.showSnackbar("Unit/Program added!")
                    } else {
                        repository.updateApplication(app)
                        snackbarHostState.showSnackbar("Unit updated!")
                    }
                    showAddAppDialog = false
                    appToEdit = null
                }
            }
        )
    }

    if (showAddEventDialog) {
        AddEditEventDialog(
            universities = universities,
            applications = applications,
            initialEvent = eventToEdit,
            preselectedUniversityId = preselectedUniForEvent,
            preselectedDateMillis = preselectedDateForEvent,
            onDismiss = { showAddEventDialog = false; eventToEdit = null },
            onSave = { ev ->
                scope.launch {
                    if (eventToEdit == null) {
                        repository.insertAdmissionEvent(ev)
                        snackbarHostState.showSnackbar("Event added and reminders scheduled!")
                    } else {
                        repository.updateAdmissionEvent(ev)
                        snackbarHostState.showSnackbar("Event updated!")
                    }
                    showAddEventDialog = false
                    eventToEdit = null
                }
            }
        )
    }

    // Delete University Confirmation
    if (uniToDelete != null) {
        AlertDialog(
            onDismissRequest = { uniToDelete = null },
            title = { Text(strings.confirmDelete) },
            text = { Text("Are you sure you want to delete ${uniToDelete?.name}? All its units, events, and reminders will also be deleted.") },
            confirmButton = {
                TextButton(onClick = {
                    val u = uniToDelete!!
                    uniToDelete = null
                    scope.launch {
                        repository.deleteUniversity(u)
                        snackbarHostState.showSnackbar("University deleted")
                    }
                }) {
                    Text(strings.delete, color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { uniToDelete = null }) {
                    Text(strings.cancel)
                }
            }
        )
    }

    // Delete Application Confirmation
    if (appToDelete != null) {
        AlertDialog(
            onDismissRequest = { appToDelete = null },
            title = { Text(strings.confirmDelete) },
            text = { Text("Are you sure you want to delete ${appToDelete?.unitName}?") },
            confirmButton = {
                TextButton(onClick = {
                    val a = appToDelete!!
                    appToDelete = null
                    scope.launch {
                        repository.deleteApplication(a)
                        snackbarHostState.showSnackbar("Unit deleted")
                    }
                }) {
                    Text(strings.delete, color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { appToDelete = null }) {
                    Text(strings.cancel)
                }
            }
        )
    }
}

@Composable
fun AdmissionOverviewTab(
    stats: AdmissionOverviewStats,
    events: List<AdmissionEventEntity>,
    universities: List<UniversityEntity>,
    applications: List<AdmissionApplicationEntity>,
    language: AppLanguage,
    onAddUniversity: () -> Unit,
    onAddEvent: () -> Unit,
    onOpenCalendar: () -> Unit,
    onOpenUniversities: () -> Unit,
    onEventClick: (AdmissionEventEntity) -> Unit
) {
    val strings = LocalFokusStrings.current
    var selectedFilter by remember { mutableStateOf<AdmissionEventType?>(null) }

    val filteredUpcoming = remember(events, selectedFilter) {
        val now = System.currentTimeMillis()
        events.filter { ev ->
            val target = ev.deadlineDateTimeMillis ?: ev.startDateTimeMillis
            val isFutureOrToday = (target ?: 0L) >= (now - 24 * 3600 * 1000L)
            val matchesFilter = selectedFilter == null || ev.eventType == selectedFilter
            isFutureOrToday && matchesFilter
        }.sortedBy { it.deadlineDateTimeMillis ?: it.startDateTimeMillis }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(vertical = 12.dp)
    ) {
        // Summary Metrics Grid
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MetricCard(
                    title = strings.totalUniversities,
                    value = if (language == AppLanguage.BANGLA) AdmissionCountdownEngine.formatBanglaDigits(stats.totalUniversities.toString()) else stats.totalUniversities.toString(),
                    modifier = Modifier.weight(1f),
                    accentColor = FokusElectricLime
                )
                MetricCard(
                    title = strings.applicationsSubmitted,
                    value = if (language == AppLanguage.BANGLA) AdmissionCountdownEngine.formatBanglaDigits(stats.submittedCount.toString()) else stats.submittedCount.toString(),
                    modifier = Modifier.weight(1f),
                    accentColor = FokusBlue
                )
                MetricCard(
                    title = strings.pendingPayments,
                    value = if (language == AppLanguage.BANGLA) AdmissionCountdownEngine.formatBanglaDigits(stats.pendingPaymentCount.toString()) else stats.pendingPaymentCount.toString(),
                    modifier = Modifier.weight(1f),
                    accentColor = if (stats.pendingPaymentCount > 0) FokusCoral else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Hero Card: Nearest Important Event (Next Deadline or Test)
        item {
            val heroEvent = stats.nearestDeadlineEvent ?: stats.nextExamEvent ?: events.firstOrNull { it.isDateConfirmed }
            if (heroEvent != null) {
                val uni = universities.firstOrNull { it.id == heroEvent.universityId }
                val countdown = AdmissionCountdownEngine.calculateCountdown(heroEvent, language = language)

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (countdown.isUrgent) FokusCoral.copy(alpha = 0.6f) else FokusElectricLime.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onEventClick(heroEvent) }
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(if (countdown.isUrgent) FokusCoral else FokusElectricLime)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (heroEvent.eventType == AdmissionEventType.APPLICATION_DEADLINE) strings.nearestDeadline else strings.upcomingTests,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (countdown.isUrgent) FokusCoral else FokusElectricLime
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (countdown.isUrgent) FokusCoral.copy(alpha = 0.2f) else FokusElectricLime.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = countdown.label,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = if (countdown.isUrgent) FokusCoral else FokusElectricLime,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Text(
                            text = heroEvent.title,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = uni?.name ?: "University",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "📅 ${countdown.formattedDate}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            } else if (universities.isEmpty()) {
                // Empty state invitation
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    border = androidx.compose.foundation.BorderStroke(1.dp, FokusElectricLime.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.School,
                            contentDescription = null,
                            tint = FokusElectricLime,
                            modifier = Modifier.size(40.dp)
                        )
                        Text(
                            text = strings.emptyUniversities,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = strings.emptyUniversitiesSub,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Button(
                            onClick = onAddUniversity,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = FokusElectricLime,
                                contentColor = FokusOnLime
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(strings.addUniversity, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Urgent Applications Notice (if any)
        if (stats.urgentApplicationsCount > 0) {
            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = FokusAmber.copy(alpha = 0.15f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, FokusAmber.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = FokusAmber, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Action Required: ${stats.urgentApplicationsCount} Urgent Applications",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = FokusAmber
                            )
                            Text(
                                text = "You have applications with upcoming deadlines or pending payments.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        TextButton(onClick = onOpenUniversities) {
                            Text("Review", fontSize = 11.sp, color = FokusAmber, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Quick Actions
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onAddUniversity,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(imageVector = Icons.Default.School, contentDescription = null, modifier = Modifier.size(16.dp), tint = FokusElectricLime)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(strings.addUniversity, fontSize = 11.sp, maxLines = 1)
                }

                OutlinedButton(
                    onClick = onAddEvent,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(imageVector = Icons.Default.Event, contentDescription = null, modifier = Modifier.size(16.dp), tint = FokusElectricLime)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(strings.addAdmissionEvent, fontSize = 11.sp, maxLines = 1)
                }
            }
        }

        // Upcoming Events Section Header & Filter Chips
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Upcoming Timeline (${filteredUpcoming.size})",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                TextButton(onClick = onOpenCalendar) {
                    Text(strings.admissionCalendar, fontSize = 11.sp, color = FokusElectricLime)
                }
            }
        }

        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    FilterChip(
                        selected = selectedFilter == null,
                        onClick = { selectedFilter = null },
                        label = { Text(strings.filterAll, fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = FokusElectricLime,
                            selectedLabelColor = FokusOnLime
                        )
                    )
                }
                item {
                    FilterChip(
                        selected = selectedFilter == AdmissionEventType.APPLICATION_DEADLINE,
                        onClick = {
                            selectedFilter = if (selectedFilter == AdmissionEventType.APPLICATION_DEADLINE) null else AdmissionEventType.APPLICATION_DEADLINE
                        },
                        label = { Text(strings.filterDeadlines, fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = FokusCoral,
                            selectedLabelColor = Color.White
                        )
                    )
                }
                item {
                    FilterChip(
                        selected = selectedFilter == AdmissionEventType.ADMISSION_TEST,
                        onClick = {
                            selectedFilter = if (selectedFilter == AdmissionEventType.ADMISSION_TEST) null else AdmissionEventType.ADMISSION_TEST
                        },
                        label = { Text(strings.filterExams, fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = FokusElectricLime,
                            selectedLabelColor = FokusOnLime
                        )
                    )
                }
                item {
                    FilterChip(
                        selected = selectedFilter == AdmissionEventType.ADMIT_CARD_OPENS,
                        onClick = {
                            selectedFilter = if (selectedFilter == AdmissionEventType.ADMIT_CARD_OPENS) null else AdmissionEventType.ADMIT_CARD_OPENS
                        },
                        label = { Text(strings.filterAdmitCard, fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = FokusBlue,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }
        }

        if (filteredUpcoming.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = strings.emptyEvents,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(20.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            items(filteredUpcoming, key = { it.id }) { ev ->
                CalendarEventRow(
                    event = ev,
                    universities = universities,
                    language = language,
                    onClick = { onEventClick(ev) }
                )
            }
        }
    }
}

@Composable
fun MetricCard(
    title: String,
    value: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = value,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = accentColor
            )
        }
    }
}

@Composable
fun AdmissionUniversitiesTab(
    universities: List<UniversityEntity>,
    applications: List<AdmissionApplicationEntity>,
    events: List<AdmissionEventEntity>,
    repository: FokusRepository,
    language: AppLanguage,
    onAddUniversity: () -> Unit,
    onEditUniversity: (UniversityEntity) -> Unit,
    onDeleteUniversity: (UniversityEntity) -> Unit,
    onAddApplication: (universityId: String) -> Unit,
    onEditApplication: (AdmissionApplicationEntity) -> Unit,
    onDeleteApplication: (AdmissionApplicationEntity) -> Unit,
    onAddEventForUni: (universityId: String) -> Unit
) {
    val strings = LocalFokusStrings.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var searchQuery by remember { mutableStateOf("") }
    var expandedAppId by remember { mutableStateOf<String?>(null) }

    val filteredUnis = remember(universities, searchQuery) {
        if (searchQuery.isBlank()) universities
        else {
            universities.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                it.shortName.contains(searchQuery, ignoreCase = true) ||
                it.location.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(vertical = 12.dp)
    ) {
        // Search bar
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search universities, units or locations...") },
                leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = FokusElectricLime,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                )
            )
        }

        if (filteredUnis.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = strings.emptyUniversities,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Button(
                            onClick = onAddUniversity,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = FokusElectricLime,
                                contentColor = FokusOnLime
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(strings.addUniversity, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } else {
            items(filteredUnis, key = { it.id }) { uni ->
                val uniApps = applications.filter { it.universityId == uni.id }
                val uniEvents = events.filter { it.universityId == uni.id }

                UniversityCard(
                    university = uni,
                    applications = uniApps,
                    events = uniEvents,
                    repository = repository,
                    language = language,
                    expandedAppId = expandedAppId,
                    onToggleAppExpand = { appId ->
                        expandedAppId = if (expandedAppId == appId) null else appId
                    },
                    onEditUniversity = { onEditUniversity(uni) },
                    onDeleteUniversity = { onDeleteUniversity(uni) },
                    onAddApplication = { onAddApplication(uni.id) },
                    onEditApplication = onEditApplication,
                    onDeleteApplication = onDeleteApplication,
                    onAddEvent = { onAddEventForUni(uni.id) }
                )
            }
        }
    }
}

@Composable
fun UniversityCard(
    university: UniversityEntity,
    applications: List<AdmissionApplicationEntity>,
    events: List<AdmissionEventEntity>,
    repository: FokusRepository,
    language: AppLanguage,
    expandedAppId: String?,
    onToggleAppExpand: (String) -> Unit,
    onEditUniversity: () -> Unit,
    onDeleteUniversity: () -> Unit,
    onAddApplication: () -> Unit,
    onEditApplication: (AdmissionApplicationEntity) -> Unit,
    onDeleteApplication: (AdmissionApplicationEntity) -> Unit,
    onAddEvent: () -> Unit
) {
    val context = LocalContext.current
    val strings = LocalFokusStrings.current
    var menuExpanded by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Header: Name, short name badge, location, menu
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (university.shortName.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = FokusElectricLime.copy(alpha = 0.2f),
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Text(
                                text = university.shortName,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = FokusElectricLime,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Column {
                        Text(
                            text = university.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (university.location.isNotBlank()) {
                            Text(
                                text = "📍 ${university.location}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Box {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(imageVector = Icons.Default.MoreVert, contentDescription = "Menu")
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(strings.addApplication) },
                            leadingIcon = { Icon(Icons.Default.Add, contentDescription = null) },
                            onClick = { menuExpanded = false; onAddApplication() }
                        )
                        DropdownMenuItem(
                            text = { Text(strings.addAdmissionEvent) },
                            leadingIcon = { Icon(Icons.Default.Event, contentDescription = null) },
                            onClick = { menuExpanded = false; onAddEvent() }
                        )
                        DropdownMenuItem(
                            text = { Text(strings.edit) },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                            onClick = { menuExpanded = false; onEditUniversity() }
                        )
                        DropdownMenuItem(
                            text = { Text(strings.delete, color = MaterialTheme.colorScheme.error) },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                            onClick = { menuExpanded = false; onDeleteUniversity() }
                        )
                    }
                }
            }

            // Portal Link Buttons
            if (university.admissionPortalUrl.isNotBlank() || university.websiteUrl.isNotBlank()) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (university.admissionPortalUrl.isNotBlank()) {
                        OutlinedButton(
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(university.admissionPortalUrl))
                                context.startActivity(intent)
                            },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(12.dp), tint = FokusElectricLime)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Admission Portal", fontSize = 10.sp, color = FokusElectricLime)
                        }
                    }

                    if (university.websiteUrl.isNotBlank()) {
                        OutlinedButton(
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(university.websiteUrl))
                                context.startActivity(intent)
                            },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text("Website", fontSize = 10.sp)
                        }
                    }
                }
            }

            // Units / Applications Section
            if (applications.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "No units/cycles tracked yet",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        TextButton(onClick = onAddApplication) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp), tint = FokusElectricLime)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(strings.addApplication, fontSize = 11.sp, color = FokusElectricLime)
                        }
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    applications.forEach { app ->
                        ApplicationUnitItem(
                            application = app,
                            repository = repository,
                            isExpanded = expandedAppId == app.id,
                            language = language,
                            onToggleExpand = { onToggleAppExpand(app.id) },
                            onEdit = { onEditApplication(app) },
                            onDelete = { onDeleteApplication(app) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ApplicationUnitItem(
    application: AdmissionApplicationEntity,
    repository: FokusRepository,
    isExpanded: Boolean,
    language: AppLanguage,
    onToggleExpand: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val strings = LocalFokusStrings.current

    val checklists by repository.getChecklistsForApplication(application.id).collectAsState(initial = emptyList())
    var showAddChecklistDialog by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                if (application.paymentStatus == PaymentStatus.PAYMENT_PENDING) FokusAmber.copy(alpha = 0.4f) else Color.Transparent,
                RoundedCornerShape(12.dp)
            )
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleExpand() },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = application.unitName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (application.applicationDeadline != null) {
                        val deadlineCal = AdmissionCountdownEngine.formatDate(application.applicationDeadline, true, language)
                        Text(
                            text = "Deadline: $deadlineCal",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Application Status & Payment Status Chips
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    // Quick cycle application status on tap
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = when (application.applicationStatus) {
                            ApplicationStatus.NOT_APPLIED -> MaterialTheme.colorScheme.surfaceVariant
                            ApplicationStatus.IN_PROGRESS -> FokusBlue.copy(alpha = 0.2f)
                            ApplicationStatus.SUBMITTED -> FokusElectricLime.copy(alpha = 0.2f)
                            ApplicationStatus.CONFIRMED -> FokusElectricLime.copy(alpha = 0.35f)
                            ApplicationStatus.CANCELLED -> FokusCoral.copy(alpha = 0.2f)
                        },
                        modifier = Modifier.clickable {
                            val next = when (application.applicationStatus) {
                                ApplicationStatus.NOT_APPLIED -> ApplicationStatus.IN_PROGRESS
                                ApplicationStatus.IN_PROGRESS -> ApplicationStatus.SUBMITTED
                                ApplicationStatus.SUBMITTED -> ApplicationStatus.CONFIRMED
                                ApplicationStatus.CONFIRMED -> ApplicationStatus.NOT_APPLIED
                                ApplicationStatus.CANCELLED -> ApplicationStatus.NOT_APPLIED
                            }
                            scope.launch {
                                repository.updateApplication(application.copy(applicationStatus = next, updatedAt = System.currentTimeMillis()))
                            }
                        }
                    ) {
                        Text(
                            text = when (application.applicationStatus) {
                                ApplicationStatus.NOT_APPLIED -> strings.applicationsNotApplied
                                ApplicationStatus.IN_PROGRESS -> strings.applicationsInProgress
                                ApplicationStatus.SUBMITTED -> strings.applicationsSubmitted
                                ApplicationStatus.CONFIRMED -> strings.applicationsConfirmed
                                ApplicationStatus.CANCELLED -> "Cancelled"
                            },
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = when (application.applicationStatus) {
                                ApplicationStatus.NOT_APPLIED -> MaterialTheme.colorScheme.onSurfaceVariant
                                ApplicationStatus.IN_PROGRESS -> FokusBlue
                                ApplicationStatus.SUBMITTED, ApplicationStatus.CONFIRMED -> FokusElectricLime
                                ApplicationStatus.CANCELLED -> FokusCoral
                            },
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    // Payment status toggle
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = when (application.paymentStatus) {
                            PaymentStatus.NOT_REQUIRED -> Color.Transparent
                            PaymentStatus.PAYMENT_PENDING -> FokusAmber.copy(alpha = 0.2f)
                            PaymentStatus.PAID -> FokusElectricLime.copy(alpha = 0.2f)
                        },
                        modifier = Modifier.clickable {
                            val nextPay = when (application.paymentStatus) {
                                PaymentStatus.NOT_REQUIRED -> PaymentStatus.PAYMENT_PENDING
                                PaymentStatus.PAYMENT_PENDING -> PaymentStatus.PAID
                                PaymentStatus.PAID -> PaymentStatus.NOT_REQUIRED
                            }
                            scope.launch {
                                repository.updateApplication(application.copy(paymentStatus = nextPay, updatedAt = System.currentTimeMillis()))
                            }
                        }
                    ) {
                        Text(
                            text = when (application.paymentStatus) {
                                PaymentStatus.NOT_REQUIRED -> "No Fee"
                                PaymentStatus.PAYMENT_PENDING -> "Unpaid ⚠️"
                                PaymentStatus.PAID -> "Paid ✓"
                            },
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = when (application.paymentStatus) {
                                PaymentStatus.NOT_REQUIRED -> MaterialTheme.colorScheme.onSurfaceVariant
                                PaymentStatus.PAYMENT_PENDING -> FokusAmber
                                PaymentStatus.PAID -> FokusElectricLime
                            },
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            // Expanded Unit Details
            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Application Reference & Trx ID
                    if (application.applicationIdRef.isNotBlank() || application.transactionRef.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                if (application.applicationIdRef.isNotBlank()) {
                                    Text(
                                        text = "Roll / App ID: ${application.applicationIdRef}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                if (application.transactionRef.isNotBlank()) {
                                    Text(
                                        text = "Payment Trx: ${application.transactionRef}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }

                    // Checklist items
                    val completedCount = checklists.count { it.isCompleted }
                    val totalCount = checklists.size
                    val progress = if (totalCount > 0) completedCount.toFloat() / totalCount.toFloat() else 0f

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${strings.checklistProgress}: $completedCount/$totalCount",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        TextButton(onClick = { showAddChecklistDialog = true }) {
                            Text("+ ${strings.addItem}", fontSize = 11.sp, color = FokusElectricLime)
                        }
                    }

                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = FokusElectricLime,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )

                    checklists.forEach { item ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = item.isCompleted,
                                onCheckedChange = { checked ->
                                    scope.launch {
                                        repository.updateAdmissionChecklist(
                                            item.copy(isCompleted = checked, completedAt = if (checked) System.currentTimeMillis() else null)
                                        )
                                    }
                                },
                                modifier = Modifier.size(24.dp),
                                colors = CheckboxDefaults.colors(
                                    checkedColor = FokusElectricLime,
                                    checkmarkColor = FokusOnLime
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = item.title,
                                fontSize = 11.sp,
                                color = if (item.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = {
                                    scope.launch { repository.deleteAdmissionChecklist(item) }
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Close, contentDescription = "Delete", modifier = Modifier.size(12.dp))
                            }
                        }
                    }

                    // Unit actions (Edit & Delete)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = onEdit) {
                            Text(strings.edit, fontSize = 11.sp)
                        }
                        TextButton(onClick = onDelete) {
                            Text(strings.delete, fontSize = 11.sp, color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }

    if (showAddChecklistDialog) {
        var newItemText by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddChecklistDialog = false },
            title = { Text(strings.addItem) },
            text = {
                OutlinedTextField(
                    value = newItemText,
                    onValueChange = { newItemText = it },
                    label = { Text("Checklist Item") },
                    placeholder = { Text("e.g. Bring scientific calculator") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = FokusElectricLime,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newItemText.isNotBlank()) {
                        scope.launch {
                            repository.insertAdmissionChecklist(
                                AdmissionChecklistEntity(
                                    id = UUID.randomUUID().toString(),
                                    universityId = application.universityId,
                                    applicationId = application.id,
                                    title = newItemText.trim(),
                                    isCompleted = false,
                                    isCustom = true
                                )
                            )
                            showAddChecklistDialog = false
                        }
                    }
                }) {
                    Text(strings.save, color = FokusElectricLime)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddChecklistDialog = false }) {
                    Text(strings.cancel)
                }
            }
        )
    }
}

@Composable
fun AdmissionRemindersTab(
    activeReminders: List<AdmissionReminderEntity>,
    events: List<AdmissionEventEntity>,
    universities: List<UniversityEntity>,
    language: AppLanguage,
    hasNotificationPermission: Boolean,
    onRequestPermission: () -> Unit,
    onToggleReminder: (AdmissionReminderEntity) -> Unit,
    onDeleteReminder: (AdmissionReminderEntity) -> Unit
) {
    val strings = LocalFokusStrings.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(vertical = 12.dp)
    ) {
        // Notification Permission banner (if denied)
        if (!hasNotificationPermission) {
            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = FokusAmber.copy(alpha = 0.15f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, FokusAmber.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = FokusAmber)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Notification Permission Required",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = FokusAmber
                            )
                        }
                        Text(
                            text = strings.notificationPermissionRequired,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(
                            onClick = onRequestPermission,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = FokusAmber,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Text(strings.grantPermission, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        item {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.NotificationsActive, contentDescription = null, tint = FokusElectricLime)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = strings.admissionReminders,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        text = strings.remindersNotificationNotice,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item {
            Text(
                text = "Active Scheduled Reminders (${activeReminders.size})",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        if (activeReminders.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                ) {
                    Text(
                        text = "No active scheduled reminders. Add admission events with confirmed dates to automatically schedule timely alerts.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(20.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            items(activeReminders, key = { it.id }) { reminder ->
                val ev = events.firstOrNull { it.id == reminder.eventId }
                val uni = universities.firstOrNull { it.id == reminder.universityId }
                val formattedTime = AdmissionCountdownEngine.formatDate(reminder.scheduledTimeMillis, false, language)

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Alarm,
                            contentDescription = null,
                            tint = if (reminder.isEnabled) FokusElectricLime else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(22.dp)
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = reminder.title,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (uni != null) {
                                Text(
                                    text = uni.name,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = "⏰ Scheduled: $formattedTime",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Switch(
                            checked = reminder.isEnabled,
                            onCheckedChange = { onToggleReminder(reminder) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = FokusOnLime,
                                checkedTrackColor = FokusElectricLime
                            )
                        )

                        IconButton(
                            onClick = { onDeleteReminder(reminder) },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Delete", modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }
}
