package com.faster.festival.ui.onboarding

/**
 * Enum defining all possible onboarding steps.
 */
enum class OnboardingStep {
    USERNAME,
    DATE_OF_BIRTH,
    RACE_ETHNICITY,
    GENDER_IDENTITY,
    EMERGENCY_CONTACT,
    WRISTBAND,
    TERMS_ACCEPTANCE
}

/**
 * Coordinator that determines the ordered list of onboarding steps
 * based on the `missing` fields returned by the backend.
 *
 * The ordering is fixed:
 * 1. USERNAME (if in missing)
 * 2. DATE_OF_BIRTH (always present, typically first step)
 * 3. RACE_ETHNICITY (if in missing or as default step)
 * 4. GENDER_IDENTITY (if in missing or as default step)
 * 5. EMERGENCY_CONTACT (if in missing or as default step)
 * 6. WRISTBAND (always present, final step)
 * 7. TERMS_ACCEPTANCE (if in missing)
 */
object OnboardingStepCoordinator {

    /**
     * Build an ordered list of OnboardingStep based on the missing fields.
     *
     * ⭐ IMPORTANT: TERMS_ACCEPTANCE is ALWAYS included as the final step,
     * regardless of the missing fields list, to ensure users accept terms
     * before completing onboarding.
     *
     * @param missing List of missing field names from the backend response
     * @return Ordered list of OnboardingStep to present to the user
     */
    fun buildOrderedSteps(missing: List<String>?): List<OnboardingStep> {
        if (missing.isNullOrEmpty()) {
            // Default flow if no missing fields (shouldn't happen in onboarding)
            return defaultSteps()
        }

        val steps = mutableListOf<OnboardingStep>()

        // Order matters: USERNAME first (if present)
        if (missing.contains("username")) {
            steps.add(OnboardingStep.USERNAME)
        }

        // DATE_OF_BIRTH - always included if in missing
        if (missing.contains("date_of_birth")) {
            steps.add(OnboardingStep.DATE_OF_BIRTH)
        }

        // RACE_ETHNICITY - if in missing
        if (missing.contains("race_ethnicity")) {
            steps.add(OnboardingStep.RACE_ETHNICITY)
        }

        // GENDER_IDENTITY - if in missing
        if (missing.contains("gender_identity")) {
            steps.add(OnboardingStep.GENDER_IDENTITY)
        }

        // EMERGENCY_CONTACT - if in missing
        if (missing.contains("emergency_contact")) {
            steps.add(OnboardingStep.EMERGENCY_CONTACT)
        }

        // WRISTBAND - if in missing
        if (missing.contains("wristband")) {
            steps.add(OnboardingStep.WRISTBAND)
        }

        // ⭐ TERMS_ACCEPTANCE - ALWAYS added as final step
        // (even if not in missing list, to ensure it's always shown)
        if (!steps.contains(OnboardingStep.TERMS_ACCEPTANCE)) {
            steps.add(OnboardingStep.TERMS_ACCEPTANCE)
        }

        // If no steps were added (shouldn't happen), return defaults
        if (steps.isEmpty()) {
            return defaultSteps()
        }

        return steps
    }

    /**
     * Default steps when no missing fields are provided.
     *
     * Note: TERMS_ACCEPTANCE is ALWAYS included as the final step to ensure
     * users accept terms and conditions before completing onboarding.
     * This maintains consistency with buildOrderedSteps() behavior.
     */
    private fun defaultSteps(): List<OnboardingStep> {
        return listOf(
            OnboardingStep.DATE_OF_BIRTH,
            OnboardingStep.RACE_ETHNICITY,
            OnboardingStep.GENDER_IDENTITY,
            OnboardingStep.EMERGENCY_CONTACT,
            OnboardingStep.WRISTBAND,
            OnboardingStep.TERMS_ACCEPTANCE  // ⭐ ALWAYS last
        )
    }

    /**
     * Find the index of a given step in the ordered list.
     */
    fun getStepIndex(steps: List<OnboardingStep>, step: OnboardingStep): Int {
        return steps.indexOf(step)
    }

    /**
     * Get the step at a given index.
     */
    fun getStepAtIndex(steps: List<OnboardingStep>, index: Int): OnboardingStep? {
        return if (index >= 0 && index < steps.size) steps[index] else null
    }
}
