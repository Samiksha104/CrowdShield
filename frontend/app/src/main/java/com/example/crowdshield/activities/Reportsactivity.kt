package com.example.crowdshield.activities

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.crowdshield.models.ReportRange
import com.example.crowdshield.repository.ReportRepository
import com.example.crowdshield.ui.components.CrowdShieldBottomBar
import com.example.crowdshield.ui.theme.CrowdShieldTheme

private val BrandBlue = Color(0xFF2743E0)
private val PageBackground = Color(0xFFF4F5F9)
private val CardWhite = Color(0xFFFFFFFF)
private val TextPrimary = Color(0xFF1A1A1A)
private val TextSecondary = Color(0xFF6B7280)
private val TextHint = Color(0xFF9CA3AF)
private val SafeGreen = Color(0xFF16A34A)
private val WarnAmber = Color(0xFFF59E0B)
private val RiskRed = Color(0xFFDC2626)

class ReportsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CrowdShieldTheme {
                ReportsScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen() {
    val context = LocalContext.current
    var selectedRange by remember { mutableStateOf(ReportRange.TODAY) }
    // "Line" is implemented now; "Bar" is a placeholder for the planned bar-chart view.
    var chartType by remember { mutableStateOf(ChartType.LINE) }
    val summary = remember(selectedRange) { ReportRepository.getSummary(selectedRange) }

    Scaffold(
        containerColor = PageBackground,
        topBar = {
            TopAppBar(
                title = { Text("Reports", fontWeight = FontWeight.Bold) },
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
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(PageBackground)
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            // Time range filter
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ReportRange.entries.forEach { range ->
                    RangeChip(
                        label = range.label,
                        selected = range == selectedRange,
                        onClick = { selectedRange = range }
                    )
                }
            }
            Spacer(modifier = Modifier.height(20.dp))

            // Crowd Graph card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(CardWhite)
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Crowd Graph", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    ChartTypeToggle(
                        selected = chartType,
                        onSelect = { type ->
                            if (type == ChartType.BAR) {
                                Toast.makeText(context, "Bar chart view is coming soon.", Toast.LENGTH_SHORT).show()
                            } else {
                                chartType = type
                            }
                        }
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                CrowdLineChart(series = summary.crowdSeries)
            }
            Spacer(modifier = Modifier.height(16.dp))

            // Stat cards
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard(Modifier.weight(1f), "Peak Crowd", "%,d".format(summary.peakCrowd), BrandBlue)
                StatCard(Modifier.weight(1f), "Average Crowd", "%,d".format(summary.averageCrowd), SafeGreen)
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard(Modifier.weight(1f), "Alerts", "${summary.alertCount}", WarnAmber)
                StatCard(Modifier.weight(1f), "Incidents", "${summary.incidentCount}", RiskRed)
            }
            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = { Toast.makeText(context, "Export PDF — coming soon.", Toast.LENGTH_SHORT).show() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BrandBlue)
            ) {
                Icon(Icons.Filled.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Export PDF", fontWeight = FontWeight.SemiBold)
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

private enum class ChartType { LINE, BAR }

@Composable
private fun ChartTypeToggle(selected: ChartType, onSelect: (ChartType) -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(PageBackground)
            .padding(2.dp)
    ) {
        ChartTypeOption("Line", selected == ChartType.LINE) { onSelect(ChartType.LINE) }
        ChartTypeOption("Bar", selected == ChartType.BAR) { onSelect(ChartType.BAR) }
    }
}

@Composable
private fun ChartTypeOption(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (selected) CardWhite else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(label, color = if (selected) BrandBlue else TextHint, fontSize = 11.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun RangeChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (selected) BrandBlue else CardWhite)
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 9.dp)
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
private fun StatCard(modifier: Modifier, label: String, value: String, accent: Color) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(CardWhite)
            .padding(16.dp)
    ) {
        Text(label, color = TextSecondary, fontSize = 13.sp)
        Spacer(modifier = Modifier.height(8.dp))
        Text(value, color = accent, fontSize = 22.sp, fontWeight = FontWeight.Bold)
    }
}

/**
 * Simple line chart drawn with Canvas — no charting library needed for this.
 * When the bar-chart view is added later, it can live alongside this as a
 * second composable selected by ChartTypeToggle above.
 */
@Composable
private fun CrowdLineChart(series: List<Int>) {
    if (series.isEmpty()) return
    val maxValue = series.max().toFloat()
    val minValue = series.min().toFloat()
    val range = (maxValue - minValue).coerceAtLeast(1f)

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
    ) {
        val stepX = size.width / (series.size - 1).coerceAtLeast(1)
        val points = series.mapIndexed { index, value ->
            val x = stepX * index
            val normalized = (value - minValue) / range
            val y = size.height - (normalized * size.height)
            Offset(x, y)
        }

        // Filled area under the curve
        val fillPath = Path().apply {
            moveTo(points.first().x, size.height)
            points.forEach { lineTo(it.x, it.y) }
            lineTo(points.last().x, size.height)
            close()
        }
        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(BrandBlue.copy(alpha = 0.25f), BrandBlue.copy(alpha = 0f))
            )
        )

        // Line
        val linePath = Path().apply {
            moveTo(points.first().x, points.first().y)
            points.drop(1).forEach { lineTo(it.x, it.y) }
        }
        drawPath(path = linePath, color = BrandBlue, style = Stroke(width = 6f))

        // Points, with the peak highlighted
        val peakIndex = series.indexOf(series.max())
        points.forEachIndexed { index, point ->
            val isPeak = index == peakIndex
            drawCircle(
                color = if (isPeak) RiskRed else BrandBlue,
                radius = if (isPeak) 8f else 5f,
                center = point
            )
        }
    }
}