package com.faster.festival.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.faster.festival.data.local.EncryptedSessionManager
import com.faster.festival.data.repository.ProfileRepository
import com.faster.festival.di.NetworkModule
import com.faster.festival.ui.viewmodel.EnhancedProfileViewModel
import com.faster.festival.ui.viewmodel.ProfileEditUiState
import com.faster.festival.ui.viewmodel.ProfileEditViewModel
import com.faster.festival.ui.viewmodel.ProfileState

private val CoralRed = Color(0xFFE53935)

private val genderOptions = listOf("Male", "Female", "Non-binary", "Prefer not to say", "Other")

/**
 * Personal Information Edit Screen
 * Allows users to view and edit their personal information
 * with inline form fields and a clean minimal design.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonalInfoEditScreen(
        modifier: Modifier = Modifier,
        onBackClick: () -> Unit = {},
        onSaveSuccess: () -> Unit = {}
) {
    val context = LocalContext.current
    val sessionManager = remember { EncryptedSessionManager(context) }
    val profileRepository = remember { ProfileRepository(NetworkModule.profileApiService) }

    val readViewModel: EnhancedProfileViewModel =
            viewModel(factory = EnhancedProfileViewModel.createFactory(profileRepository))
    val editViewModel: ProfileEditViewModel =
            viewModel(factory = ProfileEditViewModel.Factory(profileRepository, sessionManager))

    val profileState by readViewModel.profileState.collectAsState()
    val formState by editViewModel.formState.collectAsState()
    val editState by editViewModel.editState.collectAsState()

    // Local fields for phone/email (not in ViewModel)
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var genderExpanded by remember { mutableStateOf(false) }

    // Load profile data
    LaunchedEffect(Unit) {
        val token = sessionManager.getAccessToken()
        if (token != null) {
            readViewModel.loadProfile(token)
        }
    }

    // Pre-populate form fields from loaded profile
    LaunchedEffect(profileState) {
        if (profileState is ProfileState.Success) {
            val profile = (profileState as ProfileState.Success).profile
            val firstName = profile.legalFirstName ?: ""
            val lastName = profile.legalLastName ?: ""
            if (formState.firstName.isBlank() && firstName.isNotBlank()) {
                editViewModel.updateFirstName(firstName)
            }
            if (formState.lastName.isBlank() && lastName.isNotBlank()) {
                editViewModel.updateLastName(lastName)
            }
        }
    }

    // Navigate back on save success
    LaunchedEffect(editState) {
        if (editState is ProfileEditUiState.Success) {
            kotlinx.coroutines.delay(1500)
            onSaveSuccess()
        }
    }

    Column(
            modifier =
                    modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
    ) {
        // Top bar — clean white with back arrow and title
        Row(
                modifier =
                        Modifier.fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surface)
                                .statusBarsPadding()
                                .padding(horizontal = 4.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                    "Update Personal Information",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
            )
        }

        // Scrollable form content
        Column(
                modifier =
                        Modifier.fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = 24.dp, vertical = 20.dp)
        ) {
            // Full Legal Name
            FormFieldLabel("Full Legal Name")
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                    value =
                            "${formState.firstName}${if (formState.firstName.isNotBlank() && formState.lastName.isNotBlank()) " " else ""}${formState.lastName}",
                    onValueChange = { fullName ->
                        val parts = fullName.trim().split(" ", limit = 2)
                        editViewModel.updateFirstName(parts.getOrElse(0) { "" })
                        editViewModel.updateLastName(parts.getOrElse(1) { "" })
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = formFieldColors()
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Phone Number
            FormFieldLabel("Phone Number")
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    placeholder = { Text("111-111-1111", color = Color(0xFFB0B0B0)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = formFieldColors()
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Email
            FormFieldLabel("Email")
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    placeholder = { Text("email@example.com", color = Color(0xFFB0B0B0)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = formFieldColors()
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Date of Birth
            FormFieldLabel("Date of Birth")
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                    value = formState.dateOfBirth,
                    onValueChange = { editViewModel.updateDateOfBirth(it) },
                    placeholder = { Text("May 13, 1964", color = Color(0xFFB0B0B0)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = formFieldColors()
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Gender Identity dropdown
            FormFieldLabel("Gender Identity")
            Spacer(modifier = Modifier.height(6.dp))
            ExposedDropdownMenuBox(
                    expanded = genderExpanded,
                    onExpandedChange = { genderExpanded = it }
            ) {
                OutlinedTextField(
                        value = formState.genderIdentity,
                        onValueChange = {},
                        readOnly = true,
                        placeholder = { Text("Select", color = Color(0xFFB0B0B0)) },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = genderExpanded)
                        },
                        modifier =
                                Modifier.fillMaxWidth()
                                        .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                        shape = RoundedCornerShape(10.dp),
                        colors = formFieldColors()
                )
                ExposedDropdownMenu(
                        expanded = genderExpanded,
                        onDismissRequest = { genderExpanded = false }
                ) {
                    genderOptions.forEach { option ->
                        DropdownMenuItem(
                                text = { Text(option) },
                                onClick = {
                                    editViewModel.updateGenderIdentity(option)
                                    genderExpanded = false
                                }
                        )
                    }
                }
            }

            // Status messages
            Spacer(modifier = Modifier.height(16.dp))
            when (editState) {
                is ProfileEditUiState.Loading -> {
                    Box(
                            modifier = Modifier.fillMaxWidth().padding(8.dp),
                            contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = CoralRed
                        )
                    }
                }
                is ProfileEditUiState.Success -> {
                    Text(
                            (editState as ProfileEditUiState.Success).message,
                            color = Color(0xFF4CAF50),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
                is ProfileEditUiState.Error -> {
                    Text(
                            (editState as ProfileEditUiState.Error).message,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
                ProfileEditUiState.Idle -> {}
            }

            Spacer(modifier = Modifier.weight(1f))

            // Save Button
            Button(
                    onClick = {
                        editViewModel.saveLegalName()
                        if (formState.dateOfBirth.isNotBlank() ||
                                        formState.genderIdentity.isNotBlank()
                        ) {
                            editViewModel.saveDemographics()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors =
                            ButtonDefaults.buttonColors(
                                    containerColor = CoralRed,
                                    contentColor = Color.White
                            ),
                    enabled = editState !is ProfileEditUiState.Loading
            ) {
                if (editState is ProfileEditUiState.Loading) {
                    CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                    )
                } else {
                    Text("Save", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun FormFieldLabel(text: String) {
    Text(
            text = text,
            fontWeight = FontWeight.SemiBold,
            fontSize = 15.sp,
            color = MaterialTheme.colorScheme.onSurface
    )
}

@Composable
private fun formFieldColors(): TextFieldColors {
    return OutlinedTextFieldDefaults.colors(
            focusedBorderColor = CoralRed,
            unfocusedBorderColor = Color(0xFFD6D6D6),
            cursorColor = CoralRed
    )
}
