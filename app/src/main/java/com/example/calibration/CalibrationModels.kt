package com.example.calibration

import com.example.colorengine.Matrix3x3
import com.example.colorengine.VitaClassicalData
import com.example.colorengine.Xyz

enum class CalibrationStatus {
    VALIDATED,
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
    val notes: String
)

object CalibrationRegistry {

    val GENERIC_PROFILE = CalibrationProfile(
        id = "GENERIC_SRGB_CALIB",
        deviceModel = "Generic / Unvalidated",
        cameraId = "camera_back_0",
        hardwareSetup = "Standard Smartphone Dual/Triple Sensor",
        referenceTarget = "Generic standard observer reference",
        matrix = Matrix3x3.SRGB_TO_XYZ_D65,
        validationDate = "2024-01-01",
        validated = false,
        notes = "Standard generic Bradford sRGB matrix. Non-certified device profile."
    )

    // Pre-calibrated factory profiles for reference testing
    val VALIDATED_PROFILES: List<CalibrationProfile> = listOf(
        CalibrationProfile(
            id = "CALIB_PIXEL_8_STD",
            deviceModel = "Pixel 8",
            cameraId = "camera_0_wide",
            hardwareSetup = "50MP 1/1.31\" GN2 sensor + cross-polarization ring light",
            referenceTarget = "X-Rite ColorChecker Classic & VITA Reference Standard",
            matrix = Matrix3x3(
                0.428512, 0.342110, 0.179850,
                0.220140, 0.708910, 0.070950,
                0.018910, 0.114520, 0.955400
            ),
            validationDate = "2024-02-20",
            validated = true,
            notes = "Factory calibrated with cross-polarization filter under 5500K CRI>95 daylight"
        ),
        CalibrationProfile(
            id = "CALIB_GALAXY_S24_STD",
            deviceModel = "SM-S921",
            cameraId = "camera_0_wide",
            hardwareSetup = "50MP ISOCELL GN3 + polarized medical illuminator",
            referenceTarget = "VITA Classical Master Reference & certified 18% neutral target",
            matrix = Matrix3x3(
                0.419500, 0.352100, 0.178870,
                0.215400, 0.712300, 0.072300,
                0.019100, 0.118900, 0.950800
            ),
            validationDate = "2024-03-01",
            validated = true,
            notes = "Polarized multi-point spectrophotometric cross-match"
        )
    )

    fun evaluateDevice(currentModel: String): DeviceValidationResult {
        val matchedProfile = VALIDATED_PROFILES.find {
            currentModel.contains(it.deviceModel, ignoreCase = true)
        }

        return if (matchedProfile != null && matchedProfile.validated) {
            DeviceValidationResult(
                status = CalibrationStatus.VALIDATED,
                profile = matchedProfile,
                isPreciseAnalysisAllowed = true,
                message = "Validated camera calibration profile: ${matchedProfile.deviceModel} (${matchedProfile.hardwareSetup})"
            )
        } else {
            DeviceValidationResult(
                status = CalibrationStatus.GENERIC_UNCALIBRATED,
                profile = GENERIC_PROFILE.copy(deviceModel = currentModel),
                isPreciseAnalysisAllowed = true,
                message = "Device calibration is unverified for '$currentModel'. Analysis operates in standard uncalibrated estimation mode. Physical certified shade-tab check recommended."
            )
        }
    }
}
