package com.example.crowdshield.activities

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.togetherWith
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.crowdshield.models.RiskLevel
import com.example.crowdshield.models.Zone
import com.example.crowdshield.repository.ZoneRepository
import com.example.crowdshield.ui.components.BottomTab
import com.example.crowdshield.ui.components.CrowdShieldBottomBar
import com.example.crowdshield.ui.theme.CrowdShieldTheme

private val BrandBlue = Color(0xFF2743E0)
private val PageBackground = Color(0xFFF4F5F9)
private val CardWhite = Color(0xFFFFFFFF)
private val TextPrimary = Color(0xFF1A1A1A)
private val TextSecondary = Color(0xFF6B7280)
private val TextHint = Color(0xFF9CA3AF)

private val NormalGreen = Color(0xFF16A34A)
private val BusyYellow = Color(0xFFEAB308)
private val CongestedOrange = Color(0xFFF97316)
private val CriticalRed = Color(0xFFDC2626)

private val MapBase = Color(0xFFE7ECF5)
private val MapRoad = Color(0xFFD3DCEE)
private val MapTerrain = Color(0xFFDCEBDC)

class HeatmapActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CrowdShieldTheme {
                HeatmapScreen(
                    onBack = { finish() },
                    onViewDetails = { zone ->
                        // Zones and cameras share names in this prototype — reuse the camera
                        // detail screen where an id match exists.
                        val cameraId = "cam_" + zone.id.removePrefix("zone_")
                        val intent = Intent(this, CameraDetailActivity::class.java)
                        intent.putExtra("camera_id", cameraId)
                        startActivity(intent)
                    }
                )
            }
        }
    }
}

private fun riskColor(level: RiskLevel): Color = when (level) {
    RiskLevel.NORMAL -> NormalGreen
    RiskLevel.BUSY -> BusyYellow
    RiskLevel.CONGESTED -> CongestedOrange
    RiskLevel.CRITICAL -> CriticalRed
}

private fun riskLabel(level: RiskLevel): String = when (level) {
    RiskLevel.NORMAL -> "Normal"
    RiskLevel.BUSY -> "Busy"
    RiskLevel.CONGESTED -> "Congested"
    RiskLevel.CRITICAL -> "Critical"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HeatmapScreen(onBack: () -> Unit, onViewDetails: (Zone) -> Unit) {
    var zones by remember { mutableStateOf<List<Zone>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var loadError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        try {
            zones = ZoneRepository.getAll()
            loadError = null
        } catch (e: Exception) {
            loadError = e.message ?: "Couldn't reach the server."
        } finally {
            isLoading = false
        }
    }

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize().background(PageBackground), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = BrandBlue)
        }
        return
    }
    if (loadError != null) {
        Box(modifier = Modifier.fillMaxSize().background(PageBackground), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Couldn't load zones", color = TextPrimary, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(4.dp))
                Text(loadError ?: "", color = TextSecondary, fontSize = 13.sp)
            }
        }
        return
    }
    if (zones.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize().background(PageBackground), contentAlignment = Alignment.Center) {
            Text("Pipeline hasn't processed any frames yet — no zones to show.", color = TextSecondary)
        }
        return
    }

    var selectedZone by remember { mutableStateOf(zones.first()) }
    val context = LocalContext.current
    var blueprintBitmap by remember { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }

    val blueprintPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val bitmap = BitmapFactory.decodeStream(stream)
                if (bitmap != null) {
                    blueprintBitmap = bitmap.asImageBitmap()
                }
            }
        }
    }

    Scaffold(
        containerColor = PageBackground,
        topBar = {
            TopAppBar(
                title = { Text("Crowd Heatmap", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (blueprintBitmap != null) {
                        IconButton(onClick = { blueprintBitmap = null }) {
                            Icon(Icons.Filled.Map, contentDescription = "Use default map")
                        }
                    }
                    IconButton(onClick = { blueprintPickerLauncher.launch("image/*") }) {
                        Icon(Icons.Filled.UploadFile, contentDescription = "Upload pathway blueprint")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = CardWhite,
                    titleContentColor = TextPrimary
                )
            )
        },
        bottomBar = {
            CrowdShieldBottomBar(selected = BottomTab.MAP)
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(PageBackground)
        ) {
            // Map surface
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(16.dp)
                    .clip(RoundedCornerShape(20.dp))
            ) {
                val bitmap = blueprintBitmap
                if (bitmap != null) {
                    // Authority-uploaded pathway blueprint, used in place of the stylized map.
                    Image(
                        bitmap = bitmap,
                        contentDescription = "Uploaded pathway blueprint",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    MapCanvas()
                }

                zones.forEach { zone ->
                    ZoneMarker(
                        zone = zone,
                        mapWidth = maxWidth,
                        mapHeight = maxHeight,
                        isSelected = zone.id == selectedZone.id,
                        onClick = { selectedZone = zone }
                    )
                }

                // Legend, floating top-left over the map
                Legend(modifier = Modifier.align(Alignment.TopStart).padding(12.dp))
            }

            // Selected zone panel
            AnimatedContent(
                targetState = selectedZone,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "selected_zone"
            ) { zone ->
                SelectedZoneCard(zone = zone, onViewDetails = { onViewDetails(zone) })
            }
        }
    }
}

@Composable
private fun MapCanvas() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        drawRect(color = MapBase)

        // Soft terrain patches for a map-like feel
        drawCircle(color = MapTerrain, radius = size.minDimension * 0.28f, center = Offset(size.width * 0.18f, size.height * 0.75f))
        drawCircle(color = MapTerrain, radius = size.minDimension * 0.20f, center = Offset(size.width * 0.85f, size.height * 0.15f))

        // Road grid
        val roadStroke = 6f
        for (i in 1..3) {
            val y = size.height * (i / 4f)
            drawLine(MapRoad, Offset(0f, y), Offset(size.width, y), strokeWidth = roadStroke, cap = StrokeCap.Round)
        }
        for (i in 1..3) {
            val x = size.width * (i / 4f)
            drawLine(MapRoad, Offset(x, 0f), Offset(x, size.height), strokeWidth = roadStroke, cap = StrokeCap.Round)
        }
    }
}

