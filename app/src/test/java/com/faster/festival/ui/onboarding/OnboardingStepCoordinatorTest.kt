package com.faster.festival.ui.onboarding

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * JUnit 4 Test Suite for OnboardingStepCoordinator
 * Tests step ordering logic based on missing fields
 */
class OnboardingStepCoordinatorTest {

    // ============= STEP ORDERING TESTS =============

    @Test
    fun `test single field missing returns correct order`() {
        // Arrange
        val missing = listOf("username")

        // Act
        val steps = OnboardingStepCoordinator.buildOrderedSteps(missing)

        // Assert
        assertTrue(steps.contains(OnboardingStep.USERNAME))
        assertEquals(OnboardingStep.TERMS_ACCEPTANCE, steps.last())
    }

    @Test
    fun `test all 7 fields missing returns correct order`() {
        // Arrange
        val missing = listOf(
            "username",
            "date_of_birth",
            "race_ethnicity",
            "gender_identity",
            "emergency_contact",
            "wristband",
            "terms_acceptance"
        )

        // Act
        val steps = OnboardingStepCoordinator.buildOrderedSteps(missing)

        // Assert
        val expectedOrder = listOf(
            OnboardingStep.USERNAME,
            OnboardingStep.DATE_OF_BIRTH,
            OnboardingStep.RACE_ETHNICITY,
            OnboardingStep.GENDER_IDENTITY,
            OnboardingStep.EMERGENCY_CONTACT,
            OnboardingStep.WRISTBAND,
            OnboardingStep.TERMS_ACCEPTANCE
        )
        assertEquals(expectedOrder, steps)
    }

    @Test
    fun `test TERMS_ACCEPTANCE always last even if not in missing`() {
        // Arrange
        val missing = listOf("username", "date_of_birth")

        // Act
        val steps = OnboardingStepCoordinator.buildOrderedSteps(missing)

        // Assert
        assertEquals(OnboardingStep.TERMS_ACCEPTANCE, steps.last())
    }

    @Test
    fun `test empty missing list returns default steps with TERMS_ACCEPTANCE last`() {
        // Act
        val steps = OnboardingStepCoordinator.buildOrderedSteps(emptyList())

        // Assert
        assertTrue(steps.isNotEmpty())
        assertEquals(OnboardingStep.TERMS_ACCEPTANCE, steps.last())
    }

    @Test
    fun `test null missing list returns default steps`() {
        // Act
        val steps = OnboardingStepCoordinator.buildOrderedSteps(null)

        // Assert
        assertTrue(steps.isNotEmpty())
        assertEquals(OnboardingStep.TERMS_ACCEPTANCE, steps.last())
    }

    // ============= INDEX LOOKUP TESTS =============

    @Test
    fun `test getStepIndex returns correct indices`() {
        // Arrange
        val missing = listOf("username", "date_of_birth", "wristband", "terms_acceptance")
        val steps = OnboardingStepCoordinator.buildOrderedSteps(missing)

        // Act
        val usernameIndex = OnboardingStepCoordinator.getStepIndex(steps, OnboardingStep.USERNAME)
        val wristbandIndex = OnboardingStepCoordinator.getStepIndex(steps, OnboardingStep.WRISTBAND)

        // Assert
        assertEquals(0, usernameIndex)
        assertTrue(wristbandIndex > usernameIndex)
    }

    @Test
    fun `test getStepAtIndex returns correct steps`() {
        // Arrange
        val missing = listOf("username", "date_of_birth", "wristband")
        val steps = OnboardingStepCoordinator.buildOrderedSteps(missing)

        // Act
        val firstStep = OnboardingStepCoordinator.getStepAtIndex(steps, 0)
        val lastStep = OnboardingStepCoordinator.getStepAtIndex(steps, steps.size - 1)

        // Assert
        assertEquals(OnboardingStep.USERNAME, firstStep)
        assertEquals(OnboardingStep.TERMS_ACCEPTANCE, lastStep)
    }

    @Test
    fun `test getStepAtIndex returns null for out of bounds`() {
        // Arrange
        val missing = listOf("username")
        val steps = OnboardingStepCoordinator.buildOrderedSteps(missing)

        // Act
        val outOfBounds = OnboardingStepCoordinator.getStepAtIndex(steps, 999)

        // Assert
        assertNull(outOfBounds)
    }

