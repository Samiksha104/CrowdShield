package com.example.crowdshield.activities

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.crowdshield.ui.theme.CrowdShieldTheme
import kotlinx.coroutines.delay

private val BrandBlue = Color(0xFF2743E0)
private val BackgroundDark = Color(0xFF0B0B0C)
private val TextMuted = Color(0xFF9CA3AF)

class SplashActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CrowdShieldTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = BackgroundDark) {
                    SplashScreen(
                        onFinished = {
                            // TODO: once session persistence exists, check for a saved
                            // session/token here and route straight to MainActivity if valid.
                            startActivity(Intent(this, LoginActivity::class.java))
                            finish()
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun SplashScreen(onFinished: () -> Unit) {
    val alphaAnim = remember { Animatable(0f) }
    val scaleAnim = remember { Animatable(0.85f) }

    LaunchedEffect(Unit) {
        alphaAnim.animateTo(1f, animationSpec = tween(durationMillis = 500))
        scaleAnim.animateTo(1f, animationSpec = tween(durationMillis = 500))
        delay(1400)
        onFinished()
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(88.dp)
                .scale(scaleAnim.value)
                .alpha(alphaAnim.value)
                .clip(RoundedCornerShape(22.dp))
                .then(Modifier),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(22.dp))
            ) {
                Surface(color = BrandBlue, modifier = Modifier.fillMaxSize()) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Icon(
                            imageVector = Icons.Filled.Lock,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "CrowdShield",
            color = Color.White,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.alpha(alphaAnim.value)
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "AI Crowd Monitoring & Stampede Prevention",
            color = TextMuted,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .alpha(alphaAnim.value)
                .padding(horizontal = 40.dp)
        )

        Spacer(modifier = Modifier.height(40.dp))

        CircularProgressIndicator(
            color = BrandBlue,
            strokeWidth = 3.dp,
            modifier = Modifier
                .size(28.dp)
                .alpha(alphaAnim.value)
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "Public safety authority portal",
            color = TextMuted,
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp)
                .fillMaxWidth()
                .alpha(alphaAnim.value)
        )
    }
}