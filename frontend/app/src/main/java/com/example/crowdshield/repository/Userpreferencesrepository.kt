package com.example.crowdshield.repository

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "crowdshield_prefs")

/**
 * Single DataStore-backed source of truth for everything that must survive
 * an app restart: theme, notifications, language, and profile edits.
 *
 * Requires the dependency:
 *   implementation("androidx.datastore:datastore-preferences:1.1.1")
 */
object UserPreferencesRepository {

    private val DARK_MODE = booleanPreferencesKey("dark_mode")
    private val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
    private val LANGUAGE = stringPreferencesKey("language")
    private val PROFILE_NAME = stringPreferencesKey("profile_name")
    private val PROFILE_PHONE = stringPreferencesKey("profile_phone")
    private val PROFILE_ZONE = stringPreferencesKey("profile_zone")

    // --- Dark mode ---
    fun darkModeFlow(context: Context): Flow<Boolean> =
        context.dataStore.data.map { it[DARK_MODE] ?: false }

    suspend fun setDarkMode(context: Context, enabled: Boolean) {
        context.dataStore.edit { it[DARK_MODE] = enabled }
    }

    // --- Notifications ---
    fun notificationsEnabledFlow(context: Context): Flow<Boolean> =
        context.dataStore.data.map { it[NOTIFICATIONS_ENABLED] ?: true }

    suspend fun setNotificationsEnabled(context: Context, enabled: Boolean) {
        context.dataStore.edit { it[NOTIFICATIONS_ENABLED] = enabled }
    }

    /** One-shot read, for call sites outside Compose (e.g. before firing a system notification). */
    suspend fun isNotificationsEnabled(context: Context): Boolean =
        context.dataStore.data.first()[NOTIFICATIONS_ENABLED] ?: true

    // --- Language ---
    fun languageFlow(context: Context): Flow<String> =
        context.dataStore.data.map { it[LANGUAGE] ?: "English" }

    suspend fun setLanguage(context: Context, language: String) {
        context.dataStore.edit { it[LANGUAGE] = language }
    }

    // --- Profile edits (overrides on top of AuthRepository's seed data) ---
    data class ProfileOverride(val name: String?, val phone: String?, val zone: String?)

    fun profileOverrideFlow(context: Context): Flow<ProfileOverride> =
        context.dataStore.data.map {
            ProfileOverride(
                name = it[PROFILE_NAME],
                phone = it[PROFILE_PHONE],
                zone = it[PROFILE_ZONE]
            )
        }

    suspend fun setProfileOverride(context: Context, name: String, phone: String, zone: String) {
        context.dataStore.edit {
            it[PROFILE_NAME] = name
            it[PROFILE_PHONE] = phone
            it[PROFILE_ZONE] = zone
        }
    }
}