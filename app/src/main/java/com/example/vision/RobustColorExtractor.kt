package com.example.vision

import com.example.colorengine.Lab
import kotlin.math.abs

data class RobustLabStats(
    val medianLab: Lab,
    val madL: Double,
    val madA: Double,
    val madB: Double,
    val compositeMad: Double,
    val sampleSize: Int
)

object RobustColorExtractor {

    /**
     * Calculates robust median and Median Absolute Deviation (MAD) for a collection of Lab pixels.
     * Robust to outliers compared to simple mean.
     */
    fun extractRobustLab(labPixels: List<Lab>): RobustLabStats? {
        if (labPixels.isEmpty()) return null

        val sortedL = labPixels.map { it.l }.sorted()
        val sortedA = labPixels.map { it.a }.sorted()
        val sortedB = labPixels.map { it.b }.sorted()

        val medianL = calculateMedian(sortedL)
        val medianA = calculateMedian(sortedA)
        val medianB = calculateMedian(sortedB)

        // Median Absolute Deviation (MAD)
        val deviationsL = sortedL.map { abs(it - medianL) }.sorted()
        val deviationsA = sortedA.map { abs(it - medianA) }.sorted()
        val deviationsB = sortedB.map { abs(it - medianB) }.sorted()

        val madL = calculateMedian(deviationsL)
        val madA = calculateMedian(deviationsA)
        val madB = calculateMedian(deviationsB)

        val compositeMad = (madL + madA + madB) / 3.0

        return RobustLabStats(
            medianLab = Lab(medianL, medianA, medianB),
            madL = (madL * 100).toInt() / 100.0,
            madA = (madA * 100).toInt() / 100.0,
            madB = (madB * 100).toInt() / 100.0,
            compositeMad = (compositeMad * 100).toInt() / 100.0,
            sampleSize = labPixels.size
        )
    }

    private fun calculateMedian(sortedList: List<Double>): Double {
        if (sortedList.isEmpty()) return 0.0
        val size = sortedList.size
        val mid = size / 2
        return if (size % 2 == 1) {
            sortedList[mid]
        } else {
            (sortedList[mid - 1] + sortedList[mid]) / 2.0
        }
    }
}
