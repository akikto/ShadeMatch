package com.example.colorengine.xyz

/**
 * Standard CIE Standard Illuminants and Reference White Points.
 */
object Illuminants {
    /**
     * CIE Standard Illuminant D65, 2° Standard Observer (CIE 1931).
     * Reference White Point: Xn = 95.047, Yn = 100.000, Zn = 108.883.
     */
    val D65_2DEG = XyzColor(95.047, 100.000, 108.883)

    /**
     * CIE Standard Illuminant D50, 2° Standard Observer.
     * Reference White Point: Xn = 96.422, Yn = 100.000, Zn = 82.521.
     */
    val D50_2DEG = XyzColor(96.422, 100.000, 82.521)
}

/**
 * 3x3 Colorimetric Transformation Matrix.
 */
data class ColorTransformationMatrix(
    val m00: Double, val m01: Double, val m02: Double,
    val m10: Double, val m11: Double, val m12: Double,
    val m20: Double, val m21: Double, val m22: Double
) {
    companion object {
        /**
         * Standard sRGB to XYZ (D65) Bradford-adapted transformation matrix.
         * [X]   [0.4124564  0.3575761  0.1804375] [R_lin]
         * [Y] = [0.2126729  0.7151522  0.0721750] [G_lin] * 100
         * [Z]   [0.0193339  0.1191920  0.9503041] [B_lin]
         */
        val SRGB_TO_XYZ_D65 = ColorTransformationMatrix(
            0.4124564, 0.3575761, 0.1804375,
            0.2126729, 0.7151522, 0.0721750,
            0.0193339, 0.1191920, 0.9503041
        )
    }

    fun multiply(r: Double, g: Double, b: Double): XyzColor {
        val x = (m00 * r + m01 * g + m02 * b) * 100.0
        val y = (m10 * r + m11 * g + m12 * b) * 100.0
        val z = (m20 * r + m21 * g + m22 * b) * 100.0
        return XyzColor(x, y, z)
    }
}
