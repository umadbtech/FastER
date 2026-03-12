package com.faster.festival.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Step indicator dots composable for onboarding flow.
 *
 * Shows a row of dots where:
 * - Completed steps are filled with primary color
 * - Current step is filled with primary color and slightly wider
 * - Upcoming steps are outlined in LightGray
 *
 * Transitions between states are animated.
 *
 * @param currentStep Zero-based index of the current step.
 * @param totalSteps Total number of steps.
 * @param modifier Modifier for the row container.
 */
@Composable
fun StepIndicator(
    currentStep: Int,
    totalSteps: Int,
    modifier: Modifier = Modifier
) {
    val lightGray = Color(0xFFDFDCDC)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (index in 0 until totalSteps) {
            val isCompleted = index < currentStep
            val isCurrent = index == currentStep

            val dotColor by animateColorAsState(
                targetValue = when {
                    isCompleted -> MaterialTheme.colorScheme.primary
                    isCurrent -> MaterialTheme.colorScheme.primary
                    else -> lightGray
                },
                animationSpec = tween(durationMillis = 300),
                label = "dotColor_$index"
            )

            val dotWidth by animateDpAsState(
                targetValue = if (isCurrent) 24.dp else 10.dp,
                animationSpec = tween(durationMillis = 300),
                label = "dotWidth_$index"
            )

            Box(
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .width(dotWidth)
                    .height(10.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )
        }
    }
}

/**
 * Horizontal stepper with icons for completed, current, and upcoming steps.
 * Retained for backward compatibility with other screens.
 */
@Composable
fun HorizontalStepper(
    modifier: Modifier = Modifier,
    currentStep: Int,
    totalSteps: Int,
    stepLabels: List<String> = emptyList()
) {
    StepIndicator(
        currentStep = currentStep - 1,
        totalSteps = totalSteps,
        modifier = modifier
    )
}
