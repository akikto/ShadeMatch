package com.example.vision

import android.graphics.Bitmap
import android.graphics.Color
import kotlin.math.abs
import kotlin.math.max

enum class QualityStatus {
    PASS,
    WARNING,
    FAIL
}

data class QualityIssue(
    val code: String,
    val severity: QualityStatus,
    val message: String
)

data class ImageQualityEvaluation(
    val status: QualityStatus,
    val blurScore: Double,
    val underexposedFraction: Double,
    val overexposedFraction: Double,
    val specularFraction: Double,
    val toothAreaFraction: Double,
    val issues: List<QualityIssue>
) {
    val canProceed: Boolean
        get() = status != QualityStatus.FAIL
}

object ImageQualityChecker {

    const val CODE_BLUR_FAIL = "ERR_BLUR_EXCESSIVE"
    const val CODE_BLUR_WARN = "WARN_BLUR_BORDERLINE"
    const val CODE_OVEREXPOSED = "ERR_OVEREXPOSURE_CLIPPING"
    const val CODE_UNDEREXPOSED = "ERR_UNDEREXPOSURE_DARK"
    const val CODE_SPECULAR_GLARE = "WARN_SPECULAR_GLARE"
    const val CODE_INSUFFICIENT_AREA = "ERR_INSUFFICIENT_TOOTH_AREA"
    const val CODE_COLOR_CAST = "WARN_STRONG_COLOR_CAST"

