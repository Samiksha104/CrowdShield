package com.example.crowdshield.models

enum class NotificationSeverity { HIGH, MEDIUM, LOW }

data class NotificationItem(
    val id: String,
    val title: String,
    val location: String,
    val severity: NotificationSeverity,
    val timeAgo: String
)