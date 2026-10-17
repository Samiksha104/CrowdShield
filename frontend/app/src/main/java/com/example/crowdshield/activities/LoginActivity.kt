package com.example.crowdshield.activities

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.crowdshield.repository.AuthRepository
import com.example.crowdshield.repository.AuthResult
import com.example.crowdshield.ui.theme.CrowdShieldTheme

// Brand colors matching the reference design
private val BrandBlue = Color(0xFF2743E0)
private val BrandBlueLight = Color(0xFFD6DEFB)
private val SurfaceInput = Color(0xFF2B2B2E)
private val AlertBg = Color(0xFFFDECEC)
private val AlertBorder = Color(0xFFF8D3D3)
private val AlertText = Color(0xFFDC2626)
private val TextSecondary = Color(0xFF6B7280)
private val TextHint = Color(0xFF9CA3AF)

class LoginActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CrowdShieldTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFF0B0B0C)) {
                    LoginScreen(
                        onAuthenticated = { badgeId, firstLogin ->
                            val destination = if (firstLogin) {
                                ChangePasswordActivity::class.java
                            } else {
                                MainActivity::class.java
                            }
                            val intent = Intent(this, destination)
                            intent.putExtra("badge_id", badgeId)
                            startActivity(intent)
                            finish()
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun LoginScreen(onAuthenticated: (badgeId: String, firstLogin: Boolean) -> Unit) {
    var badgeId by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 420.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color.White)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(BrandBlue)
                    .padding(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Lock, contentDescription = null, tint = Color.White)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text("CrowdShield", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text("Public safety authority portal", color = BrandBlueLight, fontSize = 14.sp)
                }
            }

            // Monitoring notice
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AlertBg)
                    .border(width = androidx.compose.ui.unit.Dp.Hairline, color = AlertBorder)
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.Warning, contentDescription = null, tint = AlertText, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Authorized personnel only. Sessions are monitored.", color = AlertText, fontSize = 13.sp)
            }

            // Form
            Column(modifier = Modifier.padding(24.dp)) {
                Text("Sign in to your account", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text("Enter your department-issued credentials.", color = TextSecondary, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(20.dp))

                Text("Officer / badge ID", fontSize = 13.sp)
                Spacer(modifier = Modifier.height(6.dp))
                TextField(
                    value = badgeId,
                    onValueChange = { badgeId = it },
                    placeholder = { Text("e.g. TN4471", color = TextHint) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp)),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = SurfaceInput,
                        unfocusedContainerColor = SurfaceInput,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        cursorColor = Color.White
                    )
                )
                Spacer(modifier = Modifier.height(16.dp))

                Text("Password", fontSize = 13.sp)
                Spacer(modifier = Modifier.height(6.dp))
                TextField(
                    value = password,
                    onValueChange = { password = it },
                    placeholder = { Text("Enter your password", color = TextHint) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp)),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = SurfaceInput,
                        unfocusedContainerColor = SurfaceInput,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        cursorColor = Color.White
                    )
                )

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(errorMessage!!, color = AlertText, fontSize = 13.sp)
                }

                Spacer(modifier = Modifier.height(12.dp))
                // No self-service reset — badge/password issuance is admin-controlled.
                Text(
                    "Forgot password? Contact your Control Room Administrator.",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    modifier = Modifier.align(Alignment.End)
                )

                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = {
                        if (badgeId.isBlank() || password.isBlank()) {
                            errorMessage = "Enter both your badge ID and password to sign in."
                            return@Button
                        }

                        when (val result = AuthRepository.authenticate(badgeId, password)) {
                            is AuthResult.Success -> {
                                errorMessage = null
                                onAuthenticated(badgeId.trim().uppercase(), result.firstLogin)
                            }
                            is AuthResult.InvalidCredentials -> {
                                errorMessage = "Invalid badge ID or password."
                            }
                            is AuthResult.AccountInactive -> {
                                errorMessage = "This account is inactive. Contact your administrator."
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandBlue)
                ) {
                    Text("Sign in", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(24.dp))
                HorizontalDivider(color = Color(0xFFF0F0F0))
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Phone, contentDescription = null, tint = AlertText, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Emergency helpline: 112", color = AlertText, fontSize = 14.sp)
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Secure access for authorized personnel only.",
                    color = TextHint,
                    fontSize = 12.sp,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }
}