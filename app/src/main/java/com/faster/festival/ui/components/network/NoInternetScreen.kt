package com.faster.festival.ui.components.network

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SignalWifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * Full-screen offline state. Use for blocking flows where there is no cached
 * content to render — e.g. first paint of a feed, a detail screen with no
 * fallback. Shows a primary "Try Again" CTA and a secondary "Open Settings".
 */
@Composable
fun NoInternetScreen(
    onRetry: () -> Unit,
    title: String = "No Internet Connection",
    description: String = "Please check your connection and try again.",
    primaryCta: String = "Try Again",
    showSettings: Boolean = true
) {
    val context = LocalContext.current
    Box(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.SignalWifiOff,
                contentDescription = null,
                tint = Color(0xFF6B6B6B),
                modifier = Modifier.size(80.dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF6B6B6B),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = onRetry,
                modifier = Modifier.fillMaxWidth()
            ) { Text(primaryCta) }
            if (showSettings) {
                OutlinedButton(
                    onClick = {
                        context.startActivity(Intent(Settings.ACTION_WIRELESS_SETTINGS))
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Open Settings") }
            }
        }
    }
}
