package com.example.crowdshield.models

/**
 * Represents an officer record as it would come from the backend / admin portal.
 * firstLogin and status are controlled by the server — the app never sets these itself.
 */
data class Officer(
    val badgeId: String,
    val password: String,
    val firstLogin: Boolean,
    val name: String = "Officer",
    val status: OfficerStatus = OfficerStatus.ACTIVE,
    val district: String? = null,
    val role: String = "Officer",
    val department: String = "Crowd Control Unit",
    val phone: String = "",
    val isAdmin: Boolean = false
)

enum class OfficerStatus {
    ACTIVE,
    INACTIVE
}