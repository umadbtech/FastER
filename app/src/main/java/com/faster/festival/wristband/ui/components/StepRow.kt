package com.faster.festival.wristband.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.faster.festival.wristband.domain.model.StepStatus

@Composable
fun StepRow(label: String, status: StepStatus) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp)
    ) {
        when (status) {
            StepStatus.Pending -> Icon(
                Icons.Default.RadioButtonUnchecked, null, tint = Color(0xFFB0B0B0)
            )
            StepStatus.Running -> CircularProgressIndicator(
                strokeWidth = 2.dp, modifier = Modifier.size(20.dp)
            )
            StepStatus.Success -> Icon(
                Icons.Default.CheckCircle, null, tint = Color(0xFF1B873F)
            )
            is StepStatus.Failed -> Icon(
                Icons.Default.ErrorOutline, null, tint = Color(0xFFB3261E)
            )
        }
        Spacer(Modifier.width(14.dp))
        Column {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            if (status is StepStatus.Failed) {
                Text(
                    status.reason,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFB3261E)
                )
            }
        }
    }
}
