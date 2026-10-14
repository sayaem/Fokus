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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
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
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.admission.AdmissionCountdownEngine
import com.example.data.model.AdmissionApplicationEntity
import com.example.data.model.AdmissionEventEntity
import com.example.data.model.AdmissionEventType
import com.example.data.model.ApplicationStatus
import com.example.data.model.PaymentStatus
import com.example.data.model.UniversityEntity
import com.example.ui.localization.LocalFokusStrings
import com.example.ui.theme.FokusDarkCardElevated
import com.example.ui.theme.FokusElectricLime
import com.example.ui.theme.FokusOnLime
import java.util.Calendar
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditUniversityDialog(
    initialUniversity: UniversityEntity? = null,
    onDismiss: () -> Unit,
    onSave: (UniversityEntity) -> Unit
) {
    val strings = LocalFokusStrings.current

    var name by remember { mutableStateOf(initialUniversity?.name ?: "") }
    var shortName by remember { mutableStateOf(initialUniversity?.shortName ?: "") }
    var location by remember { mutableStateOf(initialUniversity?.location ?: "") }
    var websiteUrl by remember { mutableStateOf(initialUniversity?.websiteUrl ?: "") }
    var admissionPortalUrl by remember { mutableStateOf(initialUniversity?.admissionPortalUrl ?: "") }
    var notes by remember { mutableStateOf(initialUniversity?.notes ?: "") }
    var errorMessage by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (initialUniversity == null) strings.addUniversity else strings.edit,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }

                if (errorMessage.isNotBlank()) {
                    Text(text = errorMessage, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; errorMessage = "" },
                    label = { Text(strings.universityName) },
                    placeholder = { Text("e.g. University of Dhaka, BUET, Medical") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = FokusElectricLime,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    )
                )

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = shortName,
                        onValueChange = { shortName = it },
                        label = { Text(strings.universityShortName) },
                        placeholder = { Text("e.g. DU, BUET") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = FokusElectricLime,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                        )
                    )

                    OutlinedTextField(
                        value = location,
                        onValueChange = { location = it },
                        label = { Text(strings.location) },
                        placeholder = { Text("e.g. Dhaka") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = FokusElectricLime,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                        )
                    )
                }

                OutlinedTextField(
                    value = admissionPortalUrl,
                    onValueChange = { admissionPortalUrl = it },
                    label = { Text(strings.admissionPortal) },
                    placeholder = { Text("https://admission.eis.du.ac.bd") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = FokusElectricLime,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    )
                )

                OutlinedTextField(
                    value = websiteUrl,
                    onValueChange = { websiteUrl = it },
                    label = { Text(strings.officialWebsite) },
                    placeholder = { Text("https://www.du.ac.bd") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = FokusElectricLime,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    )
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text(strings.descriptionInput) },
                    placeholder = { Text("Eligibility criteria, GPA requirements, etc.") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = FokusElectricLime,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(strings.cancel)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (name.isBlank()) {
                                errorMessage = "University name is required"
                            } else {
                                onSave(
                                    initialUniversity?.copy(
                                        name = name.trim(),
                                        shortName = shortName.trim().uppercase(),
                                        location = location.trim(),
                                        websiteUrl = websiteUrl.trim(),
                                        admissionPortalUrl = admissionPortalUrl.trim(),
                                        notes = notes.trim(),
                                        updatedAt = System.currentTimeMillis()
                                    ) ?: UniversityEntity(
                                        id = UUID.randomUUID().toString(),
                                        name = name.trim(),
                                        shortName = shortName.trim().uppercase(),
                                        location = location.trim(),
                                        websiteUrl = websiteUrl.trim(),
                                        admissionPortalUrl = admissionPortalUrl.trim(),
                                        notes = notes.trim()
                                    )
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = FokusElectricLime,
                            contentColor = FokusOnLime
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(strings.save, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditApplicationDialog(
    universities: List<UniversityEntity>,
    initialApplication: AdmissionApplicationEntity? = null,
    preselectedUniversityId: String? = null,
    onDismiss: () -> Unit,
    onSave: (AdmissionApplicationEntity) -> Unit
) {
    val strings = LocalFokusStrings.current

    var selectedUniId by remember {
        mutableStateOf(initialApplication?.universityId ?: preselectedUniversityId ?: universities.firstOrNull()?.id ?: "")
    }
    var unitName by remember { mutableStateOf(initialApplication?.unitName ?: "") }
    var admissionCycle by remember { mutableStateOf(initialApplication?.admissionCycle ?: "2024-2025") }
    var appStatus by remember { mutableStateOf(initialApplication?.applicationStatus ?: ApplicationStatus.NOT_APPLIED) }
    var payStatus by remember { mutableStateOf(initialApplication?.paymentStatus ?: PaymentStatus.NOT_REQUIRED) }
    var fee by remember { mutableStateOf(initialApplication?.applicationFee ?: "") }
    var deadlineDate by remember { mutableStateOf(initialApplication?.applicationDeadline) }
    var applicationIdRef by remember { mutableStateOf(initialApplication?.applicationIdRef ?: "") }
    var transactionRef by remember { mutableStateOf(initialApplication?.transactionRef ?: "") }
    var notes by remember { mutableStateOf(initialApplication?.notes ?: "") }
    var errorMessage by remember { mutableStateOf("") }

    var showDatePicker by remember { mutableStateOf(false) }
    var uniDropdownExpanded by remember { mutableStateOf(false) }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = deadlineDate ?: System.currentTimeMillis()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    deadlineDate = datePickerState.selectedDateMillis
                    showDatePicker = false
                }) {
                    Text("OK", color = FokusElectricLime)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(strings.cancel)
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (initialApplication == null) strings.addApplication else strings.edit,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }

                if (errorMessage.isNotBlank()) {
                    Text(text = errorMessage, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                }

                // University Picker
                ExposedDropdownMenuBox(
                    expanded = uniDropdownExpanded,
                    onExpandedChange = { uniDropdownExpanded = it }
                ) {
                    val currentUni = universities.firstOrNull { it.id == selectedUniId }
                    OutlinedTextField(
                        value = currentUni?.name ?: "Select University",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(strings.universityName, fontSize = 12.sp) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = uniDropdownExpanded) },
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
                        expanded = uniDropdownExpanded,
                        onDismissRequest = { uniDropdownExpanded = false }
                    ) {
                        universities.forEach { uni ->
                            DropdownMenuItem(
                                text = { Text(uni.name) },
                                onClick = {
                                    selectedUniId = uni.id
                                    uniDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = unitName,
                    onValueChange = { unitName = it; errorMessage = "" },
                    label = { Text(strings.unitName) },
                    placeholder = { Text("e.g. A Unit (Science), IBA, Ka Unit") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = FokusElectricLime,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    )
                )

                // Application Status selector chips
                Text("Application Status:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        ApplicationStatus.NOT_APPLIED to strings.applicationsNotApplied,
                        ApplicationStatus.IN_PROGRESS to strings.applicationsInProgress,
                        ApplicationStatus.SUBMITTED to strings.applicationsSubmitted,
                        ApplicationStatus.CONFIRMED to strings.applicationsConfirmed
                    ).forEach { (status, label) ->
                        FilterChip(
                            selected = appStatus == status,
                            onClick = { appStatus = status },
                            label = { Text(label, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = FokusElectricLime,
                                selectedLabelColor = FokusOnLime
                            )
                        )
                    }
                }

                // Payment Status selector chips
                Text("Payment Status:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        PaymentStatus.NOT_REQUIRED to "Not Req.",
                        PaymentStatus.PAYMENT_PENDING to "Pending",
                        PaymentStatus.PAID to "Paid"
                    ).forEach { (status, label) ->
                        FilterChip(
                            selected = payStatus == status,
                            onClick = { payStatus = status },
                            label = { Text(label, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = FokusElectricLime,
                                selectedLabelColor = FokusOnLime
                            )
                        )
                    }
                }

                // Application Deadline Picker Button
                OutlinedButton(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.CalendarMonth, contentDescription = null, tint = FokusElectricLime)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (deadlineDate != null) {
                            "Deadline: ${AdmissionCountdownEngine.formatDate(deadlineDate!!, true, com.example.ui.localization.AppLanguage.ENGLISH)}"
                        } else {
                            "Set Application Deadline"
                        },
                        fontSize = 13.sp
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = fee,
                        onValueChange = { fee = it },
                        label = { Text(strings.applicationFee) },
                        placeholder = { Text("1000 ৳") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = FokusElectricLime,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                        )
                    )

                    OutlinedTextField(
                        value = admissionCycle,
                        onValueChange = { admissionCycle = it },
                        label = { Text(strings.admissionCycle) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = FokusElectricLime,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                        )
                    )
                }

                OutlinedTextField(
                    value = applicationIdRef,
                    onValueChange = { applicationIdRef = it },
                    label = { Text(strings.applicationIdRef) },
                    placeholder = { Text("Private reference / roll") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = FokusElectricLime,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    )
                )

                OutlinedTextField(
                    value = transactionRef,
                    onValueChange = { transactionRef = it },
                    label = { Text(strings.transactionRef) },
                    placeholder = { Text("bKash/Nagad/Bank slip ID") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = FokusElectricLime,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    )
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text(strings.descriptionInput) },
                    placeholder = { Text("Subject choices, requirements, exam instructions") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = FokusElectricLime,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(strings.cancel)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (selectedUniId.isBlank()) {
                                errorMessage = "Please select a target university"
                            } else if (unitName.isBlank()) {
                                errorMessage = "Unit / Program name is required"
                            } else {
                                onSave(
                                    initialApplication?.copy(
                                        universityId = selectedUniId,
                                        unitName = unitName.trim(),
                                        admissionCycle = admissionCycle.trim(),
                                        applicationStatus = appStatus,
                                        paymentStatus = payStatus,
                                        applicationFee = fee.trim(),
                                        applicationDeadline = deadlineDate,
                                        applicationIdRef = applicationIdRef.trim(),
                                        transactionRef = transactionRef.trim(),
                                        notes = notes.trim(),
                                        updatedAt = System.currentTimeMillis()
                                    ) ?: AdmissionApplicationEntity(
                                        id = UUID.randomUUID().toString(),
                                        universityId = selectedUniId,
                                        unitName = unitName.trim(),
                                        admissionCycle = admissionCycle.trim(),
                                        applicationStatus = appStatus,
                                        paymentStatus = payStatus,
                                        applicationFee = fee.trim(),
                                        applicationDeadline = deadlineDate,
                                        applicationIdRef = applicationIdRef.trim(),
                                        transactionRef = transactionRef.trim(),
                                        notes = notes.trim()
                                    )
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = FokusElectricLime,
                            contentColor = FokusOnLime
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(strings.save, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditEventDialog(
    universities: List<UniversityEntity>,
    applications: List<AdmissionApplicationEntity>,
    initialEvent: AdmissionEventEntity? = null,
    preselectedUniversityId: String? = null,
    preselectedDateMillis: Long? = null,
    onDismiss: () -> Unit,
    onSave: (AdmissionEventEntity) -> Unit
) {
    val strings = LocalFokusStrings.current

    var selectedUniId by remember {
        mutableStateOf(initialEvent?.universityId ?: preselectedUniversityId ?: universities.firstOrNull()?.id ?: "")
    }
    var selectedAppId by remember {
        mutableStateOf(initialEvent?.applicationId)
    }
    var eventType by remember {
        mutableStateOf(initialEvent?.eventType ?: AdmissionEventType.ADMISSION_TEST)
    }
    var title by remember { mutableStateOf(initialEvent?.title ?: "") }
    var isDateConfirmed by remember { mutableStateOf(initialEvent?.isDateConfirmed ?: true) }
    var isAllDay by remember { mutableStateOf(initialEvent?.isAllDay ?: true) }
    var targetDateMillis by remember {
        mutableStateOf(initialEvent?.deadlineDateTimeMillis ?: initialEvent?.startDateTimeMillis ?: preselectedDateMillis ?: System.currentTimeMillis())
    }
    var location by remember { mutableStateOf(initialEvent?.location ?: "") }
    var officialNoticeUrl by remember { mutableStateOf(initialEvent?.officialNoticeUrl ?: "") }
    var notes by remember { mutableStateOf(initialEvent?.notes ?: "") }
    var errorMessage by remember { mutableStateOf("") }

    var showDatePicker by remember { mutableStateOf(false) }
    var uniDropdownExpanded by remember { mutableStateOf(false) }
    var eventTypeDropdownExpanded by remember { mutableStateOf(false) }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = targetDateMillis
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { targetDateMillis = it }
                    showDatePicker = false
                }) {
                    Text("OK", color = FokusElectricLime)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(strings.cancel)
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (initialEvent == null) strings.addAdmissionEvent else strings.edit,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }

                if (errorMessage.isNotBlank()) {
                    Text(text = errorMessage, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                }

                // University Picker
                ExposedDropdownMenuBox(
                    expanded = uniDropdownExpanded,
                    onExpandedChange = { uniDropdownExpanded = it }
                ) {
                    val currentUni = universities.firstOrNull { it.id == selectedUniId }
                    OutlinedTextField(
                        value = currentUni?.name ?: "Select Target University",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(strings.universityName, fontSize = 12.sp) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = uniDropdownExpanded) },
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
                        expanded = uniDropdownExpanded,
                        onDismissRequest = { uniDropdownExpanded = false }
                    ) {
                        universities.forEach { uni ->
                            DropdownMenuItem(
                                text = { Text(uni.name) },
                                onClick = {
                                    selectedUniId = uni.id
                                    selectedAppId = null
                                    uniDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                // Event Type Picker
                ExposedDropdownMenuBox(
                    expanded = eventTypeDropdownExpanded,
                    onExpandedChange = { eventTypeDropdownExpanded = it }
                ) {
                    val typeLabel = when (eventType) {
                        AdmissionEventType.APPLICATION_OPENS -> "Application Opens"
                        AdmissionEventType.APPLICATION_DEADLINE -> "Application Deadline"
                        AdmissionEventType.PAYMENT_DEADLINE -> "Payment Deadline"
                        AdmissionEventType.ADMIT_CARD_OPENS -> "Admit Card Download Opens"
                        AdmissionEventType.ADMIT_CARD_DEADLINE -> "Admit Card Download Deadline"
                        AdmissionEventType.ADMISSION_TEST -> "Admission Test / Exam"
                        AdmissionEventType.RESULT_PUBLISHED -> "Result Publication"
                        AdmissionEventType.SUBJECT_CHOICE_DEADLINE -> "Subject Choice Deadline"
                        AdmissionEventType.OTHER -> "Other Event"
                    }
                    OutlinedTextField(
                        value = typeLabel,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Event Type", fontSize = 12.sp) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = eventTypeDropdownExpanded) },
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
                        expanded = eventTypeDropdownExpanded,
                        onDismissRequest = { eventTypeDropdownExpanded = false }
                    ) {
                        listOf(
                            AdmissionEventType.APPLICATION_DEADLINE to "Application Deadline",
                            AdmissionEventType.ADMISSION_TEST to "Admission Test / Exam",
                            AdmissionEventType.APPLICATION_OPENS to "Application Opens",
                            AdmissionEventType.PAYMENT_DEADLINE to "Payment Deadline",
                            AdmissionEventType.ADMIT_CARD_OPENS to "Admit Card Download Opens",
                            AdmissionEventType.ADMIT_CARD_DEADLINE to "Admit Card Deadline",
                            AdmissionEventType.RESULT_PUBLISHED to "Result Publication",
                            AdmissionEventType.SUBJECT_CHOICE_DEADLINE to "Subject Choice Deadline",
                            AdmissionEventType.OTHER to "Other Event"
                        ).forEach { (t, lbl) ->
                            DropdownMenuItem(
                                text = { Text(lbl) },
                                onClick = {
                                    eventType = t
                                    eventTypeDropdownExpanded = false
                                    if (title.isBlank()) {
                                        val uniShort = universities.firstOrNull { it.id == selectedUniId }?.shortName ?: ""
                                        title = if (uniShort.isNotBlank()) "$uniShort $lbl" else lbl
                                    }
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it; errorMessage = "" },
                    label = { Text(strings.titleInput) },
                    placeholder = { Text("e.g. DU Ka Unit Admission Test") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = FokusElectricLime,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    )
                )

                // Date Confirmed Switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(strings.confirmDatePrompt, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Text(
                            text = if (isDateConfirmed) "Date is confirmed by university" else "Tentative / Date not yet confirmed",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = isDateConfirmed,
                        onCheckedChange = { isDateConfirmed = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = FokusOnLime,
                            checkedTrackColor = FokusElectricLime
                        )
                    )
                }

                if (isDateConfirmed) {
                    OutlinedButton(
                        onClick = { showDatePicker = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.CalendarMonth, contentDescription = null, tint = FokusElectricLime)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Date: ${AdmissionCountdownEngine.formatDate(targetDateMillis, isAllDay, com.example.ui.localization.AppLanguage.ENGLISH)}",
                            fontSize = 13.sp
                        )
                    }
                }

                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text(strings.location) },
                    placeholder = { Text("e.g. Curzon Hall, Dhaka") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = FokusElectricLime,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    )
                )

                OutlinedTextField(
                    value = officialNoticeUrl,
                    onValueChange = { officialNoticeUrl = it },
                    label = { Text("Official Notice Link (Optional)") },
                    placeholder = { Text("https://...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = FokusElectricLime,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    )
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text(strings.descriptionInput) },
                    placeholder = { Text("Reporting time, required documents, calculator rules") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = FokusElectricLime,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(strings.cancel)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (selectedUniId.isBlank()) {
                                errorMessage = "Please select a university"
                            } else if (title.isBlank()) {
                                errorMessage = "Event title is required"
                            } else {
                                onSave(
                                    initialEvent?.copy(
                                        universityId = selectedUniId,
                                        applicationId = selectedAppId,
                                        eventType = eventType,
                                        title = title.trim(),
                                        startDateTimeMillis = if (eventType == AdmissionEventType.ADMISSION_TEST) targetDateMillis else null,
                                        deadlineDateTimeMillis = if (eventType != AdmissionEventType.ADMISSION_TEST) targetDateMillis else targetDateMillis,
                                        isDateConfirmed = isDateConfirmed,
                                        isAllDay = isAllDay,
                                        location = location.trim(),
                                        officialNoticeUrl = officialNoticeUrl.trim(),
                                        notes = notes.trim(),
                                        updatedAt = System.currentTimeMillis()
                                    ) ?: AdmissionEventEntity(
                                        id = UUID.randomUUID().toString(),
                                        universityId = selectedUniId,
                                        applicationId = selectedAppId,
                                        eventType = eventType,
                                        title = title.trim(),
                                        startDateTimeMillis = if (eventType == AdmissionEventType.ADMISSION_TEST) targetDateMillis else null,
                                        deadlineDateTimeMillis = targetDateMillis,
                                        isDateConfirmed = isDateConfirmed,
                                        isAllDay = isAllDay,
                                        location = location.trim(),
                                        officialNoticeUrl = officialNoticeUrl.trim(),
                                        notes = notes.trim()
                                    )
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = FokusElectricLime,
                            contentColor = FokusOnLime
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(strings.save, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
