package com.example.crowdshield.repository

import com.example.crowdshield.models.ExitRoute

/**
 * PROTOTYPE ONLY — stands in for the real route-planning engine. Swap for a
 * live source once that exists; keep the same function signatures so
 * EvacuationActivity doesn't need to change.
 */
object EvacuationRepository {

    private val exits = listOf(
        ExitRoute(id = "exit_a", name = "Exit A", etaMinutes = 2, xFraction = 0.25f, yFraction = 0.30f),
        ExitRoute(id = "exit_b", name = "Exit B", etaMinutes = 5, xFraction = 0.75f, yFraction = 0.35f),
        ExitRoute(id = "exit_c", name = "Exit C", etaMinutes = null, xFraction = 0.50f, yFraction = 0.70f)
    )

    private const val recommendedExitId = "exit_a"
    private const val recommendedConfidence = 97

    fun getExits(): List<ExitRoute> = exits

    fun getRecommended(): Pair<ExitRoute, Int> {
        val exit = exits.find { it.id == recommendedExitId } ?: exits.first()
        return exit to recommendedConfidence
    }
}