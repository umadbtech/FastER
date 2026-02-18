package com.faster.festival.ui.auth.verification

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.faster.festival.data.local.EncryptedSessionManager
import com.faster.festival.di.NetworkModule

@Composable
fun CheckEmailScreen(
    email: String,
    sessionManager: EncryptedSessionManager,
    onResendEmail: () -> Unit = {},
    onSkip: () -> Unit = {},
    onVerificationComplete: () -> Unit = {},
    viewModel: EmailVerificationViewModel = viewModel(
        factory = EmailVerificationViewModel.Factory(
            supabaseClient = NetworkModule.supabaseClient,
            sessionManager = sessionManager
        )
    )
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val verificationState by viewModel.verificationState.collectAsState()

    // Handle verification events
    LaunchedEffect(verificationState) {
        when (verificationState) {
            is VerificationUiState.Verified -> {
                // Email verified successfully - navigate to home
                onVerificationComplete()
            }
            is VerificationUiState.Error -> {
                val errorMessage = (verificationState as VerificationUiState.Error).message
                snackbarHostState.showSnackbar(errorMessage)
            }
            else -> {} // Idle or Listening states don't require action
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Skip button in top left corner
            IconButton(
                onClick = onSkip,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Skip email verification",
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            // Main content centered
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Check Your Email",
                    style = MaterialTheme.typography.headlineMedium
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "We've sent a confirmation link to",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = email,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(32.dp))

                Text(
                    text = "Click the link in the email to verify your account and complete signup.",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.height(48.dp))

                OutlinedButton(
                    onClick = onResendEmail,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = verificationState !is VerificationUiState.Verified
                ) {
                    Text("Resend Email")
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Show different text based on verification state
                val statusText = when (verificationState) {
                    is VerificationUiState.Listening -> "Listening for email verification..."
                    is VerificationUiState.Verified -> "Email verified! Redirecting..."
                    is VerificationUiState.Error -> "Waiting for confirmation..."
                    else -> "Waiting for email confirmation..."
                }

                Text(
                    text = statusText,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )

                Spacer(modifier = Modifier.height(16.dp))

                CircularProgressIndicator(
                    modifier = Modifier.then(Modifier.height(40.dp))
                )
            }
        }
    }
}
