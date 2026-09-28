package com.example.colorengine.xyz

/**
 * CIE 1931 XYZ tristimulus color representation.
 * Standard scale: Y is normalized to 100.0 for reference diffuse white.
 */
data class XyzColor(
    val x: Double,
    val y: Double,
    val z: Double
) {
    fun formatted(): String = "X: %.3f, Y: %.3f, Z: %.3f".format(x, y, z)
}
