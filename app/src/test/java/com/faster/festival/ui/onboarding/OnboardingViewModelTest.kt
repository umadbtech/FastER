package com.faster.festival.ui.onboarding

import com.faster.festival.data.local.EncryptedSessionManager
import com.faster.festival.data.model.OnboardingResponse
import com.faster.festival.data.repository.OnboardingRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var mockRepository: OnboardingRepository
    private lateinit var sessionManager: EncryptedSessionManager
    private lateinit var viewModel: OnboardingViewModel

    private val successResponse = OnboardingResponse(
        saved = true, activated = false, status = "onboarding", missing = emptyList()
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        mockRepository = mockk(relaxed = true)
        sessionManager = mockk(relaxed = true)
        io.mockk.every { sessionManager.getUserEmail() } returns "test@example.com"

        coEvery { mockRepository.ensureOnboarding() } returns Result.success(
            "297d5837-a7b6-49a4-873b-4e3b17b60657"
        )

        viewModel = OnboardingViewModel(mockRepository, sessionManager)
    }

    // ============= STEP NAVIGATION TESTS =============

    @Test
    fun `test total steps is 6`() {
        assertEquals(6, viewModel.totalSteps)
    }

    @Test
    fun `test initial step is PROFILE_DETAILS`() {
        assertEquals(OnboardingStep.PROFILE_DETAILS, viewModel.uiState.value.currentStep)
    }

    @Test
    fun `test initial step index is 0`() {
        assertEquals(0, viewModel.currentStepIndex)
    }

    @Test
    fun `test isFirstStep is true at start`() {
        assertTrue(viewModel.isFirstStep)
    }

    @Test
    fun `test email is loaded from session manager`() {
        assertEquals("test@example.com", viewModel.uiState.value.email)
    }

    @Test
    fun `test previousStep does not go before first step`() {
        viewModel.previousStep()
        assertEquals(OnboardingStep.PROFILE_DETAILS, viewModel.uiState.value.currentStep)
        assertEquals(0, viewModel.currentStepIndex)
    }

    // ============= FIELD UPDATE TESTS =============

    @Test
    fun `test updateDateOfBirth updates state`() {
        viewModel.updateDateOfBirth("1990-01-01")
        assertEquals("1990-01-01", viewModel.uiState.value.dateOfBirth)
    }

    @Test
    fun `test updateGenderIdentity updates state`() {
        viewModel.updateGenderIdentity("Non-binary")
        assertEquals("Non-binary", viewModel.uiState.value.genderIdentity)
    }

    @Test
    fun `test updateEmergencyName updates state`() {
        viewModel.updateEmergencyName("Jane Doe")
        assertEquals("Jane Doe", viewModel.uiState.value.emergencyName)
    }

    @Test
    fun `test updateEmergencyPhone updates state`() {
        viewModel.updateEmergencyPhone("+14155551234")
        assertEquals("+14155551234", viewModel.uiState.value.emergencyPhone)
    }

    @Test
    fun `test updateEmergencyRelationship updates state`() {
        viewModel.updateEmergencyRelationship("Parent")
        assertEquals("Parent", viewModel.uiState.value.emergencyRelationship)
    }

    @Test
    fun `test updateLegalName updates state`() {
        viewModel.updateLegalName("John Doe")
        assertEquals("John Doe", viewModel.uiState.value.legalName)
    }

    @Test
    fun `test updatePhoneNumber updates state`() {
        viewModel.updatePhoneNumber("+14155551234")
        assertEquals("+14155551234", viewModel.uiState.value.phoneNumber)
    }

    @Test
    fun `test updateUsername updates state`() {
        viewModel.updateUsername("testuser")
        assertEquals("testuser", viewModel.uiState.value.username)
    }

    @Test
    fun `test updateUsername clears error`() {
        viewModel.updateUsername("testuser")
        assertNull(viewModel.uiState.value.usernameError)
    }

    @Test
    fun `test updateTermsAccepted updates state`() {
        viewModel.updateTermsAccepted(true)
        assertTrue(viewModel.uiState.value.termsAccepted)
    }

    @Test
    fun `test updateWristbandCode updates state`() {
        viewModel.updateWristbandCode("ABC123")
        assertEquals("ABC123", viewModel.uiState.value.wristbandCode)
    }

    @Test
    fun `test clearError clears error`() {
        viewModel.createAccount()
        assertNotNull(viewModel.uiState.value.legalNameError)

        viewModel.clearError()
        assertNull(viewModel.uiState.value.error)
    }

    // ============= STEP 1: PROFILE DETAILS TESTS =============

    @Test
    fun `test saveProfileDetails fails with empty DOB`() = runTest(testDispatcher) {
        viewModel.updateDateOfBirth("")
        viewModel.saveProfileDetails()
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.dateOfBirthError)
        assertEquals(OnboardingStep.PROFILE_DETAILS, viewModel.uiState.value.currentStep)
    }

    @Test
    fun `test saveProfileDetails fails with future DOB`() = runTest(testDispatcher) {
        viewModel.updateDateOfBirth("2099-01-01")
        viewModel.saveProfileDetails()
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.dateOfBirthError)
    }

    @Test
    fun `test saveProfileDetails succeeds and advances to EMERGENCY_CONTACT`() = runTest(testDispatcher) {
        coEvery { mockRepository.saveDemographics(any()) } returns Result.success(successResponse)

        viewModel.updateDateOfBirth("1990-01-01")
        viewModel.updateGenderIdentity("Male")
        viewModel.saveProfileDetails()
        advanceUntilIdle()

        assertEquals(OnboardingStep.EMERGENCY_CONTACT, viewModel.uiState.value.currentStep)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `test saveProfileDetails shows error on API failure`() = runTest(testDispatcher) {
        coEvery { mockRepository.saveDemographics(any()) } returns Result.failure(Exception("Server error"))

        viewModel.updateDateOfBirth("1990-01-01")
        viewModel.saveProfileDetails()
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.error)
        assertEquals(OnboardingStep.PROFILE_DETAILS, viewModel.uiState.value.currentStep)
    }

    // ============= STEP 2: EMERGENCY CONTACT TESTS =============

    @Test
    fun `test saveEmergencyContact fails with empty name`() = runTest(testDispatcher) {
        viewModel.updateEmergencyName("")
        viewModel.updateEmergencyPhone("+14155551234")
        viewModel.saveEmergencyContact()
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.emergencyNameError)
    }

    @Test
    fun `test saveEmergencyContact fails with empty phone`() = runTest(testDispatcher) {
        viewModel.updateEmergencyName("Jane Doe")
        viewModel.updateEmergencyPhone("")
        viewModel.saveEmergencyContact()
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.emergencyPhoneError)
    }

    @Test
    fun `test saveEmergencyContact succeeds and advances to CONFIRM_DETAILS`() = runTest(testDispatcher) {
        coEvery { mockRepository.saveEmergencyContact(any()) } returns Result.success(successResponse)

        viewModel.updateEmergencyName("Jane Doe")
        viewModel.updateEmergencyPhone("+14155551234")
        viewModel.saveEmergencyContact()
        advanceUntilIdle()

        assertEquals(OnboardingStep.CONFIRM_DETAILS, viewModel.uiState.value.currentStep)
    }

    @Test
    fun `test saveEmergencyContact shows error on API failure`() = runTest(testDispatcher) {
        coEvery { mockRepository.saveDemographics(any()) } returns Result.success(successResponse)
        viewModel.updateDateOfBirth("1990-01-01")
        viewModel.saveProfileDetails()
        advanceUntilIdle()

        coEvery { mockRepository.saveEmergencyContact(any()) } returns Result.failure(Exception("Network error"))
        viewModel.updateEmergencyName("Jane Doe")
        viewModel.updateEmergencyPhone("+14155551234")
        viewModel.saveEmergencyContact()
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.error)
        assertEquals(OnboardingStep.EMERGENCY_CONTACT, viewModel.uiState.value.currentStep)
    }

    // ============= STEP 3: CONFIRM DETAILS / CREATE ACCOUNT TESTS =============

    @Test
    fun `test createAccount fails with empty legal name`() = runTest(testDispatcher) {
        viewModel.updateLegalName("")
        viewModel.updatePhoneNumber("+14155551234")
        viewModel.createAccount()
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.legalNameError)
    }

    @Test
    fun `test createAccount fails with empty phone number`() = runTest(testDispatcher) {
        viewModel.updateLegalName("John Doe")
        viewModel.updatePhoneNumber("")
        viewModel.createAccount()
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.phoneNumberError)
    }

    @Test
    fun `test createAccount fails with single name only`() = runTest(testDispatcher) {
        viewModel.updateLegalName("John")
        viewModel.updatePhoneNumber("+14155551234")
        viewModel.createAccount()
        advanceUntilIdle()

        assertEquals("Please enter your first and last name", viewModel.uiState.value.legalNameError)
    }

    @Test
    fun `test createAccount succeeds and advances to USERNAME`() = runTest(testDispatcher) {
        coEvery { mockRepository.saveProfileName(any()) } returns Result.success(successResponse)

        viewModel.updateLegalName("John Doe")
        viewModel.updatePhoneNumber("+14155551234")
        viewModel.createAccount()
        advanceUntilIdle()

        assertEquals(OnboardingStep.USERNAME, viewModel.uiState.value.currentStep)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `test createAccount shows error when saveProfileName fails`() = runTest(testDispatcher) {
        coEvery { mockRepository.saveProfileName(any()) } returns Result.failure(Exception("Failed to save name"))

        viewModel.updateLegalName("John Doe")
        viewModel.updatePhoneNumber("+14155551234")
        viewModel.createAccount()
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.error)
    }

    // ============= STEP 4: USERNAME TESTS =============

    @Test
    fun `test saveUsername fails with empty username`() = runTest(testDispatcher) {
        viewModel.updateUsername("")
        viewModel.saveUsername()
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.usernameError)
    }

    @Test
    fun `test saveUsername fails with short username`() = runTest(testDispatcher) {
        viewModel.updateUsername("ab")
        viewModel.saveUsername()
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.usernameError)
    }

    @Test
    fun `test saveUsername fails with invalid characters`() = runTest(testDispatcher) {
        viewModel.updateUsername("test user!")
        viewModel.saveUsername()
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.usernameError)
    }

    @Test
    fun `test saveUsername succeeds and advances to ACCEPT_TERMS`() = runTest(testDispatcher) {
        coEvery { mockRepository.saveUsername(any()) } returns Result.success(successResponse)

        viewModel.updateUsername("testuser123")
        viewModel.saveUsername()
        advanceUntilIdle()

        assertEquals(OnboardingStep.ACCEPT_TERMS, viewModel.uiState.value.currentStep)
    }

    @Test
    fun `test saveUsername shows error on API failure`() = runTest(testDispatcher) {
        coEvery { mockRepository.saveUsername(any()) } returns Result.failure(Exception("Username taken"))

        viewModel.updateUsername("testuser123")
        viewModel.saveUsername()
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.error)
    }

    // ============= STEP 5: ACCEPT TERMS TESTS =============

    @Test
    fun `test acceptTermsAndContinue fails when terms not accepted`() = runTest(testDispatcher) {
        viewModel.updateTermsAccepted(false)
        viewModel.acceptTermsAndContinue()
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.error)
    }

    @Test
    fun `test acceptTermsAndContinue succeeds and advances to WRISTBAND`() = runTest(testDispatcher) {
        coEvery { mockRepository.acceptTerms() } returns Result.success(successResponse)

        viewModel.updateTermsAccepted(true)
        viewModel.acceptTermsAndContinue()
        advanceUntilIdle()

        assertEquals(OnboardingStep.WRISTBAND, viewModel.uiState.value.currentStep)
    }

    @Test
    fun `test acceptTermsAndContinue shows error on API failure`() = runTest(testDispatcher) {
        coEvery { mockRepository.acceptTerms() } returns Result.failure(Exception("Server error"))

        viewModel.updateTermsAccepted(true)
        viewModel.acceptTermsAndContinue()
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.error)
        assertFalse(viewModel.uiState.value.isComplete)
    }

    // ============= STEP 6: WRISTBAND TESTS =============

    @Test
    fun `test saveWristband with empty code marks complete (skip)`() = runTest(testDispatcher) {
        viewModel.updateWristbandCode("")
        viewModel.saveWristband()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isComplete)
    }

    @Test
    fun `test saveWristband with code marks complete on success`() = runTest(testDispatcher) {
        coEvery { mockRepository.saveWristband(any()) } returns Result.success(successResponse)

        viewModel.updateWristbandCode("ABC123")
        viewModel.saveWristband()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isComplete)
    }

    @Test
    fun `test saveWristband shows error on failure`() = runTest(testDispatcher) {
        coEvery { mockRepository.saveWristband(any()) } returns Result.failure(Exception("Wristband not found"))

        viewModel.updateWristbandCode("INVALID")
        viewModel.saveWristband()
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.error)
        assertFalse(viewModel.uiState.value.isComplete)
    }

    @Test
    fun `test skipWristband marks complete`() {
        viewModel.skipWristband()
        assertTrue(viewModel.uiState.value.isComplete)
    }

    // ============= NAVIGATION TESTS =============

    @Test
    fun `test previousStep from EMERGENCY_CONTACT goes to PROFILE_DETAILS`() = runTest(testDispatcher) {
        coEvery { mockRepository.saveDemographics(any()) } returns Result.success(successResponse)

        viewModel.updateDateOfBirth("1990-01-01")
        viewModel.saveProfileDetails()
        advanceUntilIdle()

        viewModel.previousStep()
        assertEquals(OnboardingStep.PROFILE_DETAILS, viewModel.uiState.value.currentStep)
    }

    @Test
    fun `test previousStep from USERNAME goes to CONFIRM_DETAILS`() = runTest(testDispatcher) {
        coEvery { mockRepository.saveProfileName(any()) } returns Result.success(successResponse)

        viewModel.updateLegalName("John Doe")
        viewModel.updatePhoneNumber("+14155551234")
        viewModel.createAccount()
        advanceUntilIdle()
        assertEquals(OnboardingStep.USERNAME, viewModel.uiState.value.currentStep)

        viewModel.previousStep()
        assertEquals(OnboardingStep.CONFIRM_DETAILS, viewModel.uiState.value.currentStep)
    }

    // ============= END-TO-END FLOW TEST =============

    @Test
    fun `test full onboarding flow completes successfully`() = runTest(testDispatcher) {
        coEvery { mockRepository.saveDemographics(any()) } returns Result.success(successResponse)
        coEvery { mockRepository.saveEmergencyContact(any()) } returns Result.success(successResponse)
        coEvery { mockRepository.saveProfileName(any()) } returns Result.success(successResponse)
        coEvery { mockRepository.saveUsername(any()) } returns Result.success(successResponse)
        coEvery { mockRepository.acceptTerms() } returns Result.success(successResponse)

        // Step 1: Profile Details
        viewModel.updateDateOfBirth("1990-01-01")
        viewModel.updateGenderIdentity("Male")
        viewModel.saveProfileDetails()
        advanceUntilIdle()
        assertEquals(OnboardingStep.EMERGENCY_CONTACT, viewModel.uiState.value.currentStep)

        // Step 2: Emergency Contact
        viewModel.updateEmergencyName("Jane Doe")
        viewModel.updateEmergencyPhone("+14155551234")
        viewModel.updateEmergencyRelationship("Parent")
        viewModel.saveEmergencyContact()
        advanceUntilIdle()
        assertEquals(OnboardingStep.CONFIRM_DETAILS, viewModel.uiState.value.currentStep)

        // Step 3: Confirm Details
        viewModel.updateLegalName("John Doe")
        viewModel.updatePhoneNumber("+14155551234")
        viewModel.createAccount()
        advanceUntilIdle()
        assertEquals(OnboardingStep.USERNAME, viewModel.uiState.value.currentStep)

        // Step 4: Username
        viewModel.updateUsername("johndoe2024")
        viewModel.saveUsername()
        advanceUntilIdle()
        assertEquals(OnboardingStep.ACCEPT_TERMS, viewModel.uiState.value.currentStep)

        // Step 5: Accept Terms
        viewModel.updateTermsAccepted(true)
        viewModel.acceptTermsAndContinue()
        advanceUntilIdle()
        assertEquals(OnboardingStep.WRISTBAND, viewModel.uiState.value.currentStep)

        // Step 6: Wristband (skip)
        viewModel.skipWristband()
        assertTrue(viewModel.uiState.value.isComplete)
    }
}
