package com.example.colorengine

import org.junit.Assert.*
import org.junit.Test
import kotlin.math.sqrt

class ShadeComparisonTest {

    @Test
    fun testColorMetricDifferencesCalculation() {
        val scanned = Lab(77.5, 0.5, 17.8)
        val refA2 = VitaClassicalData.getByCode("A2")?.lab ?: Lab(77.2, 0.4, 17.5)

        val deltaL = scanned.l - refA2.l
        val deltaA = scanned.a - refA2.a
        val deltaB = scanned.b - refA2.b

        val cScanned = sqrt(scanned.a * scanned.a + scanned.b * scanned.b)
        val cRef = sqrt(refA2.a * refA2.a + refA2.b * refA2.b)
        val deltaC = cScanned - cRef

        val deltaE00 = Ciede2000.calculate(scanned, refA2)

        assertTrue("ΔL should be positive (tooth lighter)", deltaL > 0)
        assertTrue("ΔC should be positive (tooth slightly more chromatic)", deltaC > 0)
        assertTrue("ΔE00 should be small (close to A2)", deltaE00 < 1.0)
    }

    @Test
    fun testComparisonMatchesAgainstAllCandidates() {
        val testLab = Lab(74.8, 1.6, 20.3) // Identical to A3
        val matchReport = ShadeMatcher.matchShade(testLab)

        assertEquals("A3", matchReport.recommendedShade.shadeCode)
        assertEquals(0.0, matchReport.recommendedShade.deltaE00, 0.01)
        assertTrue("Top matches should contain at least 3 candidates", matchReport.topMatches.size >= 3)
    }
}
