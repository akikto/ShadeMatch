package com.example.colorengine

import kotlin.math.pow

object ColorConversions {

    private const val CIE_EPSILON = 216.0 / 24389.0 // ~0.00885645167
    private const val CIE_KAPPA = 24389.0 / 27.0    // ~903.296296296

    /**
     * Converts an 8-bit sRGB channel (0..255) to normalized linear RGB (0.0..1.0).
     * Follows IEC 61966-2-1:1999 standard.
     */
    fun sRgbToLinear(channel8Bit: Int): Double {
        val c = channel8Bit.coerceIn(0, 255) / 255.0
        return if (c <= 0.04045) {
            c / 12.92
        } else {
            ((c + 0.055) / 1.055).pow(2.4)
        }
    }

    /**
     * Linear RGB to sRGB channel 0..255.
     */
    fun linearToSrgb(linear: Double): Int {
        val c = linear.coerceIn(0.0, 1.0)
        val s = if (c <= 0.0031308) {
            12.92 * c
        } else {
            1.055 * c.pow(1.0 / 2.4) - 0.055
        }
        return (s * 255.0).toInt().coerceIn(0, 255)
    }

    /**
     * Converts normalized linear RGB components to CIE XYZ (0..100) using a 3x3 transformation matrix.
     */
    fun linearRgbToXyz(
        linearRgb: LinearRgb,
        matrix: Matrix3x3 = Matrix3x3.SRGB_TO_XYZ_D65
    ): Xyz {
        return matrix.multiply(linearRgb.r, linearRgb.g, linearRgb.b)
    }

    /**
     * Converts 8-bit sRGB (r, g, b) to CIE XYZ under D65.
     */
    fun rgbToXyz(
        r: Int,
        g: Int,
        b: Int,
        matrix: Matrix3x3 = Matrix3x3.SRGB_TO_XYZ_D65
    ): Xyz {
        val linR = sRgbToLinear(r)
        val linG = sRgbToLinear(g)
        val linB = sRgbToLinear(b)
        return linearRgbToXyz(LinearRgb(linR, linG, linB), matrix)
    }

    /**
     * Converts CIE XYZ to CIE L*a*b* using standard reference white point (default D65).
     */
    fun xyzToLab(
        xyz: Xyz,
        whitePoint: Xyz = Xyz.D65_2DEG
    ): Lab {
        val xr = xyz.x / whitePoint.x
        val yr = xyz.y / whitePoint.y
        val zr = xyz.z / whitePoint.z

        val fx = if (xr > CIE_EPSILON) xr.pow(1.0 / 3.0) else (CIE_KAPPA * xr + 16.0) / 116.0
        val fy = if (yr > CIE_EPSILON) yr.pow(1.0 / 3.0) else (CIE_KAPPA * yr + 16.0) / 116.0
        val fz = if (zr > CIE_EPSILON) zr.pow(1.0 / 3.0) else (CIE_KAPPA * zr + 16.0) / 116.0

        val l = (116.0 * fy - 16.0).coerceIn(0.0, 100.0)
        val a = 500.0 * (fx - fy)
        val b = 200.0 * (fy - fz)

        return Lab(l, a, b)
    }

    /**
     * Full pipeline from sRGB (0..255) to CIE L*a*b*.
     */
    fun rgbToLab(
        r: Int,
        g: Int,
        b: Int,
        matrix: Matrix3x3 = Matrix3x3.SRGB_TO_XYZ_D65,
        whitePoint: Xyz = Xyz.D65_2DEG
    ): Lab {
        val xyz = rgbToXyz(r, g, b, matrix)
        return xyzToLab(xyz, whitePoint)
    }

    /**
     * Converts CIE L*a*b* back to approximate sRGB for UI preview chips.
     */
    fun labToRgb(
        lab: Lab,
        whitePoint: Xyz = Xyz.D65_2DEG
    ): Triple<Int, Int, Int> {
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

        // Inverse sRGB D65 matrix
        val linR = 3.2404542 * x - 1.5371385 * y - 0.4985314 * z
        val linG = -0.9692660 * x + 1.8760108 * y + 0.0415560 * z
        val linB = 0.0556434 * x - 0.2040259 * y + 1.0572252 * z

        return Triple(
            linearToSrgb(linR),
            linearToSrgb(linG),
            linearToSrgb(linB)
        )
    }
}
