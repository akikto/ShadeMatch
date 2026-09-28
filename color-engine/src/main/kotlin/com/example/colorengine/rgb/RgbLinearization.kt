package com.example.colorengine.rgb

import kotlin.math.pow

/**
 * Standard IEC 61966-2-1:1999 sRGB gamma linearization and compression.
 */
object RgbLinearization {

    /**
     * Converts an 8-bit sRGB channel (0..255) to a normalized linear value (0.0..1.0).
     * Linearization threshold: C_srgb <= 0.04045 -> C_linear = C_srgb / 12.92
     * Else: C_linear = ((C_srgb + 0.055) / 1.055) ^ 2.4
     */
    fun channelToLinear(channel8Bit: Int): Double {
        val c = channel8Bit.coerceIn(0, 255) / 255.0
        return if (c <= 0.04045) {
            c / 12.92
        } else {
            ((c + 0.055) / 1.055).pow(2.4)
        }
    }

    /**
     * Converts a normalized linear value (0.0..1.0) back to an 8-bit sRGB channel (0..255).
     * Threshold: C_linear <= 0.0031308 -> C_srgb = 12.92 * C_linear
     * Else: C_srgb = 1.055 * (C_linear ^ (1/2.4)) - 0.055
     */
    fun linearToChannel(linearValue: Double): Int {
        val c = linearValue.coerceIn(0.0, 1.0)
        val s = if (c <= 0.0031308) {
            12.92 * c
        } else {
            1.055 * c.pow(1.0 / 2.4) - 0.055
        }
        return (s * 255.0).toInt().coerceIn(0, 255)
    }

    /**
     * Converts an 8-bit RgbColor to LinearRgbColor.
     */
    fun rgbToLinearRgb(rgb: RgbColor): LinearRgbColor {
        return LinearRgbColor(
            r = channelToLinear(rgb.r),
            g = channelToLinear(rgb.g),
            b = channelToLinear(rgb.b)
        )
    }

    /**
     * Converts a LinearRgbColor to an 8-bit RgbColor.
     */
    fun linearRgbToRgb(linear: LinearRgbColor): RgbColor {
        return RgbColor(
            r = linearToChannel(linear.r),
            g = linearToChannel(linear.g),
            b = linearToChannel(linear.b)
        )
    }
}
