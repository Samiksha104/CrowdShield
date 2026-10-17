package com.example.crowdshield.models

enum class RiskLevel { NORMAL, BUSY, CONGESTED, CRITICAL }

data class Zone(
    val id: String,
    val name: String,
    // Position as a fraction (0f–1f) of the map area, so markers scale to any screen size.
    val xFraction: Float,
    val yFraction: Float,
    val peopleCount: Int,
    val riskLevel: RiskLevel,
    val recommendation: String
)