package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.FokusRepository
import com.example.ui.localization.LocalFokusStrings
import com.example.ui.theme.FokusDarkBackground
import com.example.ui.theme.FokusElectricLime
import com.example.ui.theme.FokusOnLime
import kotlinx.coroutines.launch

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
    var selectedSubjectId by remember {
        mutableStateOf(preselectedSubjectId ?: subjects.firstOrNull()?.id ?: "")
    }

    // Default sample text illustrating multiline paste
    var rawText by remember {
        mutableStateOf(
            "1. Physical World & Measurement\n2. Vectors\n3. Dynamics\n4. Newtonian Mechanics\n5. Work, Energy and Power\n6. Gravitation\n7. Structural Properties of Matter\n8. Periodic Motion\n9. Waves\n10. Ideal Gas and Kinetic Theory"
        )
    }

    val parsedList = remember { mutableStateListOf<String>() }
    var isPreviewMode by remember { mutableStateOf(false) }
    var dropdownExpanded by remember { mutableStateOf(false) }

    fun parseRawText() {
        val lines = rawText.lines()
            .map { line ->
                line.trim()
                    .replaceFirst(Regex("^(Chapter|অধ্যায়|Ch\\.?|No\\.?)\\s*[0-9০-৯]+[:.\\-]?\\s*", RegexOption.IGNORE_CASE), "")
                    .replaceFirst(Regex("^[0-9০-৯]+[:.)\\-]\\s*"), "")
                    .trim()
            }
            .filter { it.isNotBlank() }

        parsedList.clear()
        parsedList.addAll(lines)
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Target Subject Selector
            if (subjects.isNotEmpty()) {
                val currentSubject = subjects.firstOrNull { it.id == selectedSubjectId } ?: subjects.first()
                if (selectedSubjectId.isEmpty()) {
                    selectedSubjectId = currentSubject.id
                }

                ExposedDropdownMenuBox(
                    expanded = dropdownExpanded,
                    onExpandedChange = { dropdownExpanded = it }
                ) {
                    OutlinedTextField(
                        value = currentSubject.title,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Destination Subject") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = dropdownExpanded,
                        onDismissRequest = { dropdownExpanded = false }
                    ) {
                        subjects.forEach { sub ->
                            DropdownMenuItem(
                                text = { Text(sub.title) },
                                onClick = {
                                    selectedSubjectId = sub.id
                                    dropdownExpanded = false
                                }
                            )
                        }
                    }
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Please create a subject first before importing chapters.",
                        modifier = Modifier.padding(16.dp),
                        color = Color(0xFFFF5C5C)
                    )
                }
            }

            if (!isPreviewMode) {
                // Multiline Input Mode
                Text(
                    text = "Paste your syllabus text below. Each line will be parsed into an individual chapter automatically.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = rawText,
                    onValueChange = { rawText = it },
                    placeholder = { Text("Paste chapter list here...") },
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
                    onClick = { parseRawText() },
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Preview (${parsedList.size} chapters)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )

                    TextButton(onClick = { isPreviewMode = false }) {
                        Text("Edit Raw Text")
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(parsedList) { index, item ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${index + 1}.",
                                    fontWeight = FontWeight.Bold,
                                    color = FokusElectricLime,
                                    fontSize = 13.sp,
                                    modifier = Modifier.width(32.dp)
                                )

                                Text(
                                    text = item,
                                    modifier = Modifier.weight(1f),
                                    fontSize = 14.sp
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
                        if (selectedSubjectId.isNotBlank() && parsedList.isNotEmpty()) {
                            scope.launch {
                                val count = repository.bulkAddChapters(
                                    subjectId = selectedSubjectId,
                                    rawText = parsedList.joinToString("\n")
                                )
                                snackbarHostState.showSnackbar("Successfully added $count chapters!")
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
                        text = "Add ${parsedList.size} Chapters to Subject",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }
        }
    }
}
