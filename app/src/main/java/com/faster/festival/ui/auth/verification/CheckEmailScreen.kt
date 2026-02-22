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
import androidx.compose.material3.Button
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
import kotlinx.coroutines.delay

@Composable
fun CheckEmailScreen(
    email: String,
    sessionManager: EncryptedSessionManager,
    onNavigateToEnterCode: (String) -> Unit = {},
    onSkip: () -> Unit = {},
    onVerificationComplete: () -> Unit = {}
) {
    val viewModel: OtpViewModel = viewModel(
        factory = OtpViewModel.Factory(
            authRepository = com.faster.festival.data.repository.AuthRepository(
                authApiService = NetworkModule.authApiService,
                sessionManager = sessionManager
            )
        )
    )

    val snackbarHostState = remember { SnackbarHostState() }
    // Use the new ViewModel UI state
    val uiState by viewModel.uiState.collectAsState()
    val resendCooldown = uiState.resendCooldown

    // Send OTP on first composition
    LaunchedEffect(Unit) {
        viewModel.sendOtp(email)
    }

    // Poll session manager for email confirmation and call callback once when confirmed
    LaunchedEffect(sessionManager) {
        // If already confirmed, trigger immediately
        if (sessionManager.isEmailConfirmed()) {
            onVerificationComplete()
        } else {
            while (true) {
                delay(1000)
                try {
                    if (sessionManager.isEmailConfirmed()) {
                        onVerificationComplete()
                        break
                    }
                } catch (_: Exception) {
                    // ignore transient errors reading session
                }
            }
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
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
                    text = "We've sent a 6-digit verification code to",
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
                    text = when {
                        uiState.isLoading -> "Processing..."
                        uiState.error != null -> uiState.error ?: "Error sending code"
                        else -> "Waiting to send code..."
                    },
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (uiState.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.height(40.dp))
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(onClick = { onNavigateToEnterCode(email) }, modifier = Modifier.fillMaxWidth()) {
                    Text("Enter Code")
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(onClick = { viewModel.resendOtp(email) }, modifier = Modifier.fillMaxWidth(), enabled = resendCooldown == 0) {
                    if (resendCooldown > 0) {
                        Text(text = "Resend in ${resendCooldown}s")
                    } else {
                        Text("Resend Code")
                    }
                }
            }
        }
    }
}
