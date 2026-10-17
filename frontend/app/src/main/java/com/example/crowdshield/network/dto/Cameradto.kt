package com.example.crowdshield.network.dto

/**
 * Matches /api/cameras. Currently the backend represents the single active
 * video source as one camera — real multi-camera support is a planned
 * future extension, per the API docs. Empty array until the pipeline has data.
 */
data class CameraDto(
    val id: String,
    val name: String,
    val zone: String,
    val isLive: Boolean,
    val peopleCount: Int,
    val status: String, // "SAFE" | "WARNING" | "CRITICAL"
    val lastUpdated: String,
    val fps: Int? = null
)