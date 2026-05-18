package com.faster.festival.wristband.ui.progress

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.faster.festival.wristband.domain.model.ProvisioningStep
import com.faster.festival.wristband.domain.model.StepStatus
import com.faster.festival.wristband.ui.components.StepRow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProvisioningProgressScreen(
    viewModel: ProvisioningProgressViewModel,
    onSuccess: () -> Unit,
    onCancel: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.start() }
    LaunchedEffect(state.finished, state.isFailed) {
        if (state.finished && !state.isFailed) onSuccess()
    }

    Scaffold(topBar = {
        CenterAlignedTopAppBar(title = { Text("Pairing your wristband") })
    }) { padding ->
        Column(
            Modifier
                .padding(padding)
                .padding(20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                "Hang tight — this takes about 15 seconds.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF6B6B6B)
            )
            Spacer(Modifier.height(16.dp))

            ProvisioningStep.values().forEach { step ->
                StepRow(
                    label = step.label,
                    status = state.steps[step] ?: StepStatus.Pending
                )
            }

            Spacer(Modifier.height(24.dp))
            val err = state.terminalError
            if (err != null) {
                Card {
                    Column(Modifier.padding(16.dp)) {
                        Text("Pairing failed", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(6.dp))
                        Text(err.userMessage)
                    }
                }
                Spacer(Modifier.height(12.dp))
                Button(onClick = { viewModel.retry() }, modifier = Modifier.fillMaxWidth()) {
                    Text(err.ctaText)
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) {
                    Text("Cancel")
                }
            } else if (!state.finished) {
                OutlinedButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) {
                    Text("Cancel pairing")
                }
            }
        }
    }
}
