package com.faster.festival.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun HorizontalStepper(
    modifier: Modifier = Modifier,
    currentStep: Int,
    totalSteps: Int,
    stepLabels: List<String> = emptyList()
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 1..totalSteps) {
            val isCompleted = i < currentStep
            val isCurrent = i == currentStep
            val color = when {
                isCompleted -> MaterialTheme.colorScheme.primary
                isCurrent -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                val icon = when {
                    isCompleted -> Icons.Filled.CheckCircle
                    isCurrent -> Icons.Filled.RadioButtonChecked
                    else -> Icons.Filled.RadioButtonUnchecked
                }

                Icon(imageVector = icon, contentDescription = null, tint = color)

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Step $i",
                    style = MaterialTheme.typography.labelSmall,
                    color = color
                )

                if (stepLabels.size >= i) {
                    Text(
                        text = stepLabels[i - 1],
                        style = MaterialTheme.typography.bodySmall,
                        color = color
                    )
                }
            }

            if (i < totalSteps) {
                // connector line
                Spacer(modifier = Modifier.width(12.dp))
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(2.dp)
                        .background(
                            color = if (i < currentStep) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                        )
                )
                Spacer(modifier = Modifier.width(12.dp))
            }
        }
    }
}
