package com.example.colorengine

import org.junit.Assert.*
import org.junit.Test

class ColorEngineFullPipelineTest {

    private val engine = ColorEngine.DEFAULT

    @Test
    fun testColorEngineRgbToLinearConversion() {
        val linBlack = engine.rgbToLinearRgb(0, 0, 0)
        assertEquals(0.0, linBlack.r, 1e-6)
        assertEquals(0.0, linBlack.g, 1e-6)
        assertEquals(0.0, linBlack.b, 1e-6)

        val linWhite = engine.rgbToLinearRgb(255, 255, 255)
        assertEquals(1.0, linWhite.r, 1e-6)
        assertEquals(1.0, linWhite.g, 1e-6)
        assertEquals(1.0, linWhite.b, 1e-6)
    }

    @Test
    fun testColorEngineLinearToXyz() {
        val linWhite = LinearRgb(1.0, 1.0, 1.0)
        val xyz = engine.linearRgbToXyz(linWhite)
        assertEquals(95.047, xyz.x, 0.05)
        assertEquals(100.000, xyz.y, 0.05)
        assertEquals(108.883, xyz.z, 0.05)
    }

    @Test
    fun testColorEngineXyzToLab() {
        // Standard D65 white point transforms to L* = 100, a* = 0, b* = 0
        val whiteLab = engine.xyzToLab(Xyz.D65_2DEG)
        assertEquals(100.0, whiteLab.l, 0.001)
        assertEquals(0.0, whiteLab.a, 0.001)
        assertEquals(0.0, whiteLab.b, 0.001)
    }

    @Test
    fun testColorEngineRgbToLabDirect() {
        val lab = engine.rgbToLab(255, 255, 255)
        assertEquals(100.0, lab.l, 0.5)
        assertEquals(0.0, lab.a, 0.5)
        assertEquals(0.0, lab.b, 0.5)
    }

    @Test
    fun testColorEngineRoundtripLabToRgb() {
        val originalLab = Lab(77.2, 0.4, 17.5) // A2 standard
        val (r, g, b) = engine.labToRgb(originalLab)
        val roundtripLab = engine.rgbToLab(r, g, b)

        // Tolerance within integer 8-bit quantization error
        assertEquals(originalLab.l, roundtripLab.l, 1.0)
        assertEquals(originalLab.a, roundtripLab.a, 1.0)
        assertEquals(originalLab.b, roundtripLab.b, 1.0)
    }

    @Test
    fun testColorEngineCiede2000Identical() {
        val lab = Lab(74.8, 1.6, 20.3)
        val de00 = engine.calculateDeltaE00(lab, lab)
        assertEquals(0.0, de00, 1e-6)
    }

    @Test
    fun testColorEngineSelfTestReport() {
        val report = engine.verifyReferenceSelfMatching()
        assertTrue("ColorEngine self-test failed: ${report.failures}", report.passed)
        assertEquals(16, report.totalShadesTested)
        assertTrue(report.failures.isEmpty())
    }

    @Test
    fun testColorEngineFindClosestShade() {
        val sampleLab = Lab(81.3, -1.4, 14.1) // Exact B1 reference
        val result = engine.findClosestShade(sampleLab)
        assertEquals("B1", result.shadeCode)
        assertEquals(0.0, result.deltaE00, 0.001)
        assertEquals(1, result.rank)
    }

    @Test
    fun testColorEngineMatchToothColorWithZones() {
        val cervical = Lab(75.0, 1.0, 20.0)
        val middle = Lab(77.2, 0.4, 17.5) // A2
        val incisal = Lab(80.0, -0.2, 14.0)

        val report = engine.matchToothColor(
            measuredLab = middle,
            cervicalLab = cervical,
            middleLab = middle,
            incisalLab = incisal
        )

        assertEquals("A2", report.recommendedShade.shadeCode)
        assertNotNull(report.zones)
        assertEquals("A2", report.zones?.middle?.topMatch?.shadeCode)
    }

    @Test
    fun testColorEngineFindClosestVitaShade() {
        val measuredA3 = Lab(74.8, 1.6, 20.3)
        val closestShade = engine.findClosestVitaShade(measuredA3)
        assertEquals("A3", closestShade.shadeCode)
        assertEquals(74.8, closestShade.lStar, 0.01)

        val (shade, distance) = engine.findClosestVitaShadeWithDistance(measuredA3)
        assertEquals("A3", shade.shadeCode)
        assertEquals(0.0, distance, 0.01)

        val closestByComponents = engine.findClosestVitaShade(77.2, 0.4, 17.5)
        assertEquals("A2", closestByComponents.shadeCode)
    }
}

