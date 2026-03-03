package com.faster.festival.ui.navigation

object OnboardingRouter {
    /**
     * Determines the initial screen index for the Onboarding Pager based on the missing
     * requirements array returned by the server. Pages mapping (0..6): 0: Username 1: Date of Birth
     * 2: Race & Ethnicity 3: Gender Identity 4: Wristband 5: Primary Emergency Contact 6: Terms
     * Acceptance
     */
    fun determineStartScreen(missing: List<String>?): Int {
        if (missing.isNullOrEmpty())
                return 1 // Default to DOB/Demographics if empty but forced to onboarding

        // Priority rule: map the first missing value to its screen
        // using the specified mapping table
        return when (missing.first()) {
            "username" -> 0
            "date_of_birth" -> 1
            "race_ethnicity" -> 2
            "gender_identity" -> 3
            "wristband" -> 4
            "emergency_contact" -> 5
            "terms" -> 6
            else -> 1 // Default if unknown value
        }
    }
}
