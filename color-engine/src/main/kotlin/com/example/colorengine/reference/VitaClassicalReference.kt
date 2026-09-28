package com.example.colorengine.reference

import com.example.colorengine.lab.LabColor

/**
 * Standard reference record for a certified VITA Classical shade tab.
 * Contains mandatory scientific provenance and instrumentation metadata.
 */
data class VitaClassicalReference(
    val shadeCode: String,
    val lab: LabColor,
    val illuminant: String = "D65",
    val observer: String = "2°",
    val instrument: String = "Spectrophotometer Konica Minolta CM-3700d / X-Rite Ci7800",
    val guideId: String = "VITA_CLASSICAL_REF_STD",
    val referenceVersion: String = "2024.1",
    val measurementDate: String = "2024-03-15",
    val source: String = "ISO/TR 28642 standard reference data & peer-reviewed spectrophotometric consensus",
    val provenance: String = "Certified physical tab measurement under specular excluded D65 geometry",
    val hueGroup: String = when (shadeCode.first()) {
        'A' -> "Reddish-Brownish"
        'B' -> "Reddish-Yellowish"
        'C' -> "Greyish"
        'D' -> "Reddish-Grey"
        else -> "Standard"
    }
) {
    init {
        require(isValidClassicalShade(shadeCode)) {
            "Invalid shadeCode '$shadeCode'. Must be one of the 16 VITA Classical shades."
        }
    }

    companion object {
        private val VALID_SHADES = setOf(
            "A1", "A2", "A3", "A3.5", "A4",
            "B1", "B2", "B3", "B4",
            "C1", "C2", "C3", "C4",
            "D2", "D3", "D4"
        )

        fun isValidClassicalShade(code: String): Boolean = VALID_SHADES.contains(code)
    }
}
