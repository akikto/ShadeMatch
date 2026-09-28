package com.example.colorengine

import com.example.colorengine.lab.LabColor
import com.example.colorengine.reference.VitaClassicalDataset
import com.example.colorengine.rgb.RgbColor
import com.example.colorengine.xyz.Illuminants
import org.junit.Assert.*
import org.junit.Test

class ColorEnginePipelineTest {

    private val engine = ColorEngine.DEFAULT

    @Test
    fun testRgbToLinearConversion() {
        val lin0 = engine.sRgbToLinear(0)
        val lin255 = engine.sRgbToLinear(255)
        assertEquals(0.0, lin0, 1e-6)
        assertEquals(1.0, lin255, 1e-6)

        val rgbBlack = RgbColor(0, 0, 0)
        val linBlack = engine.rgbToLinearRgb(rgbBlack)
        assertEquals(0.0, linBlack.r, 1e-6)
        assertEquals(0.0, linBlack.g, 1e-6)
        assertEquals(0.0, linBlack.b, 1e-6)
    }

    @Test
    fun testLinearToXyzUnderD65() {
        val rgbWhite = RgbColor(255, 255, 255)
        val xyzWhite = engine.rgbToXyz(rgbWhite)

        assertEquals(95.047, xyzWhite.x, 0.05)
        assertEquals(100.000, xyzWhite.y, 0.05)
        assertEquals(108.883, xyzWhite.z, 0.05)
    }

    @Test
    fun testXyzToLabD65WhitePoint() {
        val labWhite = engine.xyzToLab(Illuminants.D65_2DEG)
        assertEquals(100.0, labWhite.l, 0.001)
        assertEquals(0.0, labWhite.a, 0.001)
        assertEquals(0.0, labWhite.b, 0.001)
    }

    @Test
    fun testCiede2000IdenticalColors() {
        val lab = LabColor(74.8, 1.6, 20.3)
        val deltaE = engine.calculateDeltaE00(lab, lab)
        assertEquals(0.0, deltaE, 1e-6)
    }

    @Test
    fun testCiede2000SharmaReferencePair() {
        // Standard Sharma et al. (2005) reference pair #1
        val lab1 = LabColor(50.0000, 2.6772, -79.7751)
        val lab2 = LabColor(50.0000, 0.0000, -82.7485)
        val deltaE = engine.calculateDeltaE00(lab1, lab2)
        assertEquals(2.0425, deltaE, 0.001)
    }

    @Test
    fun testAll16VitaShadesSelfMatchAsTopRanked() {
        val allShades = VitaClassicalDataset.ALL_16_SHADES
        assertEquals(16, allShades.size)

        for (ref in allShades) {
            val top = engine.findClosestShade(ref.lab)
            assertEquals("Shade ${ref.shadeCode} did not self-match as #1", ref.shadeCode, top.shadeCode)
            assertEquals("Shade ${ref.shadeCode} deltaE00 is not 0.0", 0.0, top.deltaE00, 0.01)
            assertEquals(1, top.rank)
        }
    }

    @Test
    fun testColorEngineVerifySelfMatching() {
        assertTrue("Self matching verification must pass for all 16 shades", engine.verifySelfMatching())
    }

    @Test
    fun testAmbiguityDetection() {
        // Lab intermediate between A2 (77.2, 0.4, 17.5) and D3 (73.9, 0.7, 17.2)
        val intermediate = LabColor(75.5, 0.5, 17.3)
        val matchReport = engine.matchToothColor(intermediate)

        val deltaDiff = Math.abs(matchReport.secondShade.deltaE00 - matchReport.recommendedShade.deltaE00)
        if (deltaDiff < 0.60) {
            assertTrue("Expected ambiguity warning when ΔE diff < 0.60", matchReport.isAmbiguous)
            assertNotNull(matchReport.ambiguityMessage)
        }
    }
}
