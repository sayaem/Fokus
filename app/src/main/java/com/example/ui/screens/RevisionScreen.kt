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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.EditCalendar
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.example.data.model.RevisionRecordEntity
import com.example.data.model.RevisionStatus
import com.example.data.preferences.PreferencesManager
import com.example.data.preferences.UserPreferences
import com.example.data.repository.FokusRepository
import com.example.ui.components.EmptyStateCard
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

@Composable
fun RevisionScreen(
    repository: FokusRepository,
    preferencesManager: PreferencesManager? = null,
    onStartFocus: (chapterId: String) -> Unit
) {
    val strings = LocalFokusStrings.current
    val scope = rememberCoroutineScope()

    val userPrefs by preferencesManager?.preferencesFlow?.collectAsState(initial = UserPreferences())
        ?: remember { mutableStateOf(UserPreferences()) }

    val allRevisions by repository.allRevisions.collectAsState(initial = emptyList())
    val chapters by repository.allActiveChapters.collectAsState(initial = emptyList())
    val subjects by repository.allActiveSubjects.collectAsState(initial = emptyList())

    var searchQuery by remember { mutableStateOf("") }
    var selectedSubjectFilter by remember { mutableStateOf<String?>(null) }
    var selectedTabIndex by remember { mutableIntStateOf(0) } // 0=Due Today, 1=Overdue, 2=Upcoming, 3=Completed

    var revisionToComplete by remember { mutableStateOf<RevisionRecordEntity?>(null) }
    var revisionToReschedule by remember { mutableStateOf<RevisionRecordEntity?>(null) }

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

    // Grouping
    val overdue = allRevisions.filter {
        it.scheduledAt < todayStartMillis && it.status != RevisionStatus.REVISED
    }
    val dueToday = allRevisions.filter {
        it.scheduledAt in todayStartMillis..todayEndMillis && it.status != RevisionStatus.REVISED
    }
    val upcoming = allRevisions.filter {
        it.scheduledAt > todayEndMillis && it.status != RevisionStatus.REVISED
    }
    val completed = allRevisions.filter {
        it.status == RevisionStatus.REVISED
    }

    val tabTitles = listOf(
        "Due Today (${dueToday.size})",
        "Overdue (${overdue.size})",
        "Upcoming (${upcoming.size})",
        "Completed (${completed.size})"
    )

    val currentRawList = when (selectedTabIndex) {
        0 -> dueToday
        1 -> overdue
        2 -> upcoming
        else -> completed
    }

    val chapterMap = chapters.associateBy { it.id }
    val subjectMap = subjects.associateBy { it.id }

    val filteredList = currentRawList.filter { rev ->
        val ch = chapterMap[rev.chapterId]
        val matchesSearch = searchQuery.isBlank() ||
                (ch?.title?.contains(searchQuery, ignoreCase = true) == true) ||
                rev.notes.contains(searchQuery, ignoreCase = true)
        val matchesSubject = selectedSubjectFilter == null || rev.subjectId == selectedSubjectFilter
        matchesSearch && matchesSubject
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        // Title
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Automatic Revision Scheduler",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Systematic spaced repetition intervals ensure long-term retention",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search revisions by chapter or notes...", fontSize = 13.sp) },
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

        // Subject Filter Chips
        if (subjects.isNotEmpty()) {
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

        // Tabs
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
                            text = title,
                            fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 13.sp
                        )
                    }
                )
            }
        }

        val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

        if (filteredList.isEmpty()) {
            EmptyStateCard(
                message = when (selectedTabIndex) {
                    0 -> strings.noDueRevisions
                    1 -> "No overdue revisions. Great job staying disciplined!"
                    2 -> "No upcoming revisions scheduled yet."
                    else -> "No revisions completed yet."
                }
            )
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredList, key = { it.id }) { rev ->
                    val chapter = chapterMap[rev.chapterId]
                    val subject = subjectMap[rev.subjectId]

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp)),
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = chapter?.title ?: "Chapter Revision",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "${subject?.title ?: "Subject"} • Round #${rev.revisionCount}",
                                        fontSize = 12.sp,
                                        color = FokusElectricLime,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "Due: ${dateFormat.format(Date(rev.scheduledAt))}",
                                            fontSize = 11.sp,
                                            color = when {
                                                rev.status == RevisionStatus.REVISED -> FokusElectricLime
                                                rev.scheduledAt < todayStartMillis -> FokusCoral
                                                else -> FokusAmber
                                            },
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        if (rev.outcomeRating.isNotBlank() && rev.status == RevisionStatus.REVISED) {
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "• Outcome: ${rev.outcomeRating}",
                                                fontSize = 11.sp,
                                                color = Color(0xFFA0AB96)
                                            )
                                        }
                                    }
                                }

                                if (rev.status != RevisionStatus.REVISED) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(onClick = { onStartFocus(rev.chapterId) }) {
                                            Icon(
                                                imageVector = Icons.Default.PlayArrow,
                                                contentDescription = "Start Focus",
                                                tint = FokusElectricLime
                                            )
                                        }
                                        IconButton(onClick = { revisionToReschedule = rev }) {
                                            Icon(
                                                imageVector = Icons.Default.EditCalendar,
                                                contentDescription = "Reschedule",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                        Button(
                                            onClick = { revisionToComplete = rev },
                                            shape = RoundedCornerShape(10.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = FokusElectricLime,
                                                contentColor = FokusOnLime
                                            )
                                        ) {
                                            Text("Review", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                } else {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Completed",
                                            tint = FokusElectricLime,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "${rev.confidenceRating}/5 ★",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = FokusElectricLime
                                        )
                                    }
                                }
                            }

                            if (rev.notes.isNotBlank()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Notes: ${rev.notes}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Complete Revision Dialog with Easy/Okay/Difficult Outcome Feedback
    if (revisionToComplete != null) {
        val rev = revisionToComplete!!
        var selectedOutcome by remember { mutableStateOf("OKAY") } // EASY, OKAY, DIFFICULT
        var confidenceRating by remember { mutableIntStateOf(3) }
        var revisionNotes by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { revisionToComplete = null },
            title = { Text("Complete Revision Review", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "How was your recall and understanding of this chapter?",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Outcome Buttons (Easy, Okay, Difficult)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            Triple("EASY", strings.outcomeEasy, FokusElectricLime),
                            Triple("OKAY", strings.outcomeOkay, FokusBlue),
                            Triple("DIFFICULT", strings.outcomeDifficult, FokusCoral)
                        ).forEach { (code, label, color) ->
                            FilterChip(
                                selected = selectedOutcome == code,
                                onClick = {
                                    selectedOutcome = code
                                    confidenceRating = when (code) {
                                        "EASY" -> 5
                                        "DIFFICULT" -> 2
                                        else -> 3
                                    }
                                },
                                label = { Text(label, fontWeight = FontWeight.Bold) },
                                modifier = Modifier.weight(1f),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = color.copy(alpha = 0.25f),
                                    selectedLabelColor = color
                                )
                            )
                        }
                    }

                    Text(
                        text = when (selectedOutcome) {
                            "DIFFICULT" -> "ℹ️ Difficult material will be scheduled sooner (tomorrow) to reinforce weak spots."
                            "EASY" -> "ℹ️ Solid recall! Next review will advance further along the spaced repetition curve."
                            else -> "ℹ️ Standard spaced interval will be applied for the next repetition round."
                        },
                        fontSize = 11.sp,
                        color = Color(0xFFA0AB96)
                    )

                    Text("Confidence Rating: $confidenceRating / 5", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        for (i in 1..5) {
                            IconButton(onClick = { confidenceRating = i }) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = "Rating $i",
                                    tint = if (i <= confidenceRating) FokusAmber else Color.Gray
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = revisionNotes,
                        onValueChange = { revisionNotes = it },
                        label = { Text("Notes / Formulas to remember") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            repository.completeRevisionWithOutcome(
                                record = rev,
                                outcome = selectedOutcome,
                                confidenceRating = confidenceRating,
                                notes = revisionNotes.trim(),
                                customIntervals = userPrefs.revisionIntervals
                            )
                            revisionToComplete = null
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
                TextButton(onClick = { revisionToComplete = null }) {
                    Text(strings.cancel)
                }
            }
        )
    }

    // Reschedule Revision Dialog
    if (revisionToReschedule != null) {
        val rev = revisionToReschedule!!
        var daysAhead by remember { mutableIntStateOf(3) }

        AlertDialog(
            onDismissRequest = { revisionToReschedule = null },
            title = { Text("Reschedule Revision", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Select when you want to review this item next:", fontSize = 13.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(1, 3, 7, 14).forEach { d ->
                            FilterChip(
                                selected = daysAhead == d,
                                onClick = { daysAhead = d },
                                label = { Text("+$d days") },
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
                            val newMillis = System.currentTimeMillis() + (daysAhead * 86400000L)
                            repository.rescheduleRevision(rev, newMillis)
                            revisionToReschedule = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = FokusElectricLime,
                        contentColor = FokusOnLime
                    )
                ) {
                    Text("Reschedule")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        // Option to skip
                        scope.launch {
                            repository.skipRevision(rev)
                            revisionToReschedule = null
                        }
                    }
                ) {
                    Text("Skip Round", color = Color(0xFFFF5C5C))
                }
            }
        )
    }
}
