package com.example.crowdshield.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.crowdshield.repository.UserPreferencesRepository

/**
 * Fires an actual Android system notification for critical alerts — the
 * piece that was missing before. This is a LOCAL notification triggered by
 * app code (e.g. Report Incident). Once a real backend/model exists, the
 * same showCriticalAlert() call should be made from wherever a CRITICAL
 * alert arrives (e.g. a WebSocket listener), not just from this one path.
 */
object NotificationHelper {

    private const val CHANNEL_ID = "crowdshield_critical_alerts"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java)
        val existing = manager.getNotificationChannel(CHANNEL_ID)
        if (existing == null) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Critical Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "High-priority crowd safety alerts"
            }
            manager.createNotificationChannel(channel)
        }
    }

    /**
     * Respects the Notifications toggle in Settings — call this from a
     * coroutine scope since the preference read is suspend.
     */
    suspend fun showCriticalAlertIfEnabled(context: Context, title: String, message: String) {
        if (!UserPreferencesRepository.isNotificationsEnabled(context)) return

        ensureChannel(context)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            // TODO: set a real small icon (android.R.drawable.ic_dialog_alert is a placeholder)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .build()

        // POST_NOTIFICATIONS permission is required at runtime on API 33+.
        // This assumes it's already been requested elsewhere in the app;
        // NotificationManagerCompat.areNotificationsEnabled() can guard this further if needed.
        NotificationManagerCompat.from(context).notify(System.currentTimeMillis().toInt(), notification)
    }
}