    // ============= DUPLICATE HANDLING TESTS =============

    @Test
    fun `test duplicate missing fields handled correctly`() {
        // Arrange
        val missing = listOf("username", "username", "date_of_birth", "date_of_birth")

        // Act
        val steps = OnboardingStepCoordinator.buildOrderedSteps(missing)

        // Assert
        val usernameCount = steps.filter { it == OnboardingStep.USERNAME }.size
        val dobCount = steps.filter { it == OnboardingStep.DATE_OF_BIRTH }.size
        assertEquals(1, usernameCount)
        assertEquals(1, dobCount)
    }
}

/**
 * Unit test helper for OnboardingStepCoordinator (legacy)
 * Tests step ordering logic based on missing fields
 */
object OnboardingStepCoordinatorTestHelper {

    /**
     * Test: Single field missing returns correct order
     */
    fun testSingleFieldMissing(): Result<String> {
        return try {
            val missing = listOf("username")
            val steps = OnboardingStepCoordinator.buildOrderedSteps(missing)

            if (steps.contains(OnboardingStep.USERNAME) &&
                steps.last() == OnboardingStep.TERMS_ACCEPTANCE) {
                Result.success("✓ Single field missing: correct order")
            } else {
                Result.failure(Exception("✗ Unexpected step order: $steps"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("✗ Error: ${e.message}"))
        }
    }

    /**
     * Test: Multiple fields missing returns correct order
     */
    fun testMultipleFieldsMissing(): Result<String> {
        return try {
            val missing = listOf(
                "username",
                "date_of_birth",
                "race_ethnicity",
                "gender_identity",
                "emergency_contact",
                "wristband",
                "terms_acceptance"
            )
            val steps = OnboardingStepCoordinator.buildOrderedSteps(missing)

            // Check order
            val expectedOrder = listOf(
                OnboardingStep.USERNAME,
                OnboardingStep.DATE_OF_BIRTH,
                OnboardingStep.RACE_ETHNICITY,
                OnboardingStep.GENDER_IDENTITY,
                OnboardingStep.EMERGENCY_CONTACT,
                OnboardingStep.WRISTBAND,
                OnboardingStep.TERMS_ACCEPTANCE
            )

            if (steps == expectedOrder) {
                Result.success("✓ All 7 steps in correct order")
            } else {
                Result.failure(Exception("✗ Expected $expectedOrder, got $steps"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("✗ Error: ${e.message}"))
        }
    }

    /**
     * Test: TERMS_ACCEPTANCE always last even if not in missing
     */
    fun testTermsAlwaysLast(): Result<String> {
        return try {
            val missing = listOf("username", "date_of_birth")
            val steps = OnboardingStepCoordinator.buildOrderedSteps(missing)

            if (steps.last() == OnboardingStep.TERMS_ACCEPTANCE) {
                Result.success("✓ TERMS_ACCEPTANCE always last")
            } else {
                Result.failure(Exception("✗ TERMS_ACCEPTANCE not last. Last: ${steps.last()}"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("✗ Error: ${e.message}"))
        }
    }

    /**
     * Test: Empty missing list returns default steps
     */
    fun testEmptyMissingList(): Result<String> {
        return try {
            val steps = OnboardingStepCoordinator.buildOrderedSteps(emptyList())

            if (steps.isNotEmpty() && steps.last() == OnboardingStep.TERMS_ACCEPTANCE) {
                Result.success("✓ Empty missing list returns default steps with TERMS_ACCEPTANCE last")
            } else {
                Result.failure(Exception("✗ Unexpected default steps: $steps"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("✗ Error: ${e.message}"))
        }
    }

    /**
     * Test: Null missing list returns default steps
     */
    fun testNullMissingList(): Result<String> {
        return try {
            val steps = OnboardingStepCoordinator.buildOrderedSteps(null)

            if (steps.isNotEmpty() && steps.last() == OnboardingStep.TERMS_ACCEPTANCE) {
                Result.success("✓ Null missing list returns default steps")
            } else {
                Result.failure(Exception("✗ Unexpected steps for null: $steps"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("✗ Error: ${e.message}"))
        }
    }

    /**
     * Test: Step index lookup works correctly
     */
    fun testGetStepIndex(): Result<String> {
        return try {
            val missing = listOf("username", "date_of_birth", "wristband", "terms_acceptance")
            val steps = OnboardingStepCoordinator.buildOrderedSteps(missing)

            val usernameIndex = OnboardingStepCoordinator.getStepIndex(steps, OnboardingStep.USERNAME)
            val wristbandIndex = OnboardingStepCoordinator.getStepIndex(steps, OnboardingStep.WRISTBAND)

            if (usernameIndex == 0 && wristbandIndex > usernameIndex) {
                Result.success("✓ Step index lookup works correctly")
            } else {
                Result.failure(Exception("✗ Incorrect indices: USERNAME=$usernameIndex, WRISTBAND=$wristbandIndex"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("✗ Error: ${e.message}"))
        }
    }

    /**
     * Test: Get step at index works correctly
     */
    fun testGetStepAtIndex(): Result<String> {
        return try {
            val missing = listOf("username", "date_of_birth", "wristband")
            val steps = OnboardingStepCoordinator.buildOrderedSteps(missing)

            val firstStep = OnboardingStepCoordinator.getStepAtIndex(steps, 0)
            val lastStep = OnboardingStepCoordinator.getStepAtIndex(steps, steps.size - 1)

            if (firstStep == OnboardingStep.USERNAME && lastStep == OnboardingStep.TERMS_ACCEPTANCE) {
                Result.success("✓ Get step at index works correctly")
            } else {
                Result.failure(Exception("✗ Unexpected steps at indices. First=$firstStep, Last=$lastStep"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("✗ Error: ${e.message}"))
        }
    }

    /**
     * Test: Out of bounds index returns null
     */
    fun testOutOfBoundsIndex(): Result<String> {
        return try {
            val missing = listOf("username")
            val steps = OnboardingStepCoordinator.buildOrderedSteps(missing)

            val outOfBounds = OnboardingStepCoordinator.getStepAtIndex(steps, 999)

            if (outOfBounds == null) {
                Result.success("✓ Out of bounds index returns null")
            } else {
                Result.failure(Exception("✗ Should return null for out of bounds, got: $outOfBounds"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("✗ Error: ${e.message}"))
        }
    }

    /**
     * Test: Duplicate missing fields handled correctly
     */
    fun testDuplicateMissingFields(): Result<String> {
        return try {
            val missing = listOf("username", "username", "date_of_birth", "date_of_birth")
            val steps = OnboardingStepCoordinator.buildOrderedSteps(missing)

            val usernameCount = steps.filter { it == OnboardingStep.USERNAME }.size
            val dobCount = steps.filter { it == OnboardingStep.DATE_OF_BIRTH }.size

            if (usernameCount == 1 && dobCount == 1) {
                Result.success("✓ Duplicate fields handled correctly (no duplicates in output)")
            } else {
                Result.failure(Exception("✗ Duplicates found. USERNAME count=$usernameCount, DOB count=$dobCount"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("✗ Error: ${e.message}"))
        }
    }

    /**
     * Run all tests and return results
     */
    fun runAllTests(): List<Result<String>> {
        return listOf(
            testSingleFieldMissing(),
            testMultipleFieldsMissing(),
            testTermsAlwaysLast(),
            testEmptyMissingList(),
            testNullMissingList(),
            testGetStepIndex(),
            testGetStepAtIndex(),
            testOutOfBoundsIndex(),
            testDuplicateMissingFields()
        )
    }

    /**
     * Print all test results
     */
    fun printTestResults() {
        println("=== ONBOARDING STEP COORDINATOR TESTS ===\n")
        val results = runAllTests()
        var passed = 0
        var failed = 0

        results.forEach { result ->
            when {
                result.isSuccess -> {
                    println("${result.getOrNull()}")
                    passed++
                }
                else -> {
                    println("${result.exceptionOrNull()?.message}")
                    failed++
                }
            }
        }

        println("\n=== SUMMARY ===")
        println("✅ Passed: $passed")
        println("❌ Failed: $failed")
        println("📊 Total: ${results.size}")
    }
}

/**
 * JUnit 4 Test Suite for OnboardingFormState validation
 */
class OnboardingFormStateTest {

    // ============= INITIAL STATE TESTS =============

    @Test
    fun `test initial state has empty fields`() {
        // Act
        val state = OnboardingFormState()

        // Assert
        assertTrue(state.dateOfBirth.isEmpty())
        assertTrue(state.selectedRaceEthnicity.isEmpty())
        assertTrue(state.selectedGenderIdentity.isEmpty())
        assertTrue(state.emergencyContactName.isEmpty())
        assertTrue(state.username.isEmpty())
        assertTrue(!state.termsAccepted)
    }

    // ============= FIELD UPDATE TESTS =============

    @Test
    fun `test can update username`() {
        // Arrange
        val state = OnboardingFormState()

        // Act
        val newState = state.copy(username = "testuser")

        // Assert
        assertEquals("testuser", newState.username)
    }

    @Test
    fun `test can update date of birth`() {
        // Arrange
        val state = OnboardingFormState()

        // Act
        val newState = state.copy(dateOfBirth = "1990-01-01")

        // Assert
        assertEquals("1990-01-01", newState.dateOfBirth)
    }

    @Test
    fun `test can update race ethnicity selections`() {
        // Arrange
        val state = OnboardingFormState()
        val selections = listOf("Black", "Asian")

        // Act
        val newState = state.copy(selectedRaceEthnicity = selections)

        // Assert
        assertEquals(selections, newState.selectedRaceEthnicity)
    }

    @Test
    fun `test can update gender identity`() {
        // Arrange
        val state = OnboardingFormState()

        // Act
        val newState = state.copy(selectedGenderIdentity = "Non-binary")

        // Assert
        assertEquals("Non-binary", newState.selectedGenderIdentity)
    }

    @Test
    fun `test can update emergency contact`() {
        // Arrange
        val state = OnboardingFormState()

        // Act
        val newState = state.copy(
            emergencyContactName = "John Doe",
            emergencyContactPhone = "+1234567890",
            emergencyContactRelationship = "Parent"
        )

        // Assert
        assertEquals("John Doe", newState.emergencyContactName)
        assertEquals("+1234567890", newState.emergencyContactPhone)
        assertEquals("Parent", newState.emergencyContactRelationship)
    }

    @Test
    fun `test can update wristband code`() {
        // Arrange
        val state = OnboardingFormState()

        // Act
        val newState = state.copy(wristbandCode = "ABC123XYZ")

        // Assert
        assertEquals("ABC123XYZ", newState.wristbandCode)
    }

    @Test
    fun `test can accept terms`() {
        // Arrange
        val state = OnboardingFormState()

        // Act
        val newState = state.copy(termsAccepted = true)

        // Assert
        assertTrue(newState.termsAccepted)
    }

    @Test
    fun `test can update ordered steps`() {
        // Arrange
        val state = OnboardingFormState()
        val steps = listOf(OnboardingStep.USERNAME, OnboardingStep.DATE_OF_BIRTH)

        // Act
        val newState = state.copy(orderedSteps = steps)

        // Assert
        assertEquals(steps, newState.orderedSteps)
    }

    @Test
    fun `test can update current step index`() {
        // Arrange
        val state = OnboardingFormState()

        // Act
        val newState = state.copy(currentStepIndex = 3)

        // Assert
        assertEquals(3, newState.currentStepIndex)
    }
}

/**
 * Unit test helper for OnboardingFormState validation (legacy)
 */
object OnboardingFormStateTestHelper {

    /**
     * Test: Initial state has empty fields
     */
    fun testInitialState(): Result<String> {
        return try {
            val state = OnboardingFormState()

            if (state.dateOfBirth.isEmpty() &&
                state.selectedRaceEthnicity.isEmpty() &&
                state.selectedGenderIdentity.isEmpty() &&
                state.emergencyContactName.isEmpty() &&
                state.username.isEmpty() &&
                !state.termsAccepted) {
                Result.success("✓ Initial state has empty fields")
            } else {
                Result.failure(Exception("✗ Initial state has unexpected values"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("✗ Error: ${e.message}"))
        }
    }

    /**
     * Test: Can update username
     */
    fun testUpdateUsername(): Result<String> {
        return try {
            val state = OnboardingFormState()
            val newState = state.copy(username = "testuser")

            if (newState.username == "testuser") {
                Result.success("✓ Username can be updated")
            } else {
                Result.failure(Exception("✗ Username update failed"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("✗ Error: ${e.message}"))
        }
    }

    /**
     * Test: Can update date of birth
     */
    fun testUpdateDateOfBirth(): Result<String> {
        return try {
            val state = OnboardingFormState()
            val newState = state.copy(dateOfBirth = "1990-01-01")

            if (newState.dateOfBirth == "1990-01-01") {
                Result.success("✓ Date of birth can be updated")
            } else {
                Result.failure(Exception("✗ Date of birth update failed"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("✗ Error: ${e.message}"))
        }
    }

    /**
     * Test: Can update race/ethnicity selections
     */
    fun testUpdateRaceEthnicity(): Result<String> {
        return try {
            val state = OnboardingFormState()
            val selections = listOf("Black", "Asian")
            val newState = state.copy(selectedRaceEthnicity = selections)

            if (newState.selectedRaceEthnicity == selections) {
                Result.success("✓ Race/ethnicity selections can be updated")
            } else {
                Result.failure(Exception("✗ Race/ethnicity update failed"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("✗ Error: ${e.message}"))
        }
    }

    /**
     * Test: Can update gender identity
     */
    fun testUpdateGenderIdentity(): Result<String> {
        return try {
            val state = OnboardingFormState()
            val newState = state.copy(selectedGenderIdentity = "Non-binary")

            if (newState.selectedGenderIdentity == "Non-binary") {
                Result.success("✓ Gender identity can be updated")
            } else {
                Result.failure(Exception("✗ Gender identity update failed"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("✗ Error: ${e.message}"))
        }
    }

    /**
     * Test: Can update emergency contact
     */
    fun testUpdateEmergencyContact(): Result<String> {
        return try {
            val state = OnboardingFormState()
            val newState = state.copy(
                emergencyContactName = "John Doe",
                emergencyContactPhone = "+1234567890",
                emergencyContactRelationship = "Parent"
            )

            if (newState.emergencyContactName == "John Doe" &&
                newState.emergencyContactPhone == "+1234567890" &&
                newState.emergencyContactRelationship == "Parent") {
                Result.success("✓ Emergency contact can be updated")
            } else {
                Result.failure(Exception("✗ Emergency contact update failed"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("✗ Error: ${e.message}"))
        }
    }

    /**
     * Test: Can update wristband code
     */
    fun testUpdateWristbandCode(): Result<String> {
        return try {
            val state = OnboardingFormState()
            val newState = state.copy(wristbandCode = "ABC123XYZ")

            if (newState.wristbandCode == "ABC123XYZ") {
                Result.success("✓ Wristband code can be updated")
            } else {
                Result.failure(Exception("✗ Wristband code update failed"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("✗ Error: ${e.message}"))
        }
    }

    /**
     * Test: Can accept terms
     */
    fun testAcceptTerms(): Result<String> {
        return try {
            val state = OnboardingFormState()
            val newState = state.copy(termsAccepted = true)

            if (newState.termsAccepted) {
                Result.success("✓ Terms can be accepted")
            } else {
                Result.failure(Exception("✗ Terms acceptance update failed"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("✗ Error: ${e.message}"))
        }
    }

    /**
     * Test: Can update ordered steps
     */
    fun testUpdateOrderedSteps(): Result<String> {
        return try {
            val state = OnboardingFormState()
            val steps = listOf(OnboardingStep.USERNAME, OnboardingStep.DATE_OF_BIRTH)
            val newState = state.copy(orderedSteps = steps)

            if (newState.orderedSteps == steps) {
                Result.success("✓ Ordered steps can be updated")
            } else {
                Result.failure(Exception("✗ Ordered steps update failed"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("✗ Error: ${e.message}"))
        }
    }

    /**
     * Test: Can update current step index
     */
    fun testUpdateCurrentStepIndex(): Result<String> {
        return try {
            val state = OnboardingFormState()
            val newState = state.copy(currentStepIndex = 3)

            if (newState.currentStepIndex == 3) {
                Result.success("✓ Current step index can be updated")
            } else {
                Result.failure(Exception("✗ Current step index update failed"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("✗ Error: ${e.message}"))
        }
    }

    /**
     * Run all tests and return results
     */
    fun runAllTests(): List<Result<String>> {
        return listOf(
            testInitialState(),
            testUpdateUsername(),
            testUpdateDateOfBirth(),
            testUpdateRaceEthnicity(),
            testUpdateGenderIdentity(),
            testUpdateEmergencyContact(),
            testUpdateWristbandCode(),
            testAcceptTerms(),
            testUpdateOrderedSteps(),
            testUpdateCurrentStepIndex()
        )
    }
}
