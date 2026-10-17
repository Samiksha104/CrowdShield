package com.example.crowdshield.models

enum class ReportRange(val label: String) {
    TODAY("Today"),
    WEEK("Week"),
    MONTH("Month")
}

data class ReportSummary(
    val crowdSeries: List<Int>, // sample points for the line chart
    val peakCrowd: Int,
    val averageCrowd: Int,
    val alertCount: Int,
    val incidentCount: Int
)