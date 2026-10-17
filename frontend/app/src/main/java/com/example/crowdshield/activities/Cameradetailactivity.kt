package com.example.crowdshield.activities

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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
private val PlayerDark = Color(0xFF0B0B0C)

class CameraDetailActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val cameraId = intent.getStringExtra("camera_id") ?: ""

        setContent {
            CrowdShieldTheme {
                CameraDetailScreen(cameraId = cameraId, onBack = { finish() })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraDetailScreen(cameraId: String, onBack: () -> Unit) {
    var camera by remember { mutableStateOf<Camera?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var loadError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(cameraId) {
        try {
            camera = CameraRepository.getById(cameraId)
            loadError = null
        } catch (e: Exception) {
            loadError = e.message ?: "Couldn't reach the server."
        } finally {
            isLoading = false
        }
    }

    Scaffold(
        containerColor = PageBackground,
        topBar = {
            TopAppBar(
                title = { Text(camera?.name ?: "Camera", fontWeight = FontWeight.Bold) },
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
        if (isLoading) {
            Box(
                modifier = Modifier.padding(innerPadding).fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = BrandBlue)
            }
            return@Scaffold
        }
        if (loadError != null) {
            Box(
                modifier = Modifier.padding(innerPadding).fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("Couldn't reach the server: $loadError", color = TextSecondary)
            }
            return@Scaffold
        }
        if (camera == null) {
            Box(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("Camera not found.", color = TextSecondary)
            }
            return@Scaffold
        }
        val currentCamera = camera
            ?: return@Scaffold // shadow: guarantees non-null below without relying on smart-cast through the `by remember` delegate

        val (statusColor, statusBg, statusLabel) = when (currentCamera.status) {
            CameraStatus.SAFE -> Triple(SafeGreen, SafeGreenBg, "SAFE")
            CameraStatus.WARNING -> Triple(WarnAmber, WarnAmberBg, "WARNING")
            CameraStatus.CRITICAL -> Triple(RiskRed, RiskRedBg, "CRITICAL")
        }

        val context = LocalContext.current
        val openFullFeed = {
            val intent = Intent(context, CameraFeedActivity::class.java)
            intent.putExtra("camera_id", currentCamera.id)
            context.startActivity(intent)
        }

        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(PageBackground)
        ) {
            // Live player placeholder — tap to open the full-screen AI feed.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
                    .background(PlayerDark)
                    .clickable(onClick = openFullFeed),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.Videocam,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.4f),
                    modifier = Modifier.size(56.dp)
                )
                Row(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(14.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(RiskRed)
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text("LIVE", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(14.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.Black.copy(alpha = 0.5f))
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Fullscreen, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Tap to expand", color = Color.White, fontSize = 11.sp)
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Status banner
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(statusBg)
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Warning, contentDescription = null, tint = statusColor)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(statusLabel, color = statusColor, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        Text(currentCamera.zone, color = statusColor.copy(alpha = 0.8f), fontSize = 12.sp)
                    }
                }

                // Stats row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(CardWhite)
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.People, contentDescription = null, tint = BrandBlue, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Crowd Count", color = TextSecondary, fontSize = 12.sp)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("${"%,d".format(currentCamera.peopleCount)}", color = TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Last updated", color = TextSecondary, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(currentCamera.lastUpdated, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    }
                }

                Text(
                    "AI Detection Notes",
                    color = TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    detectionNote(currentCamera.status, currentCamera.name),
                    color = TextSecondary,
                    fontSize = 13.sp
                )
            }
        }
    }
}

private fun detectionNote(status: CameraStatus, name: String): String = when (status) {
    CameraStatus.SAFE -> "Crowd density at $name is within normal range. No unusual movement patterns detected."
    CameraStatus.WARNING -> "Crowd density at $name is rising. Movement flow is slowing near entry points — monitor closely."
    CameraStatus.CRITICAL -> "Crowd density at $name has exceeded the safe threshold. Immediate attention recommended."
}