package com.example.crowdshield.models

data class ExitRoute(
    val id: String,
    val name: String,
    val etaMinutes: Int?, // null means blocked
    val xFraction: Float,
    val yFraction: Float
) {
    val isBlocked: Boolean get() = etaMinutes == null
}