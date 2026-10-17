package com.example.crowdshield.models

enum class CameraStatus { SAFE, WARNING, CRITICAL }

data class Camera(
    val id: String,
    val name: String,
    val zone: String,
    val isLive: Boolean,
    val peopleCount: Int,
    val status: CameraStatus,
    val lastUpdated: String,
    val fps: Int = 25
)