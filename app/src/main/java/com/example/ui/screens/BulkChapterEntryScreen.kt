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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DataObject
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ChapterEntity
import com.example.data.model.LearningStatus
import com.example.data.model.PracticeStatus
import com.example.data.model.Priority
import com.example.data.model.RevisionStatus
import com.example.data.repository.FokusRepository
import com.example.ui.localization.LocalFokusStrings
import com.example.ui.theme.FokusAmber
import com.example.ui.theme.FokusCoral
import com.example.ui.theme.FokusDarkBackground
import com.example.ui.theme.FokusElectricLime
import com.example.ui.theme.FokusOnLime
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class ParsedChapterItem(
    val id: String = UUID.randomUUID().toString(),
    var isSelected: Boolean = true,
    var chapterNumber: String = "",
    var title: String = "",
    var estimatedMinutes: Int = 120,
    var isDuplicate: Boolean = false
)

enum class ImportFormat(val label: String) {
    TEXT("List / Plain"),
    CSV("CSV / TSV"),
    JSON("JSON")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BulkChapterEntryScreen(
    repository: FokusRepository,
    preselectedSubjectId: String?,
    onBack: () -> Unit
) {
    val strings = LocalFokusStrings.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val subjects by repository.allActiveSubjects.collectAsState(initial = emptyList())
    val chapters by repository.allActiveChapters.collectAsState(initial = emptyList())

    var selectedSubjectId by remember {
        mutableStateOf(preselectedSubjectId ?: subjects.firstOrNull()?.id ?: "")
    }

    LaunchedEffect(subjects) {
        if (selectedSubjectId.isBlank() && subjects.isNotEmpty()) {
            selectedSubjectId = preselectedSubjectId ?: subjects.first().id
        }
    }

    var selectedFormat by remember { mutableStateOf(ImportFormat.TEXT) }
    var rawText by remember {
        mutableStateOf(
            "1. Physical World & Measurement\n2. Vectors\n3. Dynamics\n4. Newtonian Mechanics\n5. Work, Energy and Power\n6. Gravitation\n7. Structural Properties of Matter\n8. Periodic Motion\n9. Waves\n10. Ideal Gas and Kinetic Theory"
        )
    }

    var defaultEstimatedMinutes by remember { mutableIntStateOf(120) }
    var conflictStrategy by remember { mutableStateOf("SKIP_DUPLICATES") } // "SKIP_DUPLICATES", "OVERWRITE", "APPEND"

    val parsedList = remember { mutableStateListOf<ParsedChapterItem>() }
    var isPreviewMode by remember { mutableStateOf(false) }
    var dropdownExpanded by remember { mutableStateOf(false) }

    // Existing chapter titles for duplicate checking
    val existingChapterTitles = remember(chapters, selectedSubjectId) {
        chapters.filter { it.subjectId == selectedSubjectId }
            .map { it.title.trim().lowercase() }
            .toSet()
    }

    fun parseInputText() {
        parsedList.clear()
        val text = rawText.trim()
        if (text.isBlank()) return

        when (selectedFormat) {
            ImportFormat.JSON -> {
                try {
                    // Try parsing as array of objects or strings
                    if (text.startsWith("[")) {
                        val arr = JSONArray(text)
                        for (i in 0 until arr.length()) {
                            val elem = arr.get(i)
                            if (elem is JSONObject) {
                                val num = elem.optString("number", "${i + 1}")
                                val title = elem.optString("title", elem.optString("name", "Chapter ${i + 1}"))
                                val duration = elem.optInt("estimatedMinutes", elem.optInt("duration", defaultEstimatedMinutes))
                                val isDup = existingChapterTitles.contains(title.trim().lowercase())
                                parsedList.add(
                                    ParsedChapterItem(
                                        chapterNumber = num,
                                        title = title,
                                        estimatedMinutes = duration,
                                        isDuplicate = isDup
                                    )
                                )
                            } else if (elem is String) {
                                val isDup = existingChapterTitles.contains(elem.trim().lowercase())
                                parsedList.add(
                                    ParsedChapterItem(
                                        chapterNumber = "${i + 1}",
                                        title = elem.trim(),
                                        estimatedMinutes = defaultEstimatedMinutes,
                                        isDuplicate = isDup
                                    )
                                )
                            }
                        }
                    } else if (text.startsWith("{")) {
                        val obj = JSONObject(text)
                        val arr = obj.optJSONArray("chapters") ?: obj.optJSONArray("items")
                        if (arr != null) {
                            for (i in 0 until arr.length()) {
                                val elem = arr.get(i)
                                val title = if (elem is JSONObject) elem.optString("title", "Chapter") else elem.toString()
                                val isDup = existingChapterTitles.contains(title.trim().lowercase())
                                parsedList.add(
                                    ParsedChapterItem(
                                        chapterNumber = "${i + 1}",
                                        title = title,
                                        estimatedMinutes = defaultEstimatedMinutes,
                                        isDuplicate = isDup
                                    )
                                )
                            }
                        }
                    }
                } catch (e: Exception) {
                    // Fallback to text parsing
                }
            }
            ImportFormat.CSV -> {
                text.lines().forEachIndexed { index, line ->
                    val trimmed = line.trim()
                    if (trimmed.isNotBlank()) {
                        val parts = if (trimmed.contains("\t")) trimmed.split("\t") else trimmed.split(",")
                        val num = if (parts.size >= 2 && parts[0].trim().toIntOrNull() != null) parts[0].trim() else "${index + 1}"
                        val title = if (parts.size >= 2 && parts[0].trim().toIntOrNull() != null) parts[1].trim() else parts[0].trim()
                        val duration = parts.getOrNull(2)?.trim()?.toIntOrNull() ?: defaultEstimatedMinutes
                        if (title.isNotBlank()) {
                            val isDup = existingChapterTitles.contains(title.trim().lowercase())
                            parsedList.add(
                                ParsedChapterItem(
                                    chapterNumber = num,
                                    title = title,
                                    estimatedMinutes = duration,
                                    isDuplicate = isDup
                                )
                            )
                        }
                    }
                }
            }
            ImportFormat.TEXT -> {
                val lines = text.lines()
                lines.forEachIndexed { index, rawLine ->
                    val cleanLine = rawLine.trim()
                    if (cleanLine.isNotBlank()) {
                        // Extract number if present
                        val numMatch = Regex("^(?:Chapter|অধ্যায়|Ch\\.?|No\\.?\\s*)?([0-9০-৯]+)[:.\\-\\)]?\\s*(.*)", RegexOption.IGNORE_CASE).find(cleanLine)
                        val num = numMatch?.groupValues?.getOrNull(1) ?: "${index + 1}"
                        val title = (numMatch?.groupValues?.getOrNull(2)?.takeIf { it.isNotBlank() } ?: cleanLine)
                            .replaceFirst(Regex("^(Chapter|অধ্যায়|Ch\\.?|No\\.?)\\s*[0-9০-৯]+[:.\\-]?\\s*", RegexOption.IGNORE_CASE), "")
                            .trim()

                        if (title.isNotBlank()) {
                            val isDup = existingChapterTitles.contains(title.trim().lowercase())
                            parsedList.add(
                                ParsedChapterItem(
                                    chapterNumber = num,
                                    title = title,
                                    estimatedMinutes = defaultEstimatedMinutes,
                                    isDuplicate = isDup
                                )
                            )
                        }
                    }
                }
            }
        }

        isPreviewMode = true
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(strings.bulkChapterEntry, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Target Subject Selector
            ExposedDropdownMenuBox(
                expanded = dropdownExpanded,
                onExpandedChange = { dropdownExpanded = it }
            ) {
                val currentSub = subjects.firstOrNull { it.id == selectedSubjectId }
                OutlinedTextField(
                    value = currentSub?.title ?: "Select Subject",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Target Subject", fontSize = 12.sp) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = FokusElectricLime,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    )
                )

                ExposedDropdownMenu(
                    expanded = dropdownExpanded,
                    onDismissRequest = { dropdownExpanded = false }
                ) {
                    subjects.forEach { subject ->
                        DropdownMenuItem(
                            text = { Text(subject.title) },
                            onClick = {
                                selectedSubjectId = subject.id
                                dropdownExpanded = false
                            }
                        )
                    }
                }
            }

            if (!isPreviewMode) {
                // Format Selector Tabs
                TabRow(
                    selectedTabIndex = selectedFormat.ordinal,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.clip(RoundedCornerShape(12.dp))
                ) {
                    ImportFormat.values().forEach { format ->
                        Tab(
                            selected = selectedFormat == format,
                            onClick = {
                                selectedFormat = format
                                when (format) {
                                    ImportFormat.TEXT -> rawText = "1. Physical World\n2. Vectors\n3. Dynamics\n4. Work, Energy & Power"
                                    ImportFormat.CSV -> rawText = "1, Vectors, 90\n2, Dynamics, 120\n3, Gravitation, 90"
                                    ImportFormat.JSON -> rawText = "[\n  {\"number\": \"1\", \"title\": \"Vectors\", \"estimatedMinutes\": 90},\n  {\"number\": \"2\", \"title\": \"Dynamics\", \"estimatedMinutes\": 120}\n]"
                                }
                            },
                            text = { Text(format.label, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                        )
                    }
                }

                // Default Duration Option
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Default Duration: $defaultEstimatedMinutes min",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(60, 90, 120, 180).forEach { mins ->
                            FilterChip(
                                selected = defaultEstimatedMinutes == mins,
                                onClick = { defaultEstimatedMinutes = mins },
                                label = { Text("${mins}m", fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = FokusElectricLime,
                                    selectedLabelColor = FokusOnLime
                                )
                            )
                        }
                    }
                }

                // Raw Input Text Field
                OutlinedTextField(
                    value = rawText,
                    onValueChange = { rawText = it },
                    label = { Text("Paste or type syllabus chapters") },
                    placeholder = { Text("Enter chapters one per line or in CSV/JSON format") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = FokusElectricLime,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    )
                )

                Button(
                    onClick = { parseInputText() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = FokusElectricLime,
                        contentColor = FokusOnLime
                    )
                ) {
                    Icon(imageVector = Icons.Default.PlaylistAdd, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Parse & Preview Chapters", fontWeight = FontWeight.Bold)
                }
            } else {
                // Preview & Edit Mode
                val selectedCount = parsedList.count { it.isSelected }
                val duplicateCount = parsedList.count { it.isDuplicate }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Parsed Preview ($selectedCount / ${parsedList.size} selected)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        if (duplicateCount > 0) {
                            Text(
                                text = "⚠️ $duplicateCount existing chapter duplicates detected",
                                fontSize = 11.sp,
                                color = FokusAmber
                            )
                        }
                    }

                    Row {
                        TextButton(onClick = {
                            val allSelected = parsedList.all { it.isSelected }
                            parsedList.forEach { it.isSelected = !allSelected }
                        }) {
                            Text(if (parsedList.all { it.isSelected }) "Deselect All" else "Select All", fontSize = 11.sp)
                        }
                        TextButton(onClick = { isPreviewMode = false }) {
                            Text("Edit Raw", fontSize = 11.sp)
                        }
                    }
                }

                // Conflict Handling Chips
                if (duplicateCount > 0) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("On Duplicate:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        listOf(
                            "SKIP_DUPLICATES" to "Skip",
                            "OVERWRITE" to "Overwrite",
                            "APPEND" to "Append"
                        ).forEach { (strat, label) ->
                            FilterChip(
                                selected = conflictStrategy == strat,
                                onClick = { conflictStrategy = strat },
                                label = { Text(label, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = FokusElectricLime,
                                    selectedLabelColor = FokusOnLime
                                )
                            )
                        }
                    }
                }

                // Chapter items list
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(parsedList, key = { _, item -> item.id }) { index, item ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            border = if (item.isDuplicate) androidx.compose.foundation.BorderStroke(1.dp, FokusAmber.copy(alpha = 0.5f)) else null,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = item.isSelected,
                                    onCheckedChange = { item.isSelected = it },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = FokusElectricLime,
                                        checkmarkColor = FokusOnLime
                                    )
                                )

                                OutlinedTextField(
                                    value = item.chapterNumber,
                                    onValueChange = { item.chapterNumber = it },
                                    modifier = Modifier.width(54.dp),
                                    singleLine = true,
                                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = FokusElectricLime,
                                        unfocusedBorderColor = Color.Transparent
                                    )
                                )

