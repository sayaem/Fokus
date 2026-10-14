package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Priority
import com.example.data.model.StudyTaskEntity
import com.example.data.model.TaskStatus
import com.example.data.preferences.PreferencesManager
import com.example.data.preferences.UserPreferences
import com.example.data.repository.FokusRepository
import com.example.data.repository.ProposedPlanResult
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.FokusProgressBar
import com.example.ui.components.FokusSectionHeader
import com.example.ui.components.PriorityBadge
import com.example.ui.localization.LocalFokusStrings
import com.example.ui.theme.FokusAmber
import com.example.ui.theme.FokusBlue
import com.example.ui.theme.FokusCoral
import com.example.ui.theme.FokusElectricLime
import com.example.ui.theme.FokusOnLime
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlannerScreen(
    repository: FokusRepository,
    preferencesManager: PreferencesManager? = null,
    onStartFocus: (chapterId: String?) -> Unit,
    onNavigateToJournal: () -> Unit = {}
) {
    val strings = LocalFokusStrings.current
    val scope = rememberCoroutineScope()

    val userPrefs by preferencesManager?.preferencesFlow?.collectAsState(initial = UserPreferences())
        ?: remember { mutableStateOf(UserPreferences()) }

    val subjects by repository.allActiveSubjects.collectAsState(initial = emptyList())
    val chapters by repository.allActiveChapters.collectAsState(initial = emptyList())
    val revisions by repository.allRevisions.collectAsState(initial = emptyList())
    val allTasks by repository.allTasks.collectAsState(initial = emptyList())
    val admissionStats by repository.admissionOverviewFlow.collectAsState(initial = com.example.data.repository.AdmissionOverviewStats())

    var selectedDayOffset by remember { mutableIntStateOf(0) } // 0 = Today, 1 = Tomorrow, etc.
    var showAddTaskDialog by remember { mutableStateOf(false) }
    var showAvailabilityDialog by remember { mutableStateOf(false) }
    var taskToEdit by remember { mutableStateOf<StudyTaskEntity?>(null) }
    var taskToDelete by remember { mutableStateOf<StudyTaskEntity?>(null) }

    // Smart Generated Plan Proposal State
    var proposedPlanResult by remember { mutableStateOf<ProposedPlanResult?>(null) }
    var isGeneratingPlan by remember { mutableStateOf(false) }

    val calendar = Calendar.getInstance().apply {
        add(Calendar.DAY_OF_YEAR, selectedDayOffset)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
    }
    val startOfDay = calendar.timeInMillis
    calendar.set(Calendar.HOUR_OF_DAY, 23)
    calendar.set(Calendar.MINUTE, 59)
    calendar.set(Calendar.SECOND, 59)
    val endOfDay = calendar.timeInMillis

    val epochDay = startOfDay / 86400000L

    // Calculate effective available minutes for selected day (checking day-specific override)
    val dayOverridesObj = remember(userPrefs.availability.daySpecificOverridesJson) {
        try { JSONObject(userPrefs.availability.daySpecificOverridesJson) } catch (e: Exception) { JSONObject() }
    }
    val effectiveAvailableMinutes = if (dayOverridesObj.has(epochDay.toString())) {
        dayOverridesObj.getInt(epochDay.toString())
    } else {
        userPrefs.availability.availableMinutesPerDay
    }

    val dayTasks = allTasks.filter { it.plannedDate in startOfDay..endOfDay }
    val plannedMinutes = dayTasks.sumOf { it.plannedDurationMinutes }
    val completedMinutes = dayTasks.filter { it.status == TaskStatus.COMPLETED }.sumOf { it.plannedDurationMinutes }

    val dayFormat = SimpleDateFormat("EEEE, dd MMMM", Locale.getDefault())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        // Screen Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = strings.smartPlannerTitle,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = dayFormat.format(Date(startOfDay)),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row {
                IconButton(onClick = { showAvailabilityDialog = true }) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = "Availability Settings",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = {
                    taskToEdit = null
                    showAddTaskDialog = true
                }) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Task",
                        tint = FokusElectricLime
                    )
                }
            }
        }

        // Date Tabs (Today, Tomorrow, Day 3, 4, 5)
        val tabFormat = SimpleDateFormat("EEE, dd MMM", Locale.getDefault())
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(5) { offset ->
                val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, offset) }
                val label = if (offset == 0) "Today" else if (offset == 1) "Tomorrow" else tabFormat.format(cal.time)
                FilterChip(
                    selected = selectedDayOffset == offset,
                    onClick = {
                        selectedDayOffset = offset
                        proposedPlanResult = null // Reset proposal when switching days
                    },
                    label = { Text(label, fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = FokusElectricLime,
                        selectedLabelColor = FokusOnLime
                    )
                )
            }
        }

        // Admission Exam Milestone Banner (if an exam is confirmed & upcoming)
        if (admissionStats.nextExamEvent != null) {
            val nextExam = admissionStats.nextExamEvent!!
            val countdown = com.example.admission.AdmissionCountdownEngine.calculateCountdown(
                event = nextExam,
                language = userPrefs.language
            )

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = FokusElectricLime.copy(alpha = 0.12f),
                border = androidx.compose.foundation.BorderStroke(1.dp, FokusElectricLime.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.School,
                                contentDescription = null,
                                tint = FokusElectricLime,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = strings.admissionExamMilestone,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = FokusElectricLime
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = FokusElectricLime.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = countdown.label,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = FokusElectricLime,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Text(
                        text = nextExam.title,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Button(
                        onClick = {
                            val prepCandidates = chapters.filter { it.learningStatus != com.example.data.model.LearningStatus.COMPLETED }
                                .take(3)
                            val examTasks = prepCandidates.mapIndexed { idx, ch ->
                                val subName = subjects.firstOrNull { it.id == ch.subjectId }?.title ?: "Subject"
                                com.example.data.repository.ProposedTask(
                                    title = "[$subName] ${ch.title} Practice",
                                    subjectId = ch.subjectId,
                                    subjectTitle = subName,
                                    chapterId = ch.id,
                                    chapterTitle = ch.title,
                                    topicId = null,
                                    durationMinutes = 45,
                                    priority = Priority.URGENT,
                                    reason = "Targeted prep for upcoming ${nextExam.title} (${countdown.label})",
                                    isRevision = false
                                )
                            }
                            if (examTasks.isNotEmpty()) {
                                val totalMin = examTasks.sumOf { it.durationMinutes }
                                proposedPlanResult = com.example.data.repository.ProposedPlanResult(
                                    proposedTasks = examTasks,
                                    totalPlannedMinutes = totalMin,
                                    availableMinutes = effectiveAvailableMinutes,
                                    breakMinutesTotal = 15,
                                    unscheduledBacklogCount = 0,
                                    unscheduledBacklogPreview = emptyList()
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = FokusElectricLime,
                            contentColor = FokusOnLime
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(36.dp)
                    ) {
                        Text(strings.suggestAdmissionTasks, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }
            }
        }

        // Availability & Workload Summary Card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            tint = FokusElectricLime,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Daily Workload vs Availability",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }

                    Text(
                        text = "$plannedMinutes / $effectiveAvailableMinutes min",
                        fontWeight = FontWeight.Bold,
                        color = if (plannedMinutes > effectiveAvailableMinutes) FokusCoral else FokusElectricLime,
                        fontSize = 13.sp
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                FokusProgressBar(
                    progress = if (effectiveAvailableMinutes > 0) plannedMinutes.toFloat() / effectiveAvailableMinutes else 0f,
                    color = if (plannedMinutes > effectiveAvailableMinutes) FokusCoral else FokusElectricLime,
                    height = 6.dp
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Completed: $completedMinutes min (${if (plannedMinutes > 0) (completedMinutes * 100 / plannedMinutes) else 0}%)",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedButton(
                            onClick = onNavigateToJournal,
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Journal", fontSize = 11.sp)
                        }

                        Button(
                            onClick = {
                                scope.launch {
                                    isGeneratingPlan = true
                                    val result = repository.generateDailyPlan(
                                        availableMinutes = effectiveAvailableMinutes,
                                        sessionDurationMinutes = userPrefs.availability.defaultSessionDurationMinutes,
                                        breakDurationMinutes = userPrefs.availability.breakDurationMinutes,
                                        preferredSubjectIds = userPrefs.availability.preferredSubjectIds,
                                        dateMillis = startOfDay,
                                        chapters = chapters,
                                        subjects = subjects,
                                        revisions = revisions,
                                        existingTasks = dayTasks
                                    )
                                    proposedPlanResult = result
                                    isGeneratingPlan = false
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = FokusElectricLime,
                                contentColor = FokusOnLime
                            )
                        ) {
                            Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(strings.generatePlan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Proposed Plan Review Banner (if generated)
        if (proposedPlanResult != null) {
            val plan = proposedPlanResult!!
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF1F2B14),
                border = androidx.compose.foundation.BorderStroke(1.dp, FokusElectricLime)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, tint = FokusElectricLime)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = strings.planProposedNotice,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 14.sp
                            )
                        }

                        Text(
                            text = "${plan.totalPlannedMinutes} min study + ${plan.breakMinutesTotal}m breaks",
                            fontSize = 11.sp,
                            color = FokusElectricLime,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Prioritized ${plan.proposedTasks.size} tasks based on overdue revisions, due reviews, and incomplete syllabus chapters.",
                        fontSize = 12.sp,
                        color = Color(0xFFC5D4BD)
                    )

                    if (plan.unscheduledBacklogCount > 0) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "⚠️ ${plan.unscheduledBacklogCount} tasks left unscheduled to prevent plan overload (${plan.unscheduledBacklogPreview.joinToString(", ")})",
                            fontSize = 11.sp,
                            color = FokusAmber
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { proposedPlanResult = null }) {
                            Text("Dismiss", color = Color(0xFFA0AB96), fontSize = 12.sp)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                scope.launch {
                                    repository.acceptProposedPlan(plan.proposedTasks, startOfDay)
                                    proposedPlanResult = null
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = FokusElectricLime,
                                contentColor = FokusOnLime
                            )
                        ) {
                            Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(strings.acceptPlan, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // Daily Task List
        FokusSectionHeader(
            title = "Scheduled Tasks (${dayTasks.size})",
            actionLabel = "Add Custom Task",
            onAction = {
                taskToEdit = null
                showAddTaskDialog = true
            }
        )

        if (dayTasks.isEmpty()) {
            EmptyStateCard(
                message = "No study tasks scheduled for this date. Tap 'Generate Plan' to create a smart schedule or '+' to add a manual task.",
                actionLabel = "Generate Smart Plan",
                onAction = {
                    scope.launch {
                        isGeneratingPlan = true
                        val result = repository.generateDailyPlan(
                            availableMinutes = effectiveAvailableMinutes,
                            sessionDurationMinutes = userPrefs.availability.defaultSessionDurationMinutes,
                            breakDurationMinutes = userPrefs.availability.breakDurationMinutes,
                            preferredSubjectIds = userPrefs.availability.preferredSubjectIds,
                            dateMillis = startOfDay,
                            chapters = chapters,
                            subjects = subjects,
                            revisions = revisions,
                            existingTasks = dayTasks
                        )
                        proposedPlanResult = result
                        isGeneratingPlan = false
                    }
                }
            )
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(dayTasks, key = { it.id }) { task ->
                    val linkedChapter = chapters.firstOrNull { it.id == task.chapterId }
                    val linkedSubject = subjects.firstOrNull { it.id == task.subjectId }

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp)),
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = task.status == TaskStatus.COMPLETED,
                                    onCheckedChange = { checked ->
                                        scope.launch {
                                            repository.updateTaskStatus(
                                                task,
                                                if (checked) TaskStatus.COMPLETED else TaskStatus.PLANNED
                                            )
                                        }
                                    },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = FokusElectricLime,
                                        checkmarkColor = FokusOnLime
                                    )
                                )

                                Spacer(modifier = Modifier.width(8.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = task.title,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    if (linkedChapter != null || linkedSubject != null) {
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "${linkedSubject?.title ?: ""} • ${linkedChapter?.title ?: ""}",
                                            fontSize = 11.sp,
                                            color = FokusElectricLime,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(onClick = { onStartFocus(task.chapterId) }) {
                                        Icon(
                                            imageVector = Icons.Default.PlayArrow,
                                            contentDescription = "Start Focus",
                                            tint = FokusElectricLime
                                        )
                                    }
                                    IconButton(onClick = {
                                        taskToEdit = task
                                        showAddTaskDialog = true
                                    }) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Edit",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    IconButton(onClick = { taskToDelete = task }) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }

                            // Metadata & recommendation reason
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 44.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    PriorityBadge(priority = task.priority)
                                    Text(
                                        text = "${task.plannedDurationMinutes} min",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                if (task.recommendedReason.isNotBlank()) {
                                    Text(
                                        text = task.recommendedReason,
                                        fontSize = 10.sp,
                                        color = Color(0xFFA0AB96),
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Availability Configuration Dialog
    if (showAvailabilityDialog) {
        var baseAvailableMinutes by remember { mutableIntStateOf(userPrefs.availability.availableMinutesPerDay) }
        var dayOverrideMinutes by remember { mutableIntStateOf(effectiveAvailableMinutes) }
        var sessionDuration by remember { mutableIntStateOf(userPrefs.availability.defaultSessionDurationMinutes) }
        var breakDuration by remember { mutableIntStateOf(userPrefs.availability.breakDurationMinutes) }

        AlertDialog(
            onDismissRequest = { showAvailabilityDialog = false },
            title = { Text(strings.dailyAvailability, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("Configuring availability for ${dayFormat.format(Date(startOfDay))}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                    // Selected Day Availability
                    Text(
                        text = "Availability for this day: $dayOverrideMinutes min (${dayOverrideMinutes / 60}h ${dayOverrideMinutes % 60}m)",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = FokusElectricLime
                    )
                    Slider(
                        value = dayOverrideMinutes.toFloat(),
                        onValueChange = { dayOverrideMinutes = it.toInt() },
                        valueRange = 30f..720f,
                        steps = 22
                    )

                    // Default session length
                    Text("Focus session length: $sessionDuration min", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(25, 45, 50, 90).forEach { mins ->
                            FilterChip(
                                selected = sessionDuration == mins,
                                onClick = { sessionDuration = mins },
                                label = { Text("${mins}m") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = FokusElectricLime,
                                    selectedLabelColor = FokusOnLime
                                )
                            )
                        }
                    }

                    // Break length
                    Text("Break duration between tasks: $breakDuration min", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(5, 10, 15, 20).forEach { mins ->
                            FilterChip(
                                selected = breakDuration == mins,
                                onClick = { breakDuration = mins },
                                label = { Text("${mins}m") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = FokusElectricLime,
                                    selectedLabelColor = FokusOnLime
                                )
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            preferencesManager?.setDaySpecificAvailableMinutes(epochDay, dayOverrideMinutes)
                            preferencesManager?.updateAvailability(
                                availableMinutes = baseAvailableMinutes,
                                startTime = userPrefs.availability.preferredStartTime,
                                endTime = userPrefs.availability.preferredEndTime,
                                sessionDuration = sessionDuration,
                                breakDuration = breakDuration,
                                studyDays = userPrefs.availability.weeklyStudyDays,
                                preferredSubjects = userPrefs.availability.preferredSubjectIds
                            )
                            showAvailabilityDialog = false
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
                TextButton(onClick = { showAvailabilityDialog = false }) {
                    Text(strings.cancel)
                }
            }
        )
    }

    // Add / Edit Manual Task Dialog
    if (showAddTaskDialog) {
        val isEditing = taskToEdit != null
        var taskTitle by remember { mutableStateOf(taskToEdit?.title ?: "") }
        var selectedSubjectId by remember { mutableStateOf(taskToEdit?.subjectId) }
        var selectedChapterId by remember { mutableStateOf(taskToEdit?.chapterId) }
        var durationMinutes by remember { mutableStateOf((taskToEdit?.plannedDurationMinutes ?: 50).toString()) }
        var priority by remember { mutableStateOf(taskToEdit?.priority ?: Priority.MEDIUM) }

        var subMenuExpanded by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showAddTaskDialog = false },
            title = { Text(if (isEditing) "Edit Study Task" else "Add Study Task", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = taskTitle,
                        onValueChange = { taskTitle = it },
                        label = { Text("Task Title (e.g. Solve Newton's Laws Question Bank)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (subjects.isNotEmpty()) {
                        ExposedDropdownMenuBox(
                            expanded = subMenuExpanded,
                            onExpandedChange = { subMenuExpanded = it }
                        ) {
                            val subTitle = subjects.firstOrNull { it.id == selectedSubjectId }?.title ?: "Select Subject (Optional)"
                            OutlinedTextField(
                                value = subTitle,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Linked Subject") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = subMenuExpanded) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor()
                            )
                            ExposedDropdownMenu(
                                expanded = subMenuExpanded,
                                onDismissRequest = { subMenuExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("None") },
                                    onClick = {
                                        selectedSubjectId = null
                                        selectedChapterId = null
                                        subMenuExpanded = false
                                    }
                                )
                                subjects.forEach { sub ->
                                    DropdownMenuItem(
                                        text = { Text(sub.title) },
                                        onClick = {
                                            selectedSubjectId = sub.id
                                            subMenuExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = durationMinutes,
                        onValueChange = { durationMinutes = it },
                        label = { Text("Planned Duration (Minutes)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("Priority", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Priority.values().forEach { pri ->
                            FilterChip(
                                selected = priority == pri,
                                onClick = { priority = pri },
                                label = { Text(pri.name.lowercase().capitalize(), fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = FokusElectricLime,
                                    selectedLabelColor = FokusOnLime
                                )
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (taskTitle.isNotBlank()) {
                            scope.launch {
                                val task = StudyTaskEntity(
                                    id = taskToEdit?.id ?: UUID.randomUUID().toString(),
                                    title = taskTitle.trim(),
                                    description = taskToEdit?.description ?: "",
                                    subjectId = selectedSubjectId,
                                    chapterId = selectedChapterId,
                                    plannedDate = startOfDay,
                                    plannedDurationMinutes = durationMinutes.toIntOrNull() ?: 50,
                                    priority = priority,
                                    status = taskToEdit?.status ?: TaskStatus.PLANNED,
                                    recommendedReason = taskToEdit?.recommendedReason ?: "User scheduled",
                                    isAutoGenerated = taskToEdit?.isAutoGenerated ?: false,
                                    updatedAt = System.currentTimeMillis()
                                )
                                if (isEditing) {
                                    repository.updateTask(task)
                                } else {
                                    repository.insertTask(task)
                                }
                                showAddTaskDialog = false
                            }
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
                TextButton(onClick = { showAddTaskDialog = false }) {
                    Text(strings.cancel)
                }
            }
        )
    }

    // Delete Task Confirmation Dialog
    if (taskToDelete != null) {
        val task = taskToDelete!!
        AlertDialog(
            onDismissRequest = { taskToDelete = null },
            title = { Text("Delete Task?") },
            text = { Text("Are you sure you want to delete '${task.title}'? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            repository.deleteTask(task)
                            taskToDelete = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = FokusCoral)
                ) {
                    Text(strings.delete)
                }
            },
            dismissButton = {
                TextButton(onClick = { taskToDelete = null }) {
                    Text(strings.cancel)
                }
            }
        )
    }
}
