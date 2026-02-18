package com.faster.festival.ui.auth.signup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp

@Composable
fun SignupScreen(
        viewModel: SignupViewModel,
        onNavigateToVerification: (String) -> Unit // Added email arg
) {
    val uiState by viewModel.uiState.collectAsState()
    val formState by viewModel.formState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scrollState = rememberScrollState()

    LaunchedEffect(uiState) {
        when (uiState) {
            is SignupUiState.Success -> {
                val email = (uiState as SignupUiState.Success).email
                onNavigateToVerification(email)
                viewModel.resetState()
            }
            is SignupUiState.Error -> {
                snackbarHostState.showSnackbar((uiState as SignupUiState.Error).message)
            }
            else -> {}
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { paddingValues ->
        Column(
                modifier =
                        Modifier.fillMaxSize()
                                .padding(paddingValues)
                                .padding(16.dp)
                                .verticalScroll(scrollState),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
        ) {
            Text(text = "Create Account", style = MaterialTheme.typography.headlineMedium)

            Spacer(modifier = Modifier.height(32.dp))

            // Full Name
            OutlinedTextField(
                    value = formState.fullName,
                    onValueChange = viewModel::onFullNameChange,
                    label = { Text("Full Name") },
                    isError = formState.fullNameError != null,
                    supportingText = {
                        if (formState.fullNameError != null) {
                            Text(formState.fullNameError!!)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions =
                            KeyboardOptions(
                                    capitalization = KeyboardCapitalization.Words,
                                    imeAction = ImeAction.Next
                            )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Email
            OutlinedTextField(
                    value = formState.email,
                    onValueChange = viewModel::onEmailChange,
                    label = { Text("Email") },
                    isError = formState.emailError != null,
                    supportingText = {
                        if (formState.emailError != null) {
                            Text(formState.emailError!!)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions =
                            KeyboardOptions(
                                    keyboardType = KeyboardType.Email,
                                    imeAction = ImeAction.Next
                            )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Password
            var passwordVisible by remember { mutableStateOf(false) }
            OutlinedTextField(
                    value = formState.password,
                    onValueChange = viewModel::onPasswordChange,
                    label = { Text("Password") },
                    isError = formState.passwordError != null,
                    supportingText = {
                        if (formState.passwordError != null) {
                            Text(formState.passwordError!!)
                        }
                    },
                    visualTransformation =
                            if (passwordVisible) VisualTransformation.None
                            else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                    imageVector =
                                            if (passwordVisible) Icons.Filled.Visibility
                                            else Icons.Filled.VisibilityOff,
                                    contentDescription =
                                            if (passwordVisible) "Hide password"
                                            else "Show password"
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions =
                            KeyboardOptions(
                                    keyboardType = KeyboardType.Password,
                                    imeAction = ImeAction.Next
                            )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Confirm Password
            var confirmPasswordVisible by remember { mutableStateOf(false) }
            OutlinedTextField(
                    value = formState.confirmPassword,
                    onValueChange = viewModel::onConfirmPasswordChange,
                    label = { Text("Confirm Password") },
                    isError = formState.confirmPasswordError != null,
                    supportingText = {
                        if (formState.confirmPasswordError != null) {
                            Text(formState.confirmPasswordError!!)
                        }
                    },
                    visualTransformation =
                            if (confirmPasswordVisible) VisualTransformation.None
                            else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                            Icon(
                                    imageVector =
                                            if (confirmPasswordVisible) Icons.Filled.Visibility
                                            else Icons.Filled.VisibilityOff,
                                    contentDescription =
                                            if (confirmPasswordVisible) "Hide password"
                                            else "Show password"
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions =
                            KeyboardOptions(
                                    keyboardType = KeyboardType.Password,
                                    imeAction = ImeAction.Done
                            )
            )

            Spacer(modifier = Modifier.height(24.dp))

            if (uiState is SignupUiState.Loading) {
                CircularProgressIndicator()
            } else {
                Button(
                        onClick = viewModel::onSignupClick,
                        enabled = formState.isFormValid,
                        modifier = Modifier.fillMaxWidth()
                ) { Text("Create Account") }
            }
        }
    }
}
