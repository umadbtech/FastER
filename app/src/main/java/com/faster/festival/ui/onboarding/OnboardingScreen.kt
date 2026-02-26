@file:OptIn(ExperimentalFoundationApi::class)

package com.faster.festival.ui.onboarding

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.faster.festival.data.repository.OnboardingRepository
import com.faster.festival.di.NetworkModule
import com.faster.festival.data.local.EncryptedSessionManager

/**
 * Main onboarding activity composable using HorizontalPager for swipeable screens.
 */
@Composable
fun OnboardingScreen(
    sessionManager: EncryptedSessionManager,
    onOnboardingComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Create repository and ViewModel
    val onboardingRepository = remember {
        OnboardingRepository(NetworkModule.onboardingApiService, sessionManager)
    }
    val viewModel: OnboardingViewModel = viewModel(
        factory = OnboardingViewModel.createFactory(onboardingRepository)
    )

    val uiState by viewModel.uiState.collectAsState()
    val formState by viewModel.formState.collectAsState()

    // Initialize pager state with stable API
    val pagerState: PagerState = rememberPagerState(pageCount = { 4 })
    val snackbarHostState = remember { SnackbarHostState() }

    // Initialize onboarding on first load
    LaunchedEffect(Unit) {
        viewModel.initializeOnboarding()
    }

    // Handle UI state changes
    LaunchedEffect(uiState) {
        when (uiState) {
            is OnboardingUiState.Error -> {
                snackbarHostState.showSnackbar((uiState as OnboardingUiState.Error).message)
            }
            is OnboardingUiState.Success -> {
                snackbarHostState.showSnackbar((uiState as OnboardingUiState.Success).message)
            }
            OnboardingUiState.OnboardingComplete -> {
                onOnboardingComplete()
            }
            else -> {}
        }
    }

    // Update pager position when screen changes
    LaunchedEffect(formState.currentScreen) {
        pagerState.animateScrollToPage(formState.currentScreen)
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            OnboardingTopBar(currentScreen = formState.currentScreen)
        },
        bottomBar = {
            OnboardingBottomBar(
                currentScreen = formState.currentScreen,
                isLoading = uiState is OnboardingUiState.Loading,
                onBackClick = { viewModel.goBack() },
                onNextClick = {
                    when (formState.currentScreen) {
                        0 -> viewModel.proceedFromDOB()
                        1 -> viewModel.proceedFromRaceEthnicity()
                        2 -> viewModel.proceedFromGenderIdentity()
                        3 -> viewModel.submitOnboarding()
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (uiState is OnboardingUiState.Loading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.primary
                )
            } else {
                HorizontalPager(state = pagerState, userScrollEnabled = false) { page ->
                    when (page) {
                        0 -> DateOfBirthScreen(
                            formState = formState,
                            onDateChange = { viewModel.updateDateOfBirth(it) }
                        )
                        1 -> RaceEthnicityScreen(
                            formState = formState,
                            onRaceToggle = { viewModel.toggleRaceEthnicity(it) },
                            onCustomTextChange = { viewModel.updateRaceEthnicityText(it) }
                        )
                        2 -> GenderIdentityScreen(
                            formState = formState,
                            onGenderSelect = { viewModel.updateGenderIdentity(it) },
                            onCustomTextChange = { viewModel.updateGenderIdentityText(it) }
                        )
                        3 -> WristbandScreen(
                            formState = formState,
                            onSkipPairing = { viewModel.submitOnboarding() },
                            onPairingReady = { viewModel.submitOnboarding() },
                            onBackPressed = { viewModel.goBack() }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OnboardingTopBar(currentScreen: Int) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp)
    ) {
        // Step indicator text
        Text(
            text = "Step ${currentScreen + 1} of 4",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Progress dots
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
        ) {
            repeat(4) { index ->
                Box(
                    modifier = Modifier
                        .width(if (index == currentScreen) 24.dp else 8.dp)
                        .height(8.dp)
                        .background(
                            if (index <= currentScreen) {
                                Color(0xFF22C55E) // Green accent
                            } else {
                                MaterialTheme.colorScheme.outlineVariant
                            },
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp)
                        )
                )
            }
        }
    }
}

@Composable
private fun OnboardingBottomBar(
    currentScreen: Int,
    isLoading: Boolean,
    onBackClick: () -> Unit,
    onNextClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
    ) {
        // Back button (visible for screens after 0)
        if (currentScreen > 0) {
            Button(
                onClick = onBackClick,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.outlineVariant
                )
            ) {
                Text("Back")
            }
        }

        // Next/Submit button
        Button(
            onClick = onNextClick,
            modifier = if (currentScreen > 0) {
                Modifier
                    .weight(1f)
                    .height(48.dp)
            } else {
                Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF081836), // Dark navy
                contentColor = Color.White
            ),
            enabled = !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .width(20.dp)
                        .height(20.dp),
                    strokeWidth = 2.dp,
                    color = Color.White
                )
            } else {
                Text(
                    if (currentScreen == 3) "Submit" else "Continue",
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
