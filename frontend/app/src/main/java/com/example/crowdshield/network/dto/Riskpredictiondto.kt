package com.example.crowdshield.network.dto

/**
 * Matches /api/risk-prediction. CONFIRMED from a real live response:
 * {"riskPercent":0,"riskLabel":"LOW","confidencePercent":0,"expectedCrowd":0,
 *  "trend":"Stable","recommendations":[]}
 */
data class RiskPredictionDto(
    val riskPercent: Int,
    val riskLabel: String,
    val confidencePercent: Int,
    val expectedCrowd: Int,
    val trend: String,
    val recommendations: List<String>
)