package com.faster.festival.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.faster.festival.data.local.EncryptedSessionManager
import com.faster.festival.data.repository.ProfileRepository
import com.faster.festival.di.NetworkModule
import com.faster.festival.ui.viewmodel.ProfileEditUiState
import com.faster.festival.ui.viewmodel.ProfileEditViewModel
import androidx.compose.ui.platform.LocalContext

/**
 * Personal Information Edit Screen
 * Allows users to edit their legal first name, last name
 * with full validation and API integration via ProfileEditViewModel
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonalInfoEditScreen(
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {},
    onSaveSuccess: () -> Unit = {}
) {
    // ✅ FIX: Get context and create ViewModel with proper factory
    val context = LocalContext.current
    val sessionManager = EncryptedSessionManager(context)
    val profileRepository = ProfileRepository(NetworkModule.profileApiService)

    val viewModel: ProfileEditViewModel = viewModel(
        factory = ProfileEditViewModel.Factory(profileRepository, sessionManager)
    )

    val formState by viewModel.formState.collectAsState()
    val editState by viewModel.editState.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top App Bar
        TopAppBar(
            title = {
                Text(
                    "Edit Legal Name",
                    fontWeight = FontWeight.Bold
                )
            },
            navigationIcon = {
                IconButton(onClick = onBackClick) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primary,
                titleContentColor = MaterialTheme.colorScheme.onPrimary
            )
        )

        // Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Info text
            Text(
                "Update your legal name as it appears on official documents",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            // First Name Field
            OutlinedTextField(
                value = formState.firstName,
                onValueChange = { viewModel.updateFirstName(it) },
                label = { Text("First Name *") },
                modifier = Modifier.fillMaxWidth(),
                isError = formState.firstNameError != null,
                supportingText = {
                    if (formState.firstNameError != null) {
                        Text(
                            formState.firstNameError!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                },
                trailingIcon = {
                    if (formState.firstNameError == null && formState.firstName.isNotBlank()) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = "Valid",
                            tint = Color.Green
                        )
                    } else if (formState.firstNameError != null) {
                        Icon(
                            Icons.Default.Error,
                            contentDescription = "Error",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            )

            // Last Name Field
            OutlinedTextField(
                value = formState.lastName,
                onValueChange = { viewModel.updateLastName(it) },
                label = { Text("Last Name *") },
                modifier = Modifier.fillMaxWidth(),
                isError = formState.lastNameError != null,
                supportingText = {
                    if (formState.lastNameError != null) {
                        Text(
                            formState.lastNameError!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                },
                trailingIcon = {
                    if (formState.lastNameError == null && formState.lastName.isNotBlank()) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = "Valid",
                            tint = Color.Green
                        )
                    } else if (formState.lastNameError != null) {
                        Icon(
                            Icons.Default.Error,
                            contentDescription = "Error",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // State Messages
            when (editState) {
                is ProfileEditUiState.Loading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    }
                }
                is ProfileEditUiState.Success -> {
                    val successState = editState as? ProfileEditUiState.Success
                    if (successState != null) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = Color.Green.copy(alpha = 0.1f)
                            ),
                            border = CardDefaults.outlinedCardBorder().copy(
                                brush = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    Color.Green
                                ).brush
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = "Success",
                                    tint = Color.Green,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    successState.message,
                                    color = Color.Green.copy(alpha = 0.8f),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }

                        LaunchedEffect(Unit) {
                            kotlinx.coroutines.delay(2000)
                            onSaveSuccess()
                        }
                    }
                }
                is ProfileEditUiState.Error -> {
                    val errorState = editState as? ProfileEditUiState.Error
                    if (errorState != null) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Default.Error,
                                    contentDescription = "Error",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(20.dp)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        errorState.message,
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    }
                }
                ProfileEditUiState.Idle -> {
                    // No message when idle
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Save Button
            Button(
                onClick = { viewModel.saveLegalName() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                enabled = formState.isFormValid && editState !is ProfileEditUiState.Loading
            ) {
                if (editState is ProfileEditUiState.Loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Save Changes")
                }
            }

            // Cancel Button
            OutlinedButton(
                onClick = onBackClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                enabled = editState !is ProfileEditUiState.Loading
            ) {
                Text("Cancel")
            }
        }
    }
}
