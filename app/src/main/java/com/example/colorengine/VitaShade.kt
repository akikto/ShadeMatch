package com.example.colorengine

/**
 * Represents a VITA Classical shade with unverified illustrative L*a*b* coordinates.
 *
 * Properties:
 * - shadeCode: The standard VITA Classical shade designation (e.g. "A1", "A2", "D4").
 * - lStar: Standardized CIE L* lightness reference value under D65 / 2°.
 * - aStar: Standardized CIE a* green-red chromatic reference value.
 * - bStar: Standardized CIE b* blue-yellow chromatic reference value.
 * - lab: Convenience property returning the structured [Lab] object.
 */
data class VitaShade(
    val shadeCode: String,
    val lStar: Double,
    val aStar: Double,
    val bStar: Double,
    val illuminant: String = "D65",
    val observer: String = "2°",
    val system: String = "VITA_CLASSICAL",
    val provenance: String = "REQUIRES_VALIDATED_DATA",
    val hueGroup: String = when (shadeCode.firstOrNull()?.uppercaseChar()) {
        'A' -> "Reddish-Brownish"
        'B' -> "Reddish-Yellowish"
        'C' -> "Greyish"
        'D' -> "Reddish-Grey"
        else -> "Standard"
    }
) {
    val lab: Lab
        get() = Lab(lStar, aStar, bStar)

    constructor(
        shadeCode: String,
        lab: Lab,
        illuminant: String = "D65",
        observer: String = "2°",
        system: String = "VITA_CLASSICAL",
        provenance: String = "REQUIRES_VALIDATED_DATA"
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
        val ALL_16: List<VitaShade> = listOf(
            VitaShade(shadeCode = "A1", lStar = 79.8, aStar = -0.8, bStar = 15.2),
            VitaShade(shadeCode = "A2", lStar = 77.2, aStar = 0.4, bStar = 17.5),
            VitaShade(shadeCode = "A3", lStar = 74.8, aStar = 1.6, bStar = 20.3),
            VitaShade(shadeCode = "A3.5", lStar = 71.5, aStar = 2.4, bStar = 23.1),
            VitaShade(shadeCode = "A4", lStar = 68.2, aStar = 3.2, bStar = 25.4),
            VitaShade(shadeCode = "B1", lStar = 81.3, aStar = -1.4, bStar = 14.1),
            VitaShade(shadeCode = "B2", lStar = 78.4, aStar = -0.6, bStar = 17.8),
            VitaShade(shadeCode = "B3", lStar = 75.1, aStar = 1.1, bStar = 22.9),
            VitaShade(shadeCode = "B4", lStar = 72.3, aStar = 1.8, bStar = 26.2),
            VitaShade(shadeCode = "C1", lStar = 75.9, aStar = -1.1, bStar = 12.3),
            VitaShade(shadeCode = "C2", lStar = 73.1, aStar = -0.2, bStar = 15.6),
            VitaShade(shadeCode = "C3", lStar = 69.8, aStar = 0.8, bStar = 17.9),
            VitaShade(shadeCode = "C4", lStar = 65.4, aStar = 1.9, bStar = 20.8),
            VitaShade(shadeCode = "D2", lStar = 76.5, aStar = -0.5, bStar = 13.9),
            VitaShade(shadeCode = "D3", lStar = 73.9, aStar = 0.7, bStar = 17.2),
            VitaShade(shadeCode = "D4", lStar = 71.2, aStar = 1.4, bStar = 19.5)
        )

        private val shadeMap = ALL_16.associateBy { it.shadeCode }

        fun fromCode(code: String): VitaShade? = shadeMap[code.trim().uppercase()]

        fun isValidCode(code: String): Boolean = shadeMap.containsKey(code.trim().uppercase())
    }
}
