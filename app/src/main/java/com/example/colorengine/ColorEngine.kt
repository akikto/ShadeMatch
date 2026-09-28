package com.example.colorengine

import kotlin.math.abs

/**
 * ColorEngine - Deterministic Color Science Engine for Dental Shade Matching.
 *
 * Implements the scientific color processing pipeline required for dental shade assistance:
 * 1. sRGB -> Linear RGB (IEC 61966-2-1:1999)
 * 2. Linear RGB -> CIE XYZ (D65 standard illuminant, 2° observer)
 * 3. CIE XYZ -> CIE L*a*b* (CIE 1976 standard)
 * 4. CIE L*a*b* -> CIEDE2000 ΔE00 (Sharma, Wu, Dalal 2005)
 * 5. Deterministic matching and ranking against certified VITA Classical 16-shade standard.
 *
 * Strictly adheres to project requirements:
 * - Deterministic color-science pipeline (no black-box guessing).
 * - Exclusively supports VITA_CLASSICAL (16 shades: A1-A4, B1-B4, C1-C4, D2-D4).
 * - Full audit provenance and ambiguity handling.
 */
class ColorEngine(
    val calibrationMatrix: Matrix3x3 = Matrix3x3.SRGB_TO_XYZ_D65,
    val whitePoint: Xyz = Xyz.D65_2DEG,
    val referenceSet: List<VitaReference> = VitaClassicalData.ALL_16_SHADES
) {

    companion object {
        const val ENGINE_VERSION = "v1.4.2-CIEDE2000"
        const val STANDARD_SHADE_SYSTEM = VitaClassicalData.SHADE_SYSTEM

        /** Default engine configured with standard CIE D65 / 2° Bradford adaptation */
        val DEFAULT = ColorEngine()

        /** Quick helper instance */
        fun create(
            customMatrix: Matrix3x3 = Matrix3x3.SRGB_TO_XYZ_D65,
            customWhitePoint: Xyz = Xyz.D65_2DEG
        ): ColorEngine = ColorEngine(customMatrix, customWhitePoint)
    }

    // =========================================================================
    // 1. RGB -> Linear RGB
    // =========================================================================

    /**
     * Converts an 8-bit sRGB channel (0..255) to normalized linear RGB (0.0..1.0).
     * Follows IEC 61966-2-1:1999 standard gamma expansion.
     */
    fun sRgbChannelToLinear(channel8Bit: Int): Double {
        return ColorConversions.sRgbToLinear(channel8Bit)
    }

    /**
     * Converts 8-bit sRGB components (0..255) to normalized LinearRgb.
     */
    fun rgbToLinearRgb(r: Int, g: Int, b: Int): LinearRgb {
        val linR = sRgbChannelToLinear(r)
        val linG = sRgbChannelToLinear(g)
        val linB = sRgbChannelToLinear(b)
        return LinearRgb(linR, linG, linB)
    }

    // =========================================================================
    // 2. Linear RGB -> CIE XYZ (D65)
    // =========================================================================

    /**
     * Converts normalized LinearRgb to CIE XYZ (0..100) using the engine's calibration matrix.
     */
    fun linearRgbToXyz(linearRgb: LinearRgb): Xyz {
        return ColorConversions.linearRgbToXyz(linearRgb, calibrationMatrix)
    }

    /**
     * Directly converts 8-bit sRGB (0..255) to CIE XYZ (0..100).
     */
    fun rgbToXyz(r: Int, g: Int, b: Int): Xyz {
        return ColorConversions.rgbToXyz(r, g, b, calibrationMatrix)
    }

    // =========================================================================
    // 3. CIE XYZ -> CIE L*a*b*
    // =========================================================================

    /**
     * Converts CIE XYZ to CIE L*a*b* using standard reference white point (default D65/2°).
     */
    fun xyzToLab(xyz: Xyz): Lab {
        return ColorConversions.xyzToLab(xyz, whitePoint)
    }

    /**
     * End-to-end conversion from 8-bit RGB to CIE L*a*b*.
     */
    fun rgbToLab(r: Int, g: Int, b: Int): Lab {
        val xyz = rgbToXyz(r, g, b)
        return xyzToLab(xyz)
    }

    /**
     * Converts CIE L*a*b* back to approximate sRGB (0..255) for UI visualization.
     */
    fun labToRgb(lab: Lab): Triple<Int, Int, Int> {
        return ColorConversions.labToRgb(lab, whitePoint)
    }

    // =========================================================================
    // 4. CIEDE2000 Color Difference (ΔE00)
    // =========================================================================

    /**
     * Calculates the rigorous CIEDE2000 total color difference between two Lab coordinates.
     * Reference: Sharma, Wu, & Dalal (2005).
     */
    fun calculateDeltaE00(
        lab1: Lab,
        lab2: Lab,
        kL: Double = 1.0,
        kC: Double = 1.0,
        kH: Double = 1.0
    ): Double {
        return Ciede2000.calculate(lab1, lab2, kL, kC, kH)
    }

    // =========================================================================
    // 5. VITA Classical Reference Matching & Ranking
    // =========================================================================

    /**
     * Accepts a measured L*a*b* value and returns the closest [VitaShade] match
     * based on the calculated CIEDE2000 color distance (ΔE00).
     */
    fun findClosestVitaShade(measuredLab: Lab): VitaShade {
        return VitaShade.ALL_16.minByOrNull { ref ->
            calculateDeltaE00(measuredLab, ref.lab)
        } ?: VitaShade.ALL_16.first()
    }

    /**
     * Overload accepting individual L*, a*, b* scalar components.
     */
    fun findClosestVitaShade(l: Double, a: Double, b: Double): VitaShade {
        return findClosestVitaShade(Lab(l, a, b))
    }

    /**
     * Returns the closest [VitaShade] along with the calculated CIEDE2000 ΔE00 distance.
     */
    fun findClosestVitaShadeWithDistance(measuredLab: Lab): Pair<VitaShade, Double> {
        val closest = VitaShade.ALL_16.map { shade ->
            val de00 = calculateDeltaE00(measuredLab, shade.lab)
            Pair(shade, (de00 * 100).toInt() / 100.0)
        }.minByOrNull { it.second } ?: Pair(VitaShade.ALL_16.first(), 0.0)
        return closest
    }

    /**
     * Ranks all 16 VITA Classical references against a measured Lab color.
     * Sorted ascending by ΔE00 (lowest ΔE00 = closest match).
     */
    fun rankAllShades(measuredLab: Lab): List<ShadeMatchResult> {
        val ranked = referenceSet.map { ref ->
            val deltaE00 = calculateDeltaE00(measuredLab, ref.lab)
            Pair(ref, deltaE00)
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

    /**
     * Finds the single closest VITA Classical shade candidate.
     */
    fun findClosestShade(measuredLab: Lab): ShadeMatchResult {
        return rankAllShades(measuredLab).first()
    }

    /**
     * Produces a comprehensive clinical shade report with top 3 candidates,
     * ambiguity detection, clinical perceptibility status, and optional tooth zones.
     */
    fun matchToothColor(
        measuredLab: Lab,
        cervicalLab: Lab? = null,
        middleLab: Lab? = null,
        incisalLab: Lab? = null,
        dispersionMad: Double = 0.0
    ): ComprehensiveShadeReport {
        val ranked = rankAllShades(measuredLab)
        val topMatches = ranked.take(3)

        val top1 = topMatches[0]
        val top2 = topMatches[1]
        val top3 = topMatches[2]

        val deltaDiff = abs(top2.deltaE00 - top1.deltaE00)
        val isAmbiguous = deltaDiff < ShadeMatcher.AMBIGUITY_DELTA_THRESHOLD
        val ambiguityMessage = if (isAmbiguous) {
            "Close shade candidates detected (${top1.shadeCode} vs ${top2.shadeCode}, ΔE diff %.2f). Clinical review recommended.".format(deltaDiff)
        } else if (dispersionMad > 2.8) {
            "High internal optical translucency dispersion (MAD: %.2f) detected across tooth structure.".format(dispersionMad)
        } else {
            null
        }

        val perceptibilityStatus = when {
            top1.deltaE00 <= ShadeMatcher.PERCEPTIBILITY_THRESHOLD_PT -> "EXCELLENT (ΔE00 ≤ 0.8, Imperceptible)"
            top1.deltaE00 <= ShadeMatcher.ACCEPTABILITY_THRESHOLD_AT -> "ACCEPTABLE (ΔE00 ≤ 1.8, Clinically Valid)"
            top1.deltaE00 <= ShadeMatcher.MODERATE_THRESHOLD -> "MODERATE (1.8 < ΔE00 ≤ 3.2, Review Suggested)"
            else -> "SUBOPTIMAL (ΔE00 > 3.2, Check Lighting/Isolation)"
        }

        val zones = if (cervicalLab != null && middleLab != null && incisalLab != null) {
            analyzeToothZones(cervicalLab, middleLab, incisalLab)
        } else {
            null
        }

        return ComprehensiveShadeReport(
            overallLab = measuredLab,
            topMatches = topMatches,
            recommendedShade = top1,
            secondShade = top2,
            thirdShade = top3,
            isAmbiguous = isAmbiguous,
            ambiguityMessage = ambiguityMessage,
            zones = zones,
            perceptibilityStatus = perceptibilityStatus,
            colorDispersionMad = dispersionMad,
            algorithmVersion = ENGINE_VERSION
        )
    }

    /**
     * Stratified anatomical zone matching for Cervical, Middle, and Incisal zones.
     */
    fun analyzeToothZones(
        cervicalLab: Lab,
        middleLab: Lab,
        incisalLab: Lab
    ): ToothZoneShades {
        return ToothZoneShades(
            cervical = analyzeSingleZone("Cervical", cervicalLab),
            middle = analyzeSingleZone("Middle Body", middleLab),
            incisal = analyzeSingleZone("Incisal Edge", incisalLab)
        )
    }

    private fun analyzeSingleZone(name: String, lab: Lab): ShadeAnalysisZone {
        val top = findClosestShade(lab)
        return ShadeAnalysisZone(
            zoneName = name,
            lab = lab,
            topMatch = top
        )
    }

    // =========================================================================
    // 6. Scientific Self-Test & Quality Verification
    // =========================================================================

    /**
     * Self-test validating that all 16 VITA Classical references correctly rank themselves first
     * with ΔE00 = 0.0, validating mathematical correctness of the pipeline.
     */
    fun verifyReferenceSelfMatching(): EngineSelfTestReport {
        val failures = mutableListOf<String>()

        if (referenceSet.size != 16) {
            failures.add("Reference set count must be exactly 16, found ${referenceSet.size}")
        }

        for (ref in referenceSet) {
            val closest = findClosestShade(ref.lab)
            if (closest.shadeCode != ref.shadeCode) {
                failures.add("Reference ${ref.shadeCode} ranked ${closest.shadeCode} instead of itself")
            }
            if (closest.deltaE00 > 0.001) {
                failures.add("Reference ${ref.shadeCode} self-distance is ${closest.deltaE00} (expected 0.0)")
            }
        }

        return EngineSelfTestReport(
            passed = failures.isEmpty(),
            totalShadesTested = referenceSet.size,
            failures = failures,
            engineVersion = ENGINE_VERSION
        )
    }
}

data class EngineSelfTestReport(
    val passed: Boolean,
    val totalShadesTested: Int,
    val failures: List<String>,
    val engineVersion: String
)
