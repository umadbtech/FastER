package com.faster.festival.ui.onboarding

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * JUnit 4 Test Suite for OnboardingStep enum.
 * Validates the 6-step onboarding flow order.
 */
class OnboardingStepCoordinatorTest {

    @Test
    fun `test OnboardingStep has exactly 6 values`() {
        assertEquals(6, OnboardingStep.entries.size)
    }

    @Test
    fun `test OnboardingStep order is correct`() {
        val steps = OnboardingStep.entries
        assertEquals(OnboardingStep.PROFILE_DETAILS, steps[0])
        assertEquals(OnboardingStep.EMERGENCY_CONTACT, steps[1])
        assertEquals(OnboardingStep.CONFIRM_DETAILS, steps[2])
        assertEquals(OnboardingStep.USERNAME, steps[3])
        assertEquals(OnboardingStep.ACCEPT_TERMS, steps[4])
        assertEquals(OnboardingStep.WRISTBAND, steps[5])
    }

    @Test
    fun `test PROFILE_DETAILS is first step`() {
        assertEquals(0, OnboardingStep.PROFILE_DETAILS.ordinal)
    }

    @Test
    fun `test WRISTBAND is last step`() {
        val steps = OnboardingStep.entries
        assertEquals(OnboardingStep.WRISTBAND, steps.last())
    }

    @Test
    fun `test all step names are present`() {
        val names = OnboardingStep.entries.map { it.name }
        assertTrue(names.contains("PROFILE_DETAILS"))
        assertTrue(names.contains("EMERGENCY_CONTACT"))
        assertTrue(names.contains("CONFIRM_DETAILS"))
        assertTrue(names.contains("USERNAME"))
        assertTrue(names.contains("ACCEPT_TERMS"))
        assertTrue(names.contains("WRISTBAND"))
    }
}
