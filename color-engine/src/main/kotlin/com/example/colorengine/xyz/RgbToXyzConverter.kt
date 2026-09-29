package com.example.colorengine.xyz

import com.example.colorengine.rgb.LinearRgbColor
import com.example.colorengine.rgb.RgbColor
import com.example.colorengine.rgb.RgbLinearization

/**
 * Converts Linear RGB to CIE XYZ using the supplied matrix; the default sRGB matrix is not device calibration.
 */
object RgbToXyzConverter {

    /**
     * Converts normalized Linear RGB to CIE XYZ (0..100) using a 3x3 transformation matrix.
     */
    fun linearRgbToXyz(
        linearRgb: LinearRgbColor,
        matrix: ColorTransformationMatrix = ColorTransformationMatrix.SRGB_TO_XYZ_D65
    ): XyzColor {
        return matrix.multiply(linearRgb.r, linearRgb.g, linearRgb.b)
    }

    /**
     * Converts 8-bit sRGB to CIE XYZ by first linearizing and then applying the transformation matrix.
     */
    fun rgbToXyz(
        rgb: RgbColor,
        matrix: ColorTransformationMatrix = ColorTransformationMatrix.SRGB_TO_XYZ_D65
    ): XyzColor {
        val linear = RgbLinearization.rgbToLinearRgb(rgb)
        return linearRgbToXyz(linear, matrix)
    }
}
