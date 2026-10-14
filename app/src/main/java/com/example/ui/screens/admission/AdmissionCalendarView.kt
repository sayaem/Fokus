package com.example.ui.screens.admission

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material.icons.filled.ViewModule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.admission.AdmissionCountdownEngine
import com.example.data.model.AdmissionEventEntity
import com.example.data.model.AdmissionEventType
import com.example.data.model.UniversityEntity
import com.example.ui.localization.AppLanguage
import com.example.ui.localization.LocalFokusStrings
import com.example.ui.theme.FokusAmber
import com.example.ui.theme.FokusBlue
import com.example.ui.theme.FokusCoral
import com.example.ui.theme.FokusElectricLime
import com.example.ui.theme.FokusOnLime
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun AdmissionCalendarView(
    events: List<AdmissionEventEntity>,
    universities: List<UniversityEntity>,
    language: AppLanguage,
    onAddEventForDate: (Long) -> Unit,
    onEventClick: (AdmissionEventEntity) -> Unit
) {
    val strings = LocalFokusStrings.current

    val currentCal = remember { Calendar.getInstance(AdmissionCountdownEngine.DHAKA_TIMEZONE) }
    var displayedYear by remember { mutableIntStateOf(currentCal.get(Calendar.YEAR)) }
    var displayedMonth by remember { mutableIntStateOf(currentCal.get(Calendar.MONTH)) } // 0-indexed
    var selectedDayMillis by remember { mutableStateOf<Long?>(System.currentTimeMillis()) }
    var isAgendaMode by remember { mutableStateOf(false) }

    // Filters
    var selectedUniFilter by remember { mutableStateOf<String?>(null) }
    var selectedTypeFilter by remember { mutableStateOf<AdmissionEventType?>(null) }

    val filteredEvents = remember(events, selectedUniFilter, selectedTypeFilter) {
        events.filter { ev ->
            val matchesUni = selectedUniFilter == null || ev.universityId == selectedUniFilter
            val matchesType = selectedTypeFilter == null || ev.eventType == selectedTypeFilter
            matchesUni && matchesType
        }
    }

    // Days in current displayed month
    val monthCal = remember(displayedYear, displayedMonth) {
        Calendar.getInstance(AdmissionCountdownEngine.DHAKA_TIMEZONE).apply {
            set(Calendar.YEAR, displayedYear)
            set(Calendar.MONTH, displayedMonth)
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
    }

    val daysInMonth = monthCal.getActualMaximum(Calendar.DAY_OF_MONTH)
    val firstDayOfWeek = monthCal.get(Calendar.DAY_OF_WEEK) // 1 = Sunday

    // Group events by day in month
    val eventsByDay = remember(filteredEvents, displayedYear, displayedMonth) {
        val map = mutableMapOf<Int, MutableList<AdmissionEventEntity>>()
        filteredEvents.forEach { ev ->
            val eventMillis = ev.deadlineDateTimeMillis ?: ev.startDateTimeMillis
            if (eventMillis != null && ev.isDateConfirmed) {
                val evCal = Calendar.getInstance(AdmissionCountdownEngine.DHAKA_TIMEZONE).apply { timeInMillis = eventMillis }
                if (evCal.get(Calendar.YEAR) == displayedYear && evCal.get(Calendar.MONTH) == displayedMonth) {
                    val day = evCal.get(Calendar.DAY_OF_MONTH)
                    map.getOrPut(day) { mutableListOf() }.add(ev)
                }
            }
        }
        map
    }

    val monthName = remember(displayedYear, displayedMonth, language) {
        val sdf = SimpleDateFormat("MMMM yyyy", Locale.ENGLISH).apply {
            timeZone = AdmissionCountdownEngine.DHAKA_TIMEZONE
        }
        val cal = Calendar.getInstance(AdmissionCountdownEngine.DHAKA_TIMEZONE).apply {
            set(Calendar.YEAR, displayedYear)
            set(Calendar.MONTH, displayedMonth)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        val str = sdf.format(cal.time)
        if (language == AppLanguage.BANGLA) {
            val bengaliMonths = listOf(
                "January" to "জানুয়ারি", "February" to "ফেব্রুয়ারি", "March" to "মার্চ",
                "April" to "এপ্রিল", "May" to "মে", "June" to "জুন",
                "July" to "জুলাই", "August" to "আগস্ট", "September" to "সেপ্টেম্বর",
                "October" to "অক্টোবর", "November" to "নভেম্বর", "December" to "ডিসেম্বর"
            )
            var res = str
            bengaliMonths.forEach { (eng, bng) -> res = res.replace(eng, bng) }
            AdmissionCountdownEngine.formatBanglaDigits(res)
        } else {
            str
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Month Navigation and View Mode Switcher
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = {
                    if (displayedMonth == 0) {
                        displayedMonth = 11
                        displayedYear--
                    } else {
                        displayedMonth--
                    }
                }) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Prev Month")
                }

                Text(
                    text = monthName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )

                IconButton(onClick = {
                    if (displayedMonth == 11) {
                        displayedMonth = 0
                        displayedYear++
                    } else {
                        displayedMonth++
                    }
                }) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next Month")
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = {
                    val now = Calendar.getInstance(AdmissionCountdownEngine.DHAKA_TIMEZONE)
                    displayedYear = now.get(Calendar.YEAR)
                    displayedMonth = now.get(Calendar.MONTH)
                    selectedDayMillis = System.currentTimeMillis()
                }) {
                    Text(if (language == AppLanguage.BANGLA) "আজ" else "Today", fontSize = 12.sp, color = FokusElectricLime)
                }

                IconButton(onClick = { isAgendaMode = !isAgendaMode }) {
                    Icon(
                        imageVector = if (isAgendaMode) Icons.Default.ViewModule else Icons.Default.ViewAgenda,
                        contentDescription = "Toggle View",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Filter chips row
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            item {
                FilterChip(
                    selected = selectedTypeFilter == null,
                    onClick = { selectedTypeFilter = null },
                    label = { Text(strings.filterAll, fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = FokusElectricLime,
                        selectedLabelColor = FokusOnLime
                    )
                )
            }
            item {
                FilterChip(
                    selected = selectedTypeFilter == AdmissionEventType.APPLICATION_DEADLINE,
                    onClick = {
                        selectedTypeFilter = if (selectedTypeFilter == AdmissionEventType.APPLICATION_DEADLINE) null else AdmissionEventType.APPLICATION_DEADLINE
                    },
                    label = { Text(strings.filterDeadlines, fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = FokusCoral,
                        selectedLabelColor = Color.White
                    )
                )
            }
            item {
                FilterChip(
                    selected = selectedTypeFilter == AdmissionEventType.ADMISSION_TEST,
                    onClick = {
                        selectedTypeFilter = if (selectedTypeFilter == AdmissionEventType.ADMISSION_TEST) null else AdmissionEventType.ADMISSION_TEST
                    },
                    label = { Text(strings.filterExams, fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = FokusElectricLime,
                        selectedLabelColor = FokusOnLime
                    )
                )
            }
            item {
                FilterChip(
                    selected = selectedTypeFilter == AdmissionEventType.ADMIT_CARD_OPENS,
                    onClick = {
                        selectedTypeFilter = if (selectedTypeFilter == AdmissionEventType.ADMIT_CARD_OPENS) null else AdmissionEventType.ADMIT_CARD_OPENS
                    },
                    label = { Text(strings.filterAdmitCard, fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = FokusBlue,
                        selectedLabelColor = Color.White
                    )
                )
            }
            item {
                FilterChip(
                    selected = selectedTypeFilter == AdmissionEventType.PAYMENT_DEADLINE,
                    onClick = {
                        selectedTypeFilter = if (selectedTypeFilter == AdmissionEventType.PAYMENT_DEADLINE) null else AdmissionEventType.PAYMENT_DEADLINE
                    },
                    label = { Text(strings.filterPayments, fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = FokusAmber,
                        selectedLabelColor = Color.Black
                    )
                )
            }
        }

        if (!isAgendaMode) {
            // Month View Grid
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    // Day of Week Header
                    val daysHeader = if (language == AppLanguage.BANGLA) {
                        listOf("রবি", "সোম", "মঙ্গল", "বুধ", "বৃহ", "শুক্র", "শনি")
                    } else {
                        listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
                    }
                    Row(modifier = Modifier.fillMaxWidth()) {
                        daysHeader.forEach { d ->
                            Text(
                                text = d,
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.Center,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    val todayCal = Calendar.getInstance(AdmissionCountdownEngine.DHAKA_TIMEZONE)
                    val isCurrentMonthToday = todayCal.get(Calendar.YEAR) == displayedYear && todayCal.get(Calendar.MONTH) == displayedMonth
                    val todayDay = if (isCurrentMonthToday) todayCal.get(Calendar.DAY_OF_MONTH) else -1

                    val totalCells = ((firstDayOfWeek - 1) + daysInMonth + 6) / 7 * 7

                    for (row in 0 until (totalCells / 7)) {
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                            for (col in 0 until 7) {
                                val cellIndex = row * 7 + col
                                val dayNum = cellIndex - (firstDayOfWeek - 1) + 1

                                if (dayNum in 1..daysInMonth) {
                                    val isToday = dayNum == todayDay
                                    val dayEvents = eventsByDay[dayNum] ?: emptyList()

                                    // Build timestamp for this day
                                    val cellMillis = Calendar.getInstance(AdmissionCountdownEngine.DHAKA_TIMEZONE).apply {
                                        set(Calendar.YEAR, displayedYear)
                                        set(Calendar.MONTH, displayedMonth)
                                        set(Calendar.DAY_OF_MONTH, dayNum)
                                        set(Calendar.HOUR_OF_DAY, 12)
                                    }.timeInMillis

                                    val isSelected = selectedDayMillis != null && isSameDate(selectedDayMillis!!, cellMillis)

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(44.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(
                                                when {
                                                    isSelected -> FokusElectricLime.copy(alpha = 0.25f)
                                                    isToday -> MaterialTheme.colorScheme.surface
                                                    else -> Color.Transparent
                                                }
                                            )
                                            .border(
                                                width = if (isSelected) 1.dp else 0.dp,
                                                color = if (isSelected) FokusElectricLime else Color.Transparent,
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                            .clickable {
                                                selectedDayMillis = cellMillis
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Text(
                                                text = if (language == AppLanguage.BANGLA) AdmissionCountdownEngine.formatBanglaDigits(dayNum.toString()) else dayNum.toString(),
                                                fontSize = 12.sp,
                                                fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isToday) FokusElectricLime else MaterialTheme.colorScheme.onSurface
                                            )

                                            // Event dots
                                            if (dayEvents.isNotEmpty()) {
                                                Row(
                                                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                                                    modifier = Modifier.padding(top = 2.dp)
                                                ) {
                                                    dayEvents.take(3).forEach { ev ->
                                                        val dotColor = when (ev.eventType) {
                                                            AdmissionEventType.APPLICATION_DEADLINE -> FokusCoral
                                                            AdmissionEventType.ADMISSION_TEST -> FokusElectricLime
                                                            AdmissionEventType.ADMIT_CARD_OPENS, AdmissionEventType.ADMIT_CARD_DEADLINE -> FokusBlue
                                                            AdmissionEventType.PAYMENT_DEADLINE, AdmissionEventType.RESULT_PUBLISHED -> FokusAmber
                                                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                                                        }
                                                        Box(
                                                            modifier = Modifier
                                                                .size(4.dp)
                                                                .clip(CircleShape)
                                                                .background(dotColor)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }

            // Events for the selected date
            val selectedDateEvents = remember(selectedDayMillis, filteredEvents) {
                if (selectedDayMillis == null) emptyList()
                else {
                    filteredEvents.filter { ev ->
                        val target = ev.deadlineDateTimeMillis ?: ev.startDateTimeMillis
                        target != null && ev.isDateConfirmed && isSameDate(target, selectedDayMillis!!)
                    }
                }
            }

            val formattedSelectedDate = remember(selectedDayMillis, language) {
                if (selectedDayMillis == null) ""
                else AdmissionCountdownEngine.formatDate(selectedDayMillis!!, true, language)
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$formattedSelectedDate (${selectedDateEvents.size})",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                TextButton(onClick = {
                    onAddEventForDate(selectedDayMillis ?: System.currentTimeMillis())
                }) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp), tint = FokusElectricLime)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(strings.addAdmissionEvent, fontSize = 11.sp, color = FokusElectricLime)
                }
            }

            if (selectedDateEvents.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                ) {
                    Text(
                        text = strings.emptyCalendarDay,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp),
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    selectedDateEvents.forEach { ev ->
                        CalendarEventRow(
                            event = ev,
                            universities = universities,
                            language = language,
                            onClick = { onEventClick(ev) }
                        )
                    }
                }
            }
        } else {
            // Agenda View (Chronological List of all events)
            if (filteredEvents.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                ) {
                    Text(
                        text = strings.emptyEvents,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(20.dp),
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    filteredEvents.forEach { ev ->
                        CalendarEventRow(
                            event = ev,
                            universities = universities,
                            language = language,
                            onClick = { onEventClick(ev) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CalendarEventRow(
    event: AdmissionEventEntity,
    universities: List<UniversityEntity>,
    language: AppLanguage,
    onClick: () -> Unit
) {
    val uni = universities.firstOrNull { it.id == event.universityId }
    val countdown = AdmissionCountdownEngine.calculateCountdown(event, language = language)

    val badgeColor = when (event.eventType) {
        AdmissionEventType.APPLICATION_DEADLINE -> FokusCoral
        AdmissionEventType.ADMISSION_TEST -> FokusElectricLime
        AdmissionEventType.ADMIT_CARD_OPENS, AdmissionEventType.ADMIT_CARD_DEADLINE -> FokusBlue
        AdmissionEventType.PAYMENT_DEADLINE, AdmissionEventType.RESULT_PUBLISHED -> FokusAmber
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(badgeColor)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = event.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (uni != null) {
                    Text(
                        text = uni.name,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (event.location.isNotBlank()) {
                    Text(
                        text = "📍 ${event.location}",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (countdown.isUrgent) FokusCoral.copy(alpha = 0.2f) else FokusElectricLime.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = countdown.label,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (countdown.isUrgent) FokusCoral else FokusElectricLime,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = countdown.formattedDate,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun isSameDate(millis1: Long, millis2: Long): Boolean {
    val c1 = Calendar.getInstance(AdmissionCountdownEngine.DHAKA_TIMEZONE).apply { timeInMillis = millis1 }
    val c2 = Calendar.getInstance(AdmissionCountdownEngine.DHAKA_TIMEZONE).apply { timeInMillis = millis2 }
    return c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR) &&
            c1.get(Calendar.DAY_OF_YEAR) == c2.get(Calendar.DAY_OF_YEAR)
}
