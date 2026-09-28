package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.calibration.CalibrationRegistry
import com.example.colorengine.VitaClassicalData
import com.example.data.local.dao.*
import com.example.data.local.entities.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        PatientEntity::class,
        DentalCaseEntity::class,
        CaptureEntity::class,
        ShadeAnalysisEntity::class,
        FinalShadeEntity::class,
        LabOrderEntity::class,
        ShadeReferenceEntity::class,
        CalibrationProfileEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class DentalShadeDatabase : RoomDatabase() {

    abstract fun patientDao(): PatientDao
    abstract fun dentalCaseDao(): DentalCaseDao
    abstract fun captureDao(): CaptureDao
    abstract fun shadeAnalysisDao(): ShadeAnalysisDao
    abstract fun finalShadeDao(): FinalShadeDao
    abstract fun labOrderDao(): LabOrderDao
    abstract fun shadeReferenceDao(): ShadeReferenceDao
    abstract fun calibrationProfileDao(): CalibrationProfileDao

    companion object {
        @Volatile
        private var INSTANCE: DentalShadeDatabase? = null

        fun getDatabase(context: Context): DentalShadeDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    DentalShadeDatabase::class.java,
                    "dental_shade_matching.db"
                )
                    .addCallback(DatabaseCallback())
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class DatabaseCallback : Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                CoroutineScope(Dispatchers.IO).launch {
                    seedReferenceData(database)
                }
            }
        }

        private suspend fun seedReferenceData(database: DentalShadeDatabase) {
            // Seed VITA Classical references
            val vitaRefs = VitaClassicalData.ALL_16_SHADES.map { ref ->
                ShadeReferenceEntity(
                    system = VitaClassicalData.SHADE_SYSTEM,
                    shadeCode = ref.shadeCode,
                    lStar = ref.lab.l,
                    aStar = ref.lab.a,
                    bStar = ref.lab.b,
                    illuminant = ref.illuminant,
                    observer = ref.observer,
                    instrument = ref.instrument,
                    guideId = ref.guideId,
                    referenceVersion = ref.referenceVersion,
                    measurementDate = ref.measurementDate,
                    source = ref.source,
                    provenance = ref.provenance
                )
            }
            database.shadeReferenceDao().insertAll(vitaRefs)

            // Seed calibration profiles
            val calibProfiles = listOf(
                CalibrationProfileEntity(
                    id = CalibrationRegistry.GENERIC_PROFILE.id,
                    deviceModel = CalibrationRegistry.GENERIC_PROFILE.deviceModel,
                    cameraId = CalibrationRegistry.GENERIC_PROFILE.cameraId,
                    hardwareSetup = CalibrationRegistry.GENERIC_PROFILE.hardwareSetup,
                    matrixJson = "sRGB_D65_Bradford_Standard",
                    whitePointJson = "D65_2deg",
                    algorithmVersion = CalibrationRegistry.GENERIC_PROFILE.algorithmVersion,
                    validated = false,
                    validationDate = CalibrationRegistry.GENERIC_PROFILE.validationDate,
                    referenceTarget = CalibrationRegistry.GENERIC_PROFILE.referenceTarget,
                    notes = CalibrationRegistry.GENERIC_PROFILE.notes
                )
            ) + CalibrationRegistry.VALIDATED_PROFILES.map { profile ->
                CalibrationProfileEntity(
                    id = profile.id,
                    deviceModel = profile.deviceModel,
                    cameraId = profile.cameraId,
                    hardwareSetup = profile.hardwareSetup,
                    matrixJson = "Calibrated_Matrix_${profile.deviceModel}",
                    whitePointJson = "D65_2deg",
                    algorithmVersion = profile.algorithmVersion,
                    validated = profile.validated,
                    validationDate = profile.validationDate,
                    referenceTarget = profile.referenceTarget,
                    notes = profile.notes
                )
            }
            database.calibrationProfileDao().insertAll(calibProfiles)

            // Seed sample patient and initial case to give immediate clinical utility
            val samplePatientId = database.patientDao().insertPatient(
                PatientEntity(
                    patientCode = "PT-8021",
                    name = "Eleanor Vance",
                    notes = "Upper anterior aesthetic veneer consultation."
                )
            )
            database.dentalCaseDao().insertCase(
                DentalCaseEntity(
                    patientId = samplePatientId,
                    toothNumber = "11 (Maxillary Right Central)",
                    restorationType = "Ceramic Veneer",
                    shadeSystem = "VITA_CLASSICAL",
                    status = "NEW"
                )
            )
        }
    }
}
