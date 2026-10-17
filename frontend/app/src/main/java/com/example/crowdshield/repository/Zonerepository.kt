package com.example.crowdshield.repository

import com.example.crowdshield.models.RiskLevel
import com.example.crowdshield.models.Zone
import com.example.crowdshield.network.RetrofitClient

/**
 * NOW LIVE — calls /api/zones. Same function signatures as the old
 * in-memory version, so HeatmapActivity's calling code barely changes.
 */
object ZoneRepository {

    suspend fun getAll(): List<Zone> {
        return RetrofitClient.apiService.getZones().map { dto ->
            Zone(
                id = dto.id,
                name = dto.name,
                xFraction = dto.xFraction,
                yFraction = dto.yFraction,
                peopleCount = dto.peopleCount,
                riskLevel = parseRiskLevel(dto.riskLevel),
                recommendation = dto.recommendation
            )
        }
    }

    suspend fun getById(id: String): Zone? = getAll().find { it.id == id }

    private fun parseRiskLevel(raw: String): RiskLevel =
        try {
            RiskLevel.valueOf(raw.uppercase())
        } catch (e: IllegalArgumentException) {
            RiskLevel.NORMAL // safe fallback if the backend sends an unexpected value
        }
}