    /**
     * Evaluates optical quality of a captured image bitmap.
     */
    fun evaluateQuality(
        bitmap: Bitmap,
        toothPixelCount: Int = 0,
        totalPixelsInRoi: Int = bitmap.width * bitmap.height
    ): ImageQualityEvaluation {
        val width = bitmap.width
        val height = bitmap.height
        val totalPixels = width * height

        // Downsample for fast inspection if needed
        val sampleStep = max(1, width / 180)
        var sampledTotal = 0
        var darkPixels = 0
        var clippedPixels = 0
        var specularPixels = 0

        var sumR = 0.0
        var sumG = 0.0
        var sumB = 0.0

        // Grayscale values for Laplacian variance (blur)
        val grayWidth = width / sampleStep
        val grayHeight = height / sampleStep
        val gray = DoubleArray(grayWidth * grayHeight)

        var gy = 0
        for (y in 0 until height step sampleStep) {
            var gx = 0
            for (x in 0 until width step sampleStep) {
                if (gy < grayHeight && gx < grayWidth) {
                    val pixel = bitmap.getPixel(x, y)
                    val r = Color.red(pixel)
                    val g = Color.green(pixel)
                    val b = Color.blue(pixel)

                    sumR += r
                    sumG += g
                    sumB += b

                    // Standard luminance
                    val lum = 0.299 * r + 0.587 * g + 0.114 * b
                    gray[gy * grayWidth + gx] = lum

                    if (lum < 35) darkPixels++
                    if (lum > 248) clippedPixels++

                    // Specular highlight on tooth/saliva: high luminance, low saturation
                    val maxC = max(r, max(g, b))
                    val minC = minOf(r, g, b)
                    val sat = if (maxC > 0) (maxC - minC).toDouble() / maxC else 0.0
                    if (lum > 230 && sat < 0.12) {
                        specularPixels++
                    }

                    sampledTotal++
                }
                gx++
            }
            gy++
        }

        // Calculate Laplacian variance for blur
        val blurScore = calculateLaplacianVariance(gray, grayWidth, grayHeight)

        val underexposedFraction = if (sampledTotal > 0) darkPixels.toDouble() / sampledTotal else 0.0
        val overexposedFraction = if (sampledTotal > 0) clippedPixels.toDouble() / sampledTotal else 0.0
        val specularFraction = if (sampledTotal > 0) specularPixels.toDouble() / sampledTotal else 0.0
        val toothAreaFraction = if (totalPixelsInRoi > 0) toothPixelCount.toDouble() / totalPixelsInRoi else 0.0

        val issues = mutableListOf<QualityIssue>()

        // 1. Blur
        if (blurScore < 40.0) {
            issues.add(
                QualityIssue(
                    CODE_BLUR_FAIL,
                    QualityStatus.FAIL,
                    "Motion blur or severe defocus detected (Score: %.1f). Retake with stable camera.".format(blurScore)
                )
            )
        } else if (blurScore < 75.0) {
            issues.add(
                QualityIssue(
                    CODE_BLUR_WARN,
                    QualityStatus.WARNING,
                    "Mild blur detected (Score: %.1f). Enamel detail may be slightly softened.".format(blurScore)
                )
            )
        }

        // 2. Overexposure / Glare
        if (overexposedFraction > 0.25) {
            issues.add(
                QualityIssue(
                    CODE_OVEREXPOSED,
                    QualityStatus.FAIL,
                    "Excessive overexposure (%.1f%% pixels clipped). Reduce illumination or lock exposure.".format(overexposedFraction * 100)
                )
            )
        } else if (specularFraction > 0.12) {
            issues.add(
                QualityIssue(
                    CODE_SPECULAR_GLARE,
                    QualityStatus.WARNING,
                    "Saliva / flash specular glare detected (%.1f%%). Saliva reflection excluded from color sampling.".format(specularFraction * 100)
                )
            )
        }

        // 3. Underexposure
        if (underexposedFraction > 0.45) {
            issues.add(
                QualityIssue(
                    CODE_UNDEREXPOSED,
                    QualityStatus.FAIL,
                    "Subject is too dark (%.1f%% shadow). Increase neutral diffused lighting.".format(underexposedFraction * 100)
                )
            )
        }

        // 4. Tooth area coverage (if tooth detection was executed)
        if (toothPixelCount > 0 && toothAreaFraction < 0.04) {
            issues.add(
                QualityIssue(
                    CODE_INSUFFICIENT_AREA,
                    QualityStatus.FAIL,
                    "Tooth area too small (%.1f%% of framing). Bring camera closer to central tooth.".format(toothAreaFraction * 100)
                )
            )
        }

        // 5. Color Cast check
        if (sampledTotal > 0) {
            val meanR = sumR / sampledTotal
            val meanG = sumG / sampledTotal
            val meanB = sumB / sampledTotal
            if (abs(meanB - meanR) > 80) {
                issues.add(
                    QualityIssue(
                        CODE_COLOR_CAST,
                        QualityStatus.WARNING,
                        "Strong ambient color temperature cast detected. Use 5500K daylight or neutral dental operatory light."
                    )
                )
            }
        }

        val overallStatus = when {
            issues.any { it.severity == QualityStatus.FAIL } -> QualityStatus.FAIL
            issues.any { it.severity == QualityStatus.WARNING } -> QualityStatus.WARNING
            else -> QualityStatus.PASS
        }

        return ImageQualityEvaluation(
            status = overallStatus,
            blurScore = (blurScore * 10).toInt() / 10.0,
            underexposedFraction = underexposedFraction,
            overexposedFraction = overexposedFraction,
            specularFraction = specularFraction,
            toothAreaFraction = toothAreaFraction,
            issues = issues
        )
    }

    private fun calculateLaplacianVariance(gray: DoubleArray, width: Int, height: Int): Double {
        if (width < 3 || height < 3) return 0.0

        var sum = 0.0
        var sumSq = 0.0
        var count = 0

        // 3x3 Discrete Laplacian kernel: [0, 1, 0; 1, -4, 1; 0, 1, 0]
        for (y in 1 until height - 1) {
            for (x in 1 until width - 1) {
                val center = gray[y * width + x]
                val up = gray[(y - 1) * width + x]
                val down = gray[(y + 1) * width + x]
                val left = gray[y * width + (x - 1)]
                val right = gray[y * width + (x + 1)]

                val lap = up + down + left + right - 4.0 * center
                sum += lap
                sumSq += lap * lap
                count++
            }
        }

        if (count == 0) return 0.0
        val mean = sum / count
        return max(0.0, (sumSq / count) - (mean * mean))
    }
}
