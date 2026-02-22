package com.faster.festival.ui.auth.login

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import com.faster.festival.R
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmailLoginScreen(
    viewModel: LoginViewModel,
    modifier: Modifier = Modifier,
    onForgotPassword: () -> Unit = {},
    onBackToSignup: () -> Unit = {},
    onLoginSuccess: () -> Unit = {},
    onCancel: () -> Unit = {}
) {
    val form by viewModel.formState.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val focusManager = LocalFocusManager.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var passwordVisible by remember { mutableStateOf(false) }

    LaunchedEffect(uiState) {
        when (uiState) {
            is LoginUiState.Success -> {
                onLoginSuccess()
            }
            is LoginUiState.Error -> {
                val msg = (uiState as LoginUiState.Error).message
                if (msg.isNotBlank()) {
                    scope.launch {
                        snackbarHostState.showSnackbar(msg)
                    }
                }
            }
            else -> {}
        }
    }

    val signInTitle = stringResource(id = R.string.sign_in)
    val signInHint = stringResource(id = R.string.continue_with_email_hint)

    Scaffold(
        modifier = modifier,
        topBar = {
            SmallTopAppBar(
                title = { /* empty title - main title shown in content for visual parity */ Text("") },
                navigationIcon = {
                    IconButton(onClick = onBackToSignup) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = stringResource(id = R.string.back))
                    }
                },
                actions = {
                    IconButton(onClick = onCancel) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = stringResource(id = R.string.close))
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        Surface(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .animateContentSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top
            ) {
                Spacer(modifier = Modifier.height(24.dp))

                Text(text = signInTitle, style = MaterialTheme.typography.headlineSmall)
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = signInHint, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

                Spacer(modifier = Modifier.height(24.dp))

                OutlinedTextField(
                    value = form.email,
                    onValueChange = { viewModel.onEmailChange(it) },
                    label = { Text(stringResource(id = R.string.email_label)) },
                    isError = form.emailError != null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                form.emailError?.let { Text(text = it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.align(Alignment.Start).padding(top = 4.dp)) }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = form.password,
                    onValueChange = { viewModel.onPasswordChange(it) },
                    label = { Text(stringResource(id = R.string.password_label)) },
                    isError = form.passwordError != null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                    singleLine = true,
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff, contentDescription = if (passwordVisible) stringResource(id = R.string.close) else stringResource(id = R.string.close))
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                form.passwordError?.let { Text(text = it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.align(Alignment.Start).padding(top = 4.dp)) }

                Spacer(modifier = Modifier.height(12.dp))

                TextButton(onClick = onForgotPassword, modifier = Modifier.align(Alignment.End)) { Text(stringResource(id = R.string.forgot_password)) }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        focusManager.clearFocus()
                        viewModel.login(onSuccess = onLoginSuccess)
                    },
                    enabled = form.isFormValid && uiState !is LoginUiState.Loading,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = MaterialTheme.shapes.medium
                ) {
                    if (uiState is LoginUiState.Loading) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = MaterialTheme.colorScheme.onPrimary)
                    } else {
                        Text(stringResource(id = R.string.login_label))
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                TextButton(onClick = onBackToSignup) { Text(stringResource(id = R.string.back_to_signup)) }
            }
        }
    }
}
