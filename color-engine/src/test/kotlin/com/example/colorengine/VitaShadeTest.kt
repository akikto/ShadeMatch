package com.example.colorengine

import com.example.colorengine.reference.VitaShade
import org.junit.Assert.*
import org.junit.Test

class VitaShadeTest {

    @Test
    fun testVitaShadeProperties() {
        val a2 = VitaShade(shadeCode = "A2", lStar = 77.2, aStar = 0.4, bStar = 17.5)

        assertEquals("A2", a2.shadeCode)
        assertEquals(77.2, a2.lStar, 1e-6)
        assertEquals(0.4, a2.aStar, 1e-6)
        assertEquals(17.5, a2.bStar, 1e-6)
        assertEquals("Reddish-Brownish", a2.hueGroup)
        assertEquals("VITA_CLASSICAL", a2.system)

        val lab = a2.lab
        assertEquals(77.2, lab.l, 1e-6)
        assertEquals(0.4, lab.a, 1e-6)
        assertEquals(17.5, lab.b, 1e-6)
    }

    @Test
    fun testAll16VitaShadesPresent() {
        val shades = VitaShade.ALL_16
        assertEquals(16, shades.size)

        val expectedCodes = listOf(
            "A1", "A2", "A3", "A3.5", "A4",
            "B1", "B2", "B3", "B4",
            "C1", "C2", "C3", "C4",
            "D2", "D3", "D4"
        )

        for (code in expectedCodes) {
            val shade = VitaShade.fromCode(code)
            assertNotNull("Missing shade $code in VitaShade dataset", shade)
            assertEquals(code, shade?.shadeCode)
            assertTrue("lStar must be positive for $code", (shade?.lStar ?: 0.0) > 0.0)
            assertTrue("bStar must be positive for natural teeth $code", (shade?.bStar ?: 0.0) > 0.0)
        }
    }

    @Test
    fun testLookupCaseInsensitive() {
        val a1Lower = VitaShade.fromCode("a1")
        assertNotNull(a1Lower)
        assertEquals("A1", a1Lower?.shadeCode)
        assertEquals(79.8, a1Lower?.lStar ?: 0.0, 0.01)

        assertNull(VitaShade.fromCode("Z99"))
        assertFalse(VitaShade.isValidCode("E1"))
        assertTrue(VitaShade.isValidCode("B2"))
    }

    @Test
    fun testFindClosestVitaShadeUsingCiede2000() {
        val engine = ColorEngine.DEFAULT

        // Test 1: Exact coordinates for A2
        val exactA2 = com.example.colorengine.lab.LabColor(77.2, 0.4, 17.5)
        val matchA2 = engine.findClosestVitaShade(exactA2)
        assertEquals("A2", matchA2.shadeCode)

        // Test 2: Perturbed coordinates close to B1 (B1 is 81.3, -1.4, 14.1)
        val measuredNearB1 = com.example.colorengine.lab.LabColor(81.1, -1.3, 14.0)
        val matchB1 = engine.findClosestVitaShade(measuredNearB1)
        assertEquals("B1", matchB1.shadeCode)

        // Test 3: Scalar overload (L, a, b)
        val matchScalar = engine.findClosestVitaShade(74.8, 1.6, 20.3) // A3 reference
        assertEquals("A3", matchScalar.shadeCode)

        // Test 4: Distance pair
        val (shade, deltaE) = engine.findClosestVitaShadeWithDistance(exactA2)
        assertEquals("A2", shade.shadeCode)
        assertEquals(0.0, deltaE, 0.01)
    }
}

