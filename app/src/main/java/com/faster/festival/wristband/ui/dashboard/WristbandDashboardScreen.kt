package com.faster.festival.wristband.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Help
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.faster.festival.wristband.domain.model.ConnectionStatus
import com.faster.festival.wristband.domain.model.DeviceState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WristbandDashboardScreen(
    viewModel: WristbandDashboardViewModel,
    onUnpaired: () -> Unit,
    onTroubleshoot: () -> Unit,
    onBack: () -> Unit
) {
    val s by viewModel.state.collectAsStateWithLifecycle()
    val df = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text(s.deviceName) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                }
            }
        )
    }) { padding ->
        Column(
            Modifier
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ConnectionPill(s.connection, onReconnect = { viewModel.reconnect() })

            Card { Column(Modifier.padding(16.dp)) {
                SectionLabel("Device")
                LineItem("ID", s.deviceId.ifBlank { "—" })
                LineItem("State", (s.telemetry?.deviceState ?: DeviceState.Unknown).name)
                LineItem("Battery", s.telemetry?.batteryPct?.let { "$it%" } ?: "—")
                LineItem("Last sync", s.lastSyncMs?.let { df.format(Date(it)) } ?: "—")
            } }

            Card { Column(Modifier.padding(16.dp)) {
                SectionLabel("Live telemetry")
                val t = s.telemetry
                if (t == null) Text("Waiting for telemetry…", color = Color(0xFF6B6B6B))
                else {
                    LineItem("Motion", if (t.motionDetected) "Moving" else "Stationary")
                    LineItem("Accel X", "%.3f g".format(t.accelX_g))
                    LineItem("Accel Y", "%.3f g".format(t.accelY_g))
                    LineItem("Accel Z", "%.3f g".format(t.accelZ_g))
                    LineItem("Peak", "%.3f g".format(t.peakMag_g))
                    LineItem("Seq #", t.seqNum.toString())
                }
            } }

            Card { Column(Modifier.padding(16.dp)) {
                SectionLabel("Last event")
                Text(
                    s.lastEvent?.code?.name ?: "No events yet",
                    style = MaterialTheme.typography.bodyMedium
                )
            } }

            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = onTroubleshoot, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Help, null)
                Spacer(Modifier.width(8.dp))
                Text("Troubleshoot")
            }
            Button(
                onClick = { viewModel.unpair(onDone = onUnpaired) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB3261E)),
                modifier = Modifier.fillMaxWidth()
            ) { Text("Unpair Wristband") }
        }
    }
}

@Composable
private fun SectionLabel(t: String) {
    Text(
        t,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@Composable
private fun LineItem(k: String, v: String) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(k, color = Color(0xFF6B6B6B))
        Text(v, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun ConnectionPill(c: ConnectionStatus, onReconnect: () -> Unit) {
    val (label, color) = when (c) {
        ConnectionStatus.Connected -> "Connected" to Color(0xFF1B873F)
        ConnectionStatus.Connecting -> "Connecting…" to Color(0xFFB68E1A)
        ConnectionStatus.Reconnecting -> "Reconnecting…" to Color(0xFFB68E1A)
        is ConnectionStatus.Disconnected -> "Disconnected" to Color(0xFFB3261E)
        ConnectionStatus.Stale -> "Stale" to Color.DarkGray
        ConnectionStatus.Idle -> "Idle" to Color.Gray
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        AssistChip(
            onClick = {},
            label = { Text(label) },
            leadingIcon = { Icon(Icons.Default.Bluetooth, null, tint = color) }
        )
        Spacer(Modifier.weight(1f))
        if (c is ConnectionStatus.Disconnected || c == ConnectionStatus.Stale) {
            TextButton(onClick = onReconnect) { Text("Reconnect") }
        }
    }
}
