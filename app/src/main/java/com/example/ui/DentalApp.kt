package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.screens.*
import com.example.viewmodel.DentalViewModel

enum class DentalNavTab(
    val title: String,
    val icon: ImageVector,
    val testTag: String
) {
    PATIENTS("Patients", Icons.Default.People, "tab_patients"),
    CAPTURE("Capture", Icons.Default.CameraAlt, "tab_capture"),
    RESULTS("Results", Icons.Default.AutoFixHigh, "tab_results"),
    LAB_ORDER("Lab Order", Icons.Default.PictureAsPdf, "tab_lab_order"),
    CALIBRATION("Calibration", Icons.Default.SettingsSuggest, "tab_calibration")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DentalApp(viewModel: DentalViewModel) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    var currentTab by remember { mutableStateOf(DentalNavTab.PATIENTS) }
    val snackbarHostState = remember { SnackbarHostState() }

    // Show notices and errors in Snackbar
    LaunchedEffect(uiState.userNotice) {
        uiState.userNotice?.let {
            snackbarHostState.showSnackbar(it, duration = SnackbarDuration.Short)
            viewModel.clearNotice()
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it, duration = SnackbarDuration.Long)
            viewModel.clearNotice()
        }
    }

    // Handle back press to return to Patients tab if in sub-tabs
    if (currentTab != DentalNavTab.PATIENTS) {
        BackHandler {
            currentTab = DentalNavTab.PATIENTS
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.ColorLens,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "VITA Dental Shade",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = "CIEDE2000 Shade Assistance System",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    uiState.activeCase?.let { c ->
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                            modifier = Modifier.padding(end = 12.dp)
                        ) {
                            Text(
                                text = "Tooth ${c.toothNumber.take(5)}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 4.dp
            ) {
                DentalNavTab.values().forEach { tab ->
                    NavigationBarItem(
                        selected = currentTab == tab,
                        onClick = { currentTab = tab },
                        icon = { Icon(tab.icon, contentDescription = tab.title) },
                        label = { Text(tab.title) },
                        modifier = Modifier.testTag(tab.testTag)
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                DentalNavTab.PATIENTS -> {
                    PatientsScreen(
                        uiState = uiState,
                        onSelectPatient = { viewModel.selectPatient(it) },
                        onCreatePatient = { code, name, notes -> viewModel.createPatient(code, name, notes) },
                        onDeletePatient = { viewModel.deletePatient(it) },
                        onSelectCase = { viewModel.selectCase(it) },
                        onCreateCase = { patientId, tooth, rest -> viewModel.createCase(patientId, tooth, rest) },
                        onDeleteCase = { viewModel.deleteCase(it) },
                        onNavigateToCapture = { currentTab = DentalNavTab.CAPTURE }
                    )
                }
                DentalNavTab.CAPTURE -> {
                    CaptureScreen(
                        uiState = uiState,
                        onPhotoCaptured = { bitmap ->
                            // Photo is stored in state
                        },
                        onAnalyzeCurrentPhoto = { bitmap ->
                            viewModel.processToothImage(context, bitmap, isMaxillary = true)
                        },
                        onNavigateToResults = { currentTab = DentalNavTab.RESULTS }
                    )
                }
                DentalNavTab.RESULTS -> {
                    ResultsScreen(
                        uiState = uiState,
                        onConfirmShade = { viewModel.confirmAlgorithmShade() },
                        onOverrideShade = { shade, reason -> viewModel.overrideShade(shade, reason) },
                        onGenerateLabPdf = { viewModel.generateLabPdf(context) },
                        onSavePdfToDownloads = { viewModel.savePdfToDownloads(context) },
                        onNavigateToLab = { currentTab = DentalNavTab.LAB_ORDER },
                        onNavigateBackToCapture = { currentTab = DentalNavTab.CAPTURE }
                    )
                }
                DentalNavTab.LAB_ORDER -> {
                    LabOrderScreen(
                        uiState = uiState,
                        onRegeneratePdf = { viewModel.generateLabPdf(context) },
                        onSavePdfToDownloads = { viewModel.savePdfToDownloads(context) },
                        onNavigateBackToResults = { currentTab = DentalNavTab.RESULTS }
                    )
                }
                DentalNavTab.CALIBRATION -> {
                    CalibrationInfoScreen(uiState = uiState)
                }
            }

            if (uiState.isLoading) {
                Surface(
                    color = androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.4f),
                    modifier = Modifier.fillMaxSize()
                ) {
                    Box(contentAlignment = androidx.compose.ui.Alignment.Center) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
                            ) {
                                CircularProgressIndicator()
                                Spacer(modifier = Modifier.height(14.dp))
                                Text(
                                    text = "Executing CIEDE2000 Pipeline...",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Segmenting tooth & computing spectral metrics",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
