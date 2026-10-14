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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import com.example.data.repository.FokusRepository
import com.example.ui.components.EmptyStateCard
import com.example.ui.localization.LocalFokusStrings
import com.example.ui.theme.FokusAmber
import com.example.ui.theme.FokusElectricLime
import com.example.ui.theme.FokusOnLime
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun RevisionScreen(
    repository: FokusRepository,
    onStartFocus: (chapterId: String) -> Unit
) {
    val strings = LocalFokusStrings.current
    val scope = rememberCoroutineScope()

    val pendingRevisions by repository.pendingRevisions.collectAsState(initial = emptyList())
    val allRevisions by repository.allRevisions.collectAsState(initial = emptyList())
    val chapters by repository.allActiveChapters.collectAsState(initial = emptyList())
    val subjects by repository.allActiveSubjects.collectAsState(initial = emptyList())

    val now = System.currentTimeMillis()
    val dueToday = pendingRevisions.filter { it.scheduledAt <= now }
    val upcoming = pendingRevisions.filter { it.scheduledAt > now }
    val revised = allRevisions.filter { it.status == RevisionStatus.REVISED }

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("Due & Overdue (${dueToday.size})", "Upcoming (${upcoming.size})", "Revised (${revised.size})")

    var revisionToComplete by remember { mutableStateOf<RevisionRecordEntity?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        // Title
        Column {
            Text(
                text = "Revision Queue",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Spaced repetition intervals ensure long-term retention",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Tabs
        ScrollableTabRow(
            selectedTabIndex = selectedTabIndex,
            containerColor = MaterialTheme.colorScheme.background,
            contentColor = MaterialTheme.colorScheme.onBackground,
            edgePadding = 0.dp
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index },
                    text = {
                        Text(
                            text = title,
                            fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                )
            }
        }

        val displayList = when (selectedTabIndex) {
            0 -> dueToday
            1 -> upcoming
            else -> revised
        }

        val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

        if (displayList.isEmpty()) {
            EmptyStateCard(
                message = when (selectedTabIndex) {
                    0 -> strings.noDueRevisions
                    1 -> "No upcoming scheduled revisions."
                    else -> "No revisions completed yet."
                }
            )
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(displayList, key = { it.id }) { rev ->
                    val chapter = chapters.firstOrNull { it.id == rev.chapterId }
                    val subject = subjects.firstOrNull { it.id == rev.subjectId }

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
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
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
                                    text = "${subject?.title ?: ""} • Round #${rev.revisionCount}",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Scheduled: ${dateFormat.format(Date(rev.scheduledAt))}",
                                    fontSize = 11.sp,
                                    color = if (rev.scheduledAt <= now && rev.status != RevisionStatus.REVISED)
                                        FokusAmber
                                    else
                                        MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            if (rev.status != RevisionStatus.REVISED) {
                                Row {
                                    IconButton(onClick = { onStartFocus(rev.chapterId) }) {
                                        Icon(
                                            imageVector = Icons.Default.PlayArrow,
                                            contentDescription = "Focus",
                                            tint = FokusElectricLime
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
                                        Text("Mark Done", fontSize = 12.sp, fontWeight = FontWeight.Bold)
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
                                        text = "${rev.confidenceRating}/5 stars",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = FokusElectricLime
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Complete Revision Dialog
    if (revisionToComplete != null) {
        val rev = revisionToComplete!!
        var rating by remember { mutableIntStateOf(4) }
        var notes by remember { mutableStateOf("") }
        var nextIntervalDays by remember { mutableIntStateOf(7) }

        AlertDialog(
            onDismissRequest = { revisionToComplete = null },
            title = { Text("Complete Revision") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("How confident do you feel about this material?", fontSize = 13.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        for (i in 1..5) {
                            IconButton(onClick = { rating = i }) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = "Rating $i",
                                    tint = if (i <= rating) FokusAmber else Color.Gray
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Revision Notes / Insights") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("Schedule next spaced revision interval:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(3, 7, 14, 30).forEach { days ->
                            FilterChip(
                                selected = nextIntervalDays == days,
                                onClick = { nextIntervalDays = days },
                                label = { Text("+$days d") },
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
                            repository.completeRevision(rev, rating, notes)
                            // Automatically schedule next revision round
                            val nextScheduledDate = System.currentTimeMillis() + (nextIntervalDays * 86400000L)
                            repository.scheduleRevision(
                                subjectId = rev.subjectId,
                                chapterId = rev.chapterId,
                                topicId = rev.topicId,
                                scheduledDateMillis = nextScheduledDate,
                                notes = "Round #${rev.revisionCount + 1}"
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
}
