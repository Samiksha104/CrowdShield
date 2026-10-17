package com.example.crowdshield.activities

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ContactPhone
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PrivacyTip
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
import com.example.crowdshield.ui.components.BottomTab
import com.example.crowdshield.ui.components.CrowdShieldBottomBar
import com.example.crowdshield.ui.theme.CrowdShieldTheme

private val BrandBlue = Color(0xFF2743E0)
private val PageBackground = Color(0xFFF4F5F9)
private val CardWhite = Color(0xFFFFFFFF)
private val TextPrimary = Color(0xFF1A1A1A)
private val TextSecondary = Color(0xFF6B7280)
private val TextHint = Color(0xFF9CA3AF)

class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CrowdShieldTheme {
                SettingsScreen(onBack = { finish() })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current

    // TODO: persist these with DataStore instead of in-memory state once that's wired up.
    var darkMode by remember { mutableStateOf(false) }
    var notifications by remember { mutableStateOf(true) }

    Scaffold(
        containerColor = PageBackground,
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold) },
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
            // Reached from Profile, so Profile stays highlighted.
            CrowdShieldBottomBar(selected = BottomTab.PROFILE)
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
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(CardWhite)
            ) {
                ToggleRow(
                    icon = Icons.Filled.DarkMode,
                    label = "Dark Mode",
                    checked = darkMode,
                    onCheckedChange = { darkMode = it }
                )
                HorizontalDivider(color = PageBackground, thickness = 1.dp)
                ToggleRow(
                    icon = Icons.Filled.Notifications,
                    label = "Notifications",
                    checked = notifications,
                    onCheckedChange = { notifications = it }
                )
                HorizontalDivider(color = PageBackground, thickness = 1.dp)
                NavRow(
                    icon = Icons.Filled.Language,
                    label = "Language",
                    valuePreview = "English",
                    onClick = { Toast.makeText(context, "Language selection — coming soon.", Toast.LENGTH_SHORT).show() }
                )
            }
            Spacer(modifier = Modifier.height(16.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(CardWhite)
            ) {
                NavRow(
                    icon = Icons.Filled.ContactPhone,
                    label = "Emergency Contacts",
                    onClick = { Toast.makeText(context, "Emergency Contacts — coming soon.", Toast.LENGTH_SHORT).show() }
                )
                HorizontalDivider(color = PageBackground, thickness = 1.dp)
                NavRow(
                    icon = Icons.Filled.PrivacyTip,
                    label = "Privacy Policy",
                    onClick = { Toast.makeText(context, "Privacy Policy — coming soon.", Toast.LENGTH_SHORT).show() }
                )
                HorizontalDivider(color = PageBackground, thickness = 1.dp)
                NavRow(
                    icon = Icons.Filled.Info,
                    label = "About",
                    onClick = { Toast.makeText(context, "CrowdShield — AI crowd monitoring platform.", Toast.LENGTH_SHORT).show() }
                )
            }
            Spacer(modifier = Modifier.height(20.dp))

            Text(
                "CrowdShield v1.0.0",
                color = TextHint,
                fontSize = 12.sp,
                modifier = Modifier
                    .fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun ToggleRow(icon: ImageVector, label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = BrandBlue, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(14.dp))
        Text(label, color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedTrackColor = BrandBlue)
        )
    }
}

@Composable
private fun NavRow(icon: ImageVector, label: String, valuePreview: String? = null, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = BrandBlue, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(14.dp))
        Text(label, color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
        if (valuePreview != null) {
            Text(valuePreview, color = TextSecondary, fontSize = 13.sp)
            Spacer(modifier = Modifier.width(6.dp))
        }
        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = TextHint, modifier = Modifier.size(18.dp))
    }
}