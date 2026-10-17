package com.example.crowdshield.activities

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.crowdshield.models.Alert
import com.example.crowdshield.models.AlertSeverity
import com.example.crowdshield.models.Camera
import com.example.crowdshield.models.CameraStatus
import com.example.crowdshield.repository.AlertRepository
import com.example.crowdshield.repository.CameraRepository
import com.example.crowdshield.notifications.NotificationHelper
import com.example.crowdshield.ui.theme.CrowdShieldTheme
import kotlinx.coroutines.launch

private val PageBackground = Color(0xFF000000)
private val OverlayDark = Color(0xCC101014)
private val TextPrimaryLight = Color(0xFFF4F5F9)
private val TextSecondaryLight = Color(0xFFB3B5BD)
private val RiskRed = Color(0xFFDC2626)
private val SafeGreen = Color(0xFF16A34A)
private val WarnAmber = Color(0xFFF59E0B)
private val BrandBlue = Color(0xFF2743E0)

class CameraFeedActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val cameraId = intent.getStringExtra("camera_id") ?: ""

        setContent {
            CrowdShieldTheme {
                CameraFeedScreen(cameraId = cameraId, onBack = { finish() })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraFeedScreen(cameraId: String, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showReportConfirm by remember { mutableStateOf(false) }

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
                title = { Text(camera?.name ?: "Camera", fontWeight = FontWeight.Bold, color = TextPrimaryLight) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimaryLight)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = PageBackground,
                    titleContentColor = TextPrimaryLight
                )
            )
        },
        bottomBar = {
            // This screen is a full-screen immersive feed, so it swaps the standard
            // five-tab nav for its own two dedicated actions, matching the wireframe.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(OverlayDark)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = {
                        Toast.makeText(context, "Snapshot saved.", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandBlue)
                ) {
                    Icon(Icons.Filled.PhotoCamera, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Snapshot", fontWeight = FontWeight.SemiBold)
                }
                Button(
                    onClick = { showReportConfirm = true },
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = RiskRed)
                ) {
                    Icon(Icons.Filled.ReportProblem, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Report", fontWeight = FontWeight.SemiBold)
                }
            }
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
                Text("Couldn't reach the server: $loadError", color = TextSecondaryLight)
            }
            return@Scaffold
        }
        if (camera == null) {
            Box(
                modifier = Modifier.padding(innerPadding).fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("Camera not found.", color = TextSecondaryLight)
            }
            return@Scaffold
        }
        val currentCamera = camera ?: return@Scaffold // shadow: guarantees non-null below without relying on smart-cast through the `by remember` delegate

        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(PageBackground)
        ) {
            // Simulated video + AI bounding boxes
            VideoFeedWithBoundingBoxes(modifier = Modifier.fillMaxSize())

            // Top-left stat chips
            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LiveBadge()
                StatChip(label = "FPS", value = "${currentCamera.fps}")
                StatChip(label = "Detected", value = "${currentCamera.peopleCount}")
            }

            // Bottom-left label
            Text(
                "AI Bounding Boxes",
                color = TextSecondaryLight,
                fontSize = 12.sp,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(OverlayDark)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }

    val cameraForDialog = camera
    if (showReportConfirm && cameraForDialog != null) {
        AlertDialog(
            onDismissRequest = { showReportConfirm = false },
            title = { Text("Report incident at ${cameraForDialog.name}?") },
            text = { Text("This creates an alert visible to all officers on the Alerts screen.") },
            confirmButton = {
                TextButton(onClick = {
                    showReportConfirm = false
                    val severity = if (cameraForDialog.status == CameraStatus.CRITICAL) AlertSeverity.CRITICAL else AlertSeverity.MEDIUM
                    AlertRepository.addAlert(
                        Alert(
                            id = "alert_manual_${System.currentTimeMillis()}",
                            title = "Manual Report",
                            location = cameraForDialog.name,
                            severity = severity,
                            time = "Just now",
                            description = "Incident reported by officer from the live feed."
                        )
                    )
                    Toast.makeText(context, "Incident reported.", Toast.LENGTH_SHORT).show()

                    if (severity == AlertSeverity.CRITICAL) {
                        scope.launch {
                            NotificationHelper.showCriticalAlertIfEnabled(
                                context = context,
                                title = "Critical alert: ${cameraForDialog.name}",
                                message = "Incident reported by officer from the live feed."
                            )
                        }
                    }
                }) {
                    Text("Report", color = RiskRed, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showReportConfirm = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun LiveBadge() {
    Row(
        modifier = Modifier
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
}

@Composable
private fun StatChip(label: String, value: String) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(OverlayDark)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("$label ", color = TextSecondaryLight, fontSize = 12.sp)
        Text(value, color = TextPrimaryLight, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

/**
 * PROTOTYPE ONLY — draws a dark video placeholder with a few static bounding
 * boxes to simulate YOLOv8 output. Replace with the real camera stream +
 * CameraService detection overlay once that pipeline is wired up.
 */
@Composable
private fun VideoFeedWithBoundingBoxes(modifier: Modifier = Modifier) {
    val boxes = remember {
        listOf(
            BoundingBox(0.15f, 0.30f, 0.10f, 0.25f, "Person 0.94", SafeGreen),
            BoundingBox(0.45f, 0.40f, 0.12f, 0.28f, "Person 0.89", SafeGreen),
            BoundingBox(0.68f, 0.25f, 0.11f, 0.30f, "Person 0.91", WarnAmber),
            BoundingBox(0.30f, 0.60f, 0.13f, 0.27f, "Person 0.87", SafeGreen)
        )
    }

    Canvas(modifier = modifier.background(Color(0xFF0B0B0C))) {
        boxes.forEach { box ->
            val topLeft = Offset(size.width * box.xFraction, size.height * box.yFraction)
            val boxSize = Size(size.width * box.widthFraction, size.height * box.heightFraction)

            drawRect(
                color = box.color,
                topLeft = topLeft,
                size = boxSize,
                style = Stroke(width = 3f)
            )

            drawContext.canvas.nativeCanvas.drawText(
                box.label,
                topLeft.x,
                topLeft.y - 8f,
                android.graphics.Paint().apply {
                    color = box.color.toArgb()
                    textSize = 28f
                    isFakeBoldText = true
                }
            )
        }
    }
}

private data class BoundingBox(
    val xFraction: Float,
    val yFraction: Float,
    val widthFraction: Float,
    val heightFraction: Float,
    val label: String,
    val color: Color
)