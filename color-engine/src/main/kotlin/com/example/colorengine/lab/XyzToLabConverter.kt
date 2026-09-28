package com.example.colorengine.lab

import com.example.colorengine.rgb.RgbColor
import com.example.colorengine.rgb.RgbLinearization
import com.example.colorengine.xyz.Illuminants
import com.example.colorengine.xyz.XyzColor
import kotlin.math.pow

/**
 * Standard CIE 1976 transformation between CIE XYZ and CIE L*a*b*.
 * Reference: CIE Publication 15:2004.
 */
object XyzToLabConverter {

    private const val CIE_EPSILON = 216.0 / 24389.0 // ~0.00885645167
    private const val CIE_KAPPA = 24389.0 / 27.0    // ~903.296296296

    /**
     * Converts CIE XYZ to CIE L*a*b* relative to a reference white point (default: D65 2°).
     */
    fun xyzToLab(
        xyz: XyzColor,
        whitePoint: XyzColor = Illuminants.D65_2DEG
    ): LabColor {
        val xr = xyz.x / whitePoint.x
        val yr = xyz.y / whitePoint.y
        val zr = xyz.z / whitePoint.z

        val fx = if (xr > CIE_EPSILON) xr.pow(1.0 / 3.0) else (CIE_KAPPA * xr + 16.0) / 116.0
        val fy = if (yr > CIE_EPSILON) yr.pow(1.0 / 3.0) else (CIE_KAPPA * yr + 16.0) / 116.0
        val fz = if (zr > CIE_EPSILON) zr.pow(1.0 / 3.0) else (CIE_KAPPA * zr + 16.0) / 116.0

        val l = (116.0 * fy - 16.0).coerceIn(0.0, 100.0)
        val a = 500.0 * (fx - fy)
        val b = 200.0 * (fy - fz)

        return LabColor(l, a, b)
    }

    /**
     * Converts CIE L*a*b* back to CIE XYZ.
     */
    fun labToXyz(
        lab: LabColor,
        whitePoint: XyzColor = Illuminants.D65_2DEG
    ): XyzColor {
        val fy = (lab.l + 16.0) / 116.0
        val fx = lab.a / 500.0 + fy
        val fz = fy - lab.b / 200.0

        val fx3 = fx.pow(3.0)
        val fz3 = fz.pow(3.0)

        val xr = if (fx3 > CIE_EPSILON) fx3 else (116.0 * fx - 16.0) / CIE_KAPPA
        val yr = if (lab.l > CIE_KAPPA * CIE_EPSILON) ((lab.l + 16.0) / 116.0).pow(3.0) else lab.l / CIE_KAPPA
        val zr = if (fz3 > CIE_EPSILON) fz3 else (116.0 * fz - 16.0) / CIE_KAPPA

        val x = (xr * whitePoint.x) / 100.0
        val y = (yr * whitePoint.y) / 100.0
        val z = (zr * whitePoint.z) / 100.0

        return XyzColor(x * 100.0, y * 100.0, z * 100.0)
    }

    /**
     * Converts CIE L*a*b* to sRGB for display visualization.
     */
    fun labToRgb(
        lab: LabColor,
        whitePoint: XyzColor = Illuminants.D65_2DEG
    ): RgbColor {
        val xyz = labToXyz(lab, whitePoint)
        val x = xyz.x / 100.0
        val y = xyz.y / 100.0
        val z = xyz.z / 100.0

        // Inverse standard sRGB matrix (Bradford adapted to D65)
        val linR = 3.2404542 * x - 1.5371385 * y - 0.4985314 * z
        val linG = -0.9692660 * x + 1.8760108 * y + 0.0415560 * z
        val linB = 0.0556434 * x - 0.2040259 * y + 1.0572252 * z

        return RgbColor(
            r = RgbLinearization.linearToChannel(linR),
            g = RgbLinearization.linearToChannel(linG),
            b = RgbLinearization.linearToChannel(linB)
        )
    }
}
