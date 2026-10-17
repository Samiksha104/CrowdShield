package com.example.crowdshield.repository

import com.example.crowdshield.models.Camera
import com.example.crowdshield.models.CameraStatus
import com.example.crowdshield.network.RetrofitClient

/**
 * NOW LIVE — calls /api/cameras. Per the API docs, the backend currently
 * represents the single active video source as one camera; real
 * multi-camera support is a planned future extension. search()/getById()
 * keep working the same either way. Same function signatures as before,
 * so LiveMonitoringActivity's calling code barely changes.
 */
object CameraRepository {

    suspend fun getAll(): List<Camera> {
        return RetrofitClient.apiService.getCameras().map { dto ->
            Camera(
                id = dto.id,
                name = dto.name,
                zone = dto.zone,
                isLive = dto.isLive,
                peopleCount = dto.peopleCount,
                status = parseStatus(dto.status),
                lastUpdated = dto.lastUpdated,
                fps = dto.fps ?: 25
            )
        }
    }

    suspend fun search(query: String): List<Camera> {
        val all = getAll()
        if (query.isBlank()) return all
        return all.filter {
            it.name.contains(query, ignoreCase = true) || it.zone.contains(query, ignoreCase = true)
        }
    }

    suspend fun getById(id: String): Camera? = getAll().find { it.id == id }

    private fun parseStatus(raw: String): CameraStatus =
        try {
            CameraStatus.valueOf(raw.uppercase())
        } catch (e: IllegalArgumentException) {
            CameraStatus.SAFE
        }
}