                                Spacer(modifier = Modifier.width(4.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    OutlinedTextField(
                                        value = item.title,
                                        onValueChange = { item.title = it },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true,
                                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = FokusElectricLime,
                                            unfocusedBorderColor = Color.Transparent
                                        )
                                    )
                                    if (item.isDuplicate) {
                                        Text(
                                            text = "⚠️ Duplicate of existing chapter in subject",
                                            fontSize = 10.sp,
                                            color = FokusAmber,
                                            modifier = Modifier.padding(start = 8.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(4.dp))

                                OutlinedTextField(
                                    value = "${item.estimatedMinutes}m",
                                    onValueChange = { str ->
                                        item.estimatedMinutes = str.filter { it.isDigit() }.toIntOrNull() ?: 120
                                    },
                                    modifier = Modifier.width(60.dp),
                                    singleLine = true,
                                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = FokusElectricLime,
                                        unfocusedBorderColor = Color.Transparent
                                    )
                                )

                                IconButton(
                                    onClick = { parsedList.removeAt(index) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Remove",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Button(
                    onClick = {
                        val toAdd = parsedList.filter { it.isSelected && it.title.isNotBlank() }
                        if (selectedSubjectId.isNotBlank() && toAdd.isNotEmpty()) {
                            scope.launch {
                                val entities = toAdd.mapIndexed { idx, p ->
                                    ChapterEntity(
                                        id = UUID.randomUUID().toString(),
                                        subjectId = selectedSubjectId,
                                        title = p.title.trim(),
                                        chapterNumber = p.chapterNumber.ifBlank { "${idx + 1}" },
                                        learningStatus = LearningStatus.NOT_STARTED,
                                        practiceStatus = PracticeStatus.NOT_STARTED,
                                        revisionStatus = RevisionStatus.NOT_SCHEDULED,
                                        priority = Priority.MEDIUM,
                                        estimatedDurationMinutes = p.estimatedMinutes,
                                        customOrder = idx
                                    )
                                }
                                val count = repository.insertBulkChapters(
                                    subjectId = selectedSubjectId,
                                    chapters = entities,
                                    conflictStrategy = conflictStrategy
                                )
                                snackbarHostState.showSnackbar("Successfully saved $count chapters!")
                                onBack()
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = FokusElectricLime,
                        contentColor = FokusOnLime
                    )
                ) {
                    Icon(imageVector = Icons.Default.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Import $selectedCount Selected Chapters",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }
        }
    }
}
