package com.example.crowdshield.repository

import com.example.crowdshield.models.Alert
import com.example.crowdshield.models.AlertSeverity
import com.example.crowdshield.network.RetrofitClient

/**
 * NOW LIVE for reading — calls /api/alerts, which returns alert HISTORY
 * (grows automatically whenever a zone transitions into CRITICAL, per the
 * API docs). addAlert() (used by Camera Feed's "Report Incident") stays
 * LOCAL-ONLY for now — there's no POST /api/alerts endpoint yet, so a
 * manually reported incident is only visible on this device until the
 * backend adds one. Locally added alerts are merged on top of the fetched
 * list so neither source overwrites the other.
 */
object AlertRepository {

    private val localAlerts = mutableListOf<Alert>()

    // Non-suspend snapshot for contexts that can't await a network call
    // (e.g. CrowdShieldBottomBar's default parameter). Updated every time
    // getAll() successfully runs; starts at 0 until the first fetch completes.
    @Volatile
    private var cachedCriticalCount = 0

    fun cachedCriticalCount(): Int = cachedCriticalCount

    suspend fun getAll(): List<Alert> {
        val remote = RetrofitClient.apiService.getAlerts().map { dto ->
            Alert(
                id = dto.id,
                title = dto.title,
                location = dto.location,
                severity = parseSeverity(dto.severity),
                time = dto.time,
                description = dto.description
            )
        }
        val combined = localAlerts + remote // local (manual) reports shown first
        cachedCriticalCount = combined.count { it.severity == AlertSeverity.CRITICAL }
        return combined
    }

    fun addAlert(alert: Alert) {
        localAlerts.add(0, alert)
    }

    suspend fun filterBySeverity(severity: AlertSeverity?): List<Alert> {
        val all = getAll()
        return if (severity == null) all else all.filter { it.severity == severity }
    }

    suspend fun criticalCount(): Int = getAll().count { it.severity == AlertSeverity.CRITICAL }

    private fun parseSeverity(raw: String): AlertSeverity =
        try {
            AlertSeverity.valueOf(raw.uppercase())
        } catch (e: IllegalArgumentException) {
            AlertSeverity.LOW
        }
}