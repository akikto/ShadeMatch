package com.example.colorengine.reference

import com.example.colorengine.lab.LabColor

/**
 * Certified VITA Classical A1-D4 reference dataset (16 shades).
 * Reference standard: ISO/TR 28642 under standard illuminant D65, 2° observer.
 */
object VitaClassicalDataset {

    const val SHADE_SYSTEM = "VITA_CLASSICAL"
    const val REFERENCE_VERSION = "2024.1"

    val ALL_16_SHADES: List<VitaClassicalReference> = listOf(
        // Group A: Reddish-Brownish
        VitaClassicalReference(
            shadeCode = "A1",
            lab = LabColor(l = 79.8, a = -0.8, b = 15.2)
        ),
        VitaClassicalReference(
            shadeCode = "A2",
            lab = LabColor(l = 77.2, a = 0.4, b = 17.5)
        ),
        VitaClassicalReference(
            shadeCode = "A3",
            lab = LabColor(l = 74.8, a = 1.6, b = 20.3)
        ),
        VitaClassicalReference(
            shadeCode = "A3.5",
            lab = LabColor(l = 71.5, a = 2.4, b = 23.1)
        ),
        VitaClassicalReference(
            shadeCode = "A4",
            lab = LabColor(l = 68.2, a = 3.2, b = 25.4)
        ),

        // Group B: Reddish-Yellowish
        VitaClassicalReference(
            shadeCode = "B1",
            lab = LabColor(l = 81.3, a = -1.4, b = 14.1)
        ),
        VitaClassicalReference(
            shadeCode = "B2",
            lab = LabColor(l = 78.4, a = -0.6, b = 17.8)
        ),
        VitaClassicalReference(
            shadeCode = "B3",
            lab = LabColor(l = 75.1, a = 1.1, b = 22.9)
        ),
        VitaClassicalReference(
            shadeCode = "B4",
            lab = LabColor(l = 72.3, a = 1.8, b = 26.2)
        ),

        // Group C: Greyish
        VitaClassicalReference(
            shadeCode = "C1",
            lab = LabColor(l = 75.9, a = -1.1, b = 12.3)
        ),
        VitaClassicalReference(
            shadeCode = "C2",
            lab = LabColor(l = 73.1, a = -0.2, b = 15.6)
        ),
        VitaClassicalReference(
            shadeCode = "C3",
            lab = LabColor(l = 69.8, a = 0.8, b = 17.9)
        ),
        VitaClassicalReference(
            shadeCode = "C4",
            lab = LabColor(l = 65.4, a = 1.9, b = 20.8)
        ),

        // Group D: Reddish-Grey
        VitaClassicalReference(
            shadeCode = "D2",
            lab = LabColor(l = 76.5, a = -0.5, b = 13.9)
        ),
        VitaClassicalReference(
            shadeCode = "D3",
            lab = LabColor(l = 73.9, a = 0.7, b = 17.2)
        ),
        VitaClassicalReference(
            shadeCode = "D4",
            lab = LabColor(l = 71.2, a = 1.4, b = 19.5)
        )
    )

    private val shadeMap: Map<String, VitaClassicalReference> = ALL_16_SHADES.associateBy { it.shadeCode }

    fun getByCode(code: String): VitaClassicalReference? = shadeMap[code]

    fun containsShade(code: String): Boolean = shadeMap.containsKey(code)
}
