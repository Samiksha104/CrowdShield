package com.example.crowdshield.network.dto

/**
 * Matches /api/alerts. Per the API docs, this returns alert HISTORY (not
 * just current state) — grows automatically whenever a zone transitions
 * into CRITICAL. Empty array until the pipeline has data.
 */
data class AlertDto(
    val id: String,
    val title: String,
    val location: String,
    val severity: String, // "CRITICAL" | "MEDIUM" | "LOW"
    val time: String,
    val description: String
)