package com.example.crowdshield.repository

import com.example.crowdshield.models.NotificationItem
import com.example.crowdshield.models.NotificationSeverity

/**
 * PROTOTYPE ONLY — stands in for the real NotificationService push feed.
 * Swap for a live source later; keep the same function signature.
 */
object NotificationRepository {

    private val notifications = listOf(
        NotificationItem(
            id = "notif_1",
            title = "High congestion",
            location = "Temple Entrance",
            severity = NotificationSeverity.HIGH,
            timeAgo = "2 mins ago"
        ),
        NotificationItem(
            id = "notif_2",
            title = "Risk Reduced",
            location = "Platform 3",
            severity = NotificationSeverity.MEDIUM,
            timeAgo = "10 mins ago"
        ),
        NotificationItem(
            id = "notif_3",
            title = "Route Updated",
            location = "Exit A",
            severity = NotificationSeverity.LOW,
            timeAgo = "20 mins ago"
        )
    )

    fun getAll(): List<NotificationItem> = notifications
}