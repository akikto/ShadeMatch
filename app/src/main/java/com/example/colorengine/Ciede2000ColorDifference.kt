package com.example.colorengine

import kotlin.math.*

/**
 * Clinical perceptibility and acceptability levels for dental shade matching
 * as defined by ISO/TR 28642 and peer-reviewed dental literature (Paravina et al., J Dent 2015).
 */
enum class DentalPerceptibilityLevel(
    val code: String,
    val description: String,
    val thresholdLabel: String,
    val isClinicallyAcceptable: Boolean
) {
    IMPERCEPTIBLE(
        code = "PT_PASS",
        description = "Color difference is imperceptible to 50% of observers under standard clinical illumination.",
        thresholdLabel = "ΔE00 ≤ 0.8 (Perceptibility Threshold PT)",
        isClinicallyAcceptable = true
    ),
    CLINICALLY_ACCEPTABLE(
        code = "AT_PASS",
        description = "Color difference is perceptible but clinically acceptable without visual disharmony.",
        thresholdLabel = "0.8 < ΔE00 ≤ 1.8 (Acceptability Threshold AT)",
        isClinicallyAcceptable = true
    ),
    MODERATELY_ACCEPTABLE(
        code = "MODERATE",
        description = "Perceptible color difference. Acceptable in posterior regions; aesthetic anterior zone may warrant modification.",
        thresholdLabel = "1.8 < ΔE00 ≤ 3.2 (Moderate Discrepancy)",
        isClinicallyAcceptable = false
    ),
    CLINICALLY_UNACCEPTABLE(
        code = "FAIL",
        description = "Noticeable color mismatch requiring ceramic characterization, re-staining, or re-fabrication.",
        thresholdLabel = "ΔE00 > 3.2 (Clinically Unacceptable)",
        isClinicallyAcceptable = false
    )
}

/**
 * Result of comparing captured dental shade data against a VITA Classical reference value.
 */
data class DentalShadeComparisonResult(
    val capturedLab: Lab,
    val matchedShade: VitaShade,
    val deltaE00: Double,
    val deltaLPrime: Double,
    val deltaCPrime: Double,
    val deltaHPrime: Double,
    val rank: Int = 1,
    val perceptibilityLevel: DentalPerceptibilityLevel,
    val isClinicallyAcceptable: Boolean = perceptibilityLevel.isClinicallyAcceptable,
    val confidencePercentage: Double,
    val clinicalSummary: String
)

/**
 * Implements the CIEDE2000 (ΔE00) total color difference formula specified by the
 * International Commission on Illumination (CIE) and ISO/CIE 11664-6:2014, tailored
 * for comparing estimated image color against unverified VITA Classical coordinates.
 *
 * Mathematical Reference:
 * Gaurav Sharma, Wencheng Wu, Edul N. Dalal,
 * "The CIEDE2000 Color-Difference Formula: Implementation Notes, Supplementary Test Data,
 * and Mathematical Observations", Color Research & Application, Vol. 30, No. 1, Feb 2005.
 *
 * Dental Application Reference:
 * ISO/TR 28642: Dentistry — Guidance on color measurement.
 * Paravina RD et al. "Color difference thresholds in dentistry." J Esthet Restor Dent 2015.
 */
