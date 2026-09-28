package com.example.data.local.dao

import androidx.room.*
import com.example.data.local.entities.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PatientDao {
    @Query("SELECT * FROM patients ORDER BY updatedAt DESC")
    fun getAllPatientsFlow(): Flow<List<PatientEntity>>

    @Query("SELECT * FROM patients ORDER BY updatedAt DESC")
    suspend fun getAllPatients(): List<PatientEntity>

    @Query("SELECT * FROM patients WHERE id = :id LIMIT 1")
    suspend fun getPatientById(id: Long): PatientEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPatient(patient: PatientEntity): Long

    @Update
    suspend fun updatePatient(patient: PatientEntity)

    @Delete
    suspend fun deletePatient(patient: PatientEntity)
}

@Dao
interface DentalCaseDao {
    @Query("SELECT * FROM cases WHERE patientId = :patientId ORDER BY createdAt DESC")
    fun getCasesForPatientFlow(patientId: Long): Flow<List<DentalCaseEntity>>

    @Query("SELECT * FROM cases WHERE patientId = :patientId ORDER BY createdAt DESC")
    suspend fun getCasesForPatient(patientId: Long): List<DentalCaseEntity>

    @Query("SELECT * FROM cases WHERE id = :id LIMIT 1")
    suspend fun getCaseById(id: Long): DentalCaseEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCase(dentalCase: DentalCaseEntity): Long

    @Update
    suspend fun updateCase(dentalCase: DentalCaseEntity)

    @Query("UPDATE cases SET status = :status, updatedAt = :timestamp WHERE id = :caseId")
    suspend fun updateCaseStatus(caseId: Long, status: String, timestamp: Long = System.currentTimeMillis())

    @Delete
    suspend fun deleteCase(dentalCase: DentalCaseEntity)
}

@Dao
interface CaptureDao {
    @Query("SELECT * FROM captures WHERE caseId = :caseId ORDER BY createdAt DESC LIMIT 1")
    suspend fun getLatestCaptureForCase(caseId: Long): CaptureEntity?

    @Query("SELECT * FROM captures WHERE id = :id LIMIT 1")
    suspend fun getCaptureById(id: Long): CaptureEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCapture(capture: CaptureEntity): Long

    @Delete
    suspend fun deleteCapture(capture: CaptureEntity)
}

@Dao
interface ShadeAnalysisDao {
    @Query("SELECT * FROM shade_analyses WHERE captureId = :captureId LIMIT 1")
    suspend fun getAnalysisForCapture(captureId: Long): ShadeAnalysisEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnalysis(analysis: ShadeAnalysisEntity): Long
}

@Dao
interface FinalShadeDao {
    @Query("SELECT * FROM final_shades WHERE caseId = :caseId LIMIT 1")
    suspend fun getFinalShadeForCase(caseId: Long): FinalShadeEntity?

    @Query("SELECT * FROM final_shades WHERE caseId = :caseId LIMIT 1")
    fun getFinalShadeForCaseFlow(caseId: Long): Flow<FinalShadeEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveFinalShade(finalShade: FinalShadeEntity)
}

@Dao
interface LabOrderDao {
    @Query("SELECT * FROM lab_orders WHERE caseId = :caseId ORDER BY createdAt DESC LIMIT 1")
    suspend fun getLabOrderForCase(caseId: Long): LabOrderEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLabOrder(order: LabOrderEntity): Long

    @Delete
    suspend fun deleteLabOrder(order: LabOrderEntity)
}

@Dao
interface ShadeReferenceDao {
    @Query("SELECT * FROM shade_references WHERE system = :system")
    suspend fun getReferencesBySystem(system: String = "VITA_CLASSICAL"): List<ShadeReferenceEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(references: List<ShadeReferenceEntity>)
}

@Dao
interface CalibrationProfileDao {
    @Query("SELECT * FROM calibration_profiles WHERE id = :id LIMIT 1")
    suspend fun getProfileById(id: String): CalibrationProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(profiles: List<CalibrationProfileEntity>)
}
