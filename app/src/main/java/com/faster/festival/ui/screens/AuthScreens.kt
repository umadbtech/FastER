package com.faster.festival.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.faster.festival.ui.components.AuthButtons
import com.faster.festival.ui.theme.FastERTheme
import androidx.compose.ui.tooling.preview.Preview
import com.faster.festival.R
import androidx.compose.ui.draw.clip
import androidx.compose.material3.*
import androidx.compose.runtime.*

@Composable
fun LoginScreen(
    modifier: Modifier = Modifier,
    onPhoneClick: () -> Unit,
    onEmailClick: () -> Unit,
    onSignUpClick: () -> Unit = {} // new signup callback with default no-op
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.background
                    )
                )
            ),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(60.dp))

        // Logo and Title
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Use drawable-nodpi PNG and ensure a fixed size + clipping so the image renders as expected
            Image(
                painter = painterResource(id = R.drawable.faster_logo),
                contentDescription = "FASTER Logo",
                modifier = Modifier
                    .size(190.dp)
                    .clip(RoundedCornerShape(0.dp)),
                contentScale = ContentScale.Fit
            )

            Spacer(modifier = Modifier.height(24.dp))

        }

        Spacer(modifier = Modifier.weight(1f))

        // Auth Buttons
        AuthButtons(
            onPhoneClick = onPhoneClick,
            onEmailClick = onEmailClick,
            onSignupClick = onSignUpClick
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Terms
        Text(
            text = "By continuing, you agree to our\nTerms of Service and Privacy Policy",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Preview
@Composable
fun PreviewLoginScreen() {
    FastERTheme {
        LoginScreen(
            onPhoneClick = {},
            onEmailClick = {},
            onSignUpClick = {}
        )
    }
}