class Ciede2000ColorDifference(
    val kL: Double = 1.0,
    val kC: Double = 1.0,
    val kH: Double = 1.0,
    val referenceShades: List<VitaShade> = VitaShade.ALL_16
) {

    companion object {
        private const val POW7_25 = 6103515625.0 // 25^7

        /** Dental Perceptibility Threshold: ΔE00 ≤ 0.8 */
        const val PERCEPTIBILITY_THRESHOLD_PT = 0.8

        /** Dental Clinical Acceptability Threshold: ΔE00 ≤ 1.8 */
        const val ACCEPTABILITY_THRESHOLD_AT = 1.8

        /** Moderate clinical discrepancy boundary: ΔE00 ≤ 3.2 */
        const val MODERATE_THRESHOLD = 3.2

        /** Standard default instance configured with 1:1:1 parametric weighting factors */
        val DEFAULT = Ciede2000ColorDifference()

        /**
         * Creates an instance with dental custom parameters (e.g. kL=2.0 for higher lightness tolerance).
         */
        fun create(kL: Double = 1.0, kC: Double = 1.0, kH: Double = 1.0): Ciede2000ColorDifference =
            Ciede2000ColorDifference(kL, kC, kH)
    }

    /**
     * Calculates the CIEDE2000 total color difference (ΔE00) between two CIE L*a*b* coordinates.
     *
     * @param lab1 The first Lab coordinate (e.g. captured dental reading).
     * @param lab2 The second Lab coordinate (e.g. VITA Classical reference target).
     * @return The CIEDE2000 color difference (ΔE00).
     */
    fun calculateDeltaE00(lab1: Lab, lab2: Lab): Double {
        val l1 = lab1.l
        val a1 = lab1.a
        val b1 = lab1.b

        val l2 = lab2.l
        val a2 = lab2.a
        val b2 = lab2.b

        // Step 1: Calculate C'1, C'2, a'1, a'2
        val c1 = sqrt(a1 * a1 + b1 * b1)
        val c2 = sqrt(a2 * a2 + b2 * b2)

        val cBar = (c1 + c2) / 2.0
        val cBar7 = cBar.pow(7.0)

        val g = 0.5 * (1.0 - sqrt(cBar7 / (cBar7 + POW7_25)))

        val a1Prime = (1.0 + g) * a1
        val a2Prime = (1.0 + g) * a2

        val c1Prime = sqrt(a1Prime * a1Prime + b1 * b1)
        val c2Prime = sqrt(a2Prime * a2Prime + b2 * b2)

        // Step 2: Calculate h'1, h'2
        val h1Prime = calculateHueAngle(b1, a1Prime)
        val h2Prime = calculateHueAngle(b2, a2Prime)

        // Step 3: Calculate ΔL', ΔC', ΔH'
        val deltaLPrime = l2 - l1
        val deltaCPrime = c2Prime - c1Prime
        val deltaHPrime = calculateDeltaHPrime(c1Prime, c2Prime, h1Prime, h2Prime)

        // Step 4: Calculate CIEDE2000 weighting functions and rotation term
        val lBarPrime = (l1 + l2) / 2.0
        val cBarPrime = (c1Prime + c2Prime) / 2.0
        val hBarPrime = calculateHBarPrime(c1Prime, c2Prime, h1Prime, h2Prime)

        val t = 1.0 -
                0.17 * cos(Math.toRadians(hBarPrime - 30.0)) +
                0.24 * cos(Math.toRadians(2.0 * hBarPrime)) +
                0.32 * cos(Math.toRadians(3.0 * hBarPrime + 6.0)) -
                0.20 * cos(Math.toRadians(4.0 * hBarPrime - 63.0))

        val deltaTheta = 30.0 * exp(-((hBarPrime - 275.0) / 25.0).pow(2.0))

        val cBarPrime7 = cBarPrime.pow(7.0)
        val rC = 2.0 * sqrt(cBarPrime7 / (cBarPrime7 + POW7_25))

        val lBarMinus50Sq = (lBarPrime - 50.0).pow(2.0)
        val sL = 1.0 + (0.015 * lBarMinus50Sq) / sqrt(20.0 + lBarMinus50Sq)
        val sC = 1.0 + 0.045 * cBarPrime
        val sH = 1.0 + 0.015 * cBarPrime * t

        val rT = -sin(Math.toRadians(2.0 * deltaTheta)) * rC

        // Step 5: Compute total color difference ΔE00
        val termL = deltaLPrime / (kL * sL)
        val termC = deltaCPrime / (kC * sC)
        val termH = deltaHPrime / (kH * sH)

        val deltaE00Sq = termL * termL + termC * termC + termH * termH + rT * termC * termH
        return sqrt(max(0.0, deltaE00Sq))
    }

    /**
     * Compares captured dental shade data against a specific VITA Classical reference shade.
     *
     * @param capturedLab Measured tooth CIE L*a*b* coordinates.
     * @param targetShade VITA reference shade to compare against.
     * @param rank Optional ranking position (default = 1).
     * @return [DentalShadeComparisonResult] with detailed metric breakdown.
     */
    fun compare(
        capturedLab: Lab,
        targetShade: VitaShade,
        rank: Int = 1
    ): DentalShadeComparisonResult {
        val de00 = calculateDeltaE00(capturedLab, targetShade.lab)
        val roundedDe00 = (de00 * 100).roundToInt() / 100.0

        val deltaL = (targetShade.lStar - capturedLab.l * 100).roundToInt() / 100.0
        val cTooth = sqrt(capturedLab.a.pow(2) + capturedLab.b.pow(2))
        val cRef = sqrt(targetShade.aStar.pow(2) + targetShade.bStar.pow(2))
        val deltaC = ((cRef - cTooth) * 100).roundToInt() / 100.0

        val level = classifyPerceptibility(roundedDe00)
        val confidence = calculateConfidence(roundedDe00)

        val summary = "Tooth measured against VITA ${targetShade.shadeCode} (${targetShade.hueGroup}): ΔE00 = %.2f (${level.thresholdLabel}). %s".format(
            roundedDe00, level.description
        )

        return DentalShadeComparisonResult(
            capturedLab = capturedLab,
            matchedShade = targetShade,
            deltaE00 = roundedDe00,
            deltaLPrime = deltaL,
            deltaCPrime = deltaC,
            deltaHPrime = 0.0,
            rank = rank,
            perceptibilityLevel = level,
            isClinicallyAcceptable = level.isClinicallyAcceptable,
            confidencePercentage = confidence,
            clinicalSummary = summary
        )
    }

    /**
     * Overload comparing scalar L*, a*, b* values against a target VITA shade.
     */
    fun compare(l: Double, a: Double, b: Double, targetShade: VitaShade): DentalShadeComparisonResult {
        return compare(Lab(l, a, b), targetShade)
    }

    /**
     * Compares captured dental shade data against a VITA shade specified by code (e.g. "A2").
     */
    fun compare(capturedLab: Lab, shadeCode: String): DentalShadeComparisonResult? {
        val target = VitaShade.fromCode(shadeCode) ?: return null
        return compare(capturedLab, target)
    }

    /**
     * Evaluates a captured tooth reading against all 16 VITA Classical shades and identifies
     * the single closest matching VITA shade.
     *
     * @param capturedLab The captured dental CIE L*a*b* coordinate.
     * @return [DentalShadeComparisonResult] of the top matching shade.
     */
    fun findClosestMatch(capturedLab: Lab): DentalShadeComparisonResult {
        return rankAgainstVitaClassical(capturedLab).first()
    }

    /**
     * Overload for scalar L*, a*, b* values.
     */
    fun findClosestMatch(l: Double, a: Double, b: Double): DentalShadeComparisonResult {
        return findClosestMatch(Lab(l, a, b))
    }

    /**
     * Ranks all 16 illustrative VITA Classical shades (A1-A4, B1-B4, C1-C4, D2-D4)
     * against the captured tooth color, ordered ascending by CIEDE2000 color difference (ΔE00).
     *
     * @param capturedLab Measured dental CIE L*a*b* coordinates.
     * @return Ordered list of 16 [DentalShadeComparisonResult] entries (Rank #1 = closest).
     */
    fun rankAgainstVitaClassical(capturedLab: Lab): List<DentalShadeComparisonResult> {
        val rankedWithDe00 = referenceShades.map { shade ->
            val de00 = calculateDeltaE00(capturedLab, shade.lab)
            Pair(shade, de00)
        }.sortedBy { it.second }

        return rankedWithDe00.mapIndexed { index, (shade, de00) ->
            val roundedDe00 = (de00 * 100).roundToInt() / 100.0
            val level = classifyPerceptibility(roundedDe00)
            val confidence = calculateConfidence(roundedDe00)

            val summary = "#${index + 1} VITA ${shade.shadeCode}: ΔE00 = %.2f • %s".format(
                roundedDe00, level.thresholdLabel
            )

            DentalShadeComparisonResult(
                capturedLab = capturedLab,
                matchedShade = shade,
                deltaE00 = roundedDe00,
                deltaLPrime = (shade.lStar - capturedLab.l * 100).roundToInt() / 100.0,
                deltaCPrime = 0.0,
                deltaHPrime = 0.0,
                rank = index + 1,
                perceptibilityLevel = level,
                isClinicallyAcceptable = level.isClinicallyAcceptable,
                confidencePercentage = confidence,
                clinicalSummary = summary
            )
        }
    }

    /**
     * Overload for scalar L*, a*, b* values.
     */
    fun rankAgainstVitaClassical(l: Double, a: Double, b: Double): List<DentalShadeComparisonResult> {
        return rankAgainstVitaClassical(Lab(l, a, b))
    }

    /**
     * Classifies a calculated ΔE00 value into standardized dental clinical perceptibility thresholds.
     */
    fun classifyPerceptibility(deltaE00: Double): DentalPerceptibilityLevel {
        return when {
            deltaE00 <= PERCEPTIBILITY_THRESHOLD_PT -> DentalPerceptibilityLevel.IMPERCEPTIBLE
            deltaE00 <= ACCEPTABILITY_THRESHOLD_AT -> DentalPerceptibilityLevel.CLINICALLY_ACCEPTABLE
            deltaE00 <= MODERATE_THRESHOLD -> DentalPerceptibilityLevel.MODERATELY_ACCEPTABLE
            else -> DentalPerceptibilityLevel.CLINICALLY_UNACCEPTABLE
        }
    }

    /**
     * Computes an aesthetic confidence score (0% to 100%) based on clinical perceptibility models.
     */
    private fun calculateConfidence(deltaE00: Double): Double {
        // High confidence for ΔE00 ≤ 0.8 (95-100%)
        // Acceptable confidence for 0.8 < ΔE00 ≤ 1.8 (75-95%)
        // Marginal confidence for 1.8 < ΔE00 ≤ 3.2 (45-75%)
        // Low confidence for ΔE00 > 3.2 (< 45%)
        val score = when {
            deltaE00 <= 0.8 -> 100.0 - (deltaE00 / 0.8) * 5.0
            deltaE00 <= 1.8 -> 95.0 - ((deltaE00 - 0.8) / 1.0) * 20.0
            deltaE00 <= 3.2 -> 75.0 - ((deltaE00 - 1.8) / 1.4) * 30.0
            deltaE00 <= 6.0 -> 45.0 - ((deltaE00 - 3.2) / 2.8) * 35.0
            else -> 10.0
        }
        return (score * 10).roundToInt() / 10.0
    }

    // =========================================================================
    // Helper Mathematical Trigonometric Methods
    // =========================================================================

    private fun calculateHueAngle(b: Double, aPrime: Double): Double {
        if (abs(aPrime) < 1e-9 && abs(b) < 1e-9) return 0.0
        val angleRad = atan2(b, aPrime)
        var angleDeg = Math.toDegrees(angleRad)
        if (angleDeg < 0.0) {
            angleDeg += 360.0
        }
        return angleDeg
    }

    private fun calculateDeltaHPrime(
        c1Prime: Double,
        c2Prime: Double,
        h1Prime: Double,
        h2Prime: Double
    ): Double {
        if (c1Prime * c2Prime < 1e-9) return 0.0

        val diff = h2Prime - h1Prime
        val deltaHRad = when {
            abs(diff) <= 180.0 -> diff
            diff > 180.0 -> diff - 360.0
            else -> diff + 360.0
        }
        return 2.0 * sqrt(c1Prime * c2Prime) * sin(Math.toRadians(deltaHRad / 2.0))
    }

    private fun calculateHBarPrime(
        c1Prime: Double,
        c2Prime: Double,
        h1Prime: Double,
        h2Prime: Double
    ): Double {
        if (c1Prime * c2Prime < 1e-9) return h1Prime + h2Prime

        val diff = abs(h1Prime - h2Prime)
        return when {
            diff <= 180.0 -> (h1Prime + h2Prime) / 2.0
            h1Prime + h2Prime < 360.0 -> (h1Prime + h2Prime + 360.0) / 2.0
            else -> (h1Prime + h2Prime - 360.0) / 2.0
        }
    }
}
