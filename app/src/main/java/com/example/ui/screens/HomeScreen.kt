package com.example.ui.screens

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ChapterEntity
import com.example.data.model.StudyTaskEntity
import com.example.data.model.TaskStatus
import com.example.data.repository.FokusRepository
import com.example.data.repository.SubjectWithProgress
import com.example.data.repository.SyllabusProgressSummary
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.FokusProgressBar
import com.example.ui.components.FokusProgressRing
import com.example.ui.components.FokusSectionHeader
import com.example.ui.components.PriorityBadge
import com.example.ui.localization.LocalFokusStrings
import com.example.ui.theme.FokusAmber
import com.example.ui.theme.FokusBlue
import com.example.ui.theme.FokusElectricLime
import com.example.ui.theme.FokusOnLime
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    repository: FokusRepository,
    onNavigateToSyllabus: () -> Unit,
    onNavigateToSubject: (String) -> Unit,
    onNavigateToChapter: (String) -> Unit,
    onStartFocus: (chapterId: String?) -> Unit,
    onNavigateToRevisions: () -> Unit,
    onNavigateToPlanner: () -> Unit,
    onNavigateToJournal: () -> Unit = {}
) {
    val strings = LocalFokusStrings.current
    val scope = rememberCoroutineScope()

    val progressSummary by repository.syllabusProgressSummary.collectAsState(
        initial = SyllabusProgressSummary(0, 0, 0, 0, 0, 0f, false)
    )
    val subjectsWithProgress by repository.subjectsWithProgress.collectAsState(initial = emptyList())
    val activeChapters by repository.allActiveChapters.collectAsState(initial = emptyList())
    val pendingRevisions by repository.pendingRevisions.collectAsState(initial = emptyList())

    // Today's tasks
    val calendar = Calendar.getInstance()
    calendar.set(Calendar.HOUR_OF_DAY, 0)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    val startOfDay = calendar.timeInMillis
    calendar.set(Calendar.HOUR_OF_DAY, 23)
    calendar.set(Calendar.MINUTE, 59)
    calendar.set(Calendar.SECOND, 59)
    val endOfDay = calendar.timeInMillis

    val todaysTasks by repository.getTasksForDate(startOfDay, endOfDay).collectAsState(initial = emptyList())

    // Greeting logic
    val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val greeting = when (currentHour) {
        in 5..11 -> strings.greetingMorning
        in 12..16 -> strings.greetingAfternoon
        in 17..20 -> strings.greetingEvening
        else -> strings.greetingNight
    }
    val dateFormat = SimpleDateFormat("EEEE, dd MMMM", Locale.getDefault())
    val currentDateStr = dateFormat.format(Date())

    val recentlyStudiedChapter = activeChapters.filter { it.lastStudiedAt != null }
        .maxByOrNull { it.lastStudiedAt ?: 0L }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = currentDateStr.uppercase(),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = greeting,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                // Quick Start Focus button
                Button(
                    onClick = { onStartFocus(null) },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = FokusElectricLime,
                        contentColor = FokusOnLime
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Timer,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = strings.startSession, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }

        // Main Syllabus Progress Card
        item {
            if (!progressSummary.hasContent) {
                EmptyStateCard(
                    message = strings.emptySyllabusHome,
                    actionLabel = strings.createFirstSubject,
                    onAction = onNavigateToSyllabus
                )
            } else {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = strings.syllabusProgress.uppercase(),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = FokusElectricLime,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "${progressSummary.completedChapters} of ${progressSummary.totalChapters} chapters",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Column {
                                    Text(
                                        text = "${progressSummary.inProgressChapters}",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = FokusBlue
                                    )
                                    Text(
                                        text = strings.inProgress,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Column {
                                    Text(
                                        text = "${progressSummary.notStartedChapters}",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = strings.remaining,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        FokusProgressRing(
                            progress = progressSummary.overallPercentage / 100f,
                            modifier = Modifier.size(86.dp),
                            strokeWidth = 9.dp
                        ) {
                            Text(
                                text = "${progressSummary.overallPercentage.toInt()}%",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }

        // Due Revisions Banner (if any)
        if (pendingRevisions.isNotEmpty()) {
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onNavigateToRevisions() },
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF382606),
                    border = androidx.compose.foundation.BorderStroke(1.dp, FokusAmber.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.NotificationsActive,
                                contentDescription = null,
                                tint = FokusAmber,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "${pendingRevisions.size} ${strings.dueRevisions}",
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "Maintain spaced repetition discipline",
                                    color = Color(0xFFE2C485),
                                    fontSize = 12.sp
                                )
                            }
                        }
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = null,
                            tint = FokusAmber,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        // Continue Studying Card
        if (recentlyStudiedChapter != null) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = strings.continueStudying.uppercase(),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = recentlyStudiedChapter.title,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Button(
                            onClick = { onStartFocus(recentlyStudiedChapter.id) },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        ) {
                            Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Resume", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Today's Study Plan
        item {
            FokusSectionHeader(
                title = strings.todayStudyPlan,
                actionLabel = strings.addTask,
                onAction = onNavigateToPlanner
            )
        }

        if (todaysTasks.isEmpty()) {
            item {
                EmptyStateCard(
                    message = strings.noTasksToday,
                    actionLabel = strings.addTask,
                    onAction = onNavigateToPlanner
                )
            }
        } else {
            items(todaysTasks, key = { it.id }) { task ->
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
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                PriorityBadge(priority = task.priority)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "${task.plannedDurationMinutes} min",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        IconButton(onClick = { onStartFocus(task.chapterId) }) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Start Focus",
                                tint = FokusElectricLime
                            )
                        }
                    }
                }
            }
        }

        // Daily Study Journal Shortcut Card
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onNavigateToJournal() },
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(
                            imageVector = Icons.Default.EditNote,
                            contentDescription = null,
                            tint = FokusElectricLime,
                            modifier = Modifier.size(26.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = strings.journalTitle,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Record daily takeaways, stumbling blocks & self-reflection",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = "Open Journal",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Subject Breakdown
        item {
            FokusSectionHeader(
                title = strings.subjectProgress,
                actionLabel = "View All",
                onAction = onNavigateToSyllabus
            )
        }

        if (subjectsWithProgress.isEmpty()) {
            item {
                EmptyStateCard(
                    message = strings.noSubjectsFound,
                    actionLabel = strings.addSubject,
                    onAction = onNavigateToSyllabus
                )
            }
        } else {
            items(subjectsWithProgress, key = { it.subject.id }) { subProg ->
                val subColor = runCatching { Color(android.graphics.Color.parseColor(subProg.subject.colorHex)) }
                    .getOrDefault(FokusElectricLime)

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { onNavigateToSubject(subProg.subject.id) },
                    shape = RoundedCornerShape(14.dp),
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
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(subColor)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = subProg.subject.title,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Text(
                                text = "${subProg.completionPercentage.toInt()}%",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = FokusElectricLime
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        FokusProgressBar(
                            progress = subProg.completionPercentage / 100f,
                            color = subColor
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "${subProg.completedChapters} of ${subProg.totalChapters} ${strings.chaptersCount}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