@Composable
private fun BoxScope.ZoneMarker(
    zone: Zone,
    mapWidth: androidx.compose.ui.unit.Dp,
    mapHeight: androidx.compose.ui.unit.Dp,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val color = riskColor(zone.riskLevel)
    val markerX = mapWidth * zone.xFraction
    val markerY = mapHeight * zone.yFraction
    val markerSize = if (isSelected) 26.dp else 20.dp

    Box(
        modifier = Modifier
            .align(Alignment.TopStart)
            .offset(x = markerX - (markerSize / 2), y = markerY - (markerSize / 2))
            .size(markerSize)
            .shadow(4.dp, CircleShape)
            .clip(CircleShape)
            .background(color)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (isSelected) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(Color.White)
            )
        }
    }
}

@Composable
private fun Legend(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(CardWhite.copy(alpha = 0.95f))
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        LegendRow(NormalGreen, "Normal")
        Spacer(modifier = Modifier.height(6.dp))
        LegendRow(BusyYellow, "Busy")
        Spacer(modifier = Modifier.height(6.dp))
        LegendRow(CongestedOrange, "Congested")
        Spacer(modifier = Modifier.height(6.dp))
        LegendRow(CriticalRed, "Critical")
    }
}

@Composable
private fun LegendRow(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(label, color = TextPrimary, fontSize = 12.sp)
    }
}

@Composable
private fun SelectedZoneCard(zone: Zone, onViewDetails: () -> Unit) {
    val color = riskColor(zone.riskLevel)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .background(CardWhite)
            .padding(20.dp)
    ) {
        Text("Selected Zone", color = TextSecondary, fontSize = 12.sp)
        Spacer(modifier = Modifier.height(4.dp))
        Text(zone.name, color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.People, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("People", color = TextSecondary, fontSize = 12.sp)
                }
                Text("${"%,d".format(zone.peopleCount)}", color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("Risk", color = TextSecondary, fontSize = 12.sp)
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(color.copy(alpha = 0.12f))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(color)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(riskLabel(zone.riskLevel), color = color, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(PageBackground)
                .padding(12.dp)
        ) {
            Text("Recommendation", color = TextSecondary, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(2.dp))
            Text(zone.recommendation, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        }

        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onViewDetails,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(containerColor = BrandBlue)
        ) {
            Text("View Details", fontWeight = FontWeight.SemiBold)
        }
    }
}