package com.example.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.calibration.CalibrationRegistry
import com.example.calibration.DeviceValidationResult
import com.example.colorengine.*
import com.example.data.local.entities.*
import com.example.data.repository.DentalRepository
import com.example.pdf.LabReportData
import com.example.pdf.LabReportPdfGenerator
import com.example.vision.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

data class DentalUiState(
    val patients: List<PatientEntity> = emptyList(),
    val selectedPatient: PatientEntity? = null,
    val patientCases: List<DentalCaseEntity> = emptyList(),
    val activeCase: DentalCaseEntity? = null,
    val activeCapture: CaptureEntity? = null,
    val activeAnalysis: ShadeAnalysisEntity? = null,
    val comprehensiveReport: ComprehensiveShadeReport? = null,
    val qualityEvaluation: ImageQualityEvaluation? = null,
    val segmentationResult: ToothSegmentationResult? = null,
    val deviceValidation: DeviceValidationResult? = null,
    val finalShade: FinalShadeEntity? = null,
    val currentCapturedBitmap: Bitmap? = null,
    val latestGeneratedPdf: File? = null,
    val savedPdfDownloadPath: String? = null,
    val isLoading: Boolean = false,
    val userNotice: String? = null,
    val errorMessage: String? = null
)

class DentalViewModel(private val repository: DentalRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(DentalUiState())
    val uiState: StateFlow<DentalUiState> = _uiState.asStateFlow()

    init {
        val deviceResult = CalibrationRegistry.evaluateDevice(Build.MODEL ?: "Generic Device")
        _uiState.value = _uiState.value.copy(deviceValidation = deviceResult)

        viewModelScope.launch {
            repository.allPatientsFlow.collect { list ->
                _uiState.value = _uiState.value.copy(patients = list)
                if (_uiState.value.selectedPatient == null && list.isNotEmpty()) {
                    selectPatient(list.first())
                }
            }
        }
    }

    fun selectPatient(patient: PatientEntity) {
        _uiState.value = _uiState.value.copy(
            selectedPatient = patient,
            activeCase = null,
            comprehensiveReport = null,
            activeAnalysis = null,
            finalShade = null,
            latestGeneratedPdf = null
        )
        viewModelScope.launch {
            repository.getCasesForPatientFlow(patient.id).collect { cases ->
                _uiState.value = _uiState.value.copy(patientCases = cases)
            }
        }
    }

    fun createPatient(code: String, name: String, notes: String) {
        viewModelScope.launch {
            val id = repository.createPatient(code, name, notes)
            val newPatient = repository.getPatient(id)
            newPatient?.let { selectPatient(it) }
        }
    }

    fun deletePatient(patient: PatientEntity) {
        viewModelScope.launch {
            repository.deletePatient(patient)
            _uiState.value = _uiState.value.copy(
                selectedPatient = null,
                activeCase = null,
                comprehensiveReport = null
            )
        }
    }

    fun selectCase(dentalCase: DentalCaseEntity) {
        _uiState.value = _uiState.value.copy(
            activeCase = dentalCase,
            isLoading = true,
            errorMessage = null
        )
        viewModelScope.launch {
            val capture = repository.getLatestCapture(dentalCase.id)
            val finalShade = repository.getFinalShade(dentalCase.id)
            var report: ComprehensiveShadeReport? = null
            var analysis: ShadeAnalysisEntity? = null

            if (capture != null) {
                analysis = repository.getAnalysisForCapture(capture.id)
                if (analysis != null && _uiState.value.deviceValidation?.isPreciseAnalysisAllowed == true &&
                    VitaClassicalData.REFERENCE_STATUS != "REQUIRES_VALIDATED_DATA") {
                    val overallLab = Lab(analysis.overallL, analysis.overallA, analysis.overallB)
                    val cervicalLab = if (analysis.cervicalL > 0) Lab(analysis.cervicalL, analysis.cervicalA, analysis.cervicalB) else null
                    val middleLab = if (analysis.middleL > 0) Lab(analysis.middleL, analysis.middleA, analysis.middleB) else null
                    val incisalLab = if (analysis.incisalL > 0) Lab(analysis.incisalL, analysis.incisalA, analysis.incisalB) else null

                    report = ShadeMatcher.matchShade(
                        measuredLab = overallLab,
                        cervicalLab = cervicalLab,
                        middleLab = middleLab,
                        incisalLab = incisalLab
                    )
                }

                // Load existing photo bitmap if available on disk
                var loadedBitmap: Bitmap? = _uiState.value.currentCapturedBitmap
                if (loadedBitmap == null && capture.imagePath.isNotBlank()) {
                    val imgFile = File(capture.imagePath)
                    if (imgFile.exists()) {
                        loadedBitmap = BitmapFactory.decodeFile(imgFile.absolutePath)
                    }
                }

                _uiState.value = _uiState.value.copy(
                    activeCapture = capture,
                    activeAnalysis = if (report != null) analysis else null,
                    comprehensiveReport = report,
                    finalShade = finalShade,
                    currentCapturedBitmap = loadedBitmap,
                    isLoading = false
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    activeCapture = null,
                    activeAnalysis = null,
                    comprehensiveReport = null,
                    finalShade = finalShade,
                    isLoading = false
                )
            }
        }
    }

    fun createCase(patientId: Long, toothNumber: String, restorationType: String) {
        viewModelScope.launch {
            val id = repository.createCase(patientId, toothNumber, restorationType)
            val created = repository.getCase(id)
            created?.let { selectCase(it) }
        }
    }

    fun deleteCase(dentalCase: DentalCaseEntity) {
        viewModelScope.launch {
            repository.deleteCase(dentalCase)
            _uiState.value = _uiState.value.copy(
                activeCase = null,
                comprehensiveReport = null,
                activeAnalysis = null,
                finalShade = null
            )
        }
    }

    /**
     * Executes the full color-science image analysis pipeline on a captured or selected photo.
     */
    fun processToothImage(
        context: Context,
        bitmap: Bitmap,
        isMaxillary: Boolean = true
    ) {
        val currentCase = _uiState.value.activeCase
        if (currentCase == null) {
            _uiState.value = _uiState.value.copy(errorMessage = "Please select or create an active case first.")
            return
        }
        val validation = _uiState.value.deviceValidation
        if (validation?.isPreciseAnalysisAllowed != true ||
            VitaClassicalData.REFERENCE_STATUS == "REQUIRES_VALIDATED_DATA") {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                comprehensiveReport = null,
                activeAnalysis = null,
                finalShade = null,
                errorMessage = "Precise shade analysis is unavailable. Camera calibration and VITA reference data require validation."
            )
            return
        }

        _uiState.value = _uiState.value.copy(
            isLoading = true,
            errorMessage = null,
            currentCapturedBitmap = bitmap
        )

        viewModelScope.launch(Dispatchers.Default) {
            try {
                // 1. Tooth Segmentation
                val segResult = ToothSegmenter.segmentTooth(bitmap, isMaxillary)

                // 2. Optical Quality Evaluation
                val quality = ImageQualityChecker.evaluateQuality(
                    bitmap = bitmap,
                    toothPixelCount = segResult.totalToothPixels
                )

                // 3. Robust Color Extraction
                val overallStats = RobustColorExtractor.extractRobustLab(segResult.toothLabPixels)
                val cervicalStats = RobustColorExtractor.extractRobustLab(segResult.cervicalZone.labList)
                val middleStats = RobustColorExtractor.extractRobustLab(segResult.middleZone.labList)
                val incisalStats = RobustColorExtractor.extractRobustLab(segResult.incisalZone.labList)

                if (overallStats == null || !segResult.isToothDetected) {
                    withContext(Dispatchers.Main) {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            qualityEvaluation = quality,
                            segmentationResult = segResult,
                            errorMessage = "Tooth structure could not be reliably segmented. Ensure tooth is centered within the target oval reticle."
                        )
                    }
                    return@launch
                }

                // 4. Color Science CIEDE2000 Matching
                val report = ShadeMatcher.matchShade(
                    measuredLab = overallStats.medianLab,
                    cervicalLab = cervicalStats?.medianLab,
                    middleLab = middleStats?.medianLab,
                    incisalLab = incisalStats?.medianLab,
                    colorDispersionMad = overallStats.compositeMad
                )

                // 5. Persist image file locally (offline privacy)
                val imageFile = saveBitmapToLocalFiles(context, bitmap, currentCase.id)

                // 6. Persist to Database
                val calibProfile = _uiState.value.deviceValidation?.profile ?: CalibrationRegistry.GENERIC_PROFILE
                val metadataJson = buildMetadataString(bitmap)

                val captureEntity = CaptureEntity(
                    caseId = currentCase.id,
                    imagePath = imageFile.absolutePath,
                    deviceId = Build.MODEL ?: "Generic",
                    cameraId = "Back Wide Angle",
                    calibrationProfileId = calibProfile.id,
                    hardwareProfileId = calibProfile.hardwareSetup,
                    metadataJson = metadataJson,
                    qualityStatus = quality.status.name
                )
                val captureId = repository.saveCapture(captureEntity)

                val analysisEntity = ShadeAnalysisEntity(
                    captureId = captureId,
                    overallL = overallStats.medianLab.l,
                    overallA = overallStats.medianLab.a,
                    overallB = overallStats.medianLab.b,
                    cervicalL = cervicalStats?.medianLab?.l ?: 0.0,
                    cervicalA = cervicalStats?.medianLab?.a ?: 0.0,
                    cervicalB = cervicalStats?.medianLab?.b ?: 0.0,
                    middleL = middleStats?.medianLab?.l ?: 0.0,
                    middleA = middleStats?.medianLab?.a ?: 0.0,
                    middleB = middleStats?.medianLab?.b ?: 0.0,
                    incisalL = incisalStats?.medianLab?.l ?: 0.0,
                    incisalA = incisalStats?.medianLab?.a ?: 0.0,
                    incisalB = incisalStats?.medianLab?.b ?: 0.0,
                    recommendedShade = report.recommendedShade.shadeCode,
                    secondShade = report.secondShade.shadeCode,
                    thirdShade = report.thirdShade.shadeCode,
                    deltaE1 = report.recommendedShade.deltaE00,
                    deltaE2 = report.secondShade.deltaE00,
                    deltaE3 = report.thirdShade.deltaE00,
                    qualityScore = quality.blurScore,
                    qualityStatus = quality.status.name,
                    algorithmVersion = VitaClassicalData.ALGORITHM_VERSION,
                    isAmbiguous = report.isAmbiguous
                )
                repository.saveShadeAnalysis(analysisEntity)

                withContext(Dispatchers.Main) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        activeCapture = captureEntity,
                        activeAnalysis = analysisEntity,
                        comprehensiveReport = report,
                        qualityEvaluation = quality,
                        segmentationResult = segResult,
                        userNotice = if (report.isAmbiguous) report.ambiguityMessage else "Shade calculation complete."
                    )
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "Analysis error: ${e.localizedMessage ?: "Unknown failure"}"
                    )
                }
            }
        }
    }

    fun confirmAlgorithmShade() {
        val currentCase = _uiState.value.activeCase ?: return
        val report = _uiState.value.comprehensiveReport ?: return

        viewModelScope.launch {
            repository.saveFinalShade(
                caseId = currentCase.id,
                algorithmShade = report.recommendedShade.shadeCode,
                dentistSelectedShade = report.recommendedShade.shadeCode,
                overrideReason = null
            )
            val updated = repository.getFinalShade(currentCase.id)
            _uiState.value = _uiState.value.copy(
                finalShade = updated,
                userNotice = "Shade ${report.recommendedShade.shadeCode} confirmed by dentist."
            )
        }
    }

    fun overrideShade(overrideShadeCode: String, reason: String) {
        val currentCase = _uiState.value.activeCase ?: return
        val report = _uiState.value.comprehensiveReport ?: return

        viewModelScope.launch {
            repository.saveFinalShade(
                caseId = currentCase.id,
                algorithmShade = report.recommendedShade.shadeCode,
                dentistSelectedShade = overrideShadeCode,
                overrideReason = reason.trim()
            )
            val updated = repository.getFinalShade(currentCase.id)
            _uiState.value = _uiState.value.copy(
                finalShade = updated,
                userNotice = "Clinical override saved: ${overrideShadeCode}."
            )
        }
    }

    fun generateLabPdf(context: Context, dentistName: String = "", clinicName: String = ""): File? {
        if (_uiState.value.deviceValidation?.isPreciseAnalysisAllowed != true ||
            VitaClassicalData.REFERENCE_STATUS == "REQUIRES_VALIDATED_DATA") {
            _uiState.value = _uiState.value.copy(errorMessage = "A shade report cannot be generated without validated calibration and reference data.")
            return null
        }
        val currentCase = _uiState.value.activeCase ?: return null
        val currentPatient = _uiState.value.selectedPatient ?: return null
        val report = _uiState.value.comprehensiveReport ?: return null
        val finalShade = _uiState.value.finalShade

        val dentistShade = finalShade?.dentistSelectedShade ?: report.recommendedShade.shadeCode
        val overrideReason = finalShade?.overrideReason

        val calibProfile = _uiState.value.deviceValidation?.profile ?: CalibrationRegistry.GENERIC_PROFILE

        // Ensure clinical photo is available
        var photoBitmap = _uiState.value.currentCapturedBitmap
        if (photoBitmap == null && _uiState.value.activeCapture?.imagePath?.isNotBlank() == true) {
            val imgFile = File(_uiState.value.activeCapture!!.imagePath)
            if (imgFile.exists()) {
                photoBitmap = BitmapFactory.decodeFile(imgFile.absolutePath)
            }
        }

        val reportData = LabReportData(
            patient = currentPatient,
            dentalCase = currentCase,
            dentistName = dentistName,
            clinicName = clinicName,
            primaryShade = report.recommendedShade,
            secondShade = report.secondShade,
            thirdShade = report.thirdShade,
            dentistFinalShade = dentistShade,
            overrideReason = overrideReason,
            overallLab = report.overallLab,
            zones = report.zones,
            qualityStatus = _uiState.value.qualityEvaluation?.status?.name ?: "NOT_EVALUATED",
            qualityNotes = _uiState.value.qualityEvaluation?.issues?.joinToString("; ") { it.message } ?: "Quality evaluation unavailable",
            deviceModel = Build.MODEL ?: "Generic Smartphone",
            cameraId = _uiState.value.activeCapture?.cameraId ?: "Unknown camera",
            calibrationProfileId = calibProfile.id,
            algorithmVersion = report.algorithmVersion,
            clinicalPhoto = photoBitmap
        )

        val file = LabReportPdfGenerator.generatePdfReport(context, reportData)
        viewModelScope.launch {
            repository.saveLabOrder(
                caseId = currentCase.id,
                shade = dentistShade,
                restoration = currentCase.restorationType,
                notes = "Auto-generated digital lab prescription",
                pdfPath = file.absolutePath
            )
        }
        _uiState.value = _uiState.value.copy(
            latestGeneratedPdf = file,
            currentCapturedBitmap = photoBitmap ?: _uiState.value.currentCapturedBitmap,
            userNotice = "Laboratory PDF generated: ${file.name}"
        )
        return file
    }

    /**
     * Saves the latest PDF report directly to the device's public Downloads folder.
     */
    fun savePdfToDownloads(context: Context): String? {
        val pdfFile = _uiState.value.latestGeneratedPdf ?: generateLabPdf(context)
        if (pdfFile == null || !pdfFile.exists()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Could not generate PDF to save.")
            return null
        }

        val savedPath = LabReportPdfGenerator.savePdfToPublicDownloads(context, pdfFile)
        if (savedPath != null) {
            _uiState.value = _uiState.value.copy(
                savedPdfDownloadPath = savedPath,
                userNotice = "PDF successfully saved to: $savedPath"
            )
        } else {
            _uiState.value = _uiState.value.copy(errorMessage = "Could not save PDF to Downloads directory.")
        }
        return savedPath
    }

    fun clearNotice() {
        _uiState.value = _uiState.value.copy(userNotice = null, errorMessage = null, savedPdfDownloadPath = null)
    }

    private fun saveBitmapToLocalFiles(context: Context, bitmap: Bitmap, caseId: Long): File {
        val dir = File(context.filesDir, "captures")
        if (!dir.exists()) dir.mkdirs()
        val file = File(dir, "capture_case_${caseId}_${System.currentTimeMillis()}.png")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 95, out)
        }
        return file
    }

    private fun buildMetadataString(bitmap: Bitmap): String {
        return "Model: ${Build.MODEL}, Brand: ${Build.MANUFACTURER}, OS: Android ${Build.VERSION.RELEASE}, Resolution: ${bitmap.width}x${bitmap.height}, WhiteBalance: UNKNOWN"
    }
}
