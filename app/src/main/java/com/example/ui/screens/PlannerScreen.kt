package com.example.ui.screens

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Priority
import com.example.data.model.StudyTaskEntity
import com.example.data.model.TaskStatus
import com.example.data.repository.FokusRepository
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.PriorityBadge
import com.example.ui.localization.LocalFokusStrings
import com.example.ui.theme.FokusElectricLime
import com.example.ui.theme.FokusOnLime
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlannerScreen(
    repository: FokusRepository,
    onStartFocus: (chapterId: String?) -> Unit
) {
    val strings = LocalFokusStrings.current
    val scope = rememberCoroutineScope()

    val subjects by repository.allActiveSubjects.collectAsState(initial = emptyList())
    val chapters by repository.allActiveChapters.collectAsState(initial = emptyList())
    val allTasks by repository.allTasks.collectAsState(initial = emptyList())

    var selectedDayOffset by remember { mutableIntStateOf(0) } // 0 = Today, 1 = Tomorrow, 2 = Day 3
    var showAddTaskDialog by remember { mutableStateOf(false) }

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

    val filteredTasks = allTasks.filter { it.plannedDate in startOfDay..endOfDay }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Daily Study Planner",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Organize and execute targeted daily tasks",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(onClick = { showAddTaskDialog = true }) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Task",
                    tint = FokusElectricLime
                )
            }
        }

        // Date Tabs (Today, Tomorrow, +2d, +3d, +4d)
        val dayFormat = SimpleDateFormat("EEE, dd MMM", Locale.getDefault())
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(5) { offset ->
                val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, offset) }
                val label = if (offset == 0) "Today" else if (offset == 1) "Tomorrow" else dayFormat.format(cal.time)
                FilterChip(
                    selected = selectedDayOffset == offset,
                    onClick = { selectedDayOffset = offset },
                    label = { Text(label) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = FokusElectricLime,
                        selectedLabelColor = FokusOnLime
                    )
                )
            }
        }

        // Tasks List
        if (filteredTasks.isEmpty()) {
            EmptyStateCard(
                message = "No study tasks scheduled for this day.",
                actionLabel = "Schedule Task",
                onAction = { showAddTaskDialog = true }
            )
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredTasks, key = { it.id }) { task ->
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
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
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
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    PriorityBadge(priority = task.priority)
                                    Text(
                                        text = "${task.plannedDurationMinutes} min",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Row {
                                IconButton(onClick = { onStartFocus(task.chapterId) }) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = "Start Focus",
                                        tint = FokusElectricLime
                                    )
                                }
                                IconButton(onClick = {
                                    scope.launch { repository.deleteTask(task) }
                                }) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Add Task Dialog
    if (showAddTaskDialog) {
        var taskTitle by remember { mutableStateOf("") }
        var selectedSubjectId by remember { mutableStateOf<String?>(null) }
        var selectedChapterId by remember { mutableStateOf<String?>(null) }
        var durationMinutes by remember { mutableStateOf("60") }
        var priority by remember { mutableStateOf(Priority.MEDIUM) }

        var subMenuExpanded by remember { mutableStateOf(false) }
        var chMenuExpanded by remember { mutableStateOf(false) }

        val eligibleChapters = chapters.filter {
            selectedSubjectId == null || it.subjectId == selectedSubjectId
        }

        AlertDialog(
            onDismissRequest = { showAddTaskDialog = false },
            title = { Text("Schedule Study Task") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = taskTitle,
                        onValueChange = { taskTitle = it },
                        label = { Text("Task Title (e.g. Solve Newton's Laws Question Bank)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Subject Dropdown
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
                                repository.insertTask(
                                    StudyTaskEntity(
                                        id = UUID.randomUUID().toString(),
                                        title = taskTitle.trim(),
                                        subjectId = selectedSubjectId,
                                        chapterId = selectedChapterId,
                                        plannedDate = startOfDay,
                                        plannedDurationMinutes = durationMinutes.toIntOrNull() ?: 60,
                                        priority = priority
                                    )
                                )
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
}
