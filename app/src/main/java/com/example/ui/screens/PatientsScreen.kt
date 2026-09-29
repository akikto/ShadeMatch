package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.entities.DentalCaseEntity
import com.example.data.local.entities.PatientEntity
import com.example.viewmodel.DentalUiState
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun PatientsScreen(
    uiState: DentalUiState,
    onSelectPatient: (PatientEntity) -> Unit,
    onCreatePatient: (String, String, String) -> Unit,
    onDeletePatient: (PatientEntity) -> Unit,
    onSelectCase: (DentalCaseEntity) -> Unit,
    onCreateCase: (Long, String, String) -> Unit,
    onDeleteCase: (DentalCaseEntity) -> Unit,
    onNavigateToCapture: () -> Unit
) {
    var showNewPatientDialog by remember { mutableStateOf(false) }
    var showNewCaseDialog by remember { mutableStateOf(false) }

    val selectedPatient = uiState.selectedPatient

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        // Patient Header Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Clinical Patients",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Local offline records (${uiState.patients.size})",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Button(
                onClick = { showNewPatientDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                modifier = Modifier.testTag("add_patient_button")
            ) {
                Icon(Icons.Default.PersonAdd, contentDescription = "Add Patient", modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("New Patient")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Patient Horizontal Chips / Dropdown
        if (uiState.patients.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Group, contentDescription = null, modifier = Modifier.size(40.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("No patients registered yet", fontWeight = FontWeight.SemiBold)
                    Text("Tap 'New Patient' to begin a case.", style = MaterialTheme.typography.bodySmall)
                }
            }
        } else {
            // Patient selector list
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                uiState.patients.take(3).forEach { patient ->
                    val isSelected = selectedPatient?.id == patient.id
                    FilterChip(
                        selected = isSelected,
                        onClick = { onSelectPatient(patient) },
                        label = { Text("${patient.name} (${patient.patientCode})") },
                        leadingIcon = if (isSelected) {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        } else null,
                        modifier = Modifier.testTag("patient_chip_${patient.patientCode}")
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Selected Patient Details & Cases
        if (selectedPatient != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = selectedPatient.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Code: ${selectedPatient.patientCode} • Notes: ${selectedPatient.notes.ifEmpty { "None" }}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        FilledTonalButton(
                            onClick = { showNewCaseDialog = true },
                            modifier = Modifier.testTag("add_case_button")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("New Case")
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Clinical Cases for this Patient (${uiState.patientCases.size})",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    if (uiState.patientCases.isEmpty()) {
                        Text(
                            text = "No cases recorded yet for ${selectedPatient.name}. Tap 'New Case' to set tooth number & restoration type.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth().heightIn(max = 380.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(uiState.patientCases) { dentalCase ->
                                val isCaseActive = uiState.activeCase?.id == dentalCase.id
                                CaseCard(
                                    dentalCase = dentalCase,
                                    isActive = isCaseActive,
                                    onSelect = {
                                        onSelectCase(dentalCase)
                                    },
                                    onDelete = { onDeleteCase(dentalCase) },
                                    onStartCapture = {
                                        onSelectCase(dentalCase)
                                        onNavigateToCapture()
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Dialog: Create Patient
    if (showNewPatientDialog) {
        CreatePatientDialog(
            onDismiss = { showNewPatientDialog = false },
            onConfirm = { code, name, notes ->
                onCreatePatient(code, name, notes)
                showNewPatientDialog = false
            }
        )
    }

    // Dialog: Create Case
    if (showNewCaseDialog && selectedPatient != null) {
        CreateCaseDialog(
            patient = selectedPatient,
            onDismiss = { showNewCaseDialog = false },
            onConfirm = { toothNumber, restoration ->
                onCreateCase(selectedPatient.id, toothNumber, restoration)
                showNewCaseDialog = false
            }
        )
    }
}

@Composable
fun CaseCard(
    dentalCase: DentalCaseEntity,
    isActive: Boolean,
    onSelect: () -> Unit,
    onDelete: () -> Unit,
    onStartCapture: () -> Unit
) {
    val statusColor = when (dentalCase.status) {
        "CONFIRMED", "ORDER_GENERATED" -> Color(0xFF047857) // Green
        "ANALYZED" -> Color(0xFF0284C7) // Blue
        "CAPTURED" -> Color(0xFFD97706) // Amber
        else -> Color(0xFF64748B) // Slate
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() }
            .testTag("case_item_${dentalCase.id}"),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Tooth ${dentalCase.toothNumber}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(statusColor.copy(alpha = 0.15f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = dentalCase.status,
                            color = statusColor,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${dentalCase.restorationType} • Shade System: ${dentalCase.shadeSystem}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(
                    onClick = onStartCapture,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("start_capture_button_${dentalCase.id}")
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = "Capture", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Capture", style = MaterialTheme.typography.labelMedium)
                }

                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = "Delete Case", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

@Composable
fun CreatePatientDialog(
    onDismiss: () -> Unit,
    onConfirm: (code: String, name: String, notes: String) -> Unit
) {
    var code by remember { mutableStateOf("PT-${(1000..9999).random()}") }
    var name by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Register New Patient") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it },
                    label = { Text("Patient Identifier Code") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("input_patient_code")
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Patient Full Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("input_patient_name")
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Clinical Notes (Optional)") },
                    modifier = Modifier.fillMaxWidth().testTag("input_patient_notes")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (name.isNotBlank()) onConfirm(code, name, notes) },
                enabled = name.isNotBlank(),
                modifier = Modifier.testTag("confirm_create_patient_button")
            ) {
                Text("Save Patient")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun CreateCaseDialog(
    patient: PatientEntity,
    onDismiss: () -> Unit,
    onConfirm: (tooth: String, restoration: String) -> Unit
) {
    var selectedTooth by remember { mutableStateOf("11 (FDI Central Incisor)") }
    var selectedRestoration by remember { mutableStateOf("Ceramic Veneer") }

    val commonTeeth = listOf(
        "11 (FDI / #8 Univ - Max Right Central)",
        "21 (FDI / #9 Univ - Max Left Central)",
        "12 (FDI / #7 Univ - Max Right Lateral)",
        "22 (FDI / #10 Univ - Max Left Lateral)",
        "13 (FDI / #6 Univ - Max Right Canine)",
        "23 (FDI / #11 Univ - Max Left Canine)",
        "31 (FDI / #24 Univ - Mand Left Central)",
        "41 (FDI / #25 Univ - Mand Right Central)"
    )

    val restorations = listOf(
        "Ceramic Veneer",
        "Zirconia Crown",
        "e.max Lithium Disilicate Crown",
        "Composite Restoration",
        "Anterior Bridge Unit",
        "Implant-Supported Crown",
        "Inlay / Onlay"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Dental Shade Case") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Patient: ${patient.name} (${patient.patientCode})",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Text("Select Target Tooth:", style = MaterialTheme.typography.labelMedium)
                LazyColumn(modifier = Modifier.height(140.dp)) {
                    items(commonTeeth) { tooth ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedTooth = tooth }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = selectedTooth == tooth, onClick = { selectedTooth = tooth })
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(tooth, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }

                Text("Select Restoration Type:", style = MaterialTheme.typography.labelMedium)
                LazyColumn(modifier = Modifier.height(120.dp)) {
                    items(restorations) { rest ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedRestoration = rest }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = selectedRestoration == rest, onClick = { selectedRestoration = rest })
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(rest, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }

                Text(
                    text = "Shade Reference Standard: VITA_CLASSICAL (16 shades)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(selectedTooth, selectedRestoration) },
                modifier = Modifier.testTag("confirm_create_case_button")
            ) {
                Text("Create Case")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
