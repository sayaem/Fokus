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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import com.example.data.model.DailyJournalEntity
import com.example.data.repository.FokusRepository
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.FokusSectionHeader
import com.example.ui.localization.LocalFokusStrings
import com.example.ui.theme.FokusAmber
import com.example.ui.theme.FokusBlue
import com.example.ui.theme.FokusCoral
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
fun DailyStudyJournalScreen(
    repository: FokusRepository,
    onBack: (() -> Unit)? = null
) {
    val strings = LocalFokusStrings.current
    val scope = rememberCoroutineScope()

    val entries by repository.allJournalEntries.collectAsState(initial = emptyList())
    val subjects by repository.allActiveSubjects.collectAsState(initial = emptyList())
    val chapters by repository.allActiveChapters.collectAsState(initial = emptyList())

    var searchQuery by remember { mutableStateOf("") }
    var selectedSubjectFilter by remember { mutableStateOf<String?>(null) }
    var showEditDialog by remember { mutableStateOf(false) }
    var entryToEdit by remember { mutableStateOf<DailyJournalEntity?>(null) }
    var entryToDelete by remember { mutableStateOf<DailyJournalEntity?>(null) }

    val calendar = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
    }
    val todayStartMillis = calendar.timeInMillis
    calendar.set(Calendar.HOUR_OF_DAY, 23)
    calendar.set(Calendar.MINUTE, 59)
    calendar.set(Calendar.SECOND, 59)
    val todayEndMillis = calendar.timeInMillis

    val todayEntry = entries.firstOrNull { it.dateMillis in todayStartMillis..todayEndMillis }

    val filteredEntries = entries.filter { entry ->
        val matchesSearch = searchQuery.isBlank() ||
                entry.title.contains(searchQuery, ignoreCase = true) ||
                entry.learnedText.contains(searchQuery, ignoreCase = true) ||
                entry.difficultiesText.contains(searchQuery, ignoreCase = true) ||
                entry.notes.contains(searchQuery, ignoreCase = true)
        val matchesSubject = selectedSubjectFilter == null || entry.subjectId == selectedSubjectFilter
        matchesSearch && matchesSubject
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(strings.journalTitle, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                actions = {
                    IconButton(onClick = {
                        entryToEdit = null
                        showEditDialog = true
                    }) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "New Entry",
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
            // Search Bar
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search journal notes, reflections...", fontSize = 13.sp) },
                    leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = FokusElectricLime,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    ),
                    singleLine = true
                )
            }

            // Subject Filter Chips
            if (subjects.isNotEmpty()) {
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item {
                            FilterChip(
                                selected = selectedSubjectFilter == null,
                                onClick = { selectedSubjectFilter = null },
                                label = { Text("All Subjects", fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = FokusElectricLime,
                                    selectedLabelColor = FokusOnLime
                                )
                            )
                        }
                        items(subjects, key = { it.id }) { sub ->
                            FilterChip(
                                selected = selectedSubjectFilter == sub.id,
                                onClick = {
                                    selectedSubjectFilter = if (selectedSubjectFilter == sub.id) null else sub.id
                                },
                                label = { Text(sub.title, fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = FokusElectricLime,
                                    selectedLabelColor = FokusOnLime
                                )
                            )
                        }
                    }
                }
            }

            // Today's Journal Prompt Card
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    color = if (todayEntry != null) Color(0xFF1B2C0D) else MaterialTheme.colorScheme.surfaceVariant,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (todayEntry != null) FokusElectricLime else MaterialTheme.colorScheme.outlineVariant
                    )
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(if (todayEntry != null) Color(0xFF2E4814) else Color(0xFF262C22)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Psychology,
                                        contentDescription = null,
                                        tint = FokusElectricLime,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = if (todayEntry != null) "Today's Journal Recorded" else "Today's Study Journal",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = if (todayEntry != null) Color.White else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Reflect on concepts learned, hurdles faced & tomorrow's plan",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        if (todayEntry != null) {
                            Text(
                                text = todayEntry.learnedText.ifBlank { todayEntry.notes.ifBlank { "Journal recorded." } },
                                fontSize = 13.sp,
                                color = Color(0xFFC7D4BF),
                                maxLines = 3
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                                OutlinedButton(
                                    onClick = {
                                        entryToEdit = todayEntry
                                        showEditDialog = true
                                    },
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Edit Today's Journal", fontSize = 12.sp)
                                }
                            }
                        } else {
                            Button(
                                onClick = {
                                    entryToEdit = null
                                    showEditDialog = true
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = FokusElectricLime,
                                    contentColor = FokusOnLime
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(imageVector = Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Write Today's Entry (2 mins)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }

            // Entries Chronological List
            item {
                FokusSectionHeader(title = "Study Journal History (${filteredEntries.size})")
            }

            if (filteredEntries.isEmpty()) {
                item {
                    EmptyStateCard(
                        message = "No study journal entries found. Taking two minutes each day to record your learning and stumbling blocks builds self-awareness and study momentum.",
                        actionLabel = "Create First Entry",
                        onAction = {
                            entryToEdit = null
                            showEditDialog = true
                        }
                    )
                }
            } else {
                val dateFormat = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.getDefault())
                items(filteredEntries, key = { it.id }) { entry ->
                    val linkedSubject = subjects.firstOrNull { it.id == entry.subjectId }
                    val linkedChapter = chapters.firstOrNull { it.id == entry.chapterId }

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp)),
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
                                Column {
                                    Text(
                                        text = dateFormat.format(Date(entry.dateMillis)),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    if (linkedSubject != null) {
                                        Text(
                                            text = "${linkedSubject.title}${if (linkedChapter != null) " • " + linkedChapter.title else ""}",
                                            fontSize = 11.sp,
                                            color = FokusElectricLime,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Row {
                                        for (i in 1..entry.moodEnergyRating) {
                                            Icon(
                                                imageVector = Icons.Default.Star,
                                                contentDescription = null,
                                                tint = FokusAmber,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    IconButton(
                                        onClick = {
                                            entryToEdit = entry
                                            showEditDialog = true
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(16.dp))
                                    }
                                    IconButton(
                                        onClick = { entryToDelete = entry },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete",
                                            tint = FokusCoral,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }

                            if (entry.title.isNotBlank()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(text = entry.title, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                            }

                            if (entry.learnedText.isNotBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "💡 What I learned: ${entry.learnedText}",
                                    fontSize = 13.sp,
                                    lineHeight = 18.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            if (entry.difficultiesText.isNotBlank()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "⚠️ Difficulties / Questions: ${entry.difficultiesText}",
                                    fontSize = 13.sp,
                                    lineHeight = 18.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            if (entry.nextStepsText.isNotBlank()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "🎯 Tomorrow's Target: ${entry.nextStepsText}",
                                    fontSize = 13.sp,
                                    lineHeight = 18.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            if (entry.notes.isNotBlank()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = entry.notes,
                                    fontSize = 12.sp,
                                    lineHeight = 18.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
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

    // Edit/Create Journal Dialog
    if (showEditDialog) {
        val isEditing = entryToEdit != null
        var title by remember { mutableStateOf(entryToEdit?.title ?: "") }
        var learned by remember { mutableStateOf(entryToEdit?.learnedText ?: "") }
        var difficulties by remember { mutableStateOf(entryToEdit?.difficultiesText ?: "") }
        var nextSteps by remember { mutableStateOf(entryToEdit?.nextStepsText ?: "") }
        var notes by remember { mutableStateOf(entryToEdit?.notes ?: "") }
        var moodRating by remember { mutableIntStateOf(entryToEdit?.moodEnergyRating ?: 4) }
        var selectedSubjectId by remember { mutableStateOf(entryToEdit?.subjectId) }
        var selectedChapterId by remember { mutableStateOf(entryToEdit?.chapterId) }

        var subDropdownExpanded by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = {
                Text(
                    text = if (isEditing) "Edit Journal Entry" else "New Study Journal Entry",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    item {
                        OutlinedTextField(
                            value = title,
                            onValueChange = { title = it },
                            label = { Text("Title / Main Focus (Optional)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = learned,
                            onValueChange = { learned = it },
                            label = { Text("What I learned today") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = difficulties,
                            onValueChange = { difficulties = it },
                            label = { Text("What I found difficult / Tricky concepts") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = nextSteps,
                            onValueChange = { nextSteps = it },
                            label = { Text("What I should do tomorrow") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = notes,
                            onValueChange = { notes = it },
                            label = { Text("Free-form notes / reflections") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2
                        )
                    }

                    item {
                        Text("Energy & Focus Rating:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            for (star in 1..5) {
                                IconButton(onClick = { moodRating = star }) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = "Rating $star",
                                        tint = if (star <= moodRating) FokusAmber else Color.Gray
                                    )
                                }
                            }
                        }
                    }

                    if (subjects.isNotEmpty()) {
                        item {
                            ExposedDropdownMenuBox(
                                expanded = subDropdownExpanded,
                                onExpandedChange = { subDropdownExpanded = it }
                            ) {
                                val currentSub = subjects.firstOrNull { it.id == selectedSubjectId }?.title ?: "Select Subject (Optional)"
                                OutlinedTextField(
                                    value = currentSub,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Associated Subject") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = subDropdownExpanded) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .menuAnchor()
                                )
                                ExposedDropdownMenu(
                                    expanded = subDropdownExpanded,
                                    onDismissRequest = { subDropdownExpanded = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("None") },
                                        onClick = {
                                            selectedSubjectId = null
                                            selectedChapterId = null
                                            subDropdownExpanded = false
                                        }
                                    )
                                    subjects.forEach { sub ->
                                        DropdownMenuItem(
                                            text = { Text(sub.title) },
                                            onClick = {
                                                selectedSubjectId = sub.id
                                                subDropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            val entryDate = entryToEdit?.dateMillis ?: System.currentTimeMillis()
                            val entry = DailyJournalEntity(
                                id = entryToEdit?.id ?: UUID.randomUUID().toString(),
                                dateMillis = entryDate,
                                title = title.trim(),
                                learnedText = learned.trim(),
                                difficultiesText = difficulties.trim(),
                                nextStepsText = nextSteps.trim(),
                                notes = notes.trim(),
                                moodEnergyRating = moodRating,
                                subjectId = selectedSubjectId,
                                chapterId = selectedChapterId,
                                linkedSessionId = entryToEdit?.linkedSessionId,
                                updatedAt = System.currentTimeMillis()
                            )
                            if (isEditing) {
                                repository.updateJournalEntry(entry)
                            } else {
                                repository.saveJournalEntry(entry)
                            }
                            showEditDialog = false
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
                TextButton(onClick = { showEditDialog = false }) {
                    Text(strings.cancel)
                }
            }
        )
    }

    // Delete Confirmation Dialog
    if (entryToDelete != null) {
        val entry = entryToDelete!!
        AlertDialog(
            onDismissRequest = { entryToDelete = null },
            title = { Text("Delete Journal Entry?") },
            text = { Text("Are you sure you want to delete this journal entry? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            repository.deleteJournalEntry(entry)
                            entryToDelete = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = FokusCoral)
                ) {
                    Text(strings.delete)
                }
            },
            dismissButton = {
                TextButton(onClick = { entryToDelete = null }) {
                    Text(strings.cancel)
                }
            }
        )
    }
}
