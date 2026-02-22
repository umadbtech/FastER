package com.faster.festival.data.local

// Minimal placeholder used by tests when a production EncryptedSessionManager (requires Context) isn't available.
open class EncryptedSessionManagerPlaceholderBase {
    private val backing = mutableMapOf<String, String>()
    fun saveAccessToken(token: String) { backing["access"] = token }
    fun getAccessToken(): String? = backing["access"]
    fun saveRefreshToken(token: String) { backing["refresh"] = token }
    fun getRefreshToken(): String? = backing["refresh"]
    fun saveUserEmail(email: String) { backing["email"] = email }
    fun getUserEmail(): String? = backing["email"]
    fun saveUserID(id: String) { backing["id"] = id }
    fun getUserID(): String? = backing["id"]
    fun setEmailConfirmed(confirmed: Boolean) { backing["confirmed"] = confirmed.toString() }
    fun isEmailConfirmed(): Boolean = backing["confirmed"] == "true"
}
