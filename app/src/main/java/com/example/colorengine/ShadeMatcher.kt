package com.example.colorengine

import kotlin.math.abs

data class ToothZoneShades(
    val cervical: ShadeAnalysisZone,
    val middle: ShadeAnalysisZone,
    val incisal: ShadeAnalysisZone
)

data class ShadeAnalysisZone(
    val zoneName: String,
    val lab: Lab,
    val topMatch: ShadeMatchResult,
    val dispersionMad: Double = 0.0
)

data class ComprehensiveShadeReport(
    val overallLab: Lab,
    val topMatches: List<ShadeMatchResult>,
    val recommendedShade: ShadeMatchResult,
    val secondShade: ShadeMatchResult,
    val thirdShade: ShadeMatchResult,
    val isAmbiguous: Boolean,
    val ambiguityMessage: String?,
    val zones: ToothZoneShades?,
    val perceptibilityStatus: String, // "EXCELLENT" (<=0.8), "ACCEPTABLE" (<=1.8), "MODERATE" (<=3.2), "SUBOPTIMAL" (>3.2)
    val colorDispersionMad: Double = 0.0,
    val algorithmVersion: String = VitaClassicalData.ALGORITHM_VERSION
)

object ShadeMatcher {

    // Dental Color Science Standard Thresholds (Paravina et al., J Dent 2015)
    const val PERCEPTIBILITY_THRESHOLD_PT = 0.8  // ΔE00 <= 0.8 (Imperceptible to human eye)
    const val ACCEPTABILITY_THRESHOLD_AT = 1.8  // ΔE00 <= 1.8 (Clinically acceptable threshold)
    const val MODERATE_THRESHOLD = 3.2          // 1.8 < ΔE00 <= 3.2 (Noticeable, may require adjustment)
    const val AMBIGUITY_DELTA_THRESHOLD = 0.60  // Difference between #1 and #2 candidate

    /**
     * Matches a measured Lab against all 16 VITA Classical reference shades.
     */
    fun matchShade(
        measuredLab: Lab,
        cervicalLab: Lab? = null,
        middleLab: Lab? = null,
        incisalLab: Lab? = null,
        colorDispersionMad: Double = 0.0
    ): ComprehensiveShadeReport {
        return ColorEngine.DEFAULT.matchToothColor(
            measuredLab = measuredLab,
            cervicalLab = cervicalLab,
            middleLab = middleLab,
            incisalLab = incisalLab,
            dispersionMad = colorDispersionMad
        )
    }
}
