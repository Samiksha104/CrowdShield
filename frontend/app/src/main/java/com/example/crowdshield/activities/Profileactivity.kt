package com.example.crowdshield.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
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
import com.example.crowdshield.models.Officer
import com.example.crowdshield.repository.AuthRepository
import com.example.crowdshield.ui.components.BottomTab
import com.example.crowdshield.ui.components.CrowdShieldBottomBar
import com.example.crowdshield.ui.theme.CrowdShieldTheme

private val BrandBlue = Color(0xFF2743E0)
private val BrandBlueLight = Color(0xFFD6DEFB)
private val PageBackground = Color(0xFFF4F5F9)
private val CardWhite = Color(0xFFFFFFFF)
private val TextPrimary = Color(0xFF1A1A1A)
private val TextSecondary = Color(0xFF6B7280)
private val TextHint = Color(0xFF9CA3AF)
private val RiskRed = Color(0xFFDC2626)

class ProfileActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val badgeId = intent.getStringExtra("badge_id") ?: ""
        val officer = AuthRepository.getOfficer(badgeId)

        setContent {
            CrowdShieldTheme {
                ProfileScreen(
                    officer = officer,
                    onLogout = {
                        val loginIntent = Intent(this, LoginActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        }
                        startActivity(loginIntent)
                        finish()
                    }
                )
            }
        }
    }
}

private data class ProfileField(val icon: ImageVector, val label: String, val value: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(officer: Officer?, onLogout: () -> Unit) {
    val context = LocalContext.current
    var showLogoutConfirm by remember { mutableStateOf(false) }

    val fields = listOf(
        ProfileField(Icons.Filled.Badge, "Badge ID", officer?.badgeId ?: "—"),
        ProfileField(Icons.Filled.Business, "Department", officer?.department ?: "—"),
        ProfileField(Icons.Filled.Shield, "Role", officer?.role ?: "—"),
        ProfileField(Icons.Filled.LocationOn, "Assigned Zone", officer?.district ?: "Unassigned"),
        ProfileField(Icons.Filled.Phone, "Phone", officer?.phone?.takeIf { it.isNotBlank() } ?: "Not on file")
    )

    Scaffold(
        containerColor = PageBackground,
        topBar = {
            TopAppBar(
                title = { Text("Profile", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = CardWhite,
                    titleContentColor = TextPrimary
                )
            )
        },
        bottomBar = {
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
            // Officer image + name
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .clip(CircleShape)
                        .background(BrandBlueLight),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Person, contentDescription = null, tint = BrandBlue, modifier = Modifier.size(44.dp))
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(officer?.name ?: "Officer", color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text(officer?.role ?: "Officer", color = TextSecondary, fontSize = 13.sp)
            }
            Spacer(modifier = Modifier.height(24.dp))

            // Details card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(CardWhite)
            ) {
                fields.forEachIndexed { index, field ->
                    ProfileRow(field)
                    if (index != fields.lastIndex) {
                        HorizontalDivider(color = PageBackground, thickness = 1.dp)
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(
                onClick = { Toast.makeText(context, "Edit Profile — coming soon.", Toast.LENGTH_SHORT).show() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, BrandBlue),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = BrandBlue)
            ) {
                Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Edit Profile", fontWeight = FontWeight.SemiBold)
            }
            Spacer(modifier = Modifier.height(24.dp))

            // Settings / Logout
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(CardWhite)
            ) {
                ActionRow(
                    icon = Icons.Filled.Settings,
                    label = "Settings",
                    tint = TextPrimary,
                    onClick = { context.startActivity(Intent(context, SettingsActivity::class.java)) }
                )
                HorizontalDivider(color = PageBackground, thickness = 1.dp)
                ActionRow(
                    icon = Icons.AutoMirrored.Filled.Logout,
                    label = "Logout",
                    tint = RiskRed,
                    onClick = { showLogoutConfirm = true }
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
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
                    onLogout()
                }) {
                    Text("Log out", color = RiskRed, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun ProfileRow(field: ProfileField) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(field.icon, contentDescription = null, tint = BrandBlue, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(field.label, color = TextSecondary, fontSize = 12.sp)
            Text(field.value, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun ActionRow(icon: ImageVector, label: String, tint: Color, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(14.dp))
        Text(label, color = tint, fontSize = 15.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = TextHint, modifier = Modifier.size(18.dp))
    }
}