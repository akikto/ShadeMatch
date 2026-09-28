package com.example.colorengine

import kotlin.math.*

/**
 * Standard implementation of CIEDE2000 color difference formula.
 * Reference: Gaurav Sharma, Wencheng Wu, Edul N. Dalal,
 * "The CIEDE2000 Color-Difference Formula: Implementation Notes, Supplementary Test Data, and Mathematical Observations",
 * Color Research & Application, Vol. 30, No. 1, Feb 2005.
 */
object Ciede2000 {

    private const val POW7_25 = 6103515625.0 // 25^7

    fun calculate(
        lab1: Lab,
        lab2: Lab,
        kL: Double = 1.0,
        kC: Double = 1.0,
        kH: Double = 1.0
    ): Double {
        val l1 = lab1.l
        val a1 = lab1.a
        val b1 = lab1.b

        val l2 = lab2.l
        val a2 = lab2.a
        val b2 = lab2.b

        val c1 = sqrt(a1 * a1 + b1 * b1)
        val c2 = sqrt(a2 * a2 + b2 * b2)

        val cBar = (c1 + c2) / 2.0
        val cBar7 = cBar.pow(7.0)

        val g = 0.5 * (1.0 - sqrt(cBar7 / (cBar7 + POW7_25)))

        val a1Prime = (1.0 + g) * a1
        val a2Prime = (1.0 + g) * a2

        val c1Prime = sqrt(a1Prime * a1Prime + b1 * b1)
        val c2Prime = sqrt(a2Prime * a2Prime + b2 * b2)

        val h1Prime = calculateHueAngle(b1, a1Prime)
        val h2Prime = calculateHueAngle(b2, a2Prime)

        val deltaLPrime = l2 - l1
        val deltaCPrime = c2Prime - c1Prime

        val deltaHPrime = calculateDeltaHPrime(c1Prime, c2Prime, h1Prime, h2Prime)

        val lBarPrime = (l1 + l2) / 2.0
        val cBarPrime = (c1Prime + c2Prime) / 2.0
        val hBarPrime = calculateHBarPrime(c1Prime, c2Prime, h1Prime, h2Prime)

        val t = 1.0 -
                0.17 * cos(Math.toRadians(hBarPrime - 30.0)) +
                0.24 * cos(Math.toRadians(2.0 * hBarPrime)) +
                0.32 * cos(Math.toRadians(3.0 * hBarPrime + 6.0)) -
                0.20 * cos(Math.toRadians(4.0 * hBarPrime - 63.0))

        val deltaTheta = 30.0 * exp(-((hBarPrime - 275.0) / 25.0).pow(2.0))

        val cBarPrime7 = cBarPrime.pow(7.0)
        val rC = 2.0 * sqrt(cBarPrime7 / (cBarPrime7 + POW7_25))

        val lBarMinus50Sq = (lBarPrime - 50.0).pow(2.0)
        val sL = 1.0 + (0.015 * lBarMinus50Sq) / sqrt(20.0 + lBarMinus50Sq)
        val sC = 1.0 + 0.045 * cBarPrime
        val sH = 1.0 + 0.015 * cBarPrime * t

        val rT = -sin(Math.toRadians(2.0 * deltaTheta)) * rC

        val termL = deltaLPrime / (kL * sL)
        val termC = deltaCPrime / (kC * sC)
        val termH = deltaHPrime / (kH * sH)

        val deltaE00Sq = termL * termL + termC * termC + termH * termH + rT * termC * termH
        return sqrt(max(0.0, deltaE00Sq))
    }

    private fun calculateHueAngle(b: Double, aPrime: Double): Double {
        if (abs(aPrime) < 1e-9 && abs(b) < 1e-9) return 0.0
        val angleRad = atan2(b, aPrime)
        var angleDeg = Math.toDegrees(angleRad)
        if (angleDeg < 0.0) {
            angleDeg += 360.0
        }
        return angleDeg
    }

    private fun calculateDeltaHPrime(
        c1Prime: Double,
        c2Prime: Double,
        h1Prime: Double,
        h2Prime: Double
    ): Double {
        if (c1Prime * c2Prime < 1e-9) return 0.0

        val diff = h2Prime - h1Prime
        val deltaHRad = when {
            abs(diff) <= 180.0 -> diff
            diff > 180.0 -> diff - 360.0
            else -> diff + 360.0
        }
        return 2.0 * sqrt(c1Prime * c2Prime) * sin(Math.toRadians(deltaHRad / 2.0))
    }

    private fun calculateHBarPrime(
        c1Prime: Double,
        c2Prime: Double,
        h1Prime: Double,
        h2Prime: Double
    ): Double {
        if (c1Prime * c2Prime < 1e-9) return h1Prime + h2Prime

        val diff = abs(h1Prime - h2Prime)
        return when {
            diff <= 180.0 -> (h1Prime + h2Prime) / 2.0
            h1Prime + h2Prime < 360.0 -> (h1Prime + h2Prime + 360.0) / 2.0
            else -> (h1Prime + h2Prime - 360.0) / 2.0
        }
    }
}
