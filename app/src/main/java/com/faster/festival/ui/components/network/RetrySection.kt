package com.faster.festival.ui.components.network

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
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * Generic full-screen error state for non-connectivity failures (5xx, parse,
 * timeout while online). Use [NoInternetScreen] for offline.
 */
@Composable
fun ErrorRetryScreen(
    message: String,
    onRetry: () -> Unit,
    title: String = "Something went wrong",
    cta: String = "Try Again"
) {
    Box(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.ErrorOutline,
                contentDescription = null,
                tint = Color(0xFFB3261E),
                modifier = Modifier.size(72.dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF6B6B6B),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
            Button(onClick = onRetry, modifier = Modifier.fillMaxWidth()) { Text(cta) }
        }
    }
}

@Composable
fun EmptyState(
    title: String = "Nothing to show yet",
    description: String = "Check back later.",
    primaryCta: String? = null,
    onPrimary: () -> Unit = {}
) {
    Box(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF6B6B6B),
                textAlign = TextAlign.Center
            )
            if (primaryCta != null) {
                Spacer(Modifier.height(8.dp))
                Button(onClick = onPrimary) { Text(primaryCta) }
            }
        }
    }
}
