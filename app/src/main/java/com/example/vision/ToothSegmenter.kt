package com.example.vision

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Rect
import com.example.colorengine.ColorConversions
import com.example.colorengine.Lab
import kotlin.math.max
import kotlin.math.min

enum class PixelClassification {
    TOOTH_ENAMEL,
    GINGIVA,
    LIP,
    SPECULAR_REFLECTION,
    BACKGROUND_DARK,
    SHADE_TAB_REFERENCE,
    UNKNOWN
}

data class SegmentedZonePixels(
    val zoneName: String,
    val labList: List<Lab>,
    val boundingBox: Rect
)

data class ToothSegmentationResult(
    val isToothDetected: Boolean,
    val toothBoundingBox: Rect,
    val totalToothPixels: Int,
    val toothLabPixels: List<Lab>,
    val cervicalZone: SegmentedZonePixels,
    val middleZone: SegmentedZonePixels,
    val incisalZone: SegmentedZonePixels,
    val specularReflectionPixelCount: Int,
    val gingivaPixelCount: Int,
    val referenceTabDetected: Boolean,
    val referenceTabPixels: List<Lab>? = null
)

object ToothSegmenter {

    /**
     * Segments the central target tooth and extracts valid clinical enamel pixels.
     * Excludes gingiva, lips, saliva specular reflections, and dark background.
     */
    fun segmentTooth(
        bitmap: Bitmap,
        isMaxillary: Boolean = true // Upper arch = true (incisal is bottom, cervical is top)
    ): ToothSegmentationResult {
        val width = bitmap.width
        val height = bitmap.height

        // Define region of interest (central 60% of image where reticle guides the clinician)
        val roiLeft = (width * 0.15).toInt()
        val roiRight = (width * 0.85).toInt()
        val roiTop = (height * 0.15).toInt()
        val roiBottom = (height * 0.85).toInt()

        val validToothLabs = mutableListOf<Lab>()
        val specularLabs = mutableListOf<Lab>()
        var gingivaCount = 0

        var minX = width
        var maxX = 0
        var minY = height
        var maxY = 0

        // Step for sampling efficiency
        val step = max(1, width / 200)

        val toothPixelCoords = mutableListOf<Triple<Int, Int, Lab>>()

        for (y in roiTop until roiBottom step step) {
            for (x in roiLeft until roiRight step step) {
                val pixel = bitmap.getPixel(x, y)
                val r = Color.red(pixel)
                val g = Color.green(pixel)
                val b = Color.blue(pixel)

                val lab = ColorConversions.rgbToLab(r, g, b)
                val classification = classifyPixel(r, g, b, lab)

                when (classification) {
                    PixelClassification.TOOTH_ENAMEL -> {
                        validToothLabs.add(lab)
                        toothPixelCoords.add(Triple(x, y, lab))
                        if (x < minX) minX = x
                        if (x > maxX) maxX = x
                        if (y < minY) minY = y
                        if (y > maxY) maxY = y
                    }
                    PixelClassification.SPECULAR_REFLECTION -> {
                        specularLabs.add(lab)
                    }
                    PixelClassification.GINGIVA, PixelClassification.LIP -> {
                        gingivaCount++
                    }
                    else -> {}
                }
            }
        }

        val isToothDetected = validToothLabs.size > 80 && (maxX > minX) && (maxY > minY)
        val boundingBox = if (isToothDetected) {
            Rect(minX, minY, maxX, maxY)
        } else {
            Rect(roiLeft, roiTop, roiRight, roiBottom)
        }

        // Divide into cervical, middle, incisal zones
        val boxHeight = max(1, boundingBox.height())
        val cervicalLabs = mutableListOf<Lab>()
        val middleLabs = mutableListOf<Lab>()
        val incisalLabs = mutableListOf<Lab>()

        for ((_, y, lab) in toothPixelCoords) {
            val relativeY = (y - boundingBox.top).toDouble() / boxHeight
            if (isMaxillary) {
                // Upper arch: top 30% is cervical, mid 40% is middle, bottom 30% is incisal
                when {
                    relativeY < 0.30 -> cervicalLabs.add(lab)
                    relativeY < 0.70 -> middleLabs.add(lab)
                    else -> incisalLabs.add(lab)
                }
            } else {
                // Lower arch: bottom 30% is cervical, mid 40% is middle, top 30% is incisal
                when {
                    relativeY > 0.70 -> cervicalLabs.add(lab)
                    relativeY > 0.30 -> middleLabs.add(lab)
                    else -> incisalLabs.add(lab)
                }
            }
        }

        val cervicalBox = if (isMaxillary) {
            Rect(boundingBox.left, boundingBox.top, boundingBox.right, boundingBox.top + (boxHeight * 0.3).toInt())
        } else {
            Rect(boundingBox.left, boundingBox.top + (boxHeight * 0.7).toInt(), boundingBox.right, boundingBox.bottom)
        }

        val middleBox = Rect(
            boundingBox.left,
            boundingBox.top + (boxHeight * 0.3).toInt(),
            boundingBox.right,
            boundingBox.top + (boxHeight * 0.7).toInt()
        )

        val incisalBox = if (isMaxillary) {
            Rect(boundingBox.left, boundingBox.top + (boxHeight * 0.7).toInt(), boundingBox.right, boundingBox.bottom)
        } else {
            Rect(boundingBox.left, boundingBox.top, boundingBox.right, boundingBox.top + (boxHeight * 0.3).toInt())
        }

        return ToothSegmentationResult(
            isToothDetected = isToothDetected,
            toothBoundingBox = boundingBox,
            totalToothPixels = validToothLabs.size,
            toothLabPixels = validToothLabs,
            cervicalZone = SegmentedZonePixels("Cervical", cervicalLabs, cervicalBox),
            middleZone = SegmentedZonePixels("Middle Body", middleLabs, middleBox),
            incisalZone = SegmentedZonePixels("Incisal Edge", incisalLabs, incisalBox),
            specularReflectionPixelCount = specularLabs.size,
            gingivaPixelCount = gingivaCount,
            referenceTabDetected = false,
            referenceTabPixels = null
        )
    }

    private fun classifyPixel(r: Int, g: Int, b: Int, lab: Lab): PixelClassification {
        // Dark background / interproximal crevices / oral cavity
        if (lab.l < 32.0) {
            return PixelClassification.BACKGROUND_DARK
        }

        val maxC = max(r, max(g, b))
        val minC = min(r, min(g, b))
        val delta = maxC - minC
        val sat = if (maxC > 0) delta.toDouble() / maxC else 0.0

        // Specular saliva / flash reflection: very bright L*, desaturated
        if (lab.l > 89.0 && sat < 0.12) {
            return PixelClassification.SPECULAR_REFLECTION
        }

        // Soft tissue / Gingiva / Lip: reddish hue, a* > 13 or high red compared to green & blue
        if (lab.a > 13.5 && r > (g + 20) && (r > b + 15)) {
            return PixelClassification.GINGIVA
        }

        // Dental Enamel characteristic space:
        // L* typically 55..88 (natural human enamel)
        // a* typically -3.0 .. 8.0 (slight red-brown tint)
        // b* typically 6.0 .. 34.0 (yellowish hue)
        // Saturation typically 0.06 .. 0.50
        if (lab.l in 48.0..88.5 && lab.a in -3.5..9.5 && lab.b in 5.5..34.0) {
            return PixelClassification.TOOTH_ENAMEL
        }

        return PixelClassification.UNKNOWN
    }
}
