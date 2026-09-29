package com.example.colorengine

import com.example.colorengine.ciede2000.Ciede2000Calculator
import com.example.colorengine.lab.LabColor
import com.example.colorengine.lab.XyzToLabConverter
import com.example.colorengine.matching.ComprehensiveMatchReport
import com.example.colorengine.matching.ShadeMatchResult
import com.example.colorengine.matching.ShadeMatcher
import com.example.colorengine.reference.VitaClassicalDataset
import com.example.colorengine.reference.VitaClassicalReference
import com.example.colorengine.rgb.LinearRgbColor
import com.example.colorengine.rgb.RgbColor
import com.example.colorengine.rgb.RgbLinearization
import com.example.colorengine.xyz.ColorTransformationMatrix
import com.example.colorengine.xyz.Illuminants
import com.example.colorengine.xyz.RgbToXyzConverter
import com.example.colorengine.xyz.XyzColor

/**
 * ColorEngine - Unified Entry Point for Dental Color Science Pipeline.
 *
 * Implements:
 * 1. RGB -> Linear (IEC 61966-2-1)
 * 2. RGB -> XYZ (D65, 2° observer)
 * 3. XYZ -> CIELAB (CIE 1976)
 * 4. CIELAB -> CIEDE2000 (Sharma, Wu, Dalal 2005)
 * 5. Matching against unverified illustrative VITA Classical coordinates
 */
class ColorEngine(
    val calibrationMatrix: ColorTransformationMatrix = ColorTransformationMatrix.SRGB_TO_XYZ_D65,
    val whitePoint: XyzColor = Illuminants.D65_2DEG,
    val referenceSet: List<VitaClassicalReference> = VitaClassicalDataset.ALL_16_SHADES
) {

    companion object {
        const val VERSION = "v1.4.2-CIEDE2000"
        const val SYSTEM_NAME = VitaClassicalDataset.SHADE_SYSTEM

        val DEFAULT: ColorEngine = ColorEngine()
    }

    // 1. RGB to Linear
    fun sRgbToLinear(channel8Bit: Int): Double = RgbLinearization.channelToLinear(channel8Bit)

    fun rgbToLinearRgb(rgb: RgbColor): LinearRgbColor = RgbLinearization.rgbToLinearRgb(rgb)

    fun linearToSrgb(linearValue: Double): Int = RgbLinearization.linearToChannel(linearValue)

    // 2. Linear to XYZ & RGB to XYZ
    fun linearRgbToXyz(linear: LinearRgbColor): XyzColor = RgbToXyzConverter.linearRgbToXyz(linear, calibrationMatrix)

    fun rgbToXyz(rgb: RgbColor): XyzColor = RgbToXyzConverter.rgbToXyz(rgb, calibrationMatrix)

    // 3. XYZ to CIELAB
    fun xyzToLab(xyz: XyzColor): LabColor = XyzToLabConverter.xyzToLab(xyz, whitePoint)

    fun rgbToLab(rgb: RgbColor): LabColor {
        val xyz = rgbToXyz(rgb)
        return xyzToLab(xyz)
    }

    fun labToRgb(lab: LabColor): RgbColor = XyzToLabConverter.labToRgb(lab, whitePoint)

    // 4. CIEDE2000
    fun calculateDeltaE00(
        lab1: LabColor,
        lab2: LabColor,
        kL: Double = 1.0,
        kC: Double = 1.0,
        kH: Double = 1.0
    ): Double {
        return Ciede2000Calculator.calculate(lab1, lab2, kL, kC, kH)
    }

    // 5. VITA Classical Reference Matching
    /**
     * Accepts a measured L*a*b* value and returns the closest [VitaShade] match
     * based on the calculated CIEDE2000 color distance (ΔE00).
     */
    fun findClosestVitaShade(measuredLab: LabColor): VitaShade {
        return VitaShade.ALL_16.minByOrNull { ref ->
            calculateDeltaE00(measuredLab, ref.lab)
        } ?: VitaShade.ALL_16.first()
    }

    /**
     * Overload accepting individual L*, a*, b* scalar components.
     */
    fun findClosestVitaShade(l: Double, a: Double, b: Double): VitaShade {
        return findClosestVitaShade(LabColor(l, a, b))
    }

    /**
     * Returns the closest [VitaShade] along with the calculated CIEDE2000 ΔE00 distance.
     */
    fun findClosestVitaShadeWithDistance(measuredLab: LabColor): Pair<VitaShade, Double> {
        val closest = VitaShade.ALL_16.map { shade ->
            val de00 = calculateDeltaE00(measuredLab, shade.lab)
            Pair(shade, (de00 * 100).toInt() / 100.0)
        }.minByOrNull { it.second } ?: Pair(VitaShade.ALL_16.first(), 0.0)
        return closest
    }

    fun rankAllShades(measuredLab: LabColor): List<ShadeMatchResult> {
        val ranked = referenceSet.map { ref ->
            val de00 = calculateDeltaE00(measuredLab, ref.lab)
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

    fun findClosestShade(measuredLab: LabColor): ShadeMatchResult {
        return rankAllShades(measuredLab).first()
    }

    fun matchToothColor(measuredLab: LabColor): ComprehensiveMatchReport {
        return ShadeMatcher.matchShade(measuredLab)
    }

    // 6. Scientific Self-Verification
    fun verifySelfMatching(): Boolean {
        if (referenceSet.size != 16) return false
        for (ref in referenceSet) {
            val top = findClosestShade(ref.lab)
            if (top.shadeCode != ref.shadeCode || top.deltaE00 > 0.001) {
                return false
            }
        }
        return true
    }
}
