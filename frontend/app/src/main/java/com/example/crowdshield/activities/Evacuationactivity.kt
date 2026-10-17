package com.example.crowdshield.activities

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.crowdshield.models.ExitRoute
import com.example.crowdshield.repository.EvacuationRepository
import com.example.crowdshield.ui.components.CrowdShieldBottomBar
import com.example.crowdshield.ui.theme.CrowdShieldTheme

private val BrandBlue = Color(0xFF2743E0)
private val PageBackground = Color(0xFFF4F5F9)
private val CardWhite = Color(0xFFFFFFFF)
private val TextPrimary = Color(0xFF1A1A1A)
private val TextSecondary = Color(0xFF6B7280)
private val TextHint = Color(0xFF9CA3AF)

private val SafeGreen = Color(0xFF16A34A)
private val SafeGreenBg = Color(0xFFE8F8EE)
private val RiskRed = Color(0xFFDC2626)
private val RiskRedBg = Color(0xFFFDECEC)
private val ExitBlue = Color(0xFF2563EB)

private val MapBase = Color(0xFFE7ECF5)
private val MapTerrain = Color(0xFFDCEBDC)

class EvacuationActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CrowdShieldTheme {
                EvacuationScreen(onBack = { finish() })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EvacuationScreen(onBack: () -> Unit) {
    val exits = remember { EvacuationRepository.getExits() }
    val (recommended, confidence) = remember { EvacuationRepository.getRecommended() }

    Scaffold(
        containerColor = PageBackground,
        topBar = {
            TopAppBar(
                title = { Text("Emergency Evacuation", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = CardWhite,
                    titleContentColor = TextPrimary
                )
            )
        },
        bottomBar = {
            // Reached via a quick action, not one of the five root tabs.
            CrowdShieldBottomBar(selected = null)
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(PageBackground),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .padding(16.dp)
                        .clip(RoundedCornerShape(20.dp))
                ) {
                    EvacuationMap(exits = exits)
                }
            }

            item {
                Legend()
                Spacer(modifier = Modifier.height(8.dp))
            }

            items(exits, key = { it.id }) { exit ->
                ExitCard(exit = exit, isRecommended = exit.id == recommended.id)
                Spacer(modifier = Modifier.height(12.dp))
            }

            item {
                RecommendedRouteCard(exit = recommended, confidence = confidence)
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun EvacuationMap(exits: List<ExitRoute>) {
    Box(modifier = Modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(color = MapBase)
            drawCircle(color = MapTerrain, radius = size.minDimension * 0.25f, center = Offset(size.width * 0.15f, size.height * 0.85f))
            drawCircle(color = MapTerrain, radius = size.minDimension * 0.18f, center = Offset(size.width * 0.9f, size.height * 0.1f))

            // Safe route (green) toward Exit A
            drawLine(
                color = SafeGreen,
                start = Offset(size.width * 0.5f, size.height * 0.5f),
                end = Offset(size.width * 0.25f, size.height * 0.30f),
                strokeWidth = 8f,
                cap = StrokeCap.Round
            )
            // Safe route (green) toward Exit B
            drawLine(
                color = SafeGreen,
                start = Offset(size.width * 0.5f, size.height * 0.5f),
                end = Offset(size.width * 0.75f, size.height * 0.35f),
                strokeWidth = 8f,
                cap = StrokeCap.Round
            )
            // Blocked route (red) toward Exit C
            drawLine(
                color = RiskRed,
                start = Offset(size.width * 0.5f, size.height * 0.5f),
                end = Offset(size.width * 0.50f, size.height * 0.70f),
                strokeWidth = 8f,
                cap = StrokeCap.Round
            )
        }

        exits.forEach { exit ->
            ExitMarker(exit = exit)
        }
    }
}

@Composable
private fun BoxScope.ExitMarker(exit: ExitRoute) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val markerX = maxWidth * exit.xFraction
        val markerY = maxHeight * exit.yFraction
        val color = if (exit.isBlocked) RiskRed else ExitBlue

        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(x = markerX - 10.dp, y = markerY - 10.dp)
                .size(20.dp)
                .clip(CircleShape)
                .background(color),
            contentAlignment = Alignment.Center
        ) {
            Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(Color.White))
        }
    }
}

@Composable
private fun Legend() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(CardWhite)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        LegendItem(RiskRed, "Blocked Roads")
        LegendItem(SafeGreen, "Safe Routes")
        LegendItem(ExitBlue, "Nearest Exit")
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(color))
        Spacer(modifier = Modifier.width(6.dp))
        Text(label, color = TextSecondary, fontSize = 11.sp)
    }
}

@Composable
private fun ExitCard(exit: ExitRoute, isRecommended: Boolean) {
    val (accent, accentBg, statusText) = if (exit.isBlocked) {
        Triple(RiskRed, RiskRedBg, "Blocked")
    } else {
        Triple(SafeGreen, SafeGreenBg, "${exit.etaMinutes} min")
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(CardWhite)
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.DirectionsRun, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(exit.name, color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                if (isRecommended) {
                    Text("AI Recommended", color = BrandBlue, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(accentBg)
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text(statusText, color = accent, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun RecommendedRouteCard(exit: ExitRoute, confidence: Int) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(BrandBlue)
            .padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Recommended Route", color = Color.White.copy(alpha = 0.9f), fontSize = 13.sp)
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(exit.name, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("Confidence", color = Color.White.copy(alpha = 0.75f), fontSize = 12.sp)
                Text("$confidence%", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("ETA", color = Color.White.copy(alpha = 0.75f), fontSize = 12.sp)
                Text(
                    exit.etaMinutes?.let { "$it min" } ?: "Blocked",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}