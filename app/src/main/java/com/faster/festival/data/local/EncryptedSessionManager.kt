package com.faster.festival.data.local

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class EncryptedSessionManager(context: Context) {

    private val masterKey =
            MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()

    private val sharedPreferences =
            EncryptedSharedPreferences.create(
                    context,
                    "festival_auth_prefs",
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )

    fun saveAccessToken(token: String) {
        sharedPreferences.edit().putString(KEY_ACCESS_TOKEN, token).apply()
        // ✅ Track when token was saved for debugging expiry
        sharedPreferences.edit().putLong(KEY_ACCESS_TOKEN_TIMESTAMP, System.currentTimeMillis()).apply()
    }

    fun getAccessToken(): String? {
        return sharedPreferences.getString(KEY_ACCESS_TOKEN, null)
    }

    fun getAccessTokenTimestamp(): Long {
        return sharedPreferences.getLong(KEY_ACCESS_TOKEN_TIMESTAMP, 0L)
    }

    fun saveRefreshToken(token: String) {
        sharedPreferences.edit().putString(KEY_REFRESH_TOKEN, token).apply()
    }

    fun getRefreshToken(): String? {
        return sharedPreferences.getString(KEY_REFRESH_TOKEN, null)
    }

    fun saveUserEmail(email: String) {
        sharedPreferences.edit().putString(KEY_USER_EMAIL, email).apply()
    }

    fun getUserEmail(): String? {
        return sharedPreferences.getString(KEY_USER_EMAIL, null)
    }

    fun saveUserPhone(phone: String) {
        sharedPreferences.edit().putString(KEY_USER_PHONE, phone).apply()
    }

    fun getUserPhone(): String? {
        return sharedPreferences.getString(KEY_USER_PHONE, null)
    }

    fun saveUserID(userId: String) {
        sharedPreferences.edit().putString(KEY_USER_ID, userId).apply()
    }

    fun getUserID(): String? {
        return sharedPreferences.getString(KEY_USER_ID, null)
    }

    fun setEmailConfirmed(confirmed: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_IS_EMAIL_CONFIRMED, confirmed).apply()
    }

    fun isEmailConfirmed(): Boolean {
        return sharedPreferences.getBoolean(KEY_IS_EMAIL_CONFIRMED, false)
    }

    fun clearSession() {
        sharedPreferences.edit().clear().apply()
    }

    companion object {
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_ACCESS_TOKEN_TIMESTAMP = "access_token_timestamp"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_USER_PHONE = "user_phone"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_IS_2FA_ENABLED = "is_2fa_enabled"
        private const val KEY_IS_EMAIL_VERIFIED = "is_email_verified"
        private const val KEY_IS_EMAIL_CONFIRMED = "is_email_confirmed"
    }
}
