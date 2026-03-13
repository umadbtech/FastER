package com.faster.festival.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
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
private val CoralRedLight = Color(0xFFFFEBEE)

private val relationshipOptions =
        listOf("Parent", "Spouse", "Sibling", "Friend", "Partner", "Child", "Other")

/** Local form state for an individual contact card */
private data class ContactFormState(
        val id: String? = null,
        val name: String = "",
        val phone: String = "",
        val relationship: String = "",
        val isPrimary: Boolean = false
)

/** Emergency Contacts Management Screen — inline editable card-based UI */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmergencyContactsScreen(modifier: Modifier = Modifier, onBackClick: () -> Unit = {}) {
    val context = LocalContext.current
    val sessionManager = remember { EncryptedSessionManager(context) }
    val profileRepository = remember { ProfileRepository(NetworkModule.profileApiService) }

    val readViewModel: EnhancedProfileViewModel =
            viewModel(factory = EnhancedProfileViewModel.createFactory(profileRepository))
    val editViewModel: ProfileEditViewModel =
            viewModel(factory = ProfileEditViewModel.Factory(profileRepository, sessionManager))

    val profileState by readViewModel.profileState.collectAsState()
    val editState by editViewModel.editState.collectAsState()

    // Local editable contact forms
    var contactForms by remember { mutableStateOf(listOf<ContactFormState>()) }
    var hasInitialized by remember { mutableStateOf(false) }

    // Load data
    LaunchedEffect(Unit) {
        val token = sessionManager.getAccessToken()
        if (token != null) {
            readViewModel.loadProfile(token)
        }
    }

    // Populate forms from loaded contacts
    LaunchedEffect(profileState) {
        if (profileState is ProfileState.Success && !hasInitialized) {
            val contacts =
                    (profileState as ProfileState.Success).profile.emergencyContacts ?: emptyList()
            contactForms =
                    if (contacts.isEmpty()) {
                        listOf(ContactFormState(isPrimary = true))
                    } else {
                        contacts.map { c ->
                            ContactFormState(
                                    id = c.externalId,
                                    name = c.externalName ?: "",
                                    phone = c.externalPhoneE164 ?: "",
                                    relationship = c.relationship ?: "",
                                    isPrimary = c.isPrimary
                            )
                        }
                    }
            hasInitialized = true
        }
    }

    // Refresh on save success
    LaunchedEffect(editState) {
        if (editState is ProfileEditUiState.Success) {
            val token = sessionManager.getAccessToken()
            if (token != null) {
                hasInitialized = false
                readViewModel.loadProfile(token)
            }
        }
    }

    Scaffold(
            topBar = {
                TopAppBar(
                        title = {
                            Text(
                                    "Emergency Contacts",
                                    fontWeight = FontWeight.SemiBold,
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
                        colors =
                                TopAppBarDefaults.topAppBarColors(
                                        containerColor = MaterialTheme.colorScheme.surface
                                )
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
            } else {
                Column(
                        modifier =
                                Modifier.fillMaxSize()
                                        .verticalScroll(rememberScrollState())
                                        .padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    contactForms.forEachIndexed { index, form ->
                        val label =
                                if (form.isPrimary) "Primary Emergency Contact"
                                else "Secondary Emergency Contact"

                        EmergencyContactCard(
                                label = label,
                                form = form,
                                onFormChange = { updated ->
                                    contactForms =
                                            contactForms.toMutableList().also { it[index] = updated }
                                },
                                onStarClick = {
                                    // Toggle primary: set this one primary, others secondary
                                    contactForms =
                                            contactForms.mapIndexed { i, f ->
                                                f.copy(isPrimary = i == index)
                                            }
                                }
                        )

                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // Add Emergency Contact +
                    Text(
                            text = "Add Emergency Contact +",
                            color = CoralRed,
                            fontWeight = FontWeight.Medium,
                            fontSize = 15.sp,
                            modifier =
                                    Modifier.clickable {
                                                contactForms =
                                                        contactForms +
                                                                ContactFormState(isPrimary = false)
                                            }
                                            .padding(vertical = 8.dp)
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Save Button
                    Button(
                            onClick = {
                                // Save each contact via the ViewModel
                                contactForms.forEach { form ->
                                    if (form.name.isNotBlank() && form.phone.isNotBlank()) {
                                        editViewModel.updateContactName(form.name)
                                        editViewModel.updateContactPhone(form.phone)
                                        editViewModel.updateContactRelationship(form.relationship)
                                        editViewModel.saveEmergencyContact()
                                    }
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

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            // Error snackbar
            if (editState is ProfileEditUiState.Error) {
                val errorMsg = (editState as ProfileEditUiState.Error).message
                Snackbar(modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp)) {
                    Text("Error: $errorMsg")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EmergencyContactCard(
        label: String,
        form: ContactFormState,
        onFormChange: (ContactFormState) -> Unit,
        onStarClick: () -> Unit
) {
    val borderColor = if (form.isPrimary) CoralRed else Color(0xFFD6D6D6)
    val borderWidth = if (form.isPrimary) 1.5.dp else 1.dp

    var relationshipExpanded by remember { mutableStateOf(false) }

    OutlinedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(borderWidth, borderColor),
            elevation = CardDefaults.outlinedCardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header row: label + star
            Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                        text = label,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        color = if (form.isPrimary) CoralRed else MaterialTheme.colorScheme.onSurface
                )
                IconButton(onClick = onStarClick, modifier = Modifier.size(32.dp)) {
                    Icon(
                            imageVector =
                                    if (form.isPrimary) Icons.Filled.Star
                                    else Icons.Outlined.StarOutline,
                            contentDescription =
                                    if (form.isPrimary) "Primary contact" else "Set as primary",
                            tint = if (form.isPrimary) CoralRed else Color(0xFFBDBDBD),
                            modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Name field
            OutlinedTextField(
                    value = form.name,
                    onValueChange = { onFormChange(form.copy(name = it)) },
                    label = { Text("Full Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors =
                            OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = CoralRed,
                                    cursorColor = CoralRed
                            )
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Phone field
            OutlinedTextField(
                    value = form.phone,
                    onValueChange = { onFormChange(form.copy(phone = it)) },
                    label = { Text("Phone Number") },
                    placeholder = { Text("+1 234 567 8900") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors =
                            OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = CoralRed,
                                    cursorColor = CoralRed
                            )
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Relationship dropdown
            ExposedDropdownMenuBox(
                    expanded = relationshipExpanded,
                    onExpandedChange = { relationshipExpanded = it }
            ) {
                OutlinedTextField(
                        value = form.relationship,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Relationship") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = relationshipExpanded)
                        },
                        modifier =
                                Modifier.fillMaxWidth()
                                        .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                        shape = RoundedCornerShape(10.dp),
                        colors =
                                OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = CoralRed,
                                        cursorColor = CoralRed
                                )
                )
                ExposedDropdownMenu(
                        expanded = relationshipExpanded,
                        onDismissRequest = { relationshipExpanded = false }
                ) {
                    relationshipOptions.forEach { option ->
                        DropdownMenuItem(
                                text = { Text(option) },
                                onClick = {
                                    onFormChange(form.copy(relationship = option))
                                    relationshipExpanded = false
                                }
                        )
                    }
                }
            }
        }
    }
}
