package com.example.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.calibration.CalibrationStatus
import com.example.ui.capture.CameraCaptureView
import com.example.viewmodel.DentalUiState
import com.example.vision.DentalImageHelper
import com.example.vision.QualityStatus

@Composable
fun CaptureScreen(
    uiState: DentalUiState,
    onPhotoCaptured: (Bitmap) -> Unit,
    onAnalyzeCurrentPhoto: (Bitmap) -> Unit,
    onNavigateToResults: () -> Unit
) {
    val context = LocalContext.current
    var isLiveCameraActive by remember { mutableStateOf(false) }
    var selectedSimulatedShade by remember { mutableStateOf("A2") }
    var showSimulateMenu by remember { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val bitmap = if (Build.VERSION.SDK_INT < 28) {
                MediaStore.Images.Media.getBitmap(context.contentResolver, it)
            } else {
                val source = ImageDecoder.createSource(context.contentResolver, it)
                ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                    decoder.isMutableRequired = true
                }
            }
            onPhotoCaptured(bitmap)
            onAnalyzeCurrentPhoto(bitmap)
        }
    }

    val activeCase = uiState.activeCase
    val quality = uiState.qualityEvaluation
    val currentBitmap = uiState.currentCapturedBitmap
    val devValidation = uiState.deviceValidation

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Case Header
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Row(
                modifier = Modifier.padding(14.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (activeCase != null) "Tooth ${activeCase.toothNumber}" else "No Case Selected",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Patient: ${uiState.selectedPatient?.name ?: "N/A"} • ${activeCase?.restorationType ?: ""}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Device Validation Badge (Per prompt Section 9)
                val isCalibrated = devValidation?.status == CalibrationStatus.VALIDATED
                Surface(
                    color = if (isCalibrated) Color(0xFFD1FAE5) else Color(0xFFFEF3C7),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isCalibrated) Icons.Default.CheckCircle else Icons.Default.WarningAmber,
                            contentDescription = null,
                            tint = if (isCalibrated) Color(0xFF047857) else Color(0xFFB45309),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (devValidation?.isPreciseAnalysisAllowed == true) "Validated setup" else "Analysis blocked",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isCalibrated) Color(0xFF047857) else Color(0xFFB45309)
                        )
                    }
                }
            }
        }

        if (devValidation?.isPreciseAnalysisAllowed != true) {
            Text(
                text = "Camera calibration and reference data are not validated. Precise shade analysis is unavailable on this device.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.testTag("analysis_blocked_message")
            )
        }

        // Live Camera or Preview Area
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(340.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color.Black)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp))
        ) {
            if (isLiveCameraActive) {
                CameraCaptureView(
                    modifier = Modifier.fillMaxSize(),
                    onPhotoCaptured = { bitmap ->
                        isLiveCameraActive = false
                        onPhotoCaptured(bitmap)
                        onAnalyzeCurrentPhoto(bitmap)
                    }
                )
            } else if (currentBitmap != null) {
                Image(
                    bitmap = currentBitmap.asImageBitmap(),
                    contentDescription = "Captured Clinical Photo",
                    modifier = Modifier.fillMaxSize()
                )

                // Overlay Tag
                Surface(
                    color = Color.Black.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.align(Alignment.TopStart).padding(10.dp)
                ) {
                    Text(
                        text = "Clinical Frame (${currentBitmap.width}x${currentBitmap.height})",
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "No Photo Captured Yet",
                        color = Color.White,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Open Camera, choose from Gallery, or simulate clinical tooth",
                        color = Color.White.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        // Capture Mode Actions Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { isLiveCameraActive = !isLiveCameraActive },
                modifier = Modifier.weight(1f).testTag("open_camera_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isLiveCameraActive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    imageVector = if (isLiveCameraActive) Icons.Default.Close else Icons.Default.Camera,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(if (isLiveCameraActive) "Close Camera" else "Live Camera")
            }

            OutlinedButton(
                onClick = { photoPickerLauncher.launch("image/*") },
                modifier = Modifier.weight(1f).testTag("pick_gallery_button")
            ) {
                Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Gallery")
            }

            // Clinical Simulation Dropdown Button (Crucial for reliable testing in emulator)
            Box(modifier = Modifier.weight(1.2f)) {
                FilledTonalButton(
                    onClick = { showSimulateMenu = true },
                    modifier = Modifier.fillMaxWidth().testTag("simulate_tooth_button")
                ) {
                    Icon(Icons.Default.Science, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Test Tab: $selectedSimulatedShade")
                }

                DropdownMenu(
                    expanded = showSimulateMenu,
                    onDismissRequest = { showSimulateMenu = false }
                ) {
                    val sampleShades = listOf("A1", "A2", "A3", "A3.5", "B1", "B2", "C1", "C2", "D2", "D3")
                    sampleShades.forEach { shade ->
                        DropdownMenuItem(
                            text = { Text("Simulate VITA $shade") },
                            onClick = {
                                selectedSimulatedShade = shade
                                showSimulateMenu = false
                                val simBitmap = DentalImageHelper.generateSimulatedToothImage(targetVitaShade = shade)
                                onPhotoCaptured(simBitmap)
                                onAnalyzeCurrentPhoto(simBitmap)
                            }
                        )
                    }
                }
            }
        }

        // Image Quality Evaluation Panel (Section 13)
        if (quality != null) {
            QualityEvaluationCard(quality = quality)
        }

        // Proceed to Results Button
        if (uiState.comprehensiveReport != null) {
            Button(
                onClick = onNavigateToResults,
                modifier = Modifier.fillMaxWidth().height(50.dp).testTag("view_results_button"),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.AutoFixHigh, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "View Shade Analysis Results (${uiState.comprehensiveReport.recommendedShade.shadeCode})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Clinical Notice / Scientific Reminder
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
            shape = RoundedCornerShape(8.dp)
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Shade-assistance system only. Calculations are computed offline on-device using deterministic CIEDE2000 color science. Final clinical shade selection remains the dentist's responsibility.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun QualityEvaluationCard(quality: com.example.vision.ImageQualityEvaluation) {
    val statusColor = when (quality.status) {
        QualityStatus.PASS -> Color(0xFF047857) // Green
        QualityStatus.WARNING -> Color(0xFFB45309) // Amber
        QualityStatus.FAIL -> Color(0xFFB91C1C) // Red
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Image Quality Assessment",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                Surface(
                    color = statusColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = quality.status.name,
                        color = statusColor,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Metric indicators
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                QualityMetricItem("Sharpness", "%.1f".format(quality.blurScore), quality.blurScore > 75)
                QualityMetricItem("Shadow", "%.1f%%".format(quality.underexposedFraction * 100), quality.underexposedFraction < 0.35)
                QualityMetricItem("Glare", "%.1f%%".format(quality.specularFraction * 100), quality.specularFraction < 0.12)
                QualityMetricItem("Coverage", "%.1f%%".format(quality.toothAreaFraction * 100), quality.toothAreaFraction > 0.05)
            }

            if (quality.issues.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(modifier = Modifier.height(8.dp))

                quality.issues.forEach { issue ->
                    Row(
                        modifier = Modifier.padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (issue.severity == QualityStatus.FAIL) Icons.Default.Cancel else Icons.Default.Info,
                            contentDescription = null,
                            tint = if (issue.severity == QualityStatus.FAIL) Color(0xFFB91C1C) else Color(0xFFB45309),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "[${issue.code}] ${issue.message}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun QualityMetricItem(label: String, value: String, isOk: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = if (isOk) Color(0xFF047857) else Color(0xFFB45309)
        )
    }
}
