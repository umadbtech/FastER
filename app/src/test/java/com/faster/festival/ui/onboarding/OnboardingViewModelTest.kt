package com.faster.festival.ui.onboarding

import com.faster.festival.data.model.OnboardingResponse
import com.faster.festival.data.repository.OnboardingRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
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

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var mockRepository: OnboardingRepository
    private lateinit var viewModel: OnboardingViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        mockRepository = mockk(relaxed = true)

        // Mock the ensureOnboarding to return success with all 7 steps
        coEvery { mockRepository.ensureOnboarding() } returns Result.success("297d5837-a7b6-49a4-873b-4e3b17b60657")

        viewModel = OnboardingViewModel(mockRepository)
    }

    // ============= STEP ORDER & SEQUENCE TESTS =============

    @Test
    fun `test total steps is 7 when all missing fields present`() = runTest {
        // Arrange
        val allMissing = listOf(
            "username",
            "date_of_birth",
            "race_ethnicity",
            "gender_identity",
            "emergency_contact",
            "wristband",
            "terms_acceptance"
        )

        // Act
        viewModel.setMissingFields(allMissing)

        // Assert
        assertEquals(7, viewModel.getTotalSteps())
        assertEquals(7, viewModel.formState.value.orderedSteps.size)
    }

    @Test
    fun `test step order is correct USERNAME first`() = runTest {
        // Arrange
        val allMissing = listOf(
            "username",
            "date_of_birth",
            "race_ethnicity",
            "gender_identity",
            "emergency_contact",
            "wristband",
            "terms_acceptance"
        )

        // Act
        viewModel.setMissingFields(allMissing)

        // Assert
        val steps = viewModel.formState.value.orderedSteps
        assertEquals(OnboardingStep.USERNAME, steps[0])
        assertEquals(OnboardingStep.DATE_OF_BIRTH, steps[1])
        assertEquals(OnboardingStep.RACE_ETHNICITY, steps[2])
        assertEquals(OnboardingStep.GENDER_IDENTITY, steps[3])
        assertEquals(OnboardingStep.EMERGENCY_CONTACT, steps[4])
        assertEquals(OnboardingStep.WRISTBAND, steps[5])
        assertEquals(OnboardingStep.TERMS_ACCEPTANCE, steps[6])
    }

    @Test
    fun `test TERMS_ACCEPTANCE is always last step`() = runTest {
        // Arrange - even with only 1 field, TERMS_ACCEPTANCE should be last
        val singleMissing = listOf("username")

        // Act
        viewModel.setMissingFields(singleMissing)

        // Assert
        val steps = viewModel.formState.value.orderedSteps
        assertEquals(OnboardingStep.TERMS_ACCEPTANCE, steps.last())
        assertTrue(steps.size >= 2) // At least username + terms
    }

    @Test
    fun `test TERMS_ACCEPTANCE added even if not in missing fields`() = runTest {
        // Arrange
        val missingWithoutTerms = listOf("username", "date_of_birth")

        // Act
        viewModel.setMissingFields(missingWithoutTerms)

        // Assert
        val steps = viewModel.formState.value.orderedSteps
        assertTrue(steps.contains(OnboardingStep.TERMS_ACCEPTANCE))
        assertEquals(OnboardingStep.TERMS_ACCEPTANCE, steps.last())
    }

    // ============= STEP NAVIGATION TESTS =============

    @Test
    fun `test proceedToNextStep increments current step index`() = runTest {
        // Arrange
        val allMissing = listOf(
            "username",
            "date_of_birth",
            "race_ethnicity",
            "gender_identity",
            "emergency_contact",
            "wristband",
            "terms_acceptance"
        )
        viewModel.setMissingFields(allMissing)
        assertEquals(0, viewModel.formState.value.currentStepIndex)

        // Act
        viewModel.proceedToNextStep()

        // Assert
        assertEquals(1, viewModel.formState.value.currentStepIndex)
    }

    @Test
    fun `test proceedToNextStep does not go beyond last step`() = runTest {
        // Arrange
        val allMissing = listOf(
            "username",
            "date_of_birth",
            "race_ethnicity",
            "gender_identity",
            "emergency_contact",
            "wristband",
            "terms_acceptance"
        )
        viewModel.setMissingFields(allMissing)
        val totalSteps = viewModel.getTotalSteps()

        // Act - try to go beyond last step
        repeat(totalSteps + 5) {
            viewModel.proceedToNextStep()
        }

        // Assert - should stop at last index
        assertEquals(totalSteps - 1, viewModel.formState.value.currentStepIndex)
    }

    @Test
    fun `test goBack decrements current step index`() = runTest {
        // Arrange
        val allMissing = listOf(
            "username",
            "date_of_birth",
            "race_ethnicity",
            "gender_identity",
            "emergency_contact",
            "wristband",
            "terms_acceptance"
        )
        viewModel.setMissingFields(allMissing)
        viewModel.proceedToNextStep()
        viewModel.proceedToNextStep() // Now at index 2
        assertEquals(2, viewModel.formState.value.currentStepIndex)

        // Act
        viewModel.goBack()

        // Assert
        assertEquals(1, viewModel.formState.value.currentStepIndex)
    }

    @Test
    fun `test goBack does not go before first step`() = runTest {
        // Arrange
        val allMissing = listOf("username", "terms_acceptance")
        viewModel.setMissingFields(allMissing)
        assertEquals(0, viewModel.formState.value.currentStepIndex)

        // Act - try to go back from step 0
        viewModel.goBack()

        // Assert
        assertEquals(0, viewModel.formState.value.currentStepIndex)
    }

    @Test
    fun `test getCurrentStep returns correct step at index`() = runTest {
        // Arrange
        val allMissing = listOf(
            "username",
            "date_of_birth",
            "race_ethnicity",
            "gender_identity",
            "emergency_contact",
            "wristband",
            "terms_acceptance"
        )
        viewModel.setMissingFields(allMissing)

        // Act & Assert at each step
        assertEquals(OnboardingStep.USERNAME, viewModel.getCurrentStep())
        viewModel.proceedToNextStep()
        assertEquals(OnboardingStep.DATE_OF_BIRTH, viewModel.getCurrentStep())
        viewModel.proceedToNextStep()
        assertEquals(OnboardingStep.RACE_ETHNICITY, viewModel.getCurrentStep())
    }

    // ============= TERMS_ACCEPTANCE CANNOT BE SKIPPED TESTS =============

    @Test
    fun `test cannot proceed from TERMS_ACCEPTANCE without accepting terms`() = runTest {
        // Arrange
        val onlyTerms = listOf("terms_acceptance")
        viewModel.setMissingFields(onlyTerms)
        // Note: terms are not accepted by default

        // Act
        viewModel.proceedFromCurrentStep()
        advanceUntilIdle()

        // Assert - should still be at terms step (not advanced)
        assertEquals(0, viewModel.formState.value.currentStepIndex)
    }

    @Test
    fun `test proceedFromTermsAcceptance with unaccepted terms returns error`() = runTest {
        // Arrange
        val onlyTerms = listOf("terms_acceptance")
        viewModel.setMissingFields(onlyTerms)

        // Mock save response - should fail or show error
        coEvery { mockRepository.saveDemographics(any()) } returns Result.failure(
            Exception("Terms must be accepted")
        )

        // Act
        viewModel.proceedFromCurrentStep()
        advanceUntilIdle()

        // Assert
        assertTrue(viewModel.uiState.value is OnboardingUiState.Error)
    }

    @Test
    fun `test TERMS_ACCEPTANCE is last step - no step after it`() = runTest {
        // Arrange
        val allMissing = listOf(
            "username",
            "date_of_birth",
            "race_ethnicity",
            "gender_identity",
            "emergency_contact",
            "wristband",
            "terms_acceptance"
        )
        viewModel.setMissingFields(allMissing)

        // Act - jump to last step
        val lastIndex = viewModel.getTotalSteps() - 1
        repeat(lastIndex) { viewModel.proceedToNextStep() }

        // Assert - we're at TERMS_ACCEPTANCE
        assertEquals(OnboardingStep.TERMS_ACCEPTANCE, viewModel.getCurrentStep())

        // Try to go forward - should not advance
        viewModel.proceedToNextStep()
        assertEquals(lastIndex, viewModel.formState.value.currentStepIndex)
    }

    // ============= COMPLETION TESTS =============

    @Test
    fun `test onboarding not complete without TERMS_ACCEPTANCE accepted`() = runTest {
        // Arrange
        val onlyTerms = listOf("terms_acceptance")
        viewModel.setMissingFields(onlyTerms)

        // Act
        val isComplete = viewModel.formState.value.termsAccepted

        // Assert
        assertFalse(isComplete)
        assertTrue(viewModel.uiState.value is OnboardingUiState.Idle)
    }

    @Test
    fun `test completion requires TERMS_ACCEPTANCE with true value`() = runTest {
        // Arrange
        val onlyTerms = listOf("terms_acceptance")
        viewModel.setMissingFields(onlyTerms)

        coEvery { mockRepository.acceptTerms() } returns Result.success(
            OnboardingResponse(
                saved = true,
                activated = true,
                status = "onboarding",
                missing = emptyList()
            )
        )

        // Act - set terms accepted and proceed
        viewModel.updateTermsAcceptance(true)
        viewModel.proceedFromCurrentStep()
        advanceUntilIdle()

        // Assert
        assertTrue(viewModel.formState.value.termsAccepted)
        assertEquals(OnboardingUiState.OnboardingComplete, viewModel.uiState.value)
    }

    @Test
    fun `test onboarding completes only when activated is true and no missing fields`() = runTest {
        // Arrange
        val allMissing = listOf(
            "username",
            "date_of_birth",
            "race_ethnicity",
            "gender_identity",
            "emergency_contact",
            "wristband",
            "terms_acceptance"
        )
        viewModel.setMissingFields(allMissing)

        // Mock successful saves for each step
        coEvery { mockRepository.saveUsername(any()) } returns Result.success(
            OnboardingResponse(saved = true, activated = false, status = "onboarding", missing = allMissing.drop(1))
        )
        coEvery { mockRepository.saveDemographics(any()) } returns Result.success(
            OnboardingResponse(saved = true, activated = false, status = "onboarding", missing = allMissing.drop(4))
        )
        coEvery { mockRepository.saveWristband(any()) } returns Result.success(
            OnboardingResponse(saved = true, activated = false, status = "onboarding", missing = listOf("terms_acceptance"))
        )
        coEvery { mockRepository.acceptTerms() } returns Result.success(
            OnboardingResponse(saved = true, activated = true, status = "onboarding", missing = emptyList())
        )

        // Act - fill all steps and proceed
        viewModel.updateUsername("testuser")
        viewModel.proceedFromCurrentStep()
        advanceUntilIdle()

        viewModel.updateDateOfBirth("1990-01-01")
        viewModel.proceedFromCurrentStep()
        advanceUntilIdle()

        viewModel.toggleRaceEthnicity("Asian")
        viewModel.proceedFromCurrentStep()
        advanceUntilIdle()

        viewModel.updateGenderIdentity("Male")
        viewModel.proceedFromCurrentStep()
        advanceUntilIdle()

        viewModel.updateEmergencyContactName("Parent")
        viewModel.updateEmergencyContactPhone("+14155551234")
        viewModel.updateEmergencyContactRelationship("Parent")
        viewModel.proceedFromCurrentStep()
        advanceUntilIdle()

        // Wristband is optional - skip by providing empty code
        viewModel.updateWristbandCode("")
        viewModel.proceedFromCurrentStep()
        advanceUntilIdle()

        // Accept terms
        viewModel.updateTermsAcceptance(true)
        viewModel.proceedFromCurrentStep()
        advanceUntilIdle()

        // Assert - onboarding complete when activated=true
        assertEquals(OnboardingUiState.OnboardingComplete, viewModel.uiState.value)
    }

    // ============= VALIDATION GATE TESTS =============

    @Test
    fun `test proceedFromUsername fails with empty username`() = runTest {
        // Arrange
        val onlyUsername = listOf("username")
        viewModel.setMissingFields(onlyUsername)
        viewModel.updateUsername("") // Empty

        // Act
        viewModel.proceedFromCurrentStep()
        advanceUntilIdle()

        // Assert
        assertNotNull(viewModel.formState.value.usernameError)
        assertEquals(0, viewModel.formState.value.currentStepIndex) // Not advanced
    }

    @Test
    fun `test proceedFromUsername fails with username too short`() = runTest {
        // Arrange
        val onlyUsername = listOf("username")
        viewModel.setMissingFields(onlyUsername)
        viewModel.updateUsername("ab") // Too short (min 3)

        // Act
        viewModel.proceedFromCurrentStep()
        advanceUntilIdle()

        // Assert
        assertNotNull(viewModel.formState.value.usernameError)
        assertTrue(viewModel.formState.value.usernameError!!.contains("3-30"))
    }

    @Test
    fun `test proceedFromUsername fails with username too long`() = runTest {
        // Arrange
        val onlyUsername = listOf("username")
        viewModel.setMissingFields(onlyUsername)
        viewModel.updateUsername("a".repeat(31)) // Too long (max 30)

        // Act
        viewModel.proceedFromCurrentStep()
        advanceUntilIdle()

        // Assert
        assertNotNull(viewModel.formState.value.usernameError)
        assertTrue(viewModel.formState.value.usernameError!!.contains("3-30"))
    }

    @Test
    fun `test proceedFromDOB fails with empty date`() = runTest {
        // Arrange
        val onlyDOB = listOf("date_of_birth")
        viewModel.setMissingFields(onlyDOB)
        viewModel.updateDateOfBirth("") // Empty

        // Act
        viewModel.proceedFromCurrentStep()

        // Assert
        assertNotNull(viewModel.formState.value.dobError)
        assertTrue(viewModel.formState.value.dobError!!.contains("required"))
    }

    @Test
    fun `test proceedFromDOB fails with future date`() = runTest {
        // Arrange
        val onlyDOB = listOf("date_of_birth")
        viewModel.setMissingFields(onlyDOB)
        viewModel.updateDateOfBirth("2099-01-01") // Future

        // Act
        viewModel.proceedFromCurrentStep()

        // Assert
        assertNotNull(viewModel.formState.value.dobError)
        assertTrue(viewModel.formState.value.dobError!!.contains("future"))
    }

    @Test
    fun `test proceedFromEmergencyContact fails with empty name`() = runTest {
        // Arrange
        val onlyEC = listOf("emergency_contact")
        viewModel.setMissingFields(onlyEC)
        viewModel.updateEmergencyContactName("") // Empty
        viewModel.updateEmergencyContactPhone("+14155551234")

        // Act
        viewModel.proceedFromCurrentStep()

        // Assert
        assertNotNull(viewModel.formState.value.emergencyContactError)
        assertTrue(viewModel.formState.value.emergencyContactError!!.contains("name"))
    }

    @Test
    fun `test proceedFromEmergencyContact fails with empty phone`() = runTest {
        // Arrange
        val onlyEC = listOf("emergency_contact")
        viewModel.setMissingFields(onlyEC)
        viewModel.updateEmergencyContactName("Parent")
        viewModel.updateEmergencyContactPhone("") // Empty

        // Act
        viewModel.proceedFromCurrentStep()

        // Assert
        assertNotNull(viewModel.formState.value.emergencyContactError)
        assertTrue(viewModel.formState.value.emergencyContactError!!.contains("phone"))
    }

    @Test
    fun `test proceedFromEmergencyContact fails with invalid phone format`() = runTest {
        // Arrange
        val onlyEC = listOf("emergency_contact")
        viewModel.setMissingFields(onlyEC)
        viewModel.updateEmergencyContactName("Parent")
        viewModel.updateEmergencyContactPhone("5551234") // Missing country code

        // Act
        viewModel.proceedFromCurrentStep()

        // Assert
        assertNotNull(viewModel.formState.value.emergencyContactError)
        assertTrue(viewModel.formState.value.emergencyContactError!!.contains("country code"))
    }

    @Test
    fun `test proceedFromWristband skips with empty code`() = runTest {
        // Arrange
        val onlyWristband = listOf("wristband")
        viewModel.setMissingFields(onlyWristband)
        viewModel.updateWristbandCode("") // Empty - wristband is OPTIONAL

        // Act
        viewModel.proceedFromCurrentStep()
        advanceUntilIdle()

        // Assert - should proceed without error since wristband is optional
        assertNull(viewModel.formState.value.wristbandError)
        assertEquals(1, viewModel.formState.value.currentStepIndex) // Proceeded to next step
    }

    // ============= STATE UPDATE TESTS =============

    @Test
    fun `test updateUsername updates form state`() = runTest {
        // Act
        viewModel.updateUsername("testuser")

        // Assert
        assertEquals("testuser", viewModel.formState.value.username)
    }

    @Test
    fun `test updateDateOfBirth updates form state`() = runTest {
        // Act
        viewModel.updateDateOfBirth("1990-01-01")

        // Assert
        assertEquals("1990-01-01", viewModel.formState.value.dateOfBirth)
    }

    @Test
    fun `test updateTermsAcceptance updates form state`() = runTest {
        // Arrange
        assertFalse(viewModel.formState.value.termsAccepted)

        // Act
        viewModel.updateTermsAcceptance(true)

        // Assert
        assertTrue(viewModel.formState.value.termsAccepted)
    }

    @Test
    fun `test updateEmergencyContact updates all fields`() = runTest {
        // Act
        viewModel.updateEmergencyContactName("Parent")
        viewModel.updateEmergencyContactPhone("+14155551234")
        viewModel.updateEmergencyContactRelationship("Mother")

        // Assert
        assertEquals("Parent", viewModel.formState.value.emergencyContactName)
        assertEquals("+14155551234", viewModel.formState.value.emergencyContactPhone)
        assertEquals("Mother", viewModel.formState.value.emergencyContactRelationship)
    }

    @Test
    fun `test updateWristbandCode updates form state`() = runTest {
        // Act
        viewModel.updateWristbandCode("ABC123XYZ")

        // Assert
        assertEquals("ABC123XYZ", viewModel.formState.value.wristbandCode)
    }

    // ============= STEP COORDINATOR INTEGRATION TESTS =============

    @Test
    fun `test setMissingFields respects step order from coordinator`() = runTest {
        // Arrange
        val customMissing = listOf("wristband", "username", "date_of_birth") // Out of order in input

        // Act
        viewModel.setMissingFields(customMissing)

        // Assert - coordinator should reorder them
        val steps = viewModel.formState.value.orderedSteps
        val usernameIdx = steps.indexOf(OnboardingStep.USERNAME)
        val dobIdx = steps.indexOf(OnboardingStep.DATE_OF_BIRTH)
        val wristbandIdx = steps.indexOf(OnboardingStep.WRISTBAND)

        assertTrue(usernameIdx < dobIdx) // USERNAME before DOB
        assertTrue(dobIdx < wristbandIdx) // DOB before WRISTBAND
        assertEquals(OnboardingStep.TERMS_ACCEPTANCE, steps.last()) // Always last
    }

    @Test
    fun `test subset of missing fields returns correct step count`() = runTest {
        // Arrange
        val partialMissing = listOf("username", "wristband")

        // Act
        viewModel.setMissingFields(partialMissing)

        // Assert
        val steps = viewModel.formState.value.orderedSteps
        assertEquals(3, steps.size) // username + wristband + terms_acceptance (always)
        assertTrue(steps.contains(OnboardingStep.USERNAME))
        assertTrue(steps.contains(OnboardingStep.WRISTBAND))
        assertTrue(steps.contains(OnboardingStep.TERMS_ACCEPTANCE))
        assertEquals(OnboardingStep.TERMS_ACCEPTANCE, steps.last())
    }

    @Test
    fun `test getCurrentStep returns null when index out of bounds`() = runTest {
        // Arrange
        val onlyUsername = listOf("username")
        viewModel.setMissingFields(onlyUsername)

        // Act - manually set index out of bounds (shouldn't happen in normal flow)
        val steps = viewModel.formState.value.orderedSteps
        val outOfBoundsIndex = steps.size + 10

        // Assert
        assertNull(OnboardingStepCoordinator.getStepAtIndex(steps, outOfBoundsIndex))
    }
}
