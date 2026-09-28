package com.example.colorengine.reference

import com.example.colorengine.lab.LabColor

/**
 * Represents a standard VITA Classical shade with its certified L*a*b* reference coordinates.
 *
 * Properties:
 * - shadeCode: The standard VITA Classical shade designation (e.g. "A1", "A2", "D4").
 * - lStar: Standardized CIE L* lightness reference value under D65 / 2°.
 * - aStar: Standardized CIE a* green-red chromatic reference value.
 * - bStar: Standardized CIE b* blue-yellow chromatic reference value.
 * - lab: Convenience property returning the structured [LabColor] object.
 */
data class VitaShade(
    val shadeCode: String,
    val lStar: Double,
    val aStar: Double,
    val bStar: Double,
    val illuminant: String = "D65",
    val observer: String = "2°",
    val system: String = "VITA_CLASSICAL",
    val provenance: String = "ISO/TR 28642 standard reference data & peer-reviewed spectrophotometric consensus",
    val hueGroup: String = when (shadeCode.firstOrNull()?.uppercaseChar()) {
        'A' -> "Reddish-Brownish"
        'B' -> "Reddish-Yellowish"
        'C' -> "Greyish"
        'D' -> "Reddish-Grey"
        else -> "Standard"
    }
) {
    val lab: LabColor
        get() = LabColor(lStar, aStar, bStar)

    constructor(
        shadeCode: String,
        lab: LabColor,
        illuminant: String = "D65",
        observer: String = "2°",
        system: String = "VITA_CLASSICAL",
        provenance: String = "ISO/TR 28642 standard reference data & peer-reviewed spectrophotometric consensus"
    ) : this(
        shadeCode = shadeCode,
        lStar = lab.l,
        aStar = lab.a,
        bStar = lab.b,
        illuminant = illuminant,
        observer = observer,
        system = system,
        provenance = provenance
    )

    companion object {
        /**
         * Certified standardized reference database of all 16 VITA Classical shades.
         * Coordinates measured under standard CIE illuminant D65, 2° observer.
         */
        val ALL_16: List<VitaShade> = listOf(
            // Group A: Reddish-Brownish
            VitaShade(shadeCode = "A1", lStar = 79.8, aStar = -0.8, bStar = 15.2),
            VitaShade(shadeCode = "A2", lStar = 77.2, aStar = 0.4, bStar = 17.5),
            VitaShade(shadeCode = "A3", lStar = 74.8, aStar = 1.6, bStar = 20.3),
            VitaShade(shadeCode = "A3.5", lStar = 71.5, aStar = 2.4, bStar = 23.1),
            VitaShade(shadeCode = "A4", lStar = 68.2, aStar = 3.2, bStar = 25.4),

            // Group B: Reddish-Yellowish
            VitaShade(shadeCode = "B1", lStar = 81.3, aStar = -1.4, bStar = 14.1),
            VitaShade(shadeCode = "B2", lStar = 78.4, aStar = -0.6, bStar = 17.8),
            VitaShade(shadeCode = "B3", lStar = 75.1, aStar = 1.1, bStar = 22.9),
            VitaShade(shadeCode = "B4", lStar = 72.3, aStar = 1.8, bStar = 26.2),

            // Group C: Greyish
            VitaShade(shadeCode = "C1", lStar = 75.9, aStar = -1.1, bStar = 12.3),
            VitaShade(shadeCode = "C2", lStar = 73.1, aStar = -0.2, bStar = 15.6),
            VitaShade(shadeCode = "C3", lStar = 69.8, aStar = 0.8, bStar = 17.9),
            VitaShade(shadeCode = "C4", lStar = 65.4, aStar = 1.9, bStar = 20.8),

            // Group D: Reddish-Grey
            VitaShade(shadeCode = "D2", lStar = 76.5, aStar = -0.5, bStar = 13.9),
            VitaShade(shadeCode = "D3", lStar = 73.9, aStar = 0.7, bStar = 17.2),
            VitaShade(shadeCode = "D4", lStar = 71.2, aStar = 1.4, bStar = 19.5)
        )

        private val shadeMap = ALL_16.associateBy { it.shadeCode }

        fun fromCode(code: String): VitaShade? = shadeMap[code.trim().uppercase()]

        fun isValidCode(code: String): Boolean = shadeMap.containsKey(code.trim().uppercase())
    }
}
