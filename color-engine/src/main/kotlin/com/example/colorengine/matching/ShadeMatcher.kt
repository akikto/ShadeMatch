package com.example.colorengine.matching

import com.example.colorengine.ciede2000.Ciede2000Calculator
import com.example.colorengine.lab.LabColor
import com.example.colorengine.reference.VitaClassicalDataset
import kotlin.math.abs

object ShadeMatcher {

    // Dental Color Science Standards (Paravina et al., J Dent 2015)
    const val PERCEPTIBILITY_THRESHOLD_PT = 0.8  // ΔE00 <= 0.8: Imperceptible to 50% of human observers
    const val ACCEPTABILITY_THRESHOLD_AT = 1.8  // ΔE00 <= 1.8: Clinically acceptable threshold
    const val MODERATE_THRESHOLD = 3.2          // 1.8 < ΔE00 <= 3.2: Moderate discrepancy
    const val AMBIGUITY_DELTA_THRESHOLD = 0.60  // ΔE difference between candidate #1 and #2

    fun rankAllShades(measuredLab: LabColor): List<ShadeMatchResult> {
        val ranked = VitaClassicalDataset.ALL_16_SHADES.map { ref ->
            val de00 = Ciede2000Calculator.calculate(measuredLab, ref.lab)
            Pair(ref, de00)
        }.sortedBy { it.second }

        return ranked.mapIndexed { index, (ref, de00) ->
            ShadeMatchResult(
                shadeCode = ref.shadeCode,
                deltaE00 = (de00 * 100).toInt() / 100.0,
                rank = index + 1,
                referenceLab = ref.lab,
                provenance = ref.provenance
            )
        }
    }

    fun matchShade(measuredLab: LabColor): ComprehensiveMatchReport {
        val ranked = rankAllShades(measuredLab)
        val top1 = ranked[0]
        val top2 = ranked[1]
        val top3 = ranked[2]

        val deltaDiff = abs(top2.deltaE00 - top1.deltaE00)
        val isAmbiguous = deltaDiff < AMBIGUITY_DELTA_THRESHOLD
        val ambiguityMessage = if (isAmbiguous) {
            "Close shade candidates detected (${top1.shadeCode} vs ${top2.shadeCode}, ΔE diff %.2f). Clinical review recommended.".format(deltaDiff)
        } else {
            null
        }

        val perceptibility = when {
            top1.deltaE00 <= PERCEPTIBILITY_THRESHOLD_PT -> "EXCELLENT (ΔE00 ≤ 0.8, Imperceptible)"
            top1.deltaE00 <= ACCEPTABILITY_THRESHOLD_AT -> "Small numerical difference (not clinically validated)"
            top1.deltaE00 <= MODERATE_THRESHOLD -> "MODERATE (1.8 < ΔE00 ≤ 3.2, Review Suggested)"
            else -> "SUBOPTIMAL (ΔE00 > 3.2, Check Lighting/Isolation)"
        }

        return ComprehensiveMatchReport(
            measuredLab = measuredLab,
            topMatches = ranked.take(3),
            recommendedShade = top1,
            secondShade = top2,
            thirdShade = top3,
            isAmbiguous = isAmbiguous,
            ambiguityMessage = ambiguityMessage,
            perceptibilityCategory = perceptibility,
            algorithmVersion = "v1.4.2-CIEDE2000"
        )
    }
}
