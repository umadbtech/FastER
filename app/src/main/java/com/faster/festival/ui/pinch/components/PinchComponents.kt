package com.faster.festival.ui.pinch.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.SensorsOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val PinchRed = Color(0xFFD32F2F)
val PinchDarkRed = Color(0xFFB71C1C)
val PinchBg = Color(0xFFF5F5F5)
val PinchWhite = Color.White
val PinchTextDark = Color(0xFF1A1A1A)
val PinchTextMedium = Color(0xFF444444)
val PinchTextLight = Color(0xFF888888)
val PinchGreen = Color(0xFF2E7D32)
val PinchMapGreen = Color(0xFFC8E6C9)
val PinchGray = Color(0xFFE0E0E0)
val PinchOrange = Color(0xFFE65100)
val PinchAmber = Color(0xFFF9A825)

@Composable
fun MapBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    // No location context on these screens, so [LiveMapBackground] renders its
    // neutral surface (no GPS fix) rather than a simulated map.
    com.faster.festival.ui.pinch.map.LiveMapBackground(
        modifier = modifier,
        content = content
    )
}

@Composable
fun BackButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = PinchTextDark
) {
    IconButton(onClick = onClick, modifier = modifier) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "Back",
            tint = tint
        )
    }
}

/**
 * Draggable bottom card that can be swiped up/down.
 * Swipe down to collapse (show only drag handle peek).
 * Swipe up to expand (show full content).
 * Starts in expanded state.
 */
@Composable
fun BottomCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val density = LocalDensity.current

    // Drag range: negative = pull up, positive = push down
    val maxUpPx = with(density) { 150.dp.toPx() }
    val maxDownPx = with(density) { 200.dp.toPx() }
    var dragOffset by remember { mutableFloatStateOf(0f) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .offset { IntOffset(0, dragOffset.roundToInt()) },
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        colors = CardDefaults.cardColors(containerColor = PinchWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 12.dp)
                .windowInsetsPadding(WindowInsets.navigationBars),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Drag handle — swipe target
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .pointerInput(Unit) {
                        detectVerticalDragGestures(
                            onDragEnd = {
                                dragOffset = when {
                                    dragOffset < -maxUpPx * 0.3f -> -maxUpPx   // snap up
                                    dragOffset > maxDownPx * 0.3f -> maxDownPx  // snap down
                                    else -> 0f                                   // snap to default
                                }
                            },
                            onVerticalDrag = { _, amount ->
                                dragOffset = (dragOffset + amount).coerceIn(-maxUpPx, maxDownPx)
                            }
                        )
                    }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(36.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color(0xFFBBBBBB))
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            content()
        }
    }
}

@Composable
fun PinchPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(46.dp),
        shape = RoundedCornerShape(23.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = PinchRed,
            disabledContainerColor = PinchGray
        ),
        enabled = enabled
    ) {
        Text(
            text = text,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp
        )
    }
}

@Composable
fun PinchSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(46.dp),
        shape = RoundedCornerShape(23.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = PinchGray,
            contentColor = PinchTextDark
        )
    ) {
        Text(
            text = text,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp
        )
    }
}

private val timelineIcons = listOf(
    Icons.Default.SensorsOff,      // Step 0: Alert/broadcast
    Icons.Default.Call,             // Step 1: Phone call
    Icons.Default.Navigation,      // Step 2: Dispatched/navigation
    Icons.Default.LocationOn,      // Step 3: Location arrived
    Icons.Default.MedicalServices  // Step 4: Medical help
)

@Composable
fun TimelineIndicator(
    steps: List<String>,
    activeStep: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        steps.forEachIndexed { index, _ ->
            val isCompleted = index < activeStep
            val isCurrent = index == activeStep
            val icon = timelineIcons.getOrElse(index) { Icons.Default.LocationOn }

            val bgColor = when {
                isCurrent -> PinchRed
                isCompleted -> PinchGreen
                else -> Color.Transparent
            }
            val borderColor = when {
                isCurrent -> PinchRed
                isCompleted -> PinchGreen
                else -> PinchGray
            }
            val iconTint = when {
                isCurrent -> PinchWhite
                isCompleted -> PinchWhite
                else -> PinchGray
            }

            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(bgColor)
                    .border(2.dp, borderColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = iconTint
                )
            }

            if (index < steps.size - 1) {
                Box(
                    modifier = Modifier
                        .width(16.dp)
                        .height(2.dp)
                        .background(if (isCompleted) PinchGreen else PinchGray)
                )
            }
        }
    }
}

@Composable
fun RatingSelector(
    maxRating: Int = 5,
    currentRating: Int,
    onRatingSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 1..maxRating) {
            val isSelected = i == currentRating
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) PinchRed else Color.Transparent)
                    .border(2.dp, if (isSelected) PinchRed else PinchGray, CircleShape)
                    .clickable { onRatingSelected(i) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "$i",
                    color = if (isSelected) PinchWhite else PinchTextMedium,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }
    }
}

@Composable
fun RadioOption(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailingContent: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) PinchRed else PinchGray,
                shape = RoundedCornerShape(12.dp)
            )
            .background(if (selected) PinchRed.copy(alpha = 0.05f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .border(2.dp, if (selected) PinchRed else PinchGray, CircleShape)
                .background(if (selected) PinchRed else Color.Transparent),
            contentAlignment = Alignment.Center
        ) {
            if (selected) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(PinchWhite)
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = PinchTextDark,
            modifier = Modifier.weight(1f)
        )
        trailingContent?.invoke()
    }
}
