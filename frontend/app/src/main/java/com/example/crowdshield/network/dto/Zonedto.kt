package com.example.crowdshield.network.dto

/** Matches /api/zones. Empty array until the pipeline has processed a frame. */
data class ZoneDto(
    val id: String,
    val name: String,
    val xFraction: Float,
    val yFraction: Float,
    val peopleCount: Int,
    val riskLevel: String, // "NORMAL" | "BUSY" | "CONGESTED" | "CRITICAL"
    val recommendation: String
)