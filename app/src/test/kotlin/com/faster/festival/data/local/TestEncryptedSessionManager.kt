package com.faster.festival.data.local

// Test-time fake that mirrors only the surface used by the ViewModel.
class EncryptedSessionManager {
    private var userId: String? = null
    private var emailConfirmed: Boolean = false

    fun saveUserID(id: String) {
        userId = id
    }

    fun getUserID(): String? = userId

    fun setEmailConfirmed(confirmed: Boolean) {
        emailConfirmed = confirmed
    }

    fun isEmailConfirmed(): Boolean = emailConfirmed
}
