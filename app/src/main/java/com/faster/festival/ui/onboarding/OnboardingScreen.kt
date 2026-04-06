package com.faster.festival.ui.onboarding

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.faster.festival.data.local.EncryptedSessionManager
import com.faster.festival.data.repository.OnboardingRepository
import com.faster.festival.di.NetworkModule
import com.faster.festival.ui.components.StepIndicator
import com.faster.festival.utils.PermissionUtils

/**
 * Main onboarding coordinator composable.
 *
 * Manages the 6-step onboarding flow:
 * 1. Profile Details (DOB, gender identity)
 * 2. Emergency Contacts (contact info + device contacts toggle)
 * 3. Confirm Account Details (legal name, phone, review)
 * 4. Username (choose username)
 * 5. Accept Terms (terms and conditions + privacy policy)
 * 6. Wristband Pair (optional wristband code)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    sessionManager: EncryptedSessionManager,
    onOnboardingComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val onboardingRepository = remember {
        OnboardingRepository(NetworkModule.onboardingApiService, sessionManager)
    }
    val viewModel: OnboardingViewModel = viewModel(
        factory = OnboardingViewModel.createFactory(onboardingRepository, sessionManager)
    )

    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    // Permission launcher for READ_CONTACTS
    val contactsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.enableDeviceContacts()
        } else {
            viewModel.disableDeviceContacts()
        }
    }

    LaunchedEffect(uiState.isComplete) {
        if (uiState.isComplete) {
            onOnboardingComplete()
        }
    }

    LaunchedEffect(uiState.error) {
        val errorMsg = uiState.error
        if (errorMsg != null) {
            snackbarHostState.showSnackbar(errorMsg)
            viewModel.clearError()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Text(
                            text = "Step ${viewModel.currentStepIndex + 1} of ${viewModel.totalSteps}",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    },
                    navigationIcon = {
                        if (!viewModel.isFirstStep) {
                            IconButton(onClick = { viewModel.previousStep() }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
                StepIndicator(
                    currentStep = viewModel.currentStepIndex,
                    totalSteps = viewModel.totalSteps,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            Crossfade(
                targetState = uiState.currentStep,
                animationSpec = tween(durationMillis = 300),
                label = "onboarding_step"
            ) { step ->
                when (step) {
                    OnboardingStep.PROFILE_DETAILS -> ProfileDetailsScreen(
                        dateOfBirth = uiState.dateOfBirth,
                        genderIdentity = uiState.genderIdentity,
                        dateOfBirthError = uiState.dateOfBirthError,
                        onDateOfBirthChange = viewModel::updateDateOfBirth,
                        onGenderIdentityChange = viewModel::updateGenderIdentity,
                        onContinue = viewModel::saveProfileDetails
                    )

                    OnboardingStep.EMERGENCY_CONTACT -> EmergencyContactScreen(
                        emergencyName = uiState.emergencyName,
                        emergencyPhone = uiState.emergencyPhone,
                        emergencyRelationship = uiState.emergencyRelationship,
                        emergencyNameError = uiState.emergencyNameError,
                        emergencyPhoneError = uiState.emergencyPhoneError,
                        deviceContactsEnabled = uiState.deviceContactsEnabled,
                        contactSuggestions = uiState.contactSuggestions,
                        onNameChange = { value ->
                            viewModel.updateEmergencyNameWithSearch(value, context)
                        },
                        onPhoneChange = viewModel::updateEmergencyPhone,
                        onRelationshipChange = viewModel::updateEmergencyRelationship,
                        onToggleDeviceContacts = { enabled ->
                            if (enabled) {
                                if (PermissionUtils.hasContactsPermission(context)) {
                                    viewModel.enableDeviceContacts()
                                } else {
                                    contactsPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
                                }
                            } else {
                                viewModel.disableDeviceContacts()
                            }
                        },
                        onContactSelected = viewModel::selectDeviceContact,
                        onDismissSuggestions = viewModel::dismissContactSuggestions,
                        onContinue = viewModel::saveEmergencyContact
                    )

                    OnboardingStep.CONFIRM_DETAILS -> ConfirmDetailsScreen(
                        legalName = uiState.legalName,
                        phoneNumber = uiState.phoneNumber,
                        email = uiState.email,
                        dateOfBirth = uiState.dateOfBirth,
                        genderIdentity = uiState.genderIdentity,
                        legalNameError = uiState.legalNameError,
                        phoneNumberError = uiState.phoneNumberError,
                        onLegalNameChange = viewModel::updateLegalName,
                        onPhoneNumberChange = viewModel::updatePhoneNumber,
                        onCreateAccount = viewModel::createAccount
                    )

                    OnboardingStep.USERNAME -> UsernameScreen(
                        username = uiState.username,
                        usernameError = uiState.usernameError,
                        onUsernameChange = viewModel::updateUsername,
                        onContinue = viewModel::saveUsername
                    )

                    OnboardingStep.ACCEPT_TERMS -> TermsAcceptanceScreen(
                        termsAccepted = uiState.termsAccepted,
                        onTermsAcceptedChange = viewModel::updateTermsAccepted,
                        onAcceptTerms = viewModel::acceptTermsAndContinue
                    )

                    OnboardingStep.WRISTBAND -> WristbandScreen(
                        wristbandCode = uiState.wristbandCode,
                        onWristbandCodeChange = viewModel::updateWristbandCode,
                        onPairWristband = viewModel::saveWristband,
                        onSkip = viewModel::skipWristband
                    )
                }
            }

            if (uiState.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(48.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}
