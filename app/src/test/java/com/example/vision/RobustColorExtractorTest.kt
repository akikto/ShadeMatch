package com.example.vision

import com.example.calibration.CalibrationRegistry
import com.example.calibration.CalibrationStatus
import com.example.colorengine.Lab
import com.example.colorengine.VitaClassicalData
import org.junit.Assert.*
import org.junit.Test

class RobustColorExtractorTest {

    @Test
    fun testRobustLabCalculationFiltersOutliers() {
        // Enamel pixels clustered around L=77, a=0.4, b=17.5 with 2 extreme specular outliers
        val normalPixels = List(20) { Lab(77.0 + (it % 3) * 0.2, 0.4, 17.5) }
        val specularOutliers = listOf(Lab(98.0, 0.1, 1.2), Lab(99.5, 0.0, 0.8)) // bright glare
        val combined = normalPixels + specularOutliers

        val stats = RobustColorExtractor.extractRobustLab(combined)
        assertNotNull(stats)
        stats?.let {
            // Median L should remain close to 77.0 despite 99.5 outliers
            assertEquals(77.2, it.medianLab.l, 0.5)
            assertTrue("Expected sample size 22", it.sampleSize == 22)
            assertTrue("Composite MAD should be low for clustered values", it.compositeMad < 2.0)
        }
    }

    @Test
    fun testDeviceValidationRegistry() {
        val pixelResult = CalibrationRegistry.evaluateDevice("Pixel 8")
        assertFalse("A model name without verified camera configuration is not validation", pixelResult.isPreciseAnalysisAllowed)
        assertFalse(pixelResult.profile.validated)

        val uncalibResult = CalibrationRegistry.evaluateDevice("UnknownBrand XYZ Phone")
        assertEquals(CalibrationStatus.GENERIC_UNCALIBRATED, uncalibResult.status)
        assertFalse(uncalibResult.profile.validated)
        assertFalse(uncalibResult.isPreciseAnalysisAllowed)
        assertFalse(CalibrationRegistry.evaluateDevice("Pixel 8", "camera_0_wide").isPreciseAnalysisAllowed)
        assertFalse(CalibrationRegistry.evaluateDevice("SM-S921", "camera_0_wide").isPreciseAnalysisAllowed)
        assertEquals("REQUIRES_VALIDATED_DATA", VitaClassicalData.REFERENCE_STATUS)
        assertEquals(16, VitaClassicalData.ALL_16_SHADES.map { it.shadeCode }.toSet().size)
    }
}
