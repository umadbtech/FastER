package com.faster.festival.data.local

// Test-time fake that mirrors only the surface used by the ViewModel.
class TestEncryptedSessionManager {
    private var userId: String? = null
    private var emailConfirmed: Boolean = false
    private var userEmail: String? = null
    private var onboardingJustCompleted: Boolean = false

    fun saveUserID(id: String) {
        userId = id
    }

    fun getUserID(): String? = userId

    fun setEmailConfirmed(confirmed: Boolean) {
        emailConfirmed = confirmed
    }

    fun isEmailConfirmed(): Boolean = emailConfirmed

    fun setOnboardingJustCompleted(completed: Boolean) {
        onboardingJustCompleted = completed
    }

    fun isOnboardingJustCompleted(): Boolean = onboardingJustCompleted

    fun getUserEmail(): String? = userEmail

    fun setUserEmail(email: String?) {
        userEmail = email
    }
}
