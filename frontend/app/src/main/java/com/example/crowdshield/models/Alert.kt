package com.example.crowdshield.models

enum class AlertSeverity { CRITICAL, MEDIUM, LOW }

data class Alert(
    val id: String,
    val title: String,
    val location: String,
    val severity: AlertSeverity,
    val time: String,
    val description: String
)