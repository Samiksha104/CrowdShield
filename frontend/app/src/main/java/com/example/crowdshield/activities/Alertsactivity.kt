package com.example.crowdshield.activities

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.crowdshield.models.Alert
import com.example.crowdshield.models.AlertSeverity
import com.example.crowdshield.repository.AlertRepository
import com.example.crowdshield.ui.components.BottomTab
import com.example.crowdshield.ui.components.CrowdShieldBottomBar
import com.example.crowdshield.ui.theme.CrowdShieldTheme

private val BrandBlue = Color(0xFF2743E0)
private val PageBackground = Color(0xFFF4F5F9)
private val CardWhite = Color(0xFFFFFFFF)
private val TextPrimary = Color(0xFF1A1A1A)
private val TextSecondary = Color(0xFF6B7280)
private val TextHint = Color(0xFF9CA3AF)

private val CriticalRed = Color(0xFFDC2626)
private val CriticalRedBg = Color(0xFFFDECEC)
private val MediumAmber = Color(0xFFF59E0B)
private val MediumAmberBg = Color(0xFFFFF6E5)
private val LowGreen = Color(0xFF16A34A)
private val LowGreenBg = Color(0xFFE8F8EE)

class AlertsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CrowdShieldTheme {
                AlertsScreen()
            }
        }
    }
}

private fun severityColor(severity: AlertSeverity): Triple<Color, Color, String> = when (severity) {
    AlertSeverity.CRITICAL -> Triple(CriticalRed, CriticalRedBg, "Critical")
    AlertSeverity.MEDIUM -> Triple(MediumAmber, MediumAmberBg, "Medium")
    AlertSeverity.LOW -> Triple(LowGreen, LowGreenBg, "Low")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlertsScreen() {
    var selectedFilter by remember { mutableStateOf<AlertSeverity?>(null) } // null = "All"

    var allAlerts by remember { mutableStateOf<List<Alert>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var loadError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        try {
            allAlerts = AlertRepository.getAll()
            loadError = null
        } catch (e: Exception) {
            loadError = e.message ?: "Couldn't reach the server."
        } finally {
            isLoading = false
        }
    }

    val alerts = remember(selectedFilter, allAlerts) {
        if (selectedFilter == null) allAlerts else allAlerts.filter { it.severity == selectedFilter }
    }
    val criticalCount = remember(allAlerts) { allAlerts.count { it.severity == AlertSeverity.CRITICAL } }
    val mediumCount = remember(allAlerts) { allAlerts.count { it.severity == AlertSeverity.MEDIUM } }
    val lowCount = remember(allAlerts) { allAlerts.count { it.severity == AlertSeverity.LOW } }

    Scaffold(
        containerColor = PageBackground,
        topBar = {
            TopAppBar(
                title = { Text("Alerts", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = CardWhite,
                    titleContentColor = TextPrimary
                )
            )
        },
        bottomBar = {
            CrowdShieldBottomBar(selected = BottomTab.ALERTS)
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(PageBackground)
        ) {
            // Today's Alerts summary
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
                Text("Today's Alerts", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    SummaryChip(Modifier.weight(1f), "Critical", criticalCount, CriticalRed, CriticalRedBg)
                    SummaryChip(Modifier.weight(1f), "Medium", mediumCount, MediumAmber, MediumAmberBg)
                    SummaryChip(Modifier.weight(1f), "Low", lowCount, LowGreen, LowGreenBg)
                }
            }

            // Filters
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(label = "All", selected = selectedFilter == null) { selectedFilter = null }
                FilterChip(label = "Critical", selected = selectedFilter == AlertSeverity.CRITICAL) { selectedFilter = AlertSeverity.CRITICAL }
                FilterChip(label = "Medium", selected = selectedFilter == AlertSeverity.MEDIUM) { selectedFilter = AlertSeverity.MEDIUM }
                FilterChip(label = "Low", selected = selectedFilter == AlertSeverity.LOW) { selectedFilter = AlertSeverity.LOW }
            }
            Spacer(modifier = Modifier.height(12.dp))

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = BrandBlue)
                }
            } else if (loadError != null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Couldn't load alerts", color = TextPrimary, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(loadError ?: "", color = TextSecondary, fontSize = 13.sp)
                    }
                }
            } else if (alerts.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No alerts in this category.", color = TextSecondary, fontSize = 14.sp)
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(alerts, key = { it.id }) { alert ->
                        AlertCard(alert)
                    }
                    item { Spacer(modifier = Modifier.height(12.dp)) }
                }
            }
        }
    }
}

@Composable
private fun SummaryChip(modifier: Modifier, label: String, count: Int, accent: Color, accentBg: Color) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(accentBg)
            .padding(vertical = 12.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("$count", color = accent, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text(label, color = accent, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun FilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (selected) BrandBlue else CardWhite)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            label,
            color = if (selected) Color.White else TextSecondary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun AlertCard(alert: Alert) {
    val (accent, accentBg, label) = severityColor(alert.severity)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CardWhite)
            .padding(16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(accentBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.Warning, contentDescription = null, tint = accent, modifier = Modifier.size(18.dp))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(alert.title, color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(accentBg)
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(label, color = accent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(alert.location, color = TextSecondary, fontSize = 13.sp)
            Spacer(modifier = Modifier.height(6.dp))
            Text(alert.description, color = TextSecondary, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(6.dp))
            Text(alert.time, color = TextHint, fontSize = 11.sp)
        }
    }
}