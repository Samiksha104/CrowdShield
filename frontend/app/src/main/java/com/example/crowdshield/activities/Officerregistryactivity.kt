package com.example.crowdshield.activities

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import com.example.crowdshield.models.Officer
import com.example.crowdshield.models.OfficerStatus
import com.example.crowdshield.repository.AuthRepository
import com.example.crowdshield.ui.theme.CrowdShieldTheme

private val BrandBlue = Color(0xFF2743E0)
private val PageBackground = Color(0xFFF4F5F9)
private val CardWhite = Color(0xFFFFFFFF)
private val TextPrimary = Color(0xFF1A1A1A)
private val TextSecondary = Color(0xFF6B7280)
private val TextHint = Color(0xFF9CA3AF)
private val RiskRed = Color(0xFFDC2626)
private val SafeGreen = Color(0xFF16A34A)

/**
 * Admin-only. Reached ONLY from the hamburger menu's "Officer Registry" item,
 * which itself only renders when Officer.isAdmin == true. Not reachable from
 * bottom nav or Quick Actions, and not deep-linkable by badge ID alone.
 */
class OfficerRegistryActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val adminBadgeId = intent.getStringExtra("badge_id") ?: ""
        val admin = AuthRepository.getOfficer(adminBadgeId)

        setContent {
            CrowdShieldTheme {
                if (admin?.isAdmin != true) {
                    // Defense in depth: even if someone reaches this screen directly,
                    // it refuses to render the form without admin privileges.
                    AccessDeniedScreen(onBack = { finish() })
                } else {
                    OfficerRegistryScreen(onBack = { finish() })
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AccessDeniedScreen(onBack: () -> Unit) {
    Scaffold(
        containerColor = PageBackground,
        topBar = {
            TopAppBar(
                title = { Text("Officer Registry") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back") }
                }
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding).fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Administrator access required.", color = TextSecondary)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OfficerRegistryScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var officers by remember { mutableStateOf(AuthRepository.getAllOfficers()) }

    var name by remember { mutableStateOf("") }
    var badgeId by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var department by remember { mutableStateOf("Crowd Control Unit") }
    var role by remember { mutableStateOf("") }
    var assignedZone by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var status by remember { mutableStateOf(OfficerStatus.ACTIVE) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun resetForm() {
        name = ""; badgeId = ""; password = ""; role = ""; assignedZone = ""; phone = ""
        department = "Crowd Control Unit"; status = OfficerStatus.ACTIVE
    }

    Scaffold(
        containerColor = PageBackground,
        topBar = {
            TopAppBar(
                title = { Text("Officer Registry", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CardWhite, titleContentColor = TextPrimary)
            )
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
            Text("Add Officer", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text(
                "All fields below are set by you. The officer only enters this password once, then chooses their own on first login.",
                color = TextSecondary,
                fontSize = 12.sp
            )
            Spacer(modifier = Modifier.height(16.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(CardWhite)
                    .padding(20.dp)
            ) {
                RegistryField("Officer Name", name) { name = it }
                RegistryField("Badge ID", badgeId, placeholder = "e.g. TN4473") { badgeId = it.uppercase() }
                RegistryField("Initial Password", password, isPassword = true) { password = it }
                RegistryField("Department", department) { department = it }
                RegistryField("Role", role, placeholder = "e.g. Inspector") { role = it }
                RegistryField("Assigned Zone", assignedZone, placeholder = "e.g. Chennai District") { assignedZone = it }
                RegistryField("Phone Number", phone, placeholder = "+91 ...") { phone = it }

                Text("Officer Status", color = TextSecondary, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp, bottom = 6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatusChip("Active", status == OfficerStatus.ACTIVE, SafeGreen) { status = OfficerStatus.ACTIVE }
                    StatusChip("Inactive", status == OfficerStatus.INACTIVE, RiskRed) { status = OfficerStatus.INACTIVE }
                }

                Text(
                    "First Login: automatically set to Yes by the system.",
                    color = TextHint,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(top = 12.dp)
                )

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(errorMessage!!, color = RiskRed, fontSize = 13.sp)
                }

                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        if (name.isBlank() || badgeId.isBlank() || password.isBlank() || role.isBlank() || assignedZone.isBlank()) {
                            errorMessage = "Fill in all required fields."
                            return@Button
                        }
                        if (password.length < 6) {
                            errorMessage = "Initial password must be at least 6 characters."
                            return@Button
                        }
                        val created = AuthRepository.addOfficer(
                            badgeId = badgeId,
                            initialPassword = password,
                            name = name,
                            department = department,
                            role = role,
                            assignedZone = assignedZone,
                            phone = phone,
                            status = status
                        )
                        if (!created) {
                            errorMessage = "Badge ID \"$badgeId\" is already registered."
                            return@Button
                        }
                        errorMessage = null
                        Toast.makeText(context, "Officer account created for $badgeId.", Toast.LENGTH_SHORT).show()
                        resetForm()
                        officers = AuthRepository.getAllOfficers()
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandBlue)
                ) {
                    Text("Create Officer Account", fontWeight = FontWeight.SemiBold)
                }
            }
            Spacer(modifier = Modifier.height(24.dp))

            Text("Existing Accounts (${officers.size})", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(10.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(CardWhite)
            ) {
                officers.forEachIndexed { index, officer ->
                    OfficerRow(officer)
                    if (index != officers.lastIndex) HorizontalDivider(color = PageBackground, thickness = 1.dp)
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun RegistryField(
    label: String,
    value: String,
    placeholder: String = "",
    isPassword: Boolean = false,
    onValueChange: (String) -> Unit
) {
    Text(label, color = TextSecondary, fontSize = 12.sp, modifier = Modifier.padding(top = 12.dp, bottom = 6.dp))
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder.ifBlank { label }, color = TextHint) },
        singleLine = true,
        visualTransformation = if (isPassword) androidx.compose.ui.text.input.PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
        shape = RoundedCornerShape(10.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = BrandBlue,
            unfocusedBorderColor = Color(0xFFE5E7EB)
        ),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun StatusChip(label: String, selected: Boolean, accent: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (selected) accent.copy(alpha = 0.12f) else PageBackground)
    ) {
        androidx.compose.material3.TextButton(onClick = onClick) {
            Text(label, color = if (selected) accent else TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun OfficerRow(officer: Officer) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(officer.name, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text("${officer.badgeId} · ${officer.role}", color = TextSecondary, fontSize = 12.sp)
        }
        Text(
            if (officer.status == OfficerStatus.ACTIVE) "Active" else "Inactive",
            color = if (officer.status == OfficerStatus.ACTIVE) SafeGreen else RiskRed,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}