package com.faster.festival.ui.auth.forgot

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.faster.festival.R
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForgotPasswordScreen(
    viewModel: ForgotPasswordViewModel,
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {},
    onSent: (String) -> Unit = {}
) {
    val form by viewModel.formState.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val focusManager = LocalFocusManager.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val resetSentMsg = stringResource(id = R.string.we_sent_code, form.email)

    LaunchedEffect(uiState) {
        when (uiState) {
            is ForgotUiState.Sent -> {
                onSent(form.email)
                scope.launch { snackbarHostState.showSnackbar(resetSentMsg) }
            }
            is ForgotUiState.Error -> {
                val msg = (uiState as ForgotUiState.Error).message
                if (msg.isNotBlank()) scope.launch { snackbarHostState.showSnackbar(msg) }
            }
            else -> {}
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(title = { Text(stringResource(id = R.string.forgot_password)) }, navigationIcon = {
                IconButton(onClick = onBack) { Icon(imageVector = Icons.Default.ArrowBack, contentDescription = stringResource(id = R.string.back)) }
            })
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        Surface(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            Column(modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = form.email,
                    onValueChange = { viewModel.onEmailChange(it) },
                    label = { Text(stringResource(id = R.string.email_label)) },
                    isError = form.emailError != null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Done),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                form.emailError?.let { Text(text = it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.align(Alignment.Start).padding(top = 4.dp)) }

                Spacer(modifier = Modifier.height(16.dp))

                Button(onClick = {
                    focusManager.clearFocus()
                    viewModel.sendResetEmail()
                }, enabled = form.isSubmitEnabled && uiState !is ForgotUiState.Loading, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                    if (uiState is ForgotUiState.Loading) CircularProgressIndicator(modifier = Modifier.size(18.dp), color = MaterialTheme.colorScheme.onPrimary) else Text(stringResource(id = R.string.send_reset_code))
                }

                Spacer(modifier = Modifier.height(12.dp))

                val seconds = form.resendSecondsLeft
                TextButton(onClick = { viewModel.sendResetEmail() }, enabled = seconds <= 0) {
                    if (seconds > 0) Text(stringResource(id = R.string.resend_in, seconds)) else Text(stringResource(id = R.string.resend))
                }
            }
        }
    }
}
