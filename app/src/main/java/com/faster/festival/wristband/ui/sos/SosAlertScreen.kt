package com.faster.festival.wristband.ui.sos

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.faster.festival.wristband.domain.model.SosState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SosAlertScreen(
    viewModel: SosAlertViewModel,
    onClose: () -> Unit
) {
    val s by viewModel.state.collectAsStateWithLifecycle()
    val event = s.event ?: return
    val df = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }

    LaunchedEffect(s.canceled) { if (s.canceled) onClose() }

    Box(Modifier.fillMaxSize().background(Color(0xFFB3261E))) {
        Column(
            Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth().padding(top = 32.dp)
            ) {
                Icon(
                    Icons.Default.Warning, null, tint = Color.White,
                    modifier = Modifier.size(96.dp)
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    "SOS RECEIVED", color = Color.White,
                    fontSize = 32.sp, fontWeight = FontWeight.Black
                )
                Text(
                    headline(s.state),
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 18.sp
                )
            }

            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White.copy(alpha = 0.12f)
                )
            ) {
                Column(Modifier.padding(16.dp)) {
                    Line("Event ID", "#${event.eventId}")
                    Line("Status", s.state.name)
                    Line("Retry count", event.retryCount.toString())
                    Line("Battery", "${event.batteryPct}%")
                    Line("Time", df.format(Date(event.receivedAtMs)))
                    if (s.ackSent) Line("ACK", "Sent ✓")
                }
            }

            Column(
                Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (!s.responderDispatched && !s.resolved) {
                    Button(
                        onClick = { viewModel.onResponderDispatched(5) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = Color(0xFFB3261E)
                        )
                    ) { Text("Dispatch Responder", fontWeight = FontWeight.Bold) }
                }
                if (!s.resolved) {
                    OutlinedButton(
                        onClick = { viewModel.onResolved() },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                    ) { Text("Mark Resolved") }
                }
                if (s.resolved) {
                    Button(
                        onClick = { viewModel.dismiss(); onClose() },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Dismiss") }
                }
            }
        }
    }
}

private fun headline(state: SosState) = when (state) {
    SosState.Active -> "Emergency in progress"
    SosState.Retry -> "Retrying — connecting…"
    SosState.Confirmed -> "Confirmed — help is on the way"
    SosState.Responder -> "Responder en route"
    SosState.Resolved -> "Resolved"
    SosState.Canceled -> "Canceled by user"
    else -> "Status: ${state.name}"
}

@Composable
private fun Line(k: String, v: String) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(k, color = Color.White.copy(alpha = 0.7f))
        Text(v, color = Color.White, fontWeight = FontWeight.SemiBold)
    }
}
