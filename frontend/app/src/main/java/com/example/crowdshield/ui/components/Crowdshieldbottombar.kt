package com.example.crowdshield.ui.components

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.sp
import com.example.crowdshield.activities.AlertsActivity
import com.example.crowdshield.activities.HeatmapActivity
import com.example.crowdshield.activities.LiveMonitoringActivity
import com.example.crowdshield.activities.MainActivity
import com.example.crowdshield.activities.ProfileActivity
import com.example.crowdshield.repository.AlertRepository

private val BrandBlue = Color(0xFF2743E0)
private val TextHint = Color(0xFF9CA3AF)
private val CardWhite = Color(0xFFFFFFFF)
private val RiskRed = Color(0xFFDC2626)

/**
 * The five destinations every post-login screen shares.
 */
enum class BottomTab(val label: String, val icon: ImageVector) {
    HOME("Home", Icons.Filled.Home),
    LIVE("Live", Icons.Filled.Videocam),
    MAP("Map", Icons.Filled.Map),
    ALERTS("Alerts", Icons.Filled.Warning),
    PROFILE("Profile", Icons.Filled.Person)
}

/**
 * Drop this into the `bottomBar` slot of any Scaffold.
 *
 * @param selected which tab to highlight — pass null if this screen isn't one
 *   of the five root destinations (e.g. Risk Prediction, opened from a quick action).
 * @param alertBadgeCount shows a red numbered badge on the Alerts tab when > 0.
 * @param containerColor lets dark-themed screens (like Risk Prediction) match their background.
 */
@Composable
fun CrowdShieldBottomBar(
    selected: BottomTab?,
    alertBadgeCount: Int = AlertRepository.cachedCriticalCount(),
    containerColor: Color = CardWhite
) {
    val context = LocalContext.current

    NavigationBar(containerColor = containerColor) {
        BottomTab.entries.forEach { tab ->
            NavigationBarItem(
                selected = tab == selected,
                onClick = { navigateToTab(context, tab, selected) },
                icon = {
                    if (tab == BottomTab.ALERTS && alertBadgeCount > 0) {
                        BadgedBox(
                            badge = {
                                Badge(containerColor = RiskRed) { Text("$alertBadgeCount") }
                            }
                        ) {
                            Icon(tab.icon, contentDescription = tab.label)
                        }
                    } else {
                        Icon(tab.icon, contentDescription = tab.label)
                    }
                },
                label = { Text(tab.label, fontSize = 11.sp) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = BrandBlue,
                    selectedTextColor = BrandBlue,
                    indicatorColor = BrandBlue.copy(alpha = 0.1f),
                    unselectedIconColor = TextHint,
                    unselectedTextColor = TextHint
                )
            )
        }
    }
}

private fun navigateToTab(context: Context, tab: BottomTab, current: BottomTab?) {
    if (tab == current) return // already here

    val targetActivity = when (tab) {
        BottomTab.HOME -> MainActivity::class.java
        BottomTab.LIVE -> LiveMonitoringActivity::class.java
        BottomTab.MAP -> HeatmapActivity::class.java
        BottomTab.ALERTS -> AlertsActivity::class.java
        BottomTab.PROFILE -> ProfileActivity::class.java
    }

    val intent = Intent(context, targetActivity)
    // Carry the officer's badge ID forward so Home/Live/Map all know who's logged in.
    (context as? Activity)?.intent?.getStringExtra("badge_id")?.let {
        intent.putExtra("badge_id", it)
    }
    context.startActivity(intent)
    (context as? Activity)?.finish()
}