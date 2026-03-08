
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.faster.festival.data.local.EncryptedSessionManager
import com.faster.festival.data.repository.ProfileRepository
import com.faster.festival.di.NetworkModule
import com.faster.festival.ui.viewmodel.ProfileEditUiState
import com.faster.festival.ui.viewmodel.ProfileEditViewModel

/**
 * Demographics Edit Screen
 * Allows users to edit their demographics data
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DemographicsEditScreen(
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {},
    onSaveSuccess: () -> Unit = {}
) {
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
                    "Edit Demographics",
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
            Text(
                "Update your demographics information",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Date of Birth Field
            OutlinedTextField(
                value = formState.dateOfBirth,
                onValueChange = { viewModel.updateDateOfBirth(it) },
                label = { Text("Date of Birth (YYYY-MM-DD)") },
                modifier = Modifier.fillMaxWidth()
            )

            // Gender Identity Field
            OutlinedTextField(
                value = formState.genderIdentity,
                onValueChange = { viewModel.updateGenderIdentity(it) },
                label = { Text("Gender Identity") },
                modifier = Modifier.fillMaxWidth()
            )

            // Race/Ethnicity Field
            OutlinedTextField(
                value = formState.raceEthnicity,
                onValueChange = { viewModel.updateRaceEthnicity(it) },
                label = { Text("Race/Ethnicity") },
                modifier = Modifier.fillMaxWidth()
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
                onClick = { viewModel.saveDemographics() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                enabled = editState !is ProfileEditUiState.Loading
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
