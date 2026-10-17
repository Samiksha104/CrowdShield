package com.example.crowdshield.repository

import com.example.crowdshield.models.ReportRange
import com.example.crowdshield.models.ReportSummary

/**
 * PROTOTYPE ONLY — stands in for the real analytics pipeline. Swap for a
 * live/API source later; keep the same function signature so ReportsActivity
 * doesn't need to change.
 */
object ReportRepository {

    fun getSummary(range: ReportRange): ReportSummary = when (range) {
        ReportRange.TODAY -> ReportSummary(
            crowdSeries = listOf(1200, 2400, 4100, 6800, 9320, 7600, 5200, 3100),
            peakCrowd = 9320,
            averageCrowd = 6450,
            alertCount = 42,
            incidentCount = 5
        )
        ReportRange.WEEK -> ReportSummary(
            crowdSeries = listOf(4200, 5100, 4800, 6200, 7300, 9800, 8900),
            peakCrowd = 9800,
            averageCrowd = 6614,
            alertCount = 187,
            incidentCount = 21
        )
        ReportRange.MONTH -> ReportSummary(
            crowdSeries = listOf(5200, 6100, 5800, 7200, 8300, 9100, 8600, 7400, 6900, 7800, 8900, 9600),
            peakCrowd = 9600,
            averageCrowd = 7575,
            alertCount = 743,
            incidentCount = 68
        )
    }
}