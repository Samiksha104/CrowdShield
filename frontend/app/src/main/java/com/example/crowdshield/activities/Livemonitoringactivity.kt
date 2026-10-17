package com.example.crowdshield.activities

import android.content.Intent
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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.crowdshield.models.Camera
import com.example.crowdshield.models.CameraStatus
import com.example.crowdshield.repository.CameraRepository
import com.example.crowdshield.ui.components.BottomTab
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
private val WarnAmber = Color(0xFFF59E0B)
private val WarnAmberBg = Color(0xFFFFF6E5)
private val RiskRed = Color(0xFFDC2626)
private val RiskRedBg = Color(0xFFFDECEC)
private val ThumbnailDark = Color(0xFF1C1D22)

class LiveMonitoringActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CrowdShieldTheme {
                LiveMonitoringScreen(
                    onBack = { finish() },
                    onCameraClick = { camera ->
                        val intent = Intent(this, CameraDetailActivity::class.java)
                        intent.putExtra("camera_id", camera.id)
                        startActivity(intent)
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveMonitoringScreen(onBack: () -> Unit, onCameraClick: (Camera) -> Unit) {
    var query by remember { mutableStateOf("") }
    var cameras by remember { mutableStateOf<List<Camera>>(emptyList()) }
    var isLoadingCameras by remember { mutableStateOf(true) }
    var cameraLoadError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(query) {
        try {
            cameras = CameraRepository.search(query)
            cameraLoadError = null
        } catch (e: Exception) {
            cameraLoadError = e.message ?: "Couldn't reach the server."
        } finally {
            isLoadingCameras = false
        }
    }

    Scaffold(
        containerColor = PageBackground,
        topBar = {
            TopAppBar(
                title = { Text("Live Monitoring", fontWeight = FontWeight.Bold) },
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
            CrowdShieldBottomBar(selected = BottomTab.LIVE)
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(PageBackground)
        ) {
            // Search camera
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Search camera", color = TextHint) },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = TextHint) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = CardWhite,
                    unfocusedContainerColor = CardWhite,
                    focusedBorderColor = BrandBlue,
                    unfocusedBorderColor = Color(0xFFE5E7EB)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            )

            if (isLoadingCameras) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = BrandBlue)
                }
            } else if (cameraLoadError != null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Couldn't load cameras", color = TextPrimary, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(cameraLoadError ?: "", color = TextSecondary, fontSize = 13.sp)
                    }
                }
            } else if (cameras.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        if (query.isBlank()) "Pipeline hasn't processed any frames yet — no cameras to show."
                        else "No cameras match \"$query\"",
                        color = TextSecondary,
                        fontSize = 14.sp
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(cameras, key = { it.id }) { camera ->
                        CameraCard(camera = camera, onClick = { onCameraClick(camera) })
                    }
                    item { Spacer(modifier = Modifier.height(12.dp)) }
                }
            }
        }
    }
}

@Composable
private fun CameraCard(camera: Camera, onClick: () -> Unit) {
    val (statusColor, statusBg, statusLabel) = when (camera.status) {
        CameraStatus.SAFE -> Triple(SafeGreen, SafeGreenBg, "SAFE")
        CameraStatus.WARNING -> Triple(WarnAmber, WarnAmberBg, "WARNING")
        CameraStatus.CRITICAL -> Triple(RiskRed, RiskRedBg, "CRITICAL")
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CardWhite)
            .clickable(onClick = onClick)
    ) {
        // Live thumbnail
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .background(ThumbnailDark),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Filled.Videocam,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.35f),
                modifier = Modifier.size(40.dp)
            )

            if (camera.isLive) {
                Row(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(10.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(RiskRed)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("LIVE", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(10.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(statusBg)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(statusLabel, color = statusColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Info row
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(camera.name, color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                Text(camera.lastUpdated, color = TextHint, fontSize = 11.sp)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(camera.zone, color = TextSecondary, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.People, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    "${"%,d".format(camera.peopleCount)} people",
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}