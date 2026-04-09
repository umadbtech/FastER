package com.faster.festival.ui.pinch.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.LocationOn
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
    Box(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(PinchMapGreen)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .align(Alignment.Center)
                    .background(Color(0xFFAED581))
            )
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .fillMaxSize()
                    .align(Alignment.Center)
                    .background(Color(0xFFAED581))
            )

            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = null,
                modifier = Modifier
                    .size(32.dp)
                    .align(Alignment.Center)
                    .offset(y = (-20).dp),
                tint = PinchRed
            )
            Icon(
                imageVector = Icons.Default.LocalHospital,
                contentDescription = null,
                modifier = Modifier
                    .size(28.dp)
                    .align(Alignment.TopEnd)
                    .padding(top = 80.dp, end = 40.dp)
                    .clip(CircleShape)
                    .background(PinchRed)
                    .padding(4.dp),
                tint = PinchWhite
            )
            Icon(
                imageVector = Icons.Default.LocalHospital,
                contentDescription = null,
                modifier = Modifier
                    .size(28.dp)
                    .align(Alignment.TopStart)
                    .padding(top = 120.dp, start = 30.dp)
                    .clip(CircleShape)
                    .background(PinchRed)
                    .padding(4.dp),
                tint = PinchWhite
            )
            Icon(
                imageVector = Icons.Default.LocalHospital,
                contentDescription = null,
                modifier = Modifier
                    .size(28.dp)
                    .align(Alignment.CenterEnd)
                    .padding(end = 50.dp)
                    .clip(CircleShape)
                    .background(PinchRed)
                    .padding(4.dp),
                tint = PinchWhite
            )
        }
        content()
    }
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

@Composable
fun BottomCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        colors = CardDefaults.cardColors(containerColor = PinchWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 16.dp)
                .windowInsetsPadding(WindowInsets.navigationBars),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .width(40.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0xFFDDDDDD))
            )
            Spacer(modifier = Modifier.height(16.dp))
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
            .height(52.dp),
        shape = RoundedCornerShape(26.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = PinchRed,
            disabledContainerColor = PinchGray
        ),
        enabled = enabled
    ) {
        Text(
            text = text,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
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
            .height(52.dp),
        shape = RoundedCornerShape(26.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = PinchGray,
            contentColor = PinchTextDark
        )
    ) {
        Text(
            text = text,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
        )
    }
}

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
            val isActive = index <= activeStep
            val isCurrent = index == activeStep
            Box(
                modifier = Modifier
                    .size(if (isCurrent) 14.dp else 10.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            isCurrent -> PinchRed
                            isActive -> PinchGreen
                            else -> PinchGray
                        }
                    )
                    .then(
                        if (isCurrent) Modifier.border(2.dp, PinchDarkRed, CircleShape)
                        else Modifier
                    )
            )
            if (index < steps.size - 1) {
                Box(
                    modifier = Modifier
                        .width(20.dp)
                        .height(2.dp)
                        .background(if (index < activeStep) PinchGreen else PinchGray)
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
