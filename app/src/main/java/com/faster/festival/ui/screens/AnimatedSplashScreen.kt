package com.faster.festival.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.faster.festival.R
import com.faster.festival.ui.theme.FastERTheme

object AnimatedSplashDimensions {
    val LogoSize = 120.dp
    val LogoTopMargin = 80.dp
    val ButtonHeight = 56.dp
    val ButtonCornerRadius = 12.dp
    val ButtonSpacing = 16.dp
    val ButtonHorizontalPadding = 24.dp
    val BottomPadding = 60.dp
    val ButtonFontSize = 16.sp

    val LogoAnimationDurationMs = 800
    val ButtonsAnimationDurationMs = 600
    val ButtonsDelayMs = 400
}

@Composable
fun AnimatedSplashScreen(
    onLoginClick: () -> Unit,
    onSignupClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val logoAlpha = remember { Animatable(0f) }
    val buttonsAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        logoAlpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = AnimatedSplashDimensions.LogoAnimationDurationMs)
        )

        buttonsAlpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = AnimatedSplashDimensions.ButtonsAnimationDurationMs,
                delayMillis = AnimatedSplashDimensions.ButtonsDelayMs
            )
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
    ) {
        Image(
            painter = painterResource(id = R.drawable.splash_bg),
            contentDescription = "Splash Background",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(color = MaterialTheme.colorScheme.background.copy(alpha = 0f)),
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = AnimatedSplashDimensions.LogoTopMargin)
                    .alpha(logoAlpha.value),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.faster_logo),
                    contentDescription = "Festival Logo",
                    modifier = Modifier.size(AnimatedSplashDimensions.LogoSize),
                    contentScale = ContentScale.Fit
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        bottom = AnimatedSplashDimensions.BottomPadding,
                        start = AnimatedSplashDimensions.ButtonHorizontalPadding,
                        end = AnimatedSplashDimensions.ButtonHorizontalPadding
                    )
                    .alpha(buttonsAlpha.value),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(
                    AnimatedSplashDimensions.ButtonSpacing
                )
            ) {
                Button(
                    onClick = onLoginClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(AnimatedSplashDimensions.ButtonHeight),
                    shape = RoundedCornerShape(AnimatedSplashDimensions.ButtonCornerRadius),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text(
                        text = "Login",
                        fontSize = AnimatedSplashDimensions.ButtonFontSize,
                        fontWeight = FontWeight.Bold
                    )
                }

                Button(
                    onClick = onSignupClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(AnimatedSplashDimensions.ButtonHeight),
                    shape = RoundedCornerShape(AnimatedSplashDimensions.ButtonCornerRadius),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary,
                        contentColor = MaterialTheme.colorScheme.onSecondary
                    )
                ) {
                    Text(
                        text = "Signup",
                        fontSize = AnimatedSplashDimensions.ButtonFontSize,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true, device = "id:pixel_5")
@Composable
fun AnimatedSplashScreenPreview() {
    FastERTheme {
        AnimatedSplashScreen(
            onLoginClick = {},
            onSignupClick = {}
        )
    }
}

@androidx.compose.ui.tooling.preview.Preview(
    showBackground = true,
    device = "id:pixel_5",
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES
)
@Composable
fun AnimatedSplashScreenDarkPreview() {
    FastERTheme {
        AnimatedSplashScreen(
            onLoginClick = {},
            onSignupClick = {}
        )
    }
}
