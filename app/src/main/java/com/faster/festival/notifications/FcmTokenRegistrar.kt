package com.faster.festival.notifications

import android.util.Log
import com.faster.festival.data.remote.NotificationDeviceApi
import com.faster.festival.data.remote.RegisterDeviceRequest
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class FcmTokenRegistrar(
    private val notificationDeviceApi: NotificationDeviceApi
) {

    companion object {
        private const val TAG = "FcmTokenRegistrar"
    }

    /**
     * Fetches the current FCM token and POSTs it to the Supabase Edge Function
     * `/functions/v1/register-notification-device`.
     *
     * The request is authenticated via the existing AuthorizationInterceptor
     * (Bearer token from EncryptedSessionManager) — no manual header needed.
     *
     * Call this:
     * - After successful login / signup
     * - When push notifications are enabled
     * - On FCM token refresh (from FasterMessagingService.onNewToken)
     */
    suspend fun registerCurrentToken(): Result<String> = withContext(Dispatchers.IO) {
        try {
            val token = FirebaseMessaging.getInstance().token.await()
            Log.d(TAG, "FCM token obtained: $token")

            val request = RegisterDeviceRequest(pushToken = token)
            val response = notificationDeviceApi.registerDevice(request)

            if (response.isSuccessful) {
                Log.d(TAG, "Device registered successfully: ${response.body()?.message}")
                Result.success(token)
            } else {
                val errorMsg = "Registration failed: HTTP ${response.code()} — ${response.errorBody()?.string()}"
                Log.e(TAG, errorMsg)
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register FCM token", e)
            Result.failure(e)
        }
    }

    /**
     * Register a specific token (used from onNewToken callback where
     * the token is already available).
     */
    suspend fun registerToken(token: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val request = RegisterDeviceRequest(pushToken = token)
            val response = notificationDeviceApi.registerDevice(request)

            if (response.isSuccessful) {
                Log.d(TAG, "Device registered successfully: ${response.body()?.message}")
                Result.success(token)
            } else {
                val errorMsg = "Registration failed: HTTP ${response.code()}"
                Log.e(TAG, errorMsg)
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register FCM token", e)
            Result.failure(e)
        }
    }
}
