package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.example.colorengine.ColorConversions
import com.example.colorengine.Lab
import com.example.colorengine.ShadeMatchResult
import com.example.colorengine.VitaClassicalData
import com.example.ui.components.ColorSpacePlaneCanvas
import com.example.ui.components.ShadeComparisonCard
import com.example.viewmodel.DentalUiState

@Composable
fun ResultsScreen(
    uiState: DentalUiState,
    onConfirmShade: () -> Unit,
    onOverrideShade: (String, String) -> Unit,
    onGenerateLabPdf: () -> Unit,
    onSavePdfToDownloads: () -> Unit,
    onNavigateToLab: () -> Unit,
    onNavigateBackToCapture: () -> Unit
) {
    val report = uiState.comprehensiveReport
    val activeCase = uiState.activeCase
    val finalShade = uiState.finalShade

    var showOverrideDialog by remember { mutableStateOf(false) }

    if (report == null || activeCase == null) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(Icons.Default.HourglassEmpty, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(12.dp))
            Text("No shade analysis available yet", fontWeight = FontWeight.Bold)
            Text(
                if (uiState.deviceValidation?.isPreciseAnalysisAllowed != true)
                    "Precise shade analysis is unavailable until camera calibration and reference data are validated."
                else "Capture or select a tooth image first.",
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onNavigateBackToCapture) {
                Text("Go to Capture")
            }
        }
        return
    }

    val primary = report.recommendedShade
    val second = report.secondShade
    val third = report.thirdShade

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Clinical Case Header
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Tooth ${activeCase.toothNumber}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "System: VITA_CLASSICAL",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Patient: ${uiState.selectedPatient?.name ?: "N/A"} • ${activeCase.restorationType}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Ambiguity Alert Banner (Per prompt Section 18)
        if (report.isAmbiguous && report.ambiguityMessage != null) {
            Surface(
                color = Color(0xFFFEF3C7),
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF59E0B)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.WarningAmber,
                        contentDescription = null,
                        tint = Color(0xFFB45309),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Ambiguity Warning",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelLarge,
                            color = Color(0xFF92400E)
                        )
                        Text(
                            text = report.ambiguityMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF92400E)
                        )
                    }
                }
            }
        }

        // Primary Recommended Shade Hero Card
        Card(
            modifier = Modifier.fillMaxWidth().testTag("primary_shade_card"),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = "ALGORITHM RECOMMENDED SHADE",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = primary.shadeCode,
                            style = MaterialTheme.typography.displayMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "VITA Classical Group ${primary.shadeCode.first()}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }

                    // Visual Color Comparison Chips (Estimated vs Unverified Dataset)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ColorChipItem(label = "Measured", lab = report.overallLab)
                        ColorChipItem(label = "Ref ${primary.shadeCode}", lab = primary.referenceLab)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "CIEDE2000 ΔE00 = %.2f".format(primary.deltaE00),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = report.perceptibilityStatus,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "Rank #1",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        // Side-by-Side Visual Shade Comparison View
        ShadeComparisonCard(
            scannedLab = report.overallLab,
            topCandidates = report.topMatches,
            clinicalPhoto = uiState.currentCapturedBitmap,
            cervicalLab = report.zones?.cervical?.lab,
            middleLab = report.zones?.middle?.lab,
            incisalLab = report.zones?.incisal?.lab
        )

        // 2D CIELAB Color Space Plane Confirmation (Interactive Canvas Plot)
        ColorSpacePlaneCanvas(
            scannedLab = report.overallLab,
            targetShade = finalShade?.dentistSelectedShade ?: primary.shadeCode
        )

        // Top 2nd & 3rd Alternative Candidates Card (Section 20)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "Alternative Candidates (Top 3)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))

                CandidateRow(candidate = second)
                Spacer(modifier = Modifier.height(6.dp))
                CandidateRow(candidate = third)
            }
        }

        // Anatomical Zones (Cervical, Middle, Incisal) (Section 15)
        if (report.zones != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Stratified Anatomical Zones",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Cervical body typically has higher chromatic saturation; Incisal edge has higher translucency.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ZoneBox(modifier = Modifier.weight(1f), title = "Cervical", zone = report.zones.cervical)
                        ZoneBox(modifier = Modifier.weight(1f), title = "Middle", zone = report.zones.middle)
                        ZoneBox(modifier = Modifier.weight(1f), title = "Incisal", zone = report.zones.incisal)
                    }
                }
            }
        }

        // Estimated CIE L*a*b* & Dispersion
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "Estimated Image Color (not clinically validated)",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Measured CIE Lab: L* %.2f, a* %.2f, b* %.2f".format(report.overallLab.l, report.overallLab.a, report.overallLab.b),
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                )
                Text(
                    text = "Optical Dispersion (MAD): %.2f • Algorithm: ${report.algorithmVersion}".format(report.colorDispersionMad),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Dentist Confirmation & Override Section (Section 20 & 22)
        Card(
            modifier = Modifier.fillMaxWidth().testTag("dentist_decision_card"),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Dentist Verification & Final Selection",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "The final clinical shade decision remains the dentist's responsibility. Both the algorithm recommendation and dentist selection are permanently recorded.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Current Final Selection Status
                if (finalShade != null) {
                    val isOverridden = finalShade.dentistSelectedShade != finalShade.algorithmShade
                    Surface(
                        color = if (isOverridden) Color(0xFFFEF3C7) else Color(0xFFD1FAE5),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = if (isOverridden) "Dentist Override Active" else "Algorithm Shade Confirmed",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (isOverridden) Color(0xFF92400E) else Color(0xFF065F46)
                                )
                                Text(
                                    text = "Final: ${finalShade.dentistSelectedShade}",
                                    fontWeight = FontWeight.ExtraBold,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = if (isOverridden) Color(0xFF92400E) else Color(0xFF065F46)
                                )
                            }
                            if (isOverridden && !finalShade.overrideReason.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Clinical Reason: ${finalShade.overrideReason}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF92400E)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onConfirmShade,
                        modifier = Modifier.weight(1f).testTag("confirm_shade_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D5C75))
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Confirm ${primary.shadeCode}")
                    }

                    OutlinedButton(
                        onClick = { showOverrideDialog = true },
                        modifier = Modifier.weight(1f).testTag("override_shade_button")
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Override...")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                FilledTonalButton(
                    onClick = {
                        onGenerateLabPdf()
                        onNavigateToLab()
                    },
                    modifier = Modifier.fillMaxWidth().testTag("generate_lab_pdf_button")
                ) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Generate Laboratory Order PDF")
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        onSavePdfToDownloads()
                        onNavigateToLab()
                    },
                    modifier = Modifier.fillMaxWidth().testTag("save_lab_pdf_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D5C75))
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Save PDF Lab Report to Device Storage")
                }
            }
        }
    }

    // Dentist Override Dialog
    if (showOverrideDialog) {
        DentistOverrideDialog(
            algorithmShade = primary.shadeCode,
            onDismiss = { showOverrideDialog = false },
            onConfirm = { newShade, reason ->
                onOverrideShade(newShade, reason)
                showOverrideDialog = false
            }
        )
    }
}

@Composable
fun ColorChipItem(label: String, lab: Lab) {
    val rgb = ColorConversions.labToRgb(lab)
    val color = Color(rgb.first, rgb.second, rgb.third)

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(color)
                .border(1.dp, Color.Gray.copy(alpha = 0.5f), CircleShape)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun CandidateRow(candidate: ShadeMatchResult) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "#${candidate.rank}",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = candidate.shadeCode,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(8.dp))
            ColorChipItem(label = "", lab = candidate.referenceLab)
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "ΔE00 = %.2f".format(candidate.deltaE00),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Ref L* %.1f b* %.1f".format(candidate.referenceLab.l, candidate.referenceLab.b),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun ZoneBox(
    modifier: Modifier = Modifier,
    title: String,
    zone: com.example.colorengine.ShadeAnalysisZone
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = title, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = zone.topMatch.shadeCode,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "ΔE %.2f".format(zone.topMatch.deltaE00),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun DentistOverrideDialog(
    algorithmShade: String,
    onDismiss: () -> Unit,
    onConfirm: (shade: String, reason: String) -> Unit
) {
    var selectedShade by remember { mutableStateOf(algorithmShade) }
    var reason by remember { mutableStateOf("") }

    val presetReasons = listOf(
        "Patient requests brighter incisal value",
        "Adjacent tooth has higher chromatic saturation",
        "Metamerism adjustment under clinical operatory lighting",
        "Stump shade / core substrate compensation",
        "High translucency enamel matching"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Clinical Shade Override") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Algorithm recommendation: $algorithmShade",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text("Select Clinical Shade (VITA Classical):", style = MaterialTheme.typography.labelMedium)

                // 16 VITA Shades Grid
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    val rows = VitaClassicalData.ALL_16_SHADES.chunked(4)
                    rows.forEach { row ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            row.forEach { ref ->
                                val isSelected = selectedShade == ref.shadeCode
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                        .clickable { selectedShade = ref.shadeCode }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = ref.shadeCode,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text("Clinical Override Reason (Mandatory):", style = MaterialTheme.typography.labelMedium)

                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    placeholder = { Text("Enter reason for override...") },
                    modifier = Modifier.fillMaxWidth().testTag("override_reason_input")
                )

                // Quick preset buttons
                presetReasons.take(2).forEach { preset ->
                    TextButton(
                        onClick = { reason = preset },
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("• $preset", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { if (reason.isNotBlank()) onConfirm(selectedShade, reason) },
                enabled = reason.isNotBlank(),
                modifier = Modifier.testTag("confirm_override_button")
            ) {
                Text("Save Override")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
