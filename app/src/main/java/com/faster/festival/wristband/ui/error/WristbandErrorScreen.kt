package com.faster.festival.wristband.ui.error

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.faster.festival.wristband.domain.model.WristbandError

@Composable
fun WristbandErrorScreen(
    error: WristbandError,
    onPrimary: () -> Unit,
    onSecondary: () -> Unit,
    secondaryText: String = "Cancel"
) {
    Scaffold { padding ->
        Column(
            Modifier.padding(padding).padding(24.dp).fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.ErrorOutline, null,
                tint = Color(0xFFB3261E),
                modifier = Modifier.size(72.dp)
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "Something needs your attention",
                style = MaterialTheme.typography.headlineSmall
            )
            Spacer(Modifier.height(8.dp))
            Text(error.userMessage, color = Color(0xFF6B6B6B))
            Spacer(Modifier.height(24.dp))
            Button(onClick = onPrimary, modifier = Modifier.fillMaxWidth()) {
                Text(error.ctaText)
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = onSecondary, modifier = Modifier.fillMaxWidth()) {
                Text(secondaryText)
            }
        }
    }
}
