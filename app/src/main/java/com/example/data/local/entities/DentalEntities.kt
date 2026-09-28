package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "patients")
data class PatientEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val patientCode: String,
    val name: String,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "cases",
    foreignKeys = [
        ForeignKey(
            entity = PatientEntity::class,
            parentColumns = ["id"],
            childColumns = ["patientId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["patientId"])]
)
data class DentalCaseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val patientId: Long,
    val toothNumber: String,
    val restorationType: String,
    val shadeSystem: String = "VITA_CLASSICAL",
    val status: String = "NEW",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "captures",
    foreignKeys = [
        ForeignKey(
            entity = DentalCaseEntity::class,
            parentColumns = ["id"],
            childColumns = ["caseId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["caseId"])]
)
data class CaptureEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val caseId: Long,
    val imagePath: String,
    val rawImagePath: String? = null,
    val deviceId: String,
    val cameraId: String,
    val calibrationProfileId: String,
    val hardwareProfileId: String,
    val metadataJson: String,
    val qualityStatus: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "shade_analyses",
    foreignKeys = [
        ForeignKey(
            entity = CaptureEntity::class,
            parentColumns = ["id"],
            childColumns = ["captureId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["captureId"])]
)
data class ShadeAnalysisEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val captureId: Long,
    val overallL: Double,
    val overallA: Double,
    val overallB: Double,
    val cervicalL: Double = 0.0,
    val cervicalA: Double = 0.0,
    val cervicalB: Double = 0.0,
    val middleL: Double = 0.0,
    val middleA: Double = 0.0,
    val middleB: Double = 0.0,
    val incisalL: Double = 0.0,
    val incisalA: Double = 0.0,
    val incisalB: Double = 0.0,
    val recommendedShade: String,
    val secondShade: String,
    val thirdShade: String,
    val deltaE1: Double,
    val deltaE2: Double,
    val deltaE3: Double,
    val qualityScore: Double,
    val qualityStatus: String,
    val algorithmVersion: String,
    val isAmbiguous: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "final_shades",
    foreignKeys = [
        ForeignKey(
            entity = DentalCaseEntity::class,
            parentColumns = ["id"],
            childColumns = ["caseId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class FinalShadeEntity(
    @PrimaryKey
    val caseId: Long,
    val algorithmShade: String,
    val dentistSelectedShade: String,
    val overrideReason: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "lab_orders",
    foreignKeys = [
        ForeignKey(
            entity = DentalCaseEntity::class,
            parentColumns = ["id"],
            childColumns = ["caseId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["caseId"])]
)
data class LabOrderEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val caseId: Long,
    val shade: String,
    val restoration: String,
    val notes: String = "",
    val pdfPath: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "shade_references")
data class ShadeReferenceEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val system: String = "VITA_CLASSICAL",
    val shadeCode: String,
    val lStar: Double,
    val aStar: Double,
    val bStar: Double,
    val illuminant: String = "D65",
    val observer: String = "2°",
    val instrument: String,
    val guideId: String,
    val referenceVersion: String,
    val measurementDate: String,
    val source: String,
    val provenance: String
)

@Entity(tableName = "calibration_profiles")
data class CalibrationProfileEntity(
    @PrimaryKey
    val id: String,
    val deviceModel: String,
    val cameraId: String,
    val hardwareSetup: String,
    val matrixJson: String,
    val whitePointJson: String,
    val lut: String? = null,
    val algorithmVersion: String,
    val validated: Boolean,
    val validationDate: String,
    val referenceTarget: String,
    val notes: String
)
