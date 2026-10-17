package com.example.crowdshield.repository

import com.example.crowdshield.models.Officer
import com.example.crowdshield.models.OfficerStatus

/**
 * Result of an authentication attempt. The UI branches on this — it never
 * decides firstLogin itself, it only reacts to what the "backend" says.
 */
sealed class AuthResult {
    data class Success(val firstLogin: Boolean, val officer: Officer) : AuthResult()
    object InvalidCredentials : AuthResult()
    object AccountInactive : AuthResult()
}

sealed class PasswordChangeResult {
    object Success : PasswordChangeResult()
    object SamePasswordNotAllowed : PasswordChangeResult()
    object PasswordTooWeak : PasswordChangeResult()
}

/**
 * PROTOTYPE ONLY — this stands in for your real backend/API + local database.
 * Replace the in-memory map with actual network calls (Retrofit/api/ layer)
 * once your backend exists. Keep the same function signatures so LoginActivity
 * and ChangePasswordActivity don't need to change when you swap this out.
 */
object AuthRepository {

    // Simulated officer database — seeded like your admin portal would seed it.
    private val officers = mutableMapOf(
        "TN4471" to Officer(
            badgeId = "TN4471",
            password = "Temp@123",
            firstLogin = true,
            name = "Arun Kumar",
            role = "Inspector",
            district = "Chennai District",
            department = "Crowd Control Unit",
            phone = "+91 98765 43210"
        ),
        "TN4472" to Officer(
            badgeId = "TN4472",
            password = "Temp@123",
            firstLogin = true,
            name = "Priya Raman",
            role = "Sub-Inspector",
            district = "Chennai District",
            department = "Crowd Control Unit",
            phone = "+91 98765 43211"
        ),
        "ADMIN01" to Officer(
            badgeId = "ADMIN01",
            password = "Admin@123",
            firstLogin = false,
            name = "System Administrator",
            role = "Administrator",
            district = "Headquarters",
            department = "Crowd Control Unit",
            phone = "+91 90000 00000",
            isAdmin = true
        )
    )

    /** Fetch the current officer record — used by the dashboard to show real details. */
    fun getOfficer(badgeId: String): Officer? = officers[badgeId.trim().uppercase()]

    /** Used by the Officer Registry screen to list existing accounts. */
    fun getAllOfficers(): List<Officer> = officers.values.sortedBy { it.badgeId }

    /**
     * Admin-only: creates a new officer account. firstLogin is always forced
     * to true here — the admin sets a temporary password, never decides
     * whether this is the officer's first login themselves.
     * Returns false if the badge ID is already taken.
     */
    fun addOfficer(
        badgeId: String,
        initialPassword: String,
        name: String,
        department: String,
        role: String,
        assignedZone: String,
        phone: String,
        status: OfficerStatus
    ): Boolean {
        val key = badgeId.trim().uppercase()
        if (officers.containsKey(key)) return false

        officers[key] = Officer(
            badgeId = key,
            password = initialPassword,
            firstLogin = true, // enforced — never settable by the admin form
            name = name,
            status = status,
            district = assignedZone,
            role = role,
            department = department,
            phone = phone,
            isAdmin = false
        )
        return true
    }

    fun authenticate(badgeId: String, password: String): AuthResult {
        val officer = officers[badgeId.trim().uppercase()] ?: return AuthResult.InvalidCredentials

        if (officer.password != password) {
            return AuthResult.InvalidCredentials
        }
        if (officer.status == OfficerStatus.INACTIVE) {
            return AuthResult.AccountInactive
        }
        return AuthResult.Success(firstLogin = officer.firstLogin, officer = officer)
    }

    fun changePassword(badgeId: String, currentPassword: String, newPassword: String): PasswordChangeResult {
        val key = badgeId.trim().uppercase()
        val officer = officers[key] ?: return PasswordChangeResult.SamePasswordNotAllowed

        if (newPassword == currentPassword) {
            return PasswordChangeResult.SamePasswordNotAllowed
        }
        if (newPassword.length < 8) {
            return PasswordChangeResult.PasswordTooWeak
        }

        // Backend would persist this; here we just update the in-memory record.
        officers[key] = officer.copy(password = newPassword, firstLogin = false)
        return PasswordChangeResult.Success
    }
}