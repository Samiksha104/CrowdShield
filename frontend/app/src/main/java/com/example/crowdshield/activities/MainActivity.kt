package com.example.crowdshield.activities

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.crowdshield.models.AlertSeverity
import com.example.crowdshield.models.RiskLevel
import com.example.crowdshield.repository.AlertRepository
import com.example.crowdshield.repository.AuthRepository
import com.example.crowdshield.repository.RiskPredictionRepository
import com.example.crowdshield.repository.ZoneRepository
import com.example.crowdshield.ui.components.BottomTab
import com.example.crowdshield.ui.components.CrowdShieldBottomBar
import com.example.crowdshield.ui.theme.CrowdShieldTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Palette — consistent with Login / Splash
private val BrandBlue = Color(0xFF2743E0)
private val PageBackground = Color(0xFFF4F5F9)
private val CardWhite = Color(0xFFFFFFFF)
private val TextPrimary = Color(0xFF1A1A1A)
private val TextSecondary = Color(0xFF6B7280)
private val TextHint = Color(0xFF9CA3AF)
private val SafeGreen = Color(0xFF16A34A)
private val SafeGreenBg = Color(0xFFE8F8EE)
private val WarnAmber = Color(0xFFF59E0B)
private val WarnAmberBg = Color(0xFFFFF6E5)
private val RiskRed = Color(0xFFDC2626)
private val RiskRedBg = Color(0xFFFDECEC)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val badgeId = intent.getStringExtra("badge_id") ?: ""
        // TODO: once badge is fetched via real API, this becomes a repository/viewmodel call.
        val officer = AuthRepository.getOfficer(badgeId)

        setContent {
            CrowdShieldTheme {
                DashboardScreen(
                    officerName = officer?.name ?: "Officer",
                    role = officer?.role ?: "Officer",
                    district = officer?.district ?: "Unassigned",
                    badgeId = officer?.badgeId ?: badgeId,
                    isAdmin = officer?.isAdmin ?: false
                )
            }
        }
    }
}

private data class ActivityItem(
    val time: String,
    val status: ActivityStatus,
    val location: String,
    val description: String
)

private enum class ActivityStatus { RESOLVED, WARNING, CRITICAL, INFO }

