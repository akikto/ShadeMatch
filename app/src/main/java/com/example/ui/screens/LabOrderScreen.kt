package com.example.ui.screens

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.example.viewmodel.DentalUiState
import java.io.File

@Composable
fun LabOrderScreen(
    uiState: DentalUiState,
    onRegeneratePdf: () -> Unit,
    onNavigateBackToResults: () -> Unit
) {
    val context = LocalContext.current
    val activeCase = uiState.activeCase
    val patient = uiState.selectedPatient
    val report = uiState.comprehensiveReport
    val finalShade = uiState.finalShade
    val pdfFile = uiState.latestGeneratedPdf

    val dentistShade = finalShade?.dentistSelectedShade ?: report?.recommendedShade?.shadeCode ?: "A2"
    val isOverridden = finalShade != null && finalShade.dentistSelectedShade != finalShade.algorithmShade

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Laboratory Prescription",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Exportable ShadeReport PDF for Dental Lab",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(onClick = onNavigateBackToResults) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back to Results")
            }
        }

        // PDF Document Status Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = pdfFile?.name ?: "ShadeReport_Pending.pdf",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = if (pdfFile != null && pdfFile.exists()) {
                            "Ready to share • ${(pdfFile.length() / 1024)} KB"
                        } else {
                            "PDF ready for generation"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }
            }
        }

        // Prescription Summary Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Prescription Details",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(10.dp))

                PrescriptionRow("Patient", "${patient?.name ?: "N/A"} (${patient?.patientCode ?: ""})")
                PrescriptionRow("Case Number", "CS-${activeCase?.id ?: "N/A"}")
                PrescriptionRow("Tooth Number", activeCase?.toothNumber ?: "N/A")
                PrescriptionRow("Restoration Type", activeCase?.restorationType ?: "N/A")
                PrescriptionRow("Shade System", "VITA_CLASSICAL (16-Shade)")

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant)

                PrescriptionRow("Dentist Final Shade", dentistShade, isHighlight = true)
                if (isOverridden && !finalShade?.overrideReason.isNullOrBlank()) {
                    PrescriptionRow("Override Reason", finalShade?.overrideReason ?: "")
                }
                PrescriptionRow("Algorithm Recommendation", report?.recommendedShade?.shadeCode ?: "N/A")
                PrescriptionRow("CIEDE2000 ΔE00", "%.2f".format(report?.recommendedShade?.deltaE00 ?: 0.0))
                PrescriptionRow("Quality Audit Status", uiState.qualityEvaluation?.status?.name ?: "PASS")
            }
        }

        // Action Buttons Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    if (pdfFile != null && pdfFile.exists()) {
                        openPdfIntent(context, pdfFile)
                    } else {
                        onRegeneratePdf()
                    }
                },
                modifier = Modifier.weight(1f).testTag("open_pdf_button"),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("View PDF")
            }

            Button(
                onClick = {
                    if (pdfFile != null && pdfFile.exists()) {
                        sharePdfIntent(context, pdfFile, activeCase?.toothNumber ?: "")
                    } else {
                        onRegeneratePdf()
                    }
                },
                modifier = Modifier.weight(1f).testTag("share_pdf_button"),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF047857)) // Emerald
            ) {
                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Share PDF")
            }
        }

        OutlinedButton(
            onClick = onRegeneratePdf,
            modifier = Modifier.fillMaxWidth().testTag("regenerate_pdf_button")
        ) {
            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Re-generate PDF Document")
        }

        // Lab Technician Technical Guide Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "Ceramic Stratification Guide for Laboratory",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "• Body Enamel: Match primary body to VITA $dentistShade.\n" +
                            "• Cervical Third: Apply chromatic dentin with warmth (+1 chroma step if indicated).\n" +
                            "• Incisal Edge: Layer opalescent/translucent porcelain to match incisal values.\n" +
                            "• Final verification: Evaluate under 5500K standard daylight illumination before glazing.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Mandatory Disclaimer Banner (Section 22)
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            shape = RoundedCornerShape(8.dp)
        ) {
            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Digital shade-assistance information only. Final clinical shade selection remains the responsibility of the dentist.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun PrescriptionRow(label: String, value: String, isHighlight: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = if (isHighlight) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodySmall,
            fontWeight = if (isHighlight) FontWeight.Bold else FontWeight.Medium,
            color = if (isHighlight) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
    }
}

private fun openPdfIntent(context: Context, file: File) {
    try {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        // Fallback share if no dedicated PDF viewer is registered
        sharePdfIntent(context, file, "")
    }
}

private fun sharePdfIntent(context: Context, file: File, toothNumber: String) {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
    val sendIntent = Intent(Intent.ACTION_SEND).apply {
        type = "application/pdf"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_SUBJECT, "Dental Shade Laboratory Report - Tooth $toothNumber")
        putExtra(Intent.EXTRA_TEXT, "Please find attached the digital VITA Classical shade report and laboratory prescription.")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    val chooser = Intent.createChooser(sendIntent, "Share Dental Shade Report PDF")
    chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(chooser)
}
