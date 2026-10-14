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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ChapterEntity
import com.example.data.model.LearningStatus
import com.example.data.model.MistakeRecordEntity
import com.example.data.model.PersonalNoteEntity
import com.example.data.model.PracticeStatus
import com.example.data.model.RevisionStatus
import com.example.data.model.TopicEntity
import com.example.data.repository.FokusRepository
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.FokusMetricCard
import com.example.ui.components.LearningStatusBadge
import com.example.ui.components.PracticeStatusBadge
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
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChapterDetailScreen(
    chapterId: String,
    repository: FokusRepository,
    onBack: () -> Unit,
    onStartFocus: (chapterId: String) -> Unit
) {
    val strings = LocalFokusStrings.current
    val scope = rememberCoroutineScope()

    val chapter by repository.getChapterByIdFlow(chapterId).collectAsState(initial = null)
    val topics by repository.getTopicsForChapter(chapterId).collectAsState(initial = emptyList())
    val notes by repository.getNotesForChapter(chapterId).collectAsState(initial = emptyList())
    val mistakes by repository.getMistakesForChapter(chapterId).collectAsState(initial = emptyList())
    val sessions by repository.getSessionsForChapter(chapterId).collectAsState(initial = emptyList())

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabTitles = listOf("Topics", "Notes", "Mistakes", "Sessions")

    var showAddTopicDialog by remember { mutableStateOf(false) }
    var showAddNoteDialog by remember { mutableStateOf(false) }
    var showAddMistakeDialog by remember { mutableStateOf(false) }
    var showScheduleRevisionDialog by remember { mutableStateOf(false) }

    if (chapter == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Loading chapter details...", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    val currentChapter = chapter!!

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = currentChapter.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        maxLines = 1
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { onStartFocus(currentChapter.id) }) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Start Focus",
                            tint = FokusElectricLime
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Metrics Row: Estimated vs Actual study time
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val actualMinutes = (currentChapter.actualDurationSeconds / 60).toInt()
                    val actualHoursStr = if (actualMinutes >= 60) "${actualMinutes / 60}h ${actualMinutes % 60}m" else "${actualMinutes}m"

                    FokusMetricCard(
                        title = strings.estimatedDuration,
                        value = "${currentChapter.estimatedDurationMinutes}m",
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Timer
                    )
                    FokusMetricCard(
                        title = strings.actualDuration,
                        value = actualHoursStr,
                        modifier = Modifier.weight(1f),
                        accentColor = FokusBlue,
                        icon = Icons.Default.Schedule
                    )
                }
            }

            // Learning Status Selector Card
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = strings.learningStatus,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            LearningStatus.values().forEach { status ->
                                val isSelected = currentChapter.learningStatus == status
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        scope.launch {
                                            repository.updateChapter(
                                                currentChapter.copy(
                                                    learningStatus = status,
                                                    updatedAt = System.currentTimeMillis()
                                                )
                                            )
                                        }
                                    },
                                    label = { Text(status.name.replace("_", " ").lowercase().capitalize(), fontSize = 11.sp) },
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

            // Practice Status Selector Card
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = strings.practiceStatus,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            PracticeStatus.values().take(4).forEach { status ->
                                val isSelected = currentChapter.practiceStatus == status
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        scope.launch {
                                            repository.updateChapter(
                                                currentChapter.copy(
                                                    practiceStatus = status,
                                                    updatedAt = System.currentTimeMillis()
                                                )
                                            )
                                        }
                                    },
                                    label = { Text(status.name.replace("_", " ").lowercase().capitalize(), fontSize = 11.sp) },
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

            // Revision Status & Schedule Action
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
                        Column {
                            Text(
                                text = strings.revisionStatus,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = currentChapter.revisionStatus.name.replace("_", " "),
                                fontSize = 12.sp,
                                color = when (currentChapter.revisionStatus) {
                                    RevisionStatus.REVISED -> FokusElectricLime
                                    RevisionStatus.DUE, RevisionStatus.OVERDUE -> FokusAmber
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        OutlinedButton(
                            onClick = { showScheduleRevisionDialog = true },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(imageVector = Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(strings.scheduleRevision, fontSize = 12.sp)
                        }
                    }
                }
            }

            // Tab Selector
            item {
                ScrollableTabRow(
                    selectedTabIndex = selectedTabIndex,
                    containerColor = MaterialTheme.colorScheme.background,
                    contentColor = MaterialTheme.colorScheme.onBackground,
                    edgePadding = 0.dp
                ) {
                    tabTitles.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTabIndex == index,
                            onClick = { selectedTabIndex = index },
                            text = {
                                Text(
                                    text = when (index) {
                                        0 -> "$title (${topics.size})"
                                        1 -> "$title (${notes.size})"
                                        2 -> "$title (${mistakes.size})"
                                        3 -> "$title (${sessions.size})"
                                        else -> title
                                    },
                                    fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        )
                    }
                }
            }

            // Tab Content
            when (selectedTabIndex) {
                0 -> {
                    // Topics Tab
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Topic Checklist", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            TextButton(onClick = { showAddTopicDialog = true }) {
                                Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(strings.addTopic, fontSize = 12.sp)
                            }
                        }
                    }

                    if (topics.isEmpty()) {
                        item {
                            EmptyStateCard(
                                message = "No subtopics added yet. Breaking down chapters into topics enables precise progress tracking.",
                                actionLabel = strings.addTopic,
                                onAction = { showAddTopicDialog = true }
                            )
                        }
                    } else {
                        items(topics, key = { it.id }) { topic ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp)),
                                shape = RoundedCornerShape(10.dp),
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
                                        checked = topic.learningStatus == LearningStatus.COMPLETED,
                                        onCheckedChange = { checked ->
                                            scope.launch {
                                                repository.updateTopic(
                                                    topic.copy(
                                                        learningStatus = if (checked) LearningStatus.COMPLETED else LearningStatus.NOT_STARTED,
                                                        updatedAt = System.currentTimeMillis()
                                                    )
                                                )
                                            }
                                        },
                                        colors = CheckboxDefaults.colors(
                                            checkedColor = FokusElectricLime,
                                            checkmarkColor = FokusOnLime
                                        )
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = topic.title,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 14.sp,
                                        modifier = Modifier.weight(1f)
                                    )
                                    LearningStatusBadge(status = topic.learningStatus)
                                }
                            }
                        }
                    }
                }
                1 -> {
                    // Notes Tab
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(strings.notes, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            TextButton(onClick = { showAddNoteDialog = true }) {
                                Icon(imageVector = Icons.Default.NoteAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(strings.addNote, fontSize = 12.sp)
                            }
                        }
                    }

                    if (notes.isEmpty()) {
                        item {
                            EmptyStateCard(
                                message = "No personal study notes for this chapter yet.",
                                actionLabel = strings.addNote,
                                onAction = { showAddNoteDialog = true }
                            )
                        }
                    } else {
                        items(notes, key = { it.id }) { note ->
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text(text = note.title, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(text = note.content, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
                2 -> {
                    // Mistakes Tab
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(strings.mistakeLog, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            TextButton(onClick = { showAddMistakeDialog = true }) {
                                Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(strings.addMistake, fontSize = 12.sp)
                            }
                        }
                    }

                    if (mistakes.isEmpty()) {
                        item {
                            EmptyStateCard(
                                message = "Your mistake log is clean. Record tricky concepts or admission questions you struggled with to avoid repeating them.",
                                actionLabel = strings.addMistake,
                                onAction = { showAddMistakeDialog = true }
                            )
                        }
                    } else {
                        items(mistakes, key = { it.id }) { mistake ->
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = mistake.questionOrDescription,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Checkbox(
                                            checked = mistake.isResolved,
                                            onCheckedChange = { resolved ->
                                                scope.launch {
                                                    repository.updateMistake(mistake.copy(isResolved = resolved))
                                                }
                                            },
                                            colors = CheckboxDefaults.colors(
                                                checkedColor = FokusElectricLime,
                                                checkmarkColor = FokusOnLime
                                            )
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = mistake.explanation,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
                3 -> {
                    // Session History Tab
                    if (sessions.isEmpty()) {
                        item {
                            EmptyStateCard(
                                message = "No recorded focus sessions for this chapter yet.",
                                actionLabel = strings.startSession,
                                onAction = { onStartFocus(currentChapter.id) }
                            )
                        }
                    } else {
                        val sessionDateFormat = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
                        items(sessions, key = { it.id }) { session ->
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = sessionDateFormat.format(Date(session.startedAt)),
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 13.sp
                                        )
                                        Text(
                                            text = "Rating: ${session.focusQualityRating}/5 • Interruptions: ${session.interruptionCount}",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Text(
                                        text = "${(session.actualDurationSeconds / 60)} min",
                                        fontWeight = FontWeight.Bold,
                                        color = FokusElectricLime,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }

    // Add Topic Dialog
    if (showAddTopicDialog) {
        var topicTitle by remember { mutableStateOf("") }
        var topicEstMinutes by remember { mutableStateOf("45") }

        AlertDialog(
            onDismissRequest = { showAddTopicDialog = false },
            title = { Text(strings.addTopic) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = topicTitle,
                        onValueChange = { topicTitle = it },
                        label = { Text("Topic Title") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = topicEstMinutes,
                        onValueChange = { topicEstMinutes = it },
                        label = { Text("Est. Minutes") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (topicTitle.isNotBlank()) {
                            scope.launch {
                                repository.insertTopic(
                                    TopicEntity(
                                        id = UUID.randomUUID().toString(),
                                        chapterId = currentChapter.id,
                                        title = topicTitle.trim(),
                                        estimatedDurationMinutes = topicEstMinutes.toIntOrNull() ?: 45
                                    )
                                )
                                showAddTopicDialog = false
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
                TextButton(onClick = { showAddTopicDialog = false }) {
                    Text(strings.cancel)
                }
            }
        )
    }

    // Add Note Dialog
    if (showAddNoteDialog) {
        var noteTitle by remember { mutableStateOf("") }
        var noteContent by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddNoteDialog = false },
            title = { Text(strings.addNote) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = noteTitle,
                        onValueChange = { noteTitle = it },
                        label = { Text("Note Title / Key Formula") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = noteContent,
                        onValueChange = { noteContent = it },
                        label = { Text("Content / Explanation") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (noteTitle.isNotBlank()) {
                            scope.launch {
                                repository.insertNote(
                                    PersonalNoteEntity(
                                        id = UUID.randomUUID().toString(),
                                        chapterId = currentChapter.id,
                                        subjectId = currentChapter.subjectId,
                                        title = noteTitle.trim(),
                                        content = noteContent.trim()
                                    )
                                )
                                showAddNoteDialog = false
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
                TextButton(onClick = { showAddNoteDialog = false }) {
                    Text(strings.cancel)
                }
            }
        )
    }

    // Add Mistake Dialog
    if (showAddMistakeDialog) {
        var mistakeDesc by remember { mutableStateOf("") }
        var explanation by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddMistakeDialog = false },
            title = { Text(strings.addMistake) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = mistakeDesc,
                        onValueChange = { mistakeDesc = it },
                        label = { Text("Question or Mistake Description") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2
                    )
                    OutlinedTextField(
                        value = explanation,
                        onValueChange = { explanation = it },
                        label = { Text("Correct Concept / Resolution") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (mistakeDesc.isNotBlank()) {
                            scope.launch {
                                repository.insertMistake(
                                    MistakeRecordEntity(
                                        id = UUID.randomUUID().toString(),
                                        chapterId = currentChapter.id,
                                        subjectId = currentChapter.subjectId,
                                        questionOrDescription = mistakeDesc.trim(),
                                        explanation = explanation.trim()
                                    )
                                )
                                showAddMistakeDialog = false
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
                TextButton(onClick = { showAddMistakeDialog = false }) {
                    Text(strings.cancel)
                }
            }
        )
    }

    // Schedule Revision Dialog
    if (showScheduleRevisionDialog) {
        var daysAhead by remember { mutableIntStateOf(3) }
        val presets = listOf(1, 3, 7, 14, 30)

        AlertDialog(
            onDismissRequest = { showScheduleRevisionDialog = false },
            title = { Text(strings.scheduleRevision) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Select spaced repetition interval:", fontSize = 14.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        presets.forEach { d ->
                            FilterChip(
                                selected = daysAhead == d,
                                onClick = { daysAhead = d },
                                label = { Text("+$d d") },
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
                            val scheduledDate = System.currentTimeMillis() + (daysAhead * 86400000L)
                            repository.scheduleRevision(
                                subjectId = currentChapter.subjectId,
                                chapterId = currentChapter.id,
                                scheduledDateMillis = scheduledDate
                            )
                            showScheduleRevisionDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = FokusElectricLime,
                        contentColor = FokusOnLime
                    )
                ) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                TextButton(onClick = { showScheduleRevisionDialog = false }) {
                    Text(strings.cancel)
                }
            }
        )
    }
}