private data class QuickAction(val label: String, val icon: ImageVector)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(officerName: String, role: String, district: String, badgeId: String, isAdmin: Boolean) {
    val context = LocalContext.current
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var showLogoutConfirm by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }

    // Real dashboard data — see loadError below for a note on what shows
    // while the backend pipeline hasn't processed a frame yet.
    var activeAlertCount by remember { mutableStateOf(0) }
    var liveCrowdCount by remember { mutableStateOf(0) }
    var highRiskZoneCount by remember { mutableStateOf(0) }
    var todaysAlertCount by remember { mutableStateOf(0) }
    var safetyPercent by remember { mutableStateOf(100) }
    var safetyStatus by remember { mutableStateOf("SAFE") }
    var isLoadingDashboard by remember { mutableStateOf(true) }
    var dashboardLoadError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        try {
            val zones = ZoneRepository.getAll()
            val alerts = AlertRepository.getAll()
            val prediction = RiskPredictionRepository.getPrediction()

            liveCrowdCount = zones.sumOf { it.peopleCount }
            highRiskZoneCount = zones.count { it.riskLevel == RiskLevel.CRITICAL }
            todaysAlertCount = alerts.size
            activeAlertCount = alerts.count { it.severity == AlertSeverity.CRITICAL }

            safetyPercent = (100 - prediction.riskPercent).coerceIn(0, 100)
            safetyStatus = when (prediction.riskLabel.uppercase()) {
                "LOW" -> "SAFE"
                "MEDIUM", "BUSY" -> "MODERATE"
                "HIGH" -> "ELEVATED"
                "CRITICAL" -> "CRITICAL"
                else -> prediction.riskLabel.uppercase()
            }
            dashboardLoadError = null
        } catch (e: Exception) {
            dashboardLoadError = e.message ?: "Couldn't reach the server."
        } finally {
            isLoadingDashboard = false
        }
    }

    fun closeDrawer() = scope.launch { drawerState.close() }
    fun openDrawer() = scope.launch { drawerState.open() }
    fun navigateTo(activityClass: Class<out ComponentActivity>) {
        closeDrawer()
        val intent = Intent(context, activityClass)
        intent.putExtra("badge_id", badgeId)
        context.startActivity(intent)
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(drawerContainerColor = CardWhite) {
                DrawerHeader(officerName = officerName, role = role)
                HorizontalDivider(color = PageBackground, thickness = 1.dp)

                DrawerItem(Icons.Filled.Home, "Dashboard", selected = true, onClick = { closeDrawer() })
                DrawerItem(Icons.Filled.Videocam, "Live Monitoring") { navigateTo(LiveMonitoringActivity::class.java) }
                DrawerItem(Icons.Filled.Map, "Crowd Heatmap") { navigateTo(HeatmapActivity::class.java) }
                DrawerItem(Icons.Filled.Warning, "Alerts") { navigateTo(AlertsActivity::class.java) }
                DrawerItem(Icons.Filled.DirectionsRun, "Evacuation") { navigateTo(EvacuationActivity::class.java) }
                DrawerItem(Icons.Filled.Description, "Reports") { navigateTo(ReportsActivity::class.java) }
                DrawerItem(Icons.Filled.Notifications, "Notifications") { navigateTo(NotificationsActivity::class.java) }
                DrawerItem(Icons.Filled.Person, "Profile") { navigateTo(ProfileActivity::class.java) }
                DrawerItem(Icons.Filled.Settings, "Settings") { navigateTo(SettingsActivity::class.java) }
                DrawerItem(Icons.Filled.Info, "About") {
                    closeDrawer()
                    showAboutDialog = true
                }

                if (isAdmin) {
                    HorizontalDivider(color = PageBackground, thickness = 1.dp)
                    DrawerItem(
                        icon = Icons.Filled.AdminPanelSettings,
                        label = "Officer Registry",
                        tint = BrandBlue
                    ) { navigateTo(OfficerRegistryActivity::class.java) }
                }

                DrawerItem(
                    icon = Icons.AutoMirrored.Filled.Logout,
                    label = "Logout",
                    tint = RiskRed
                ) {
                    closeDrawer()
                    showLogoutConfirm = true
                }

                HorizontalDivider(color = PageBackground, thickness = 1.dp)
                Text(
                    "Version 1.0",
                    color = TextHint,
                    fontSize = 12.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    ) {
        Scaffold(
            containerColor = PageBackground,
            topBar = {
                TopAppBar(
                    title = { Text("CrowdShield", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = { openDrawer() }) {
                            Icon(Icons.Filled.Menu, contentDescription = "Menu")
                        }
                    },
                    actions = {
                        IconButton(onClick = { context.startActivity(Intent(context, NotificationsActivity::class.java)) }) {
                            Icon(Icons.Filled.Notifications, contentDescription = "Notifications")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = CardWhite,
                        titleContentColor = TextPrimary
                    )
                )
            },
            bottomBar = {
                CrowdShieldBottomBar(selected = BottomTab.HOME, alertBadgeCount = activeAlertCount)
            }
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .background(PageBackground),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    GreetingSection(
                        officerName = officerName,
                        role = role,
                        district = district,
                        badgeId = badgeId
                    )
                }

                if (isLoadingDashboard) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = BrandBlue)
                        }
                    }
                } else if (dashboardLoadError != null) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(CardWhite)
                                .padding(20.dp)
                        ) {
                            Text("Couldn't load live data", color = TextPrimary, fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(dashboardLoadError ?: "", color = TextSecondary, fontSize = 13.sp)
                        }
                    }
                } else {
                    item { CrowdSafetyIndexCard(percent = safetyPercent, status = safetyStatus) }
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            StatCard(
                                modifier = Modifier.weight(1f),
                                label = "Live Crowd",
                                value = "%,d".format(liveCrowdCount),
                                unit = "People",
                                accent = BrandBlue
                            )
                            StatCard(
                                modifier = Modifier.weight(1f),
                                label = "High Risk Zones",
                                value = "$highRiskZoneCount",
                                unit = "Zones",
                                accent = RiskRed
                            )
                        }
                    }
                    item {
                        StatCard(
                            modifier = Modifier.fillMaxWidth(),
                            label = "Today's Alerts",
                            value = "$todaysAlertCount",
                            unit = "Alerts raised",
                            accent = WarnAmber
                        )
                    }
                }

                item { QuickActionsSection() }
                item { RecentActivitySection() }
            }
        }
    }

    if (showLogoutConfirm) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirm = false },
            title = { Text("Log out?") },
            text = { Text("You'll need your badge ID and password to sign in again.") },
            confirmButton = {
                TextButton(onClick = {
                    showLogoutConfirm = false
                    val loginIntent = Intent(context, LoginActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                    context.startActivity(loginIntent)
                }) {
                    Text("Log out", color = RiskRed, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutConfirm = false }) { Text("Cancel") }
            }
        )
    }

    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = { Text("About CrowdShield") },
            text = {
                Text(
                    "CrowdShield is an AI-powered crowd monitoring and stampede " +
                            "prevention platform for temples, festivals, railway stations, " +
                            "and stadiums.\n\nVersion 1.0"
                )
            },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) { Text("Close") }
            }
        )
    }
}

@Composable
private fun DrawerHeader(officerName: String, role: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(BrandBlue.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.Person, contentDescription = null, tint = BrandBlue, modifier = Modifier.size(26.dp))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text("Officer Profile", color = TextHint, fontSize = 12.sp)
            Text(officerName, color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text(role, color = TextSecondary, fontSize = 13.sp)
        }
    }
}

@Composable
private fun DrawerItem(
    icon: ImageVector,
    label: String,
    selected: Boolean = false,
    tint: Color = TextPrimary,
    onClick: () -> Unit
) {
    NavigationDrawerItem(
        icon = { Icon(icon, contentDescription = null, tint = tint) },
        label = { Text(label, color = tint, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal) },
        selected = selected,
        onClick = onClick,
        colors = NavigationDrawerItemDefaults.colors(
            selectedContainerColor = BrandBlue.copy(alpha = 0.1f),
            unselectedContainerColor = Color.Transparent
        ),
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
    )
}

/** 1. Real officer identity + 2. Live clock and system status, top-right. */
@Composable
private fun GreetingSection(officerName: String, role: String, district: String, badgeId: String) {
    var currentTime by remember { mutableStateOf(formattedTime()) }

    LaunchedEffect(Unit) {
        while (true) {
            currentTime = formattedTime()
            delay(30_000L) // refresh every 30s — plenty for a clock display
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Column {
            Text("Good Morning,", color = TextSecondary, fontSize = 14.sp)
            Text(officerName, color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text("$role · $district", color = TextSecondary, fontSize = 13.sp)
            Text("Badge ID: $badgeId", color = TextHint, fontSize = 12.sp)
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(currentTime, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(SafeGreen)
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text("System Online", color = SafeGreen, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

private fun formattedTime(): String =
    SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())

@Composable
private fun CrowdSafetyIndexCard(percent: Int, status: String) {
    val accent = when (status) {
        "SAFE" -> SafeGreen
        "MODERATE" -> WarnAmber
        "ELEVATED", "CRITICAL" -> RiskRed
        else -> SafeGreen
    }
    val accentBg = when (status) {
        "SAFE" -> SafeGreenBg
        "MODERATE" -> WarnAmberBg
        "ELEVATED", "CRITICAL" -> RiskRedBg
        else -> SafeGreenBg
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CardWhite)
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "Crowd Safety Index",
            color = TextSecondary,
            fontSize = 13.sp,
            modifier = Modifier.align(Alignment.Start)
        )
        Spacer(modifier = Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .size(110.dp)
                .clip(CircleShape)
                .background(accentBg),
            contentAlignment = Alignment.Center
        ) {
            Text("$percent%", color = accent, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(12.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(accent)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(status, color = accent, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun StatCard(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    unit: String,
    accent: Color
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(CardWhite)
            .padding(16.dp)
    ) {
        Text(label, color = TextSecondary, fontSize = 13.sp)
        Spacer(modifier = Modifier.height(8.dp))
        Text(value, color = accent, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text(unit, color = TextHint, fontSize = 12.sp)
    }
}

/** Quick actions matching the wireframe exactly. */
@Composable
private fun QuickActionsSection() {
    val context = LocalContext.current
    val actions = listOf(
        QuickAction("Live Monitoring", Icons.Filled.Videocam),
        QuickAction("Crowd Heatmap", Icons.Filled.Map),
        QuickAction("Alerts", Icons.Filled.Warning),
        QuickAction("Evacuation", Icons.Filled.DirectionsRun),
        QuickAction("Reports", Icons.Filled.Description)
    )

    Column {
        Text("Quick Actions", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            actions.forEach { action ->
                QuickActionButton(
                    icon = action.icon,
                    label = action.label,
                    onClick = {
                        when (action.label) {
                            "Live Monitoring" -> context.startActivity(Intent(context, LiveMonitoringActivity::class.java))
                            "Crowd Heatmap" -> context.startActivity(Intent(context, HeatmapActivity::class.java))
                            "Alerts" -> context.startActivity(Intent(context, AlertsActivity::class.java))
                            "Evacuation" -> context.startActivity(Intent(context, EvacuationActivity::class.java))
                            "Reports" -> context.startActivity(Intent(context, ReportsActivity::class.java))
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun QuickActionButton(icon: ImageVector, label: String, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(84.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(CardWhite)
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(BrandBlue.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = label, tint = BrandBlue, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            label,
            color = TextPrimary,
            fontSize = 11.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            maxLines = 2
        )
    }
}

/** 4. Timestamped, specific activity feed. */
@Composable
private fun RecentActivitySection() {
    val activities = listOf(
        ActivityItem("10:24 AM", ActivityStatus.WARNING, "Temple Entrance", "High density detected"),
        ActivityItem("10:31 AM", ActivityStatus.RESOLVED, "North Exit", "Crowd reduced"),
        ActivityItem("10:37 AM", ActivityStatus.CRITICAL, "Platform 2", "Emergency alert"),
        ActivityItem("10:42 AM", ActivityStatus.INFO, "System", "AI prediction updated")
    )

    Column {
        Text("Recent Activity", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(12.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(CardWhite)
        ) {
            activities.forEachIndexed { index, activity ->
                ActivityRow(activity)
                if (index != activities.lastIndex) {
                    HorizontalDivider(color = PageBackground, thickness = 1.dp)
                }
            }
        }
    }
}

@Composable
private fun ActivityRow(item: ActivityItem) {
    val (bg, dot, symbol) = when (item.status) {
        ActivityStatus.RESOLVED -> Triple(SafeGreenBg, SafeGreen, "✓")
        ActivityStatus.WARNING -> Triple(WarnAmberBg, WarnAmber, "!")
        ActivityStatus.CRITICAL -> Triple(RiskRedBg, RiskRed, "!")
        ActivityStatus.INFO -> Triple(BrandBlue.copy(alpha = 0.1f), BrandBlue, "i")
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(bg),
            contentAlignment = Alignment.Center
        ) {
            Text(symbol, color = dot, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(item.description, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text(item.location, color = TextSecondary, fontSize = 12.sp)
        }
        Text(item.time, color = TextHint, fontSize = 11.sp)
    }
}