package com.faster.festival.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.faster.festival.R
import com.faster.festival.ui.theme.FastERTheme

private const val LOGO_SIZE_FRACTION = 0.4f
private val BUTTON_HEIGHT = 48.dp
private val BUTTON_CORNER_RADIUS = 12.dp
private val BUTTON_SPACING = 16.dp
private val TOP_LOGO_PADDING = 80.dp
private val HORIZONTAL_PADDING = 32.dp
private val BOTTOM_PADDING = 64.dp

@Composable
fun SplashScreen(onLoginClick: () -> Unit, onSignupClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
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
                .statusBarsPadding()
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.TopCenter
            ) {
                Image(
                    painter = painterResource(id = R.drawable.faster),
                    contentDescription = "FastER Logo",
                    modifier = Modifier
                        .fillMaxWidth(LOGO_SIZE_FRACTION)
                        .padding(top = TOP_LOGO_PADDING),
                    contentScale = ContentScale.Fit
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = HORIZONTAL_PADDING,
                        end = HORIZONTAL_PADDING,
                        bottom = BOTTOM_PADDING
                    ),
                verticalArrangement = Arrangement.spacedBy(BUTTON_SPACING),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(
                    onClick = onLoginClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(BUTTON_HEIGHT),
                    shape = RoundedCornerShape(BUTTON_CORNER_RADIUS),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text(
                        text = "Login",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp
                    )
                }

                Button(
                    onClick = onSignupClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(BUTTON_HEIGHT),
                    shape = RoundedCornerShape(BUTTON_CORNER_RADIUS),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary,
                        contentColor = MaterialTheme.colorScheme.onSecondary
                    )
                ) {
                    Text(
                        text = "Sign Up",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun SplashScreenPreview() {
    FastERTheme {
        SplashScreen(onLoginClick = {}, onSignupClick = {})
    }
}
