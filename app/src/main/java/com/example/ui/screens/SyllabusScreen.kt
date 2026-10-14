package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ChapterEntity
import com.example.data.model.LearningStatus
import com.example.data.model.PracticeStatus
import com.example.data.model.Priority
import com.example.data.model.SubjectEntity
import com.example.data.repository.FokusRepository
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.FokusProgressBar
import com.example.ui.components.LearningStatusBadge
import com.example.ui.components.PracticeStatusBadge
import com.example.ui.components.PriorityBadge
import com.example.ui.localization.LocalFokusStrings
import com.example.ui.theme.FokusDarkBorder
import com.example.ui.theme.FokusElectricLime
import com.example.ui.theme.FokusOnLime
import kotlinx.coroutines.launch
import java.util.UUID

@Composable
fun SyllabusScreen(
    repository: FokusRepository,
    onNavigateToChapter: (String) -> Unit,
    onNavigateToBulkAdd: (subjectId: String?) -> Unit
) {
    val strings = LocalFokusStrings.current
    val scope = rememberCoroutineScope()

    val subjects by repository.allActiveSubjects.collectAsState(initial = emptyList())
    val allChapters by repository.allActiveChapters.collectAsState(initial = emptyList())

    var searchQuery by remember { mutableStateOf("") }
    var selectedStatusFilter by remember { mutableStateOf<LearningStatus?>(null) }
    var showAddSubjectDialog by remember { mutableStateOf(false) }
    var showAddChapterSubjectId by remember { mutableStateOf<String?>(null) }
    var subjectToEdit by remember { mutableStateOf<SubjectEntity?>(null) }
    var subjectToDelete by remember { mutableStateOf<SubjectEntity?>(null) }

    // Track expanded subject cards
    val expandedSubjects = remember { mutableStateListOf<String>() }

    // Search and filter logic
    val filteredChapters = allChapters.filter { chapter ->
        val matchesSearch = searchQuery.isBlank() ||
                chapter.title.contains(searchQuery, ignoreCase = true) ||
                chapter.chapterNumber.contains(searchQuery, ignoreCase = true)
        val matchesStatus = selectedStatusFilter == null || chapter.learningStatus == selectedStatusFilter
        matchesSearch && matchesStatus
    }

    val chaptersBySubject = filteredChapters.groupBy { it.subjectId }

    Box(modifier = Modifier.fillMaxSize()) {
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
                    Text(
                        text = strings.syllabusTitle,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Row {
                        IconButton(onClick = { onNavigateToBulkAdd(null) }) {
                            Icon(
                                imageVector = Icons.Default.PlaylistAdd,
                                contentDescription = "Bulk Add",
                                tint = FokusElectricLime
                            )
                        }
                        IconButton(onClick = { showAddSubjectDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add Subject",
                                tint = FokusElectricLime
                            )
                        }
                    }
                }
            }

            // Search Bar
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text(strings.searchPlaceholder, fontSize = 14.sp) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = FokusElectricLime,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    singleLine = true
                )
            }

            // Status Filter Chips
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        FilterChip(
                            selected = selectedStatusFilter == null,
                            onClick = { selectedStatusFilter = null },
                            label = { Text("All") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = FokusElectricLime,
                                selectedLabelColor = FokusOnLime
                            )
                        )
                    }
                    LearningStatus.values().forEach { status ->
                        item {
                            FilterChip(
                                selected = selectedStatusFilter == status,
                                onClick = {
                                    selectedStatusFilter = if (selectedStatusFilter == status) null else status
                                },
                                label = { Text(status.name.replace("_", " ").lowercase().capitalize()) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = FokusElectricLime,
                                    selectedLabelColor = FokusOnLime
                                )
                            )
                        }
                    }
                }
            }

            // Subject / Chapter List
            if (subjects.isEmpty()) {
                item {
                    EmptyStateCard(
                        message = strings.noSubjectsFound,
                        actionLabel = strings.addSubject,
                        onAction = { showAddSubjectDialog = true }
                    )
                }
            } else {
                items(subjects, key = { it.id }) { subject ->
                    val isExpanded = expandedSubjects.contains(subject.id) || searchQuery.isNotBlank()
                    val subChapters = chaptersBySubject[subject.id].orEmpty()
                    val totalSubjectChapters = allChapters.count { it.subjectId == subject.id }
                    val completedCount = allChapters.count {
                        it.subjectId == subject.id && it.learningStatus == LearningStatus.COMPLETED
                    }
                    val subColor = runCatching { Color(android.graphics.Color.parseColor(subject.colorHex)) }
                        .getOrDefault(FokusElectricLime)

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            // Subject Header
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (expandedSubjects.contains(subject.id)) {
                                            expandedSubjects.remove(subject.id)
                                        } else {
                                            expandedSubjects.add(subject.id)
                                        }
                                    },
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(12.dp)
                                            .clip(CircleShape)
                                            .background(subColor)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = subject.title,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "$completedCount / $totalSubjectChapters chapters completed",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(onClick = { showAddChapterSubjectId = subject.id }) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = "Add Chapter",
                                            tint = FokusElectricLime,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Icon(
                                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            // Collapsible Chapters List
                            AnimatedVisibility(visible = isExpanded) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 10.dp)
                                ) {
                                    if (subChapters.isEmpty()) {
                                        Text(
                                            text = "No chapters matching criteria. Tap '+' to add a chapter.",
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(vertical = 8.dp)
                                        )
                                    } else {
                                        subChapters.forEach { chapter ->
                                            Surface(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 4.dp)
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .clickable { onNavigateToChapter(chapter.id) },
                                                shape = RoundedCornerShape(10.dp),
                                                color = MaterialTheme.colorScheme.surface,
                                                border = androidx.compose.foundation.BorderStroke(
                                                    0.5.dp,
                                                    MaterialTheme.colorScheme.outlineVariant
                                                )
                                            ) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(12.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(
                                                            text = chapter.title,
                                                            fontWeight = FontWeight.Medium,
                                                            fontSize = 14.sp,
                                                            color = MaterialTheme.colorScheme.onSurface
                                                        )
                                                        Spacer(modifier = Modifier.height(4.dp))
                                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                            LearningStatusBadge(status = chapter.learningStatus)
                                                            if (chapter.practiceStatus != PracticeStatus.NOT_STARTED) {
                                                                PracticeStatusBadge(status = chapter.practiceStatus)
                                                            }
                                                        }
                                                    }

                                                    Text(
                                                        text = "${chapter.estimatedDurationMinutes}m",
                                                        fontSize = 12.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    // Subject Actions Bar
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 8.dp),
                                        horizontalArrangement = Arrangement.End
                                    ) {
                                        TextButton(onClick = { onNavigateToBulkAdd(subject.id) }) {
                                            Text("Bulk Add", fontSize = 12.sp)
                                        }
                                        TextButton(onClick = { subjectToEdit = subject }) {
                                            Text("Edit", fontSize = 12.sp)
                                        }
                                        TextButton(
                                            onClick = { subjectToDelete = subject },
                                            colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFFF5C5C))
                                        ) {
                                            Text("Delete", fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(60.dp))
            }
        }
    }

    // Add Subject Dialog
    if (showAddSubjectDialog) {
        var subjectTitle by remember { mutableStateOf("") }
        var shortName by remember { mutableStateOf("") }
        var selectedColor by remember { mutableStateOf("#A6FF00") }
        val colors = listOf("#A6FF00", "#4D9FFF", "#FFB300", "#FF5C5C", "#B388FF", "#00E5FF", "#FF4081")

        AlertDialog(
            onDismissRequest = { showAddSubjectDialog = false },
            title = { Text(strings.addSubject) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = subjectTitle,
                        onValueChange = { subjectTitle = it },
                        label = { Text("Subject Name (e.g. Physics 1st Paper)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = shortName,
                        onValueChange = { shortName = it },
                        label = { Text("Short Code (e.g. PHY1)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Text("Select Accent Color", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        colors.forEach { hex ->
                            val color = Color(android.graphics.Color.parseColor(hex))
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .border(
                                        if (selectedColor == hex) 3.dp else 0.dp,
                                        Color.White,
                                        CircleShape
                                    )
                                    .clickable { selectedColor = hex }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (subjectTitle.isNotBlank()) {
                            scope.launch {
                                repository.insertSubject(
                                    SubjectEntity(
                                        id = UUID.randomUUID().toString(),
                                        syllabusId = "default_syllabus_main",
                                        title = subjectTitle.trim(),
                                        shortName = shortName.trim(),
                                        colorHex = selectedColor
                                    )
                                )
                                showAddSubjectDialog = false
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
                TextButton(onClick = { showAddSubjectDialog = false }) {
                    Text(strings.cancel)
                }
            }
        )
    }

    // Add Chapter Dialog
    if (showAddChapterSubjectId != null) {
        val targetSubId = showAddChapterSubjectId!!
        var chapterTitle by remember { mutableStateOf("") }
        var chapterNumber by remember { mutableStateOf("") }
        var estimatedMinutes by remember { mutableStateOf("120") }
        var priority by remember { mutableStateOf(Priority.MEDIUM) }

        AlertDialog(
            onDismissRequest = { showAddChapterSubjectId = null },
            title = { Text(strings.addChapter) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = chapterTitle,
                        onValueChange = { chapterTitle = it },
                        label = { Text("Chapter Title (e.g. Vector)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = chapterNumber,
                        onValueChange = { chapterNumber = it },
                        label = { Text("Chapter Number (e.g. 2)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = estimatedMinutes,
                        onValueChange = { estimatedMinutes = it },
                        label = { Text("Estimated Duration (Minutes)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (chapterTitle.isNotBlank()) {
                            scope.launch {
                                repository.insertChapter(
                                    ChapterEntity(
                                        id = UUID.randomUUID().toString(),
                                        subjectId = targetSubId,
                                        title = chapterTitle.trim(),
                                        chapterNumber = chapterNumber.trim(),
                                        estimatedDurationMinutes = estimatedMinutes.toIntOrNull() ?: 120,
                                        priority = priority
                                    )
                                )
                                showAddChapterSubjectId = null
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
                TextButton(onClick = { showAddChapterSubjectId = null }) {
                    Text(strings.cancel)
                }
            }
        )
    }

    // Delete Confirmation Dialog
    if (subjectToDelete != null) {
        val sub = subjectToDelete!!
        AlertDialog(
            onDismissRequest = { subjectToDelete = null },
            title = { Text("Delete Subject?") },
            text = { Text("Are you sure you want to delete '${sub.title}' and all its chapters? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            repository.deleteSubject(sub)
                            subjectToDelete = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5C5C))
                ) {
                    Text(strings.delete)
                }
            },
            dismissButton = {
                TextButton(onClick = { subjectToDelete = null }) {
                    Text(strings.cancel)
                }
            }
        )
    }
}
