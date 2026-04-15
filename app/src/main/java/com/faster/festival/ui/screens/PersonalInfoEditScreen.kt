package com.faster.festival.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import androidx.lifecycle.viewmodel.compose.viewModel
import com.faster.festival.data.local.EncryptedSessionManager
import com.faster.festival.data.repository.ProfileRepository
import com.faster.festival.di.NetworkModule
import com.faster.festival.ui.viewmodel.EnhancedProfileViewModel
import com.faster.festival.ui.viewmodel.ProfileEditUiState
import com.faster.festival.ui.viewmodel.ProfileEditViewModel
import com.faster.festival.ui.viewmodel.ProfileState

private val CoralRed = Color(0xFFE53935)

private val genderOptions = listOf("Male", "Female", "Non-binary", "Prefer not to say", "Self-describe")

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
    var genderIdentityText by remember { mutableStateOf("") }
    var showDatePicker by remember { mutableStateOf(false) }

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
            // Phone & Email (local state)
            if (phone.isBlank()) {
                phone = profile.phone ?: ""
            }
            if (email.isBlank()) {
                email = profile.email ?: ""
            }
            // Date of Birth
            if (formState.dateOfBirth.isBlank() && !profile.dateOfBirth.isNullOrBlank()) {
                editViewModel.updateDateOfBirth(profile.dateOfBirth)
            }
            // Gender Identity
            if (formState.genderIdentity.isBlank() && !profile.genderIdentity.isNullOrBlank()) {
                // Convert API value to display label (e.g. "male" -> "Male")
                val displayLabel = com.faster.festival.data.model.GenderIdentity
                        .toDisplayLabel(profile.genderIdentity) ?: profile.genderIdentity
                editViewModel.updateGenderIdentity(displayLabel)
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

    Scaffold(
            topBar = {
                TopAppBar(
                        title = {
                            Text(
                                    "Update Personal Information",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = onBackClick) {
                                Icon(
                                        Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Back"
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.surface
                        )
                )
            }
    ) { innerPadding ->
        // Scrollable form content
        Column(
                modifier =
                        modifier.fillMaxSize()
                                .padding(innerPadding)
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = 24.dp, vertical = 20.dp)
        ) {
            // First Name (Legal)
            FormFieldLabel("First Name")
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                    value = formState.firstName,
                    onValueChange = { editViewModel.updateFirstName(it) },
                    placeholder = { Text("First name", color = Color(0xFFB0B0B0)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = formFieldColors()
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Last Name (Legal)
            FormFieldLabel("Last Name")
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                    value = formState.lastName,
                    onValueChange = { editViewModel.updateLastName(it) },
                    placeholder = { Text("Last name", color = Color(0xFFB0B0B0)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = formFieldColors()
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Phone Number
            FormFieldLabel("Phone Number")
            Spacer(modifier = Modifier.height(6.dp))
            com.faster.festival.ui.components.PhoneNumberField(
                value = phone,
                onValueChange = { phone = it },
                label = "",
                placeholder = "+1 234 567 8900",
                showLeadingIcon = false
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

            // Date of Birth — read-only field that opens a date picker
            FormFieldLabel("Date of Birth")
            Spacer(modifier = Modifier.height(6.dp))
            Box(modifier = Modifier.fillMaxWidth().clickable { showDatePicker = true }) {
                OutlinedTextField(
                        value = formatDateForDisplay(formState.dateOfBirth),
                        onValueChange = {},
                        readOnly = true,
                        enabled = false,
                        placeholder = { Text("Select date", color = Color(0xFFB0B0B0)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                disabledBorderColor = Color(0xFFD6D6D6),
                                disabledPlaceholderColor = Color(0xFFB0B0B0)
                        )
                )
            }

            if (showDatePicker) {
                val datePickerState = rememberDatePickerState(
                        initialSelectedDateMillis = parseDateToMillis(formState.dateOfBirth)
                )
                DatePickerDialog(
                        onDismissRequest = { showDatePicker = false },
                        confirmButton = {
                            TextButton(
                                    onClick = {
                                        datePickerState.selectedDateMillis?.let { millis ->
                                            val apiDate = formatMillisToApiDate(millis)
                                            editViewModel.updateDateOfBirth(apiDate)
                                        }
                                        showDatePicker = false
                                    },
                                    colors = ButtonDefaults.textButtonColors(contentColor = CoralRed)
                            ) {
                                Text("OK")
                            }
                        },
                        dismissButton = {
                            TextButton(
                                    onClick = { showDatePicker = false },
                                    colors = ButtonDefaults.textButtonColors(contentColor = CoralRed)
                            ) {
                                Text("Cancel")
                            }
                        },
                        colors = DatePickerDefaults.colors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    DatePicker(
                            state = datePickerState,
                            colors = DatePickerDefaults.colors(
                                    selectedDayContainerColor = CoralRed,
                                    todayDateBorderColor = CoralRed
                            )
                    )
                }
            }

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

            // Gender identity text (required when "Self-describe" is selected)
            if (formState.genderIdentity == "Self-describe") {
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                        value = genderIdentityText,
                        onValueChange = { genderIdentityText = it },
                        placeholder = { Text("Please describe", color = Color(0xFFB0B0B0)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = formFieldColors()
                )
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
                            editViewModel.saveDemographics(
                                    genderIdentityText = genderIdentityText.ifBlank { null }
                            )
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

/** Convert API date "YYYY-MM-DD" to display format "MMM dd, yyyy" (e.g. "Jan 01, 1990") */
private fun formatDateForDisplay(apiDate: String): String {
    if (apiDate.isBlank()) return ""
    return try {
        val parser = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        parser.timeZone = TimeZone.getTimeZone("UTC")
        val date = parser.parse(apiDate) ?: return apiDate
        SimpleDateFormat("MMM dd, yyyy", Locale.US).format(date)
    } catch (_: Exception) {
        apiDate
    }
}

/** Parse API date "YYYY-MM-DD" to epoch millis for DatePickerState */
private fun parseDateToMillis(apiDate: String): Long? {
    if (apiDate.isBlank()) return null
    return try {
        val parser = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        parser.timeZone = TimeZone.getTimeZone("UTC")
        parser.parse(apiDate)?.time
    } catch (_: Exception) {
        null
    }
}

/** Convert epoch millis from DatePicker to API date "YYYY-MM-DD" */
private fun formatMillisToApiDate(millis: Long): String {
    val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    formatter.timeZone = TimeZone.getTimeZone("UTC")
    return formatter.format(millis)
}
