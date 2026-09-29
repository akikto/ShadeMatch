package com.example.colorengine.reference

import com.example.colorengine.lab.LabColor

/**
 * VITA Classical shade record. Default coordinates have no verified measurement provenance.
 */
data class VitaClassicalReference(
    val shadeCode: String,
    val lab: LabColor,
    val illuminant: String = "D65",
    val observer: String = "2°",
    val instrument: String = "UNKNOWN",
    val guideId: String = "VITA_CLASSICAL_UNVERIFIED",
    val referenceVersion: String = "UNVERIFIED",
    val measurementDate: String = "UNKNOWN",
    val source: String = "REQUIRES_VALIDATED_DATA",
    val provenance: String = "REQUIRES_VALIDATED_DATA",
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
