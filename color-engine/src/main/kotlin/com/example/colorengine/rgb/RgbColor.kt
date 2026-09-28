package com.example.colorengine.rgb

/**
 * Standard 8-bit integer sRGB color representation (0..255 per channel).
 */
data class RgbColor(
    val r: Int,
    val g: Int,
    val b: Int
) {
    init {
        require(r in 0..255) { "Red component must be in 0..255: $r" }
        require(g in 0..255) { "Green component must be in 0..255: $g" }
        require(b in 0..255) { "Blue component must be in 0..255: $b" }
    }

    fun toLinearRgb(): LinearRgbColor {
        return RgbLinearization.rgbToLinearRgb(this)
    }

    fun toHex(): String {
        return "#%02X%02X%02X".format(r, g, b)
    }
}

/**
 * Normalized Linear RGB color representation (0.0..1.0 per channel).
 * De-gamma expanded, suitable for optical and matrix transformations.
 */
data class LinearRgbColor(
    val r: Double,
    val g: Double,
    val b: Double
) {
    fun toRgbColor(): RgbColor {
        return RgbLinearization.linearRgbToRgb(this)
    }
}
