package com.faster.festival.ui.auth.reset

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResetPasswordScreen(
    email: String,
    token: String,
    viewModel: ResetPasswordViewModel,
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {},
    onResetSuccess: () -> Unit = {}
) {
    val form by viewModel.formState.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val focusManager = LocalFocusManager.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var passwordVisible by remember { mutableStateOf(false) }

    LaunchedEffect(uiState) {
        when (uiState) {
            is ResetUiState.Success -> onResetSuccess()
            is ResetUiState.Error -> {
                val msg = (uiState as ResetUiState.Error).message
                if (msg.isNotBlank()) scope.launch { snackbarHostState.showSnackbar(msg) }
            }
            else -> {}
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(title = { Text("Reset Password") }, navigationIcon = {
                IconButton(onClick = onBack) { Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back") }
            })
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Surface(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            Column(modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .animateContentSize(), horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = "Resetting for: $email", style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = form.otp,
                    onValueChange = { viewModel.onOtpChange(it) },
                    label = { Text("6-digit code") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                form.otpError?.let { Text(text = it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.align(Alignment.Start).padding(top = 4.dp)) }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = form.password,
                    onValueChange = { viewModel.onPasswordChange(it) },
                    label = { Text("New Password") },
                    isError = form.passwordError != null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
                    singleLine = true,
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff, contentDescription = if (passwordVisible) "Hide password" else "Show password")
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                form.passwordError?.let { Text(text = it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.align(Alignment.Start).padding(top = 4.dp)) }

                // Live password requirements checklist
                com.faster.festival.ui.components.PasswordRequirementsList(
                    password = form.password,
                    modifier = Modifier.align(Alignment.Start)
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = form.confirmPassword,
                    onValueChange = { viewModel.onConfirmPasswordChange(it) },
                    label = { Text("Confirm Password") },
                    isError = form.confirmPasswordError != null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                    singleLine = true,
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
                form.confirmPasswordError?.let { Text(text = it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.align(Alignment.Start).padding(top = 4.dp)) }

                Spacer(modifier = Modifier.height(16.dp))

                Button(onClick = {
                    focusManager.clearFocus()
                    // Launch suspend function from Compose scope
                    scope.launch {
                        viewModel.verifyOtpAndSetPassword(email = email, token = token)
                    }
                }, enabled = form.isFormValid && uiState !is ResetUiState.Loading, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                    if (uiState is ResetUiState.Loading) CircularProgressIndicator(modifier = Modifier.size(18.dp), color = MaterialTheme.colorScheme.onPrimary) else Text("Verify & Set New Password")
                }
            }
        }
    }
}
