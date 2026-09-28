package com.example.colorengine

import kotlin.math.pow

/**
 * Fundamental color models for the dental shade matching pipeline.
 */

data class LinearRgb(
    val r: Double,
    val g: Double,
    val b: Double
)

data class Xyz(
    val x: Double,
    val y: Double,
    val z: Double
) {
    companion object {
        // Standard CIE 1931 2° observer D65 reference white point
        val D65_2DEG = Xyz(95.047, 100.000, 108.883)
    }
}

data class Lab(
    val l: Double,
    val a: Double,
    val b: Double
) {
    fun formatted(): String {
        return "L*: %.2f, a*: %.2f, b*: %.2f".format(l, a, b)
    }
}

data class Matrix3x3(
    val m00: Double, val m01: Double, val m02: Double,
    val m10: Double, val m11: Double, val m12: Double,
    val m20: Double, val m21: Double, val m22: Double
) {
    companion object {
        // Standard sRGB to XYZ (D65) Bradford-adapted transformation matrix
        val SRGB_TO_XYZ_D65 = Matrix3x3(
            0.4124564, 0.3575761, 0.1804375,
            0.2126729, 0.7151522, 0.0721750,
            0.0193339, 0.1191920, 0.9503041
        )
    }

    fun multiply(r: Double, g: Double, b: Double): Xyz {
        val x = (m00 * r + m01 * g + m02 * b) * 100.0
        val y = (m10 * r + m11 * g + m12 * b) * 100.0
        val z = (m20 * r + m21 * g + m22 * b) * 100.0
        return Xyz(x, y, z)
    }
}

data class ShadeMatchResult(
    val shadeCode: String,
    val deltaE00: Double,
    val rank: Int,
    val referenceLab: Lab,
    val provenance: String
)
