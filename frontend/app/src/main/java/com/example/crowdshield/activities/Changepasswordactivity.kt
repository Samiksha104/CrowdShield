package com.example.crowdshield.activities

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import com.example.crowdshield.repository.PasswordChangeResult
import com.example.crowdshield.ui.theme.CrowdShieldTheme

private val BrandBlue = Color(0xFF2743E0)
private val SurfaceInput = Color(0xFF2B2B2E)
private val AlertText = Color(0xFFDC2626)
private val TextSecondary = Color(0xFF6B7280)
private val TextHint = Color(0xFF9CA3AF)

/**
 * Shown ONLY when the backend reports firstLogin = true. There is no way to
 * reach this screen except by that server-decided flag — the officer never
 * self-declares "this is my first login".
 */
class ChangePasswordActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val badgeId = intent.getStringExtra("badge_id") ?: ""
        val tempPassword = intent.getStringExtra("temp_password") ?: ""
        setContent {
            CrowdShieldTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFF0B0B0C)) {
                    ChangePasswordScreen(
                        badgeId = badgeId,
                        onPasswordChanged = {
                            val intent = Intent(this, MainActivity::class.java)
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
fun ChangePasswordScreen(
    badgeId: String,
    onPasswordChanged: () -> Unit
) {
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
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
                .padding(24.dp)
        ) {
            Text("Set a new password", fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "This is your first sign-in as $badgeId. Choose a permanent password to continue.",
                color = TextSecondary,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(24.dp))

            Text("Temporary password", fontSize = 13.sp)
            Spacer(modifier = Modifier.height(6.dp))
            PasswordField(currentPassword) { currentPassword = it }
            Spacer(modifier = Modifier.height(16.dp))

            Text("New password", fontSize = 13.sp)
            Spacer(modifier = Modifier.height(6.dp))
            PasswordField(newPassword) { newPassword = it }
            Spacer(modifier = Modifier.height(16.dp))

            Text("Confirm new password", fontSize = 13.sp)
            Spacer(modifier = Modifier.height(6.dp))
            PasswordField(confirmPassword) { confirmPassword = it }

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(errorMessage!!, color = AlertText, fontSize = 13.sp)
            }

            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = {
                    if (currentPassword.isBlank() || newPassword.isBlank() || confirmPassword.isBlank()) {
                        errorMessage = "Fill in all three fields."
                        return@Button
                    }
                    if (newPassword != confirmPassword) {
                        errorMessage = "New password and confirmation don't match."
                        return@Button
                    }

                    when (AuthRepository.changePassword(badgeId, currentPassword, newPassword)) {
                        PasswordChangeResult.Success -> {
                            errorMessage = null
                            onPasswordChanged()
                        }
                        PasswordChangeResult.SamePasswordNotAllowed -> {
                            errorMessage = "New password must be different from the temporary one."
                        }
                        PasswordChangeResult.PasswordTooWeak -> {
                            errorMessage = "Password must be at least 8 characters."
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BrandBlue)
            ) {
                Text("Update password", fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun PasswordField(value: String, onValueChange: (String) -> Unit) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        placeholder = { Text("••••••••", color = TextHint) },
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
}