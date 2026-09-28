package com.example.data.repository

import com.example.data.local.DentalShadeDatabase
import com.example.data.local.entities.*
import kotlinx.coroutines.flow.Flow

class DentalRepository(private val db: DentalShadeDatabase) {

    val allPatientsFlow: Flow<List<PatientEntity>> = db.patientDao().getAllPatientsFlow()

    suspend fun getPatient(id: Long): PatientEntity? = db.patientDao().getPatientById(id)

    suspend fun createPatient(patientCode: String, name: String, notes: String): Long {
        return db.patientDao().insertPatient(
            PatientEntity(
                patientCode = patientCode.trim(),
                name = name.trim(),
                notes = notes.trim()
            )
        )
    }

    suspend fun deletePatient(patient: PatientEntity) {
        db.patientDao().deletePatient(patient)
    }

    fun getCasesForPatientFlow(patientId: Long): Flow<List<DentalCaseEntity>> =
        db.dentalCaseDao().getCasesForPatientFlow(patientId)

    suspend fun getCase(id: Long): DentalCaseEntity? = db.dentalCaseDao().getCaseById(id)

    suspend fun createCase(
        patientId: Long,
        toothNumber: String,
        restorationType: String,
        shadeSystem: String = "VITA_CLASSICAL"
    ): Long {
        return db.dentalCaseDao().insertCase(
            DentalCaseEntity(
                patientId = patientId,
                toothNumber = toothNumber,
                restorationType = restorationType,
                shadeSystem = shadeSystem,
                status = "NEW"
            )
        )
    }

    suspend fun deleteCase(dentalCase: DentalCaseEntity) {
        db.dentalCaseDao().deleteCase(dentalCase)
    }

    suspend fun saveCapture(capture: CaptureEntity): Long {
        return db.captureDao().insertCapture(capture)
    }

    suspend fun getLatestCapture(caseId: Long): CaptureEntity? {
        return db.captureDao().getLatestCaptureForCase(caseId)
    }

    suspend fun saveShadeAnalysis(analysis: ShadeAnalysisEntity): Long {
        return db.shadeAnalysisDao().insertAnalysis(analysis)
    }

    suspend fun getAnalysisForCapture(captureId: Long): ShadeAnalysisEntity? {
        return db.shadeAnalysisDao().getAnalysisForCapture(captureId)
    }

    suspend fun saveFinalShade(
        caseId: Long,
        algorithmShade: String,
        dentistSelectedShade: String,
        overrideReason: String? = null
    ) {
        db.finalShadeDao().saveFinalShade(
            FinalShadeEntity(
                caseId = caseId,
                algorithmShade = algorithmShade,
                dentistSelectedShade = dentistSelectedShade,
                overrideReason = overrideReason
            )
        )
        db.dentalCaseDao().updateCaseStatus(caseId, "CONFIRMED")
    }

    suspend fun getFinalShade(caseId: Long): FinalShadeEntity? {
        return db.finalShadeDao().getFinalShadeForCase(caseId)
    }

    fun getFinalShadeFlow(caseId: Long): Flow<FinalShadeEntity?> {
        return db.finalShadeDao().getFinalShadeForCaseFlow(caseId)
    }

    suspend fun saveLabOrder(
        caseId: Long,
        shade: String,
        restoration: String,
        notes: String,
        pdfPath: String
    ): Long {
        val orderId = db.labOrderDao().insertLabOrder(
            LabOrderEntity(
                caseId = caseId,
                shade = shade,
                restoration = restoration,
                notes = notes,
                pdfPath = pdfPath
            )
        )
        db.dentalCaseDao().updateCaseStatus(caseId, "ORDER_GENERATED")
        return orderId
    }

    suspend fun getLabOrder(caseId: Long): LabOrderEntity? {
        return db.labOrderDao().getLabOrderForCase(caseId)
    }
}
