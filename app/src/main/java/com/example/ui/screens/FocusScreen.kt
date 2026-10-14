package com.example.ui.screens

import android.content.Context
import android.os.SystemClock
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Stop
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.blocker.BlockerManager
import com.example.blocker.FokusForegroundService
import com.example.data.model.SessionStatus
import com.example.data.model.StudySessionEntity
import com.example.data.repository.FokusRepository
import com.example.ui.components.FokusProgressRing
import com.example.ui.localization.LocalFokusStrings
import com.example.ui.theme.FokusAmber
import com.example.ui.theme.FokusBlue
import com.example.ui.theme.FokusElectricLime
import com.example.ui.theme.FokusOnLime
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FocusScreen(
    repository: FokusRepository,
    initialChapterId: String? = null
) {
    val context = LocalContext.current
    val strings = LocalFokusStrings.current
    val scope = rememberCoroutineScope()

    val subjects by repository.allActiveSubjects.collectAsState(initial = emptyList())
    val chapters by repository.allActiveChapters.collectAsState(initial = emptyList())
    val blockerConfig by repository.blockerConfigFlow.collectAsState(initial = null)

    var targetDurationMinutes by remember { mutableIntStateOf(25) }
    var selectedSubjectId by remember { mutableStateOf<String?>(null) }
    var selectedChapterId by remember { mutableStateOf(initialChapterId) }
    var strictBlockerEnabled by remember { mutableStateOf(true) }

    // If initial chapter was passed, sync subject
    LaunchedEffect(initialChapterId, chapters) {
        if (initialChapterId != null && selectedSubjectId == null) {
            val ch = chapters.firstOrNull { it.id == initialChapterId }
            if (ch != null) {
                selectedSubjectId = ch.subjectId
                selectedChapterId = ch.id
            }
        }
    }

    // Timer States
    var isRunning by remember { mutableStateOf(false) }
    var isPaused by remember { mutableStateOf(false) }
    var remainingSeconds by remember { mutableLongStateOf(25 * 60L) }
    var sessionStartUptime by remember { mutableLongStateOf(0L) }
    var sessionEndWallTime by remember { mutableLongStateOf(0L) }
    var elapsedSeconds by remember { mutableLongStateOf(0L) }
    var currentSessionId by remember { mutableStateOf(UUID.randomUUID().toString()) }

    var showSummaryDialog by remember { mutableStateOf(false) }
    var sessionInterruptionCount by remember { mutableIntStateOf(0) }

    // Monotonic Clock Ticker
    LaunchedEffect(isRunning, isPaused) {
        if (isRunning && !isPaused) {
            while (isRunning && !isPaused && remainingSeconds > 0) {
                delay(1000L)
                remainingSeconds--
                elapsedSeconds++
            }
            if (remainingSeconds <= 0L && isRunning) {
                isRunning = false
                showSummaryDialog = true
                FokusForegroundService.stopService(context)
                BlockerManager.stopFocusSession()
            }
        }
    }

    fun startTimer() {
        val totalSecs = targetDurationMinutes * 60L
        remainingSeconds = totalSecs
        elapsedSeconds = 0L
        currentSessionId = UUID.randomUUID().toString()
        sessionStartUptime = SystemClock.elapsedRealtime()
        sessionEndWallTime = System.currentTimeMillis() + (totalSecs * 1000L)

        isRunning = true
        isPaused = false

        // Start Foreground Service
        val chapterTitle = chapters.firstOrNull { it.id == selectedChapterId }?.title ?: "Deep Study"
        FokusForegroundService.startService(context, "FOKUS: $chapterTitle")

        // Arm Blocker if strict
        if (strictBlockerEnabled) {
            val blocked = blockerConfig?.selectedPackagesJson?.let {
                BlockerManager.parsePackagesJson(it)
            } ?: emptySet()
            BlockerManager.startFocusSession(currentSessionId, blocked)
        }
    }

    fun pauseTimer() {
        isPaused = true
    }

    fun resumeTimer() {
        isPaused = false
    }

    fun stopTimerEarly() {
        isRunning = false
        isPaused = false
        FokusForegroundService.stopService(context)
        BlockerManager.stopFocusSession()
        showSummaryDialog = true
    }

    val totalSecs = (targetDurationMinutes * 60L).coerceAtLeast(1L)
    val progressRatio = (totalSecs - remainingSeconds).toFloat() / totalSecs.toFloat()

    val displayMinutes = remainingSeconds / 60
    val displaySeconds = remainingSeconds % 60
    val timerString = String.format("%02d:%02d", displayMinutes, displaySeconds)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top Header
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = strings.focusScreenTitle.uppercase(),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = FokusElectricLime,
                letterSpacing = 2.sp
            )
            Spacer(modifier = Modifier.height(6.dp))

            val selectedChapterTitle = chapters.firstOrNull { it.id == selectedChapterId }?.title
            Text(
                text = selectedChapterTitle ?: "Intensive Study Sprint",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )
        }

        // Circular Countdown Timer
        Box(
            modifier = Modifier.size(260.dp),
            contentAlignment = Alignment.Center
        ) {
            FokusProgressRing(
                progress = progressRatio,
                modifier = Modifier.fillMaxSize(),
                strokeWidth = 14.dp,
                color = if (strictBlockerEnabled) FokusElectricLime else FokusBlue
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = timerString,
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isPaused) "PAUSED" else if (isRunning) "FOCUSING" else "READY",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp,
                        color = if (isPaused) FokusAmber else if (isRunning) FokusElectricLime else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Configuration Section (Only visible when stopped)
        AnimatedVisibility(visible = !isRunning) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Presets Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        Pair(25, strings.preset25m),
                        Pair(50, strings.preset50m),
                        Pair(90, strings.preset90m)
                    ).forEach { (mins, label) ->
                        FilterChip(
                            selected = targetDurationMinutes == mins,
                            onClick = {
                                targetDurationMinutes = mins
                                remainingSeconds = mins * 60L
                            },
                            label = { Text(label, fontSize = 12.sp) },
                            modifier = Modifier.weight(1f),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = FokusElectricLime,
                                selectedLabelColor = FokusOnLime
                            )
                        )
                    }
                }

                // Strict Mode Blocker Switch
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = if (strictBlockerEnabled) FokusElectricLime else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = strings.strictMode,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = "Intercepts distracting apps",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Switch(
                            checked = strictBlockerEnabled,
                            onCheckedChange = { strictBlockerEnabled = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = FokusOnLime,
                                checkedTrackColor = FokusElectricLime
                            )
                        )
                    }
                }
            }
        }

        // Action Controls
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (!isRunning) {
                Button(
                    onClick = { startTimer() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = FokusElectricLime,
                        contentColor = FokusOnLime
                    )
                ) {
                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = strings.startFocus, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            } else {
                if (isPaused) {
                    Button(
                        onClick = { resumeTimer() },
                        modifier = Modifier
                            .weight(1f)
                            .height(54.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = FokusElectricLime,
                            contentColor = FokusOnLime
                        )
                    ) {
                        Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(strings.resume, fontWeight = FontWeight.Bold)
                    }
                } else {
                    OutlinedButton(
                        onClick = { pauseTimer() },
                        modifier = Modifier
                            .weight(1f)
                            .height(54.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Pause, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(strings.pause)
                    }
                }

                Button(
                    onClick = { stopTimerEarly() },
                    modifier = Modifier
                        .weight(1f)
                        .height(54.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5C5C))
                ) {
                    Icon(imageVector = Icons.Default.Stop, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(strings.endSession, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    // Session Summary Dialog (Explicit student confirmation)
    if (showSummaryDialog) {
        var focusQuality by remember { mutableIntStateOf(5) }
        var sessionNotes by remember { mutableStateOf("") }

        val measuredMinutes = (elapsedSeconds / 60).coerceAtLeast(1)

        AlertDialog(
            onDismissRequest = { showSummaryDialog = false },
            title = { Text(strings.sessionComplete, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Measured Study Duration: $measuredMinutes minutes",
                        fontWeight = FontWeight.SemiBold,
                        color = FokusElectricLime,
                        fontSize = 15.sp
                    )

                    Text(
                        text = "Remember: Completing a focus session tracks real study time, but does not automatically mark chapters Completed. You retain complete manual control of your syllabus.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(strings.focusQualityPrompt, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        for (star in 1..5) {
                            IconButton(onClick = { focusQuality = star }) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = "Rating $star",
                                    tint = if (star <= focusQuality) FokusAmber else Color.Gray
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = sessionNotes,
                        onValueChange = { sessionNotes = it },
                        label = { Text("Session Notes / Reflections") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            repository.recordStudySession(
                                StudySessionEntity(
                                    id = currentSessionId,
                                    subjectId = selectedSubjectId,
                                    chapterId = selectedChapterId,
                                    plannedDurationMinutes = targetDurationMinutes,
                                    actualDurationSeconds = elapsedSeconds,
                                    startedAt = System.currentTimeMillis() - (elapsedSeconds * 1000L),
                                    endedAt = System.currentTimeMillis(),
                                    status = SessionStatus.COMPLETED,
                                    focusQualityRating = focusQuality,
                                    notes = sessionNotes.trim(),
                                    interruptionCount = sessionInterruptionCount,
                                    blockerEnforced = strictBlockerEnabled
                                )
                            )
                            showSummaryDialog = false
                            // Reset timer
                            remainingSeconds = targetDurationMinutes * 60L
                            elapsedSeconds = 0L
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = FokusElectricLime,
                        contentColor = FokusOnLime
                    )
                ) {
                    Text(strings.saveSession)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showSummaryDialog = false
                        remainingSeconds = targetDurationMinutes * 60L
                        elapsedSeconds = 0L
                    }
                ) {
                    Text(strings.discardSession)
                }
            }
        )
    }
}
