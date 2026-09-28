package com.example.colorengine

import org.junit.Assert.*
import org.junit.Test

class ColorEngineTest {

    @Test
    fun testSrgbLinearizationEndpoints() {
        val lin0 = ColorConversions.sRgbToLinear(0)
        val lin255 = ColorConversions.sRgbToLinear(255)
        assertEquals(0.0, lin0, 1e-6)
        assertEquals(1.0, lin255, 1e-6)
    }

    @Test
    fun testRgbToLabD65WhitePoint() {
        // Pure sRGB white (255, 255, 255) under standard D65 transforms to L* ≈ 100, a* ≈ 0, b* ≈ 0
        val labWhite = ColorConversions.rgbToLab(255, 255, 255)
        assertEquals(100.0, labWhite.l, 0.5)
        assertEquals(0.0, labWhite.a, 0.8)
        assertEquals(0.0, labWhite.b, 0.8)

        // Pure black (0, 0, 0) transforms to L* = 0, a* = 0, b* = 0
        val labBlack = ColorConversions.rgbToLab(0, 0, 0)
        assertEquals(0.0, labBlack.l, 0.1)
        assertEquals(0.0, labBlack.a, 0.1)
        assertEquals(0.0, labBlack.b, 0.1)
    }

    @Test
    fun testCiede2000IdenticalColors() {
        val lab1 = Lab(74.8, 1.6, 20.3)
        val deltaE = Ciede2000.calculate(lab1, lab1)
        assertEquals(0.0, deltaE, 1e-6)
    }

    @Test
    fun testCiede2000SharmaReferencePair() {
        // Standard Sharma et al. (2005) CIEDE2000 Test Pair #1:
        // L1=50.0000, a1=2.6772, b1=-79.7751
        // L2=50.0000, a2=0.0000, b2=-82.7485
        // Expected ΔE00 ≈ 2.0425
        val lab1 = Lab(50.0000, 2.6772, -79.7751)
        val lab2 = Lab(50.0000, 0.0000, -82.7485)
        val deltaE = Ciede2000.calculate(lab1, lab2)
        assertEquals(2.0425, deltaE, 0.001)
    }

    @Test
    fun testAll16VitaShadesSelfMatchAsTopRanked() {
        // Acceptance Criterion: All 16 reference shades must correctly rank their own reference first
        // with deltaE00 = 0.0
        val shades = VitaClassicalData.ALL_16_SHADES
        assertEquals(16, shades.size)

        for (shade in shades) {
            val report = ShadeMatcher.matchShade(shade.lab)
            assertEquals("Shade ${shade.shadeCode} did not rank #1", shade.shadeCode, report.recommendedShade.shadeCode)
            assertEquals("Shade ${shade.shadeCode} deltaE00 is not 0", 0.0, report.recommendedShade.deltaE00, 0.01)
            assertEquals(1, report.recommendedShade.rank)
            assertNotNull(report.secondShade)
            assertNotNull(report.thirdShade)
        }
    }

    @Test
    fun testCloseAmbiguityDetection() {
        // A2 Lab is (77.2, 0.4, 17.5) and D3 is (73.9, 0.7, 17.2)
        // Construct a measured Lab intermediate between A2 and D3
        val intermediateLab = Lab(75.5, 0.5, 17.3)
        val report = ShadeMatcher.matchShade(intermediateLab)
        val diff = Math.abs(report.secondShade.deltaE00 - report.recommendedShade.deltaE00)
        if (diff < ShadeMatcher.AMBIGUITY_DELTA_THRESHOLD) {
            assertTrue("Expected ambiguity warning for close candidates", report.isAmbiguous)
            assertNotNull(report.ambiguityMessage)
        }
    }
}
