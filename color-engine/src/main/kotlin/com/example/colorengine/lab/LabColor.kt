package com.example.colorengine.lab

import kotlin.math.sqrt

/**
 * CIE 1976 L*a*b* color space representation.
 * - L*: Lightness (0..100)
 * - a*: Green (-) to Red (+) axis
 * - b*: Blue (-) to Yellow (+) axis
 */
data class LabColor(
    val l: Double,
    val a: Double,
    val b: Double
) {
    /**
     * Metric Chroma C* = sqrt(a*^2 + b*^2)
     */
    val chroma: Double
        get() = sqrt(a * a + b * b)

    /**
     * Metric Hue angle h_ab in degrees (0..360)
     */
    val hueAngleDeg: Double
        get() {
            var deg = Math.toDegrees(kotlin.math.atan2(b, a))
            if (deg < 0.0) deg += 360.0
            return deg
        }

    fun formatted(): String {
        return "L*: %.2f, a*: %.2f, b*: %.2f".format(l, a, b)
    }
}
