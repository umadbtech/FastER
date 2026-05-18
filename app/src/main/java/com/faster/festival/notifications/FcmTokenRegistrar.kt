package com.faster.festival.notifications

import android.annotation.SuppressLint
import android.content.Context
import android.provider.Settings
import android.util.Log
import com.faster.festival.BuildConfig
import com.faster.festival.data.remote.NotificationApi
import com.faster.festival.data.remote.RegisterDeviceRequest
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Locale
import java.util.TimeZone

class FcmTokenRegistrar(
    private val notificationApi: NotificationApi,
    private val context: Context
) {

    companion object {
        private const val TAG = "FcmTokenRegistrar"
    }

    private val deviceId: String by lazy {
        @SuppressLint("HardwareIds")
        val id = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
        id ?: "unknown-device"
    }

    private val appVersion: String = try {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.0.0"
    } catch (e: Exception) {
        "1.0.0"
    }

    /**
     * Fetches the current FCM token and registers it with the backend.
     * Sends full device metadata per `notification_api.txt` contract.
     */
    suspend fun registerCurrentToken(): Result<String> = withContext(Dispatchers.IO) {
        try {
            val token = FirebaseMessaging.getInstance().token.await()
            registerToken(token).also {
                if (it.isSuccess) Log.d(TAG, "FCM token registered: $token")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch FCM token", e)
            Result.failure(e)
        }
    }

    /**
     * Register a specific token (used from FasterMessagingService.onNewToken).
     */
    suspend fun registerToken(token: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val request = RegisterDeviceRequest(
                platform = "android",
                provider = "fcm",
                pushToken = token,
                deviceId = deviceId,
                appVersion = appVersion,
                locale = Locale.getDefault().toLanguageTag(),
                timezone = TimeZone.getDefault().id,
                enabled = true
            )
            val response = notificationApi.registerDevice(request)

            if (response.isSuccessful && response.body()?.ok == true) {
                Log.d(TAG, "Device registered: ${response.body()?.device?.id}")
                Result.success(token)
            } else {
                val errorMsg = "Registration failed: HTTP ${response.code()} — ${response.body()?.error ?: response.errorBody()?.string()}"
                Log.e(TAG, errorMsg)
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register FCM token", e)
            Result.failure(e)
        }
    }

    /**
     * Returns the cached FCM token if available (used by settings repository
     * to include push_token in get/save settings calls).
     */
    suspend fun getCurrentToken(): String? = try {
        FirebaseMessaging.getInstance().token.await()
    } catch (e: Exception) {
        Log.w(TAG, "Could not fetch FCM token: ${e.message}")
        null
    }
}
