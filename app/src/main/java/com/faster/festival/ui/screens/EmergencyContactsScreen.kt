package com.faster.festival.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.faster.festival.data.local.EncryptedSessionManager
import com.faster.festival.data.repository.ProfileRepository
import com.faster.festival.di.NetworkModule
import com.faster.festival.ui.viewmodel.EnhancedProfileViewModel
import com.faster.festival.ui.viewmodel.ProfileEditUiState
import com.faster.festival.ui.viewmodel.ProfileEditViewModel
import com.faster.festival.ui.viewmodel.ProfileState

/** Emergency Contacts Management Screen Allows users to manage their emergency contacts via APIs */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmergencyContactsScreen(modifier: Modifier = Modifier, onBackClick: () -> Unit = {}) {
    val context = LocalContext.current
    val sessionManager = remember { EncryptedSessionManager(context) }
    val profileRepository = remember { ProfileRepository(NetworkModule.profileApiService) }

    // Using two view models: one to fetch the list, one to edit
    val readViewModel: EnhancedProfileViewModel =
            viewModel(factory = EnhancedProfileViewModel.createFactory(profileRepository))
    val editViewModel: ProfileEditViewModel =
            viewModel(factory = ProfileEditViewModel.Factory(profileRepository, sessionManager))

    val profileState by readViewModel.profileState.collectAsState()
    val formState by editViewModel.formState.collectAsState()
    val editState by editViewModel.editState.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }

    // Load data
    LaunchedEffect(Unit) {
        val token = sessionManager.getAccessToken()
        if (token != null) {
            readViewModel.loadProfile(token)
        }
    }

    // Refresh on save success
    LaunchedEffect(editState) {
        if (editState is ProfileEditUiState.Success) {
            showAddDialog = false
            val token = sessionManager.getAccessToken()
            if (token != null) {
                readViewModel.loadProfile(token)
            }
        }
    }

    val contacts =
            (profileState as? ProfileState.Success)?.profile?.emergencyContacts ?: emptyList()

    Scaffold(
            topBar = {
                TopAppBar(
                        title = { Text("Emergency Contacts") },
                        navigationIcon = {
                            IconButton(onClick = onBackClick) {
                                Icon(
                                        Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Back"
                                )
                            }
                        },
                        actions = {
                            IconButton(onClick = { showAddDialog = true }) {
                                Icon(Icons.Default.Add, contentDescription = "Add Contact")
                            }
                        }
                )
            }
    ) { paddingValues ->
        Box(
                modifier =
                        modifier.fillMaxSize()
                                .background(MaterialTheme.colorScheme.background)
                                .padding(paddingValues)
        ) {
            if (profileState is ProfileState.Loading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (contacts.isEmpty()) {
                Text(
                        "No emergency contacts found. Tap + to add one.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.Center)
                )
            } else {
                LazyColumn(
                        modifier =
                                Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(contacts.size) { index ->
                        val contact = contacts[index]
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                            text = contact.externalName ?: "Unknown",
                                            style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                            text = contact.externalPhoneE164 ?: "No phone",
                                            style = MaterialTheme.typography.bodySmall
                                    )
                                    contact.relationship?.let { relationship ->
                                        Text(
                                                text = relationship,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                                IconButton(
                                        onClick = {
                                            contact.externalId?.let {
                                                editViewModel.deleteEmergencyContact(it)
                                            }
                                        }
                                ) {
                                    Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Delete",
                                            tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Edit State Banner
            if (editState is ProfileEditUiState.Error) {
                val errorMsg = (editState as ProfileEditUiState.Error).message
                Snackbar(modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp)) {
                    Text("Error: $errorMsg")
                }
            }

            // Add Contact Dialog
            if (showAddDialog) {
                AlertDialog(
                        onDismissRequest = { showAddDialog = false },
                        title = { Text("Add Emergency Contact") },
                        text = {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                        value = formState.contactName,
                                        onValueChange = { editViewModel.updateContactName(it) },
                                        label = { Text("Name") },
                                        isError = formState.contactNameError != null,
                                        supportingText =
                                                formState.contactNameError?.let { { Text(it) } },
                                        modifier = Modifier.fillMaxWidth()
                                )
                                OutlinedTextField(
                                        value = formState.contactPhone,
                                        onValueChange = { editViewModel.updateContactPhone(it) },
                                        label = { Text("Phone (e.g. +123...)") },
                                        isError = formState.contactPhoneError != null,
                                        supportingText =
                                                formState.contactPhoneError?.let { { Text(it) } },
                                        modifier = Modifier.fillMaxWidth()
                                )
                                OutlinedTextField(
                                        value = formState.contactRelationship,
                                        onValueChange = {
                                            editViewModel.updateContactRelationship(it)
                                        },
                                        label = { Text("Relationship (Optional)") },
                                        modifier = Modifier.fillMaxWidth()
                                )
                            }
                        },
                        confirmButton = {
                            Button(
                                    onClick = { editViewModel.saveEmergencyContact() },
                                    enabled = editState !is ProfileEditUiState.Loading
                            ) { Text("Save") }
                        },
                        dismissButton = {
                            TextButton(
                                    onClick = { showAddDialog = false },
                                    enabled = editState !is ProfileEditUiState.Loading
                            ) { Text("Cancel") }
                        }
                )
            }
        }
    }
}
