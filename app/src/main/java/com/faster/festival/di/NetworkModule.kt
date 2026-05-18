package com.faster.festival.di

import android.util.Log
import com.faster.festival.BuildConfig
import com.faster.festival.data.remote.AppExperienceBundleApi
import com.faster.festival.data.remote.AppHomeApi
import com.faster.festival.data.remote.AuthApiService
import com.faster.festival.data.remote.AuthorizationInterceptor
import com.faster.festival.data.remote.TokenRefreshInterceptor
import com.faster.festival.data.remote.ContentArtistDetailApi
import com.faster.festival.data.remote.ContentFaqApi
import com.faster.festival.data.remote.ContentHomeApi
import com.faster.festival.data.remote.ContentLineupApi
import com.faster.festival.data.remote.ContentMapApi
import com.faster.festival.data.remote.ContentStageScheduleApi
import com.faster.festival.data.remote.FestivalApiService
import com.faster.festival.data.remote.FestivalExperienceApi
import com.faster.festival.data.remote.FestivalHeaderApi
import com.faster.festival.data.remote.OfflineBundleApi
import com.faster.festival.data.remote.FriendshipApiService
import com.faster.festival.data.remote.NotificationApi
import com.faster.festival.data.remote.OnboardingApiService
import com.faster.festival.data.remote.ProfileApiService
import com.faster.festival.data.remote.Project1ApiService
import com.faster.festival.data.repository.FriendshipRepository
import com.faster.festival.data.repository.ProfileRepository
import com.faster.festival.data.local.EncryptedSessionManager
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
// Supabase client and realtime support removed for OTP-based verification flow.
import java.util.concurrent.TimeUnit
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit

private const val TAG = "NetworkModule"

object NetworkModule {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    private val loggingInterceptor =
            HttpLoggingInterceptor().apply {
                level =
                        if (BuildConfig.DEBUG) {
                            HttpLoggingInterceptor.Level.BODY
                        } else {
                            HttpLoggingInterceptor.Level.NONE
                        }
            }

    // ✅ SOLUTION PART 1: Session Manager (source of truth for tokens)
    // This is set during app initialization in MainActivity
    private var sessionManager: EncryptedSessionManager? = null

    /**
     * Initialize the NetworkModule with SessionManager.
     * ✅ MUST be called from MainActivity before making any API calls.
     *
     * This ensures that TokenRefreshInterceptor can access the session.
     */
    fun initializeWithSessionManager(manager: EncryptedSessionManager) {
        sessionManager = manager
        Log.d(TAG, "✅ NetworkModule initialized with SessionManager")
    }

    private val apiKeyInterceptor =
            okhttp3.Interceptor { chain ->
                val original = chain.request()
                val requestBuilder =
                        original.newBuilder()
                                .header("apikey", BuildConfig.VITE_SUPABASE_ANON_KEY)
                                .header("Content-Type", "application/json")
                                .method(original.method, original.body)

                chain.proceed(requestBuilder.build())
            }

    /**
     * ✅ SOLUTION PART 2: Authorization Interceptor
     * Adds Bearer token to all requests (if available in SessionManager)
     */
    private fun createAuthorizationInterceptor(): AuthorizationInterceptor {
        return AuthorizationInterceptor {
            sessionManager?.getAccessToken()
        }
    }

    /**
     * ✅ SOLUTION PART 3: Token Refresh Interceptor
     * Detects 401 responses and automatically refreshes the token
     * Retries the request once with the new token
     */
    private fun createTokenRefreshInterceptor(): TokenRefreshInterceptor? {
        return if (sessionManager != null) {
            Log.d(TAG, "✅ Creating TokenRefreshInterceptor with SessionManager")
            // Use lambda to lazily get authApiService when actually needed
            TokenRefreshInterceptor(sessionManager!!) {
                authApiService
            }
        } else {
            Log.w(TAG, "⚠️ SessionManager not initialized, TokenRefreshInterceptor disabled")
            null
        }
    }

    // ✅ Build OkHttp client LAZILY so it's created AFTER initializeWithSessionManager()
    // This is critical: if built eagerly, sessionManager is still null and
    // TokenRefreshInterceptor won't be installed — causing all 401s to go unhandled.
    private val client by lazy {
        OkHttpClient.Builder()
                // Step 1: Log requests
                .addInterceptor(loggingInterceptor)
                // Step 2: Add API key to all requests
                .addInterceptor(apiKeyInterceptor)
                // Step 3: Add Bearer token if available
                .addInterceptor(createAuthorizationInterceptor())
                // Step 4: Detect 401 and refresh token (MUST be LAST to retry with new token)
                .also { builder ->
                    createTokenRefreshInterceptor()?.let {
                        builder.addInterceptor(it)
                        Log.d(TAG, "✅ TokenRefreshInterceptor installed")
                    } ?: Log.w(TAG, "⚠️ TokenRefreshInterceptor NOT installed (sessionManager is null)")
                }
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build()
    }

    private val retrofit by lazy {
        val baseUrl = BuildConfig.VITE_SUPABASE_URL
        if (baseUrl.isEmpty() || !baseUrl.startsWith("http")) {
            throw IllegalArgumentException(
                "Supabase URL is not configured. " +
                "Expected format: https://your-project.supabase.co"
            )
        }

        Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(client)
                .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
                .build()
    }

    // ...existing code...
    val onboardingApiService: OnboardingApiService by lazy { retrofit.create(OnboardingApiService::class.java) }

    val authApiService: AuthApiService by lazy { retrofit.create(AuthApiService::class.java) }

    val festivalApiService: FestivalApiService by lazy { retrofit.create(FestivalApiService::class.java) }

    val appHomeApi: AppHomeApi by lazy { retrofit.create(AppHomeApi::class.java) }


    val profileApiService: ProfileApiService by lazy { retrofit.create(ProfileApiService::class.java) }

    val profileRepository: ProfileRepository by lazy { ProfileRepository(profileApiService) }

    // ========== Notification Device API ==========

    val notificationApi: NotificationApi by lazy { retrofit.create(NotificationApi::class.java) }

    /**
     * Project 1 backend surface for wristband CRUD + telemetry batch + SOS
     * history (`Wristband-Backend-API.md` §5). Reuses the existing Project 1
     * Retrofit instance — no new OkHttp client, no new interceptor chain.
     */
    val project1ApiService: Project1ApiService by lazy {
        retrofit.create(Project1ApiService::class.java)
    }

    // ========== Content API Services ==========

    val festivalHeaderApi: FestivalHeaderApi by lazy { retrofit.create(FestivalHeaderApi::class.java) }

    val contentHomeApi: ContentHomeApi by lazy { retrofit.create(ContentHomeApi::class.java) }

    val contentLineupApi: ContentLineupApi by lazy { retrofit.create(ContentLineupApi::class.java) }

    val contentArtistDetailApi: ContentArtistDetailApi by lazy { retrofit.create(ContentArtistDetailApi::class.java) }

    val contentStageScheduleApi: ContentStageScheduleApi by lazy { retrofit.create(ContentStageScheduleApi::class.java) }

    val contentMapApi: ContentMapApi by lazy { retrofit.create(ContentMapApi::class.java) }

    val contentFaqApi: ContentFaqApi by lazy { retrofit.create(ContentFaqApi::class.java) }

    // ========== Experience API Services ==========

    val festivalExperienceApi: FestivalExperienceApi by lazy { retrofit.create(FestivalExperienceApi::class.java) }

    val appExperienceBundleApi: AppExperienceBundleApi by lazy { retrofit.create(AppExperienceBundleApi::class.java) }

    // ========== Offline Bundle API Service ==========

    val offlineBundleApi: OfflineBundleApi by lazy { retrofit.create(OfflineBundleApi::class.java) }

    // ========== Friendship API Services ==========

    val friendshipApiService: FriendshipApiService by lazy { retrofit.create(FriendshipApiService::class.java) }

    val friendshipRepository: FriendshipRepository by lazy {
        FriendshipRepository(friendshipApiService, sessionManager!!)
    }

    // ========== Note: ContentRepository is created directly in repositories when needed ==========
    // No need to expose it here as it's only used internally

    // Supabase realtime client support removed. If you need to re-enable, re-add the dependency and
    // restore the client creation logic here.
}
