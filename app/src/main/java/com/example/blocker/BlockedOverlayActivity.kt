package com.example.blocker

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.MainActivity
import com.example.ui.theme.FokusDarkBackground
import com.example.ui.theme.FokusDarkCard
import com.example.ui.theme.FokusElectricLime
import com.example.ui.theme.FokusOnLime
import com.example.ui.theme.FokusTheme

class BlockedOverlayActivity : ComponentActivity() {

    companion object {
        const val EXTRA_BLOCKED_APP_NAME = "extra_blocked_app_name"
        const val EXTRA_BLOCKED_PACKAGE = "extra_blocked_package"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val appName = intent.getStringExtra(EXTRA_BLOCKED_APP_NAME) ?: "Distracting App"

        setContent {
            FokusTheme(darkTheme = true) {
                BlockedScreen(
                    blockedAppName = appName,
                    onReturnToFokus = {
                        val mainIntent = Intent(this, MainActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                        }
                        startActivity(mainIntent)
                        finish()
                    },
                    onEmergencyUnblock = {
                        BlockerManager.emergencyUnblock()
                        finish()
                    }
                )
            }
        }
    }
}

@Composable
fun BlockedScreen(
    blockedAppName: String,
    onReturnToFokus: () -> Unit,
    onEmergencyUnblock: () -> Unit
) {
    var showEmergencyDialog by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = FokusDarkBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(28.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF203508))
                    .border(2.dp, FokusElectricLime, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Locked",
                    tint = FokusElectricLime,
                    modifier = Modifier.size(48.dp)
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = "STAY DISCIPLINED",
                color = FokusElectricLime,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "$blockedAppName is Blocked",
                color = Color.White,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(FokusDarkCard)
                    .padding(16.dp)
            ) {
                Text(
                    text = "You are currently in an active FOKUS study session. Resist the urge to check notifications or browse distractions. Your admission goals require undivided attention.",
                    color = Color(0xFFA0AB96),
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(36.dp))

            Button(
                onClick = onReturnToFokus,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = FokusElectricLime,
                    contentColor = FokusOnLime
                )
            ) {
                Text(
                    text = "Return to FOKUS",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedButton(
                onClick = { showEmergencyDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color(0xFFA0AB96)
                )
            ) {
                Text("Emergency Recovery / Exit")
            }
        }
    }

    if (showEmergencyDialog) {
        AlertDialog(
            onDismissRequest = { showEmergencyDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.WarningAmber,
                    contentDescription = "Warning",
                    tint = Color(0xFFFFB300)
                )
            },
            title = { Text("Emergency Unblock") },
            text = {
                Text(
                    "FOKUS will never lock you out of your device. Are you sure you want to pause or disarm this focus session blocker right now?"
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showEmergencyDialog = false
                        onEmergencyUnblock()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5C5C))
                ) {
                    Text("Disarm Blocker")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEmergencyDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
