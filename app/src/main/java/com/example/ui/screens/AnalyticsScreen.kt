package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.FokusRepository
import com.example.data.repository.SyllabusProgressSummary
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.FokusMetricCard
import com.example.ui.components.FokusProgressBar
import com.example.ui.components.FokusSectionHeader
import com.example.ui.localization.LocalFokusStrings
import com.example.ui.theme.FokusAmber
import com.example.ui.theme.FokusBlue
import com.example.ui.theme.FokusCoral
import com.example.ui.theme.FokusElectricLime
import com.example.ui.theme.FokusOnLime
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun AnalyticsScreen(
    repository: FokusRepository
) {
    val strings = LocalFokusStrings.current

    val allSessions by repository.allSessions.collectAsState(initial = emptyList())
    val progressSummary by repository.syllabusProgressSummary.collectAsState(
        initial = SyllabusProgressSummary(0, 0, 0, 0, 0, 0f, false)
    )
    val subjectsWithProgress by repository.subjectsWithProgress.collectAsState(initial = emptyList())
    val totalInterruptions by repository.totalInterruptionsCount.collectAsState(initial = 0)

    var selectedFilterIndex by remember { mutableIntStateOf(1) } // 0 = Today, 1 = This Week, 2 = This Month
    val filterOptions = listOf(strings.filterDay, strings.filterWeek, strings.filterMonth)

    val calendar = Calendar.getInstance()
    val now = calendar.timeInMillis
    val sinceTimestamp = when (selectedFilterIndex) {
        0 -> {
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.timeInMillis
        }
        1 -> {
            calendar.add(Calendar.DAY_OF_YEAR, -7)
            calendar.timeInMillis
        }
        else -> {
            calendar.add(Calendar.DAY_OF_YEAR, -30)
            calendar.timeInMillis
        }
    }

    val filteredSessions = allSessions.filter { it.startedAt >= sinceTimestamp }
    val totalSeconds = filteredSessions.sumOf { it.actualDurationSeconds }
    val totalMinutes = totalSeconds / 60
    val totalHours = totalMinutes / 60
    val remainingMins = totalMinutes % 60
    val durationFormatted = if (totalHours > 0) "${totalHours}h ${remainingMins}m" else "${remainingMins}m"

    val sessionCount = filteredSessions.size
    val avgSessionDuration = if (sessionCount > 0) totalMinutes / sessionCount else 0

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = strings.analyticsTitle,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Measured study effort and discipline trends",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Time Filter Chips
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                filterOptions.forEachIndexed { index, label ->
                    FilterChip(
                        selected = selectedFilterIndex == index,
                        onClick = { selectedFilterIndex = index },
                        label = { Text(label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = FokusElectricLime,
                            selectedLabelColor = FokusOnLime
                        )
                    )
                }
            }
        }

        // Metrics Grid
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FokusMetricCard(
                    title = strings.totalStudyTime,
                    value = durationFormatted,
                    modifier = Modifier.weight(1f),
                    accentColor = FokusElectricLime,
                    icon = Icons.Default.Timer
                )
                FokusMetricCard(
                    title = strings.completedSessions,
                    value = "$sessionCount",
                    modifier = Modifier.weight(1f),
                    accentColor = FokusBlue,
                    icon = Icons.Default.CheckCircle
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FokusMetricCard(
                    title = strings.avgSessionDuration,
                    value = "${avgSessionDuration}m",
                    modifier = Modifier.weight(1f),
                    accentColor = FokusAmber,
                    icon = Icons.Default.Speed
                )
                FokusMetricCard(
                    title = strings.interruptionsBlocked,
                    value = "$totalInterruptions",
                    modifier = Modifier.weight(1f),
                    accentColor = FokusCoral,
                    icon = Icons.Default.Block
                )
            }
        }

        // Daily Study Time Trend Chart (Canvas Bar Chart)
        item {
            FokusSectionHeader(title = "Study Duration Trend")
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                // Group last 7 days of sessions
                val dayLabels = mutableListOf<String>()
                val dayValues = mutableListOf<Float>()
                val cal = Calendar.getInstance()
                val dayFormat = SimpleDateFormat("EEE", Locale.getDefault())

                for (i in 6 downTo 0) {
                    val dCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -i) }
                    dayLabels.add(dayFormat.format(dCal.time))

                    dCal.set(Calendar.HOUR_OF_DAY, 0)
                    dCal.set(Calendar.MINUTE, 0)
                    val dStart = dCal.timeInMillis
                    dCal.set(Calendar.HOUR_OF_DAY, 23)
                    dCal.set(Calendar.MINUTE, 59)
                    val dEnd = dCal.timeInMillis

                    val dSeconds = allSessions.filter { it.startedAt in dStart..dEnd }.sumOf { it.actualDurationSeconds }
                    dayValues.add(dSeconds / 3600f) // Hours
                }

                val maxHours = (dayValues.maxOrNull() ?: 1f).coerceAtLeast(4f)

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        val barWidth = size.width / (dayValues.size * 2f)
                        val stepX = size.width / dayValues.size

                        dayValues.forEachIndexed { index, hours ->
                            val barHeight = (hours / maxHours) * size.height
                            val x = (index * stepX) + (stepX - barWidth) / 2
                            val y = size.height - barHeight

                            // Background slot
                            drawRoundRect(
                                color = Color(0xFF22281E),
                                topLeft = Offset(x, 0f),
                                size = Size(barWidth, size.height),
                                cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                            )

                            // Active bar
                            if (barHeight > 0) {
                                drawRoundRect(
                                    color = FokusElectricLime,
                                    topLeft = Offset(x, y),
                                    size = Size(barWidth, barHeight),
                                    cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        dayLabels.forEach { label ->
                            Text(
                                text = label,
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Subject Breakdown
        item {
            FokusSectionHeader(title = strings.subjectDistribution)
        }

        if (subjectsWithProgress.isEmpty()) {
            item {
                EmptyStateCard(message = strings.noSubjectsFound)
            }
        } else {
            items(subjectsWithProgress.size) { index ->
                val subProg = subjectsWithProgress[index]
                val subColor = runCatching { Color(android.graphics.Color.parseColor(subProg.subject.colorHex)) }
                    .getOrDefault(FokusElectricLime)

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
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
                                        .background(subColor)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = subProg.subject.title,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                            Text(
                                text = "${subProg.completionPercentage.toInt()}%",
                                fontWeight = FontWeight.Bold,
                                color = FokusElectricLime,
                                fontSize = 13.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        FokusProgressBar(
                            progress = subProg.completionPercentage / 100f,
                            color = subColor
                        )
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}
