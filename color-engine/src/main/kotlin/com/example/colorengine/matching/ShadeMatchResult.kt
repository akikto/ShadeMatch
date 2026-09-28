package com.example.colorengine.matching

import com.example.colorengine.lab.LabColor

data class ShadeMatchResult(
    val shadeCode: String,
    val deltaE00: Double,
    val rank: Int,
    val referenceLab: LabColor,
    val provenance: String
)

data class ComprehensiveMatchReport(
    val measuredLab: LabColor,
    val topMatches: List<ShadeMatchResult>,
    val recommendedShade: ShadeMatchResult,
    val secondShade: ShadeMatchResult,
    val thirdShade: ShadeMatchResult,
    val isAmbiguous: Boolean,
    val ambiguityMessage: String?,
    val perceptibilityCategory: String,
    val algorithmVersion: String
)
