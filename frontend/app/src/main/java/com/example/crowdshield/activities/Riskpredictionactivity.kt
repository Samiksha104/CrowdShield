package com.example.crowdshield.activities

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.crowdshield.repository.RiskPredictionRepository
import com.example.crowdshield.ui.components.CrowdShieldBottomBar
import com.example.crowdshield.ui.theme.CrowdShieldTheme
import kotlin.math.cos
import kotlin.math.sin

private val BrandBlue = Color(0xFF2743E0)
private val PageBackground = Color(0xFF0B0B0C)
private val CardDark = Color(0xFF17181D)
private val CardBorder = Color(0xFF25262E)
private val TextPrimaryLight = Color(0xFFF4F5F9)
private val TextSecondaryLight = Color(0xFF9CA3AF)

private val SafeGreen = Color(0xFF16A34A)
private val WarnAmber = Color(0xFFF59E0B)
private val RiskRed = Color(0xFFDC2626)

class RiskPredictionActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CrowdShieldTheme {
                RiskPredictionScreen(onBack = { finish() })
            }
        }
    }
}

private data class Recommendation(val icon: ImageVector, val title: String, val detail: String? = null)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RiskPredictionScreen(onBack: () -> Unit) {
    var riskPercent by remember { mutableStateOf(0) }
    var riskLabel by remember { mutableStateOf("LOW") }
    var confidence by remember { mutableStateOf(0) }
    var expectedCrowd by remember { mutableStateOf(0) }
    var trend by remember { mutableStateOf("Stable") }
    var recommendations by remember { mutableStateOf<List<Recommendation>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var loadError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        try {
            val prediction = RiskPredictionRepository.getPrediction()
            riskPercent = prediction.riskPercent
            riskLabel = prediction.riskLabel
            confidence = prediction.confidencePercent
            expectedCrowd = prediction.expectedCrowd
            trend = prediction.trend
            recommendations = prediction.recommendations.map { text ->
                Recommendation(icon = Icons.Filled.AutoAwesome, title = text)
            }
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
                title = { Text("Crowd Risk Prediction", fontWeight = FontWeight.Bold, color = TextPrimaryLight) },
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
            // No tab highlighted — Risk Prediction isn't one of the five root
            // destinations, it's reached via a quick action.
            CrowdShieldBottomBar(selected = null, containerColor = CardDark)
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
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Couldn't load risk prediction", color = TextPrimaryLight, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(loadError ?: "", color = TextSecondaryLight, fontSize = 13.sp)
                }
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(PageBackground)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(12.dp))
            RiskGauge(percent = riskPercent, label = riskLabel)
            Spacer(modifier = Modifier.height(28.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricCard(
                    modifier = Modifier.weight(1f),
                    label = "Prediction Confidence",
                    value = "$confidence%",
                    accent = SafeGreen
                )
                MetricCard(
                    modifier = Modifier.weight(1f),
                    label = "Expected Crowd",
                    value = "%,d".format(expectedCrowd),
                    accent = BrandBlue
                )
            }
            Spacer(modifier = Modifier.height(12.dp))

            MetricCard(
                modifier = Modifier.fillMaxWidth(),
                label = "Congestion Trend",
                value = trend,
                accent = RiskRed,
                icon = Icons.Filled.TrendingUp
            )
            Spacer(modifier = Modifier.height(24.dp))

            Text(
                "AI Recommendation",
                color = TextPrimaryLight,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.align(Alignment.Start)
            )
            Spacer(modifier = Modifier.height(12.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(CardDark)
                    .padding(vertical = 4.dp)
            ) {
                recommendations.forEachIndexed { index, rec ->
                    RecommendationRow(rec)
                    if (index != recommendations.lastIndex) {
                        HorizontalDivider(color = CardBorder, thickness = 1.dp)
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun RiskGauge(percent: Int, label: String) {
    val animatedPercent = remember { Animatable(0f) }
    LaunchedEffect(percent) {
        animatedPercent.animateTo(percent.toFloat(), animationSpec = tween(durationMillis = 1200))
    }

    val gaugeColor = when {
        percent < 40 -> SafeGreen
        percent < 70 -> WarnAmber
        else -> RiskRed
    }

    Box(
        modifier = Modifier.size(240.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 20.dp.toPx()
            val startAngle = 135f
            val sweepAngle = 270f
            val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
            val topLeft = Offset(strokeWidth / 2, strokeWidth / 2)

            // Background track
            drawArc(
                color = CardBorder,
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            // Progress arc
            val progressSweep = sweepAngle * (animatedPercent.value / 100f)
            drawArc(
                brush = Brush.sweepGradient(listOf(SafeGreen, WarnAmber, RiskRed)),
                startAngle = startAngle,
                sweepAngle = progressSweep,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            // Glowing dot at the tip of the progress arc
            val angleRad = Math.toRadians((startAngle + progressSweep).toDouble())
            val radius = arcSize.width / 2
            val center = Offset(size.width / 2, size.height / 2)
            val dotX = center.x + radius * cos(angleRad).toFloat()
            val dotY = center.y + radius * sin(angleRad).toFloat()
            drawCircle(color = Color.White, radius = strokeWidth * 0.35f, center = Offset(dotX, dotY))
            drawCircle(color = gaugeColor, radius = strokeWidth * 0.22f, center = Offset(dotX, dotY))
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "${animatedPercent.value.toInt()}%",
                color = TextPrimaryLight,
                fontSize = 44.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(gaugeColor.copy(alpha = 0.15f))
                    .padding(horizontal = 14.dp, vertical = 5.dp)
            ) {
                Text(label, color = gaugeColor, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun MetricCard(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    accent: Color,
    icon: ImageVector? = null
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(CardDark)
            .padding(16.dp)
    ) {
        Text(label, color = TextSecondaryLight, fontSize = 12.sp)
        Spacer(modifier = Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(value, color = accent, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            if (icon != null) {
                Spacer(modifier = Modifier.width(6.dp))
                Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
private fun RecommendationRow(rec: Recommendation) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(BrandBlue.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(rec.icon, contentDescription = null, tint = BrandBlue, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column {
            Text(rec.title, color = TextPrimaryLight, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            if (rec.detail != null) {
                Text(rec.detail, color = TextSecondaryLight, fontSize = 12.sp)
            }
        }
    }
}