package com.example.calibration

import com.example.colorengine.Matrix3x3
import com.example.colorengine.VitaClassicalData
import com.example.colorengine.Xyz

enum class CalibrationStatus {
    VALIDATED,
    UNVALIDATED,
    UNSUPPORTED,
    GENERIC_UNCALIBRATED,
    FAILED_TARGET_VERIFICATION,
    EXPIRED
}

data class DeviceValidationResult(
    val status: CalibrationStatus,
    val profile: CalibrationProfile,
    val isPreciseAnalysisAllowed: Boolean,
    val message: String
)

data class CalibrationProfile(
    val id: String,
    val deviceModel: String,
    val cameraId: String,
    val hardwareSetup: String,
    val referenceTarget: String,
    val matrix: Matrix3x3,
    val whitePoint: Xyz = Xyz.D65_2DEG,
    val lut: String? = null,
    val algorithmVersion: String = VitaClassicalData.ALGORITHM_VERSION,
    val validationDate: String,
    val validated: Boolean,
    val notes: String,
    val lensInfo: String = "UNKNOWN",
    val validationStatus: CalibrationStatus = CalibrationStatus.UNVALIDATED
)

object CalibrationRegistry {

    val GENERIC_PROFILE = CalibrationProfile(
        id = "GENERIC_SRGB_CALIB",
        deviceModel = "Generic / Unvalidated",
        cameraId = "UNKNOWN",
        hardwareSetup = "UNKNOWN",
        referenceTarget = "NONE",
        matrix = Matrix3x3.SRGB_TO_XYZ_D65,
        validationDate = "",
        validated = false,
        notes = "sRGB conversion is not a camera calibration. Validation evidence is required.",
        validationStatus = CalibrationStatus.GENERIC_UNCALIBRATED
    )

    // Historical candidate configurations only. No evidence of validation is bundled.
    val CANDIDATE_PROFILES: List<CalibrationProfile> = listOf(
        CalibrationProfile(
            id = "CALIB_PIXEL_8_STD",
            deviceModel = "Pixel 8",
            cameraId = "camera_0_wide",
            hardwareSetup = "50MP 1/1.31\" GN2 sensor + cross-polarization ring light",
            referenceTarget = "Proposed target; verification required",
            matrix = Matrix3x3(
                0.428512, 0.342110, 0.179850,
                0.220140, 0.708910, 0.070950,
                0.018910, 0.114520, 0.955400
            ),
            validationDate = "",
            validated = false,
            notes = "Candidate configuration only. Matrix provenance and validation required.",
            lensInfo = "wide"
        ),
        CalibrationProfile(
            id = "CALIB_GALAXY_S24_STD",
            deviceModel = "SM-S921",
            cameraId = "camera_0_wide",
            hardwareSetup = "50MP ISOCELL GN3 + polarized medical illuminator",
            referenceTarget = "Proposed target; verification required",
            matrix = Matrix3x3(
                0.419500, 0.352100, 0.178870,
                0.215400, 0.712300, 0.072300,
                0.019100, 0.118900, 0.950800
            ),
            validationDate = "",
            validated = false,
            notes = "Candidate configuration only. Matrix provenance and validation required.",
            lensInfo = "wide"
        )
    )

    fun evaluateDevice(currentModel: String, cameraId: String? = null): DeviceValidationResult {
        val matchedProfile = CANDIDATE_PROFILES.find {
            currentModel.equals(it.deviceModel, ignoreCase = true) &&
                cameraId != null && cameraId == it.cameraId
        }

        return if (matchedProfile != null &&
            matchedProfile.validated &&
            matchedProfile.validationStatus == CalibrationStatus.VALIDATED &&
            matchedProfile.validationDate.isNotBlank()
        ) {
            DeviceValidationResult(
                status = CalibrationStatus.VALIDATED,
                profile = matchedProfile,
                isPreciseAnalysisAllowed = VitaClassicalData.REFERENCE_STATUS != "REQUIRES_VALIDATED_DATA",
                message = "Validated camera calibration profile: ${matchedProfile.deviceModel} (${matchedProfile.hardwareSetup})"
            )
        } else {
            DeviceValidationResult(
                status = CalibrationStatus.GENERIC_UNCALIBRATED,
                profile = GENERIC_PROFILE.copy(deviceModel = currentModel),
                isPreciseAnalysisAllowed = false,
                message = "Camera calibration is not validated for this device and camera. Precise shade analysis is unavailable."
            )
        }
    }
}
