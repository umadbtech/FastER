package com.faster.festival.di

import com.faster.festival.BuildConfig
import com.faster.festival.data.remote.AppExperienceBundleApi
import com.faster.festival.data.remote.AppHomeApi
import com.faster.festival.data.remote.AuthApiService
import com.faster.festival.data.remote.AuthorizationInterceptor
import com.faster.festival.data.remote.TokenRefreshInterceptor
import com.faster.festival.data.remote.ContentArtistDetailApi
import com.faster.festival.data.remote.ContentHomeApi
import com.faster.festival.data.remote.ContentLineupApi
import com.faster.festival.data.remote.ContentMapApi
import com.faster.festival.data.remote.ContentStageScheduleApi
import com.faster.festival.data.remote.FestivalApiService
import com.faster.festival.data.remote.FestivalExperienceApi
import com.faster.festival.data.remote.FestivalHeaderApi
import com.faster.festival.data.remote.OfflineBundleApi
import com.faster.festival.data.remote.OnboardingApiService
import com.faster.festival.data.remote.ProfileApiService
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

    // ✅ FIX: Inject SessionManager to get real access token
    private var sessionManager: EncryptedSessionManager? = null

    fun setSessionManager(manager: EncryptedSessionManager) {
        sessionManager = manager
    }

    private val authInterceptor =
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
     * Authorization interceptor for adding Bearer token when available
     * ✅ FIXED: Now fetches real access token from SessionManager
     */
    fun createAuthorizationInterceptor(): AuthorizationInterceptor {
        return AuthorizationInterceptor {
            // ✅ FIX: Get access token from SessionManager if available
            sessionManager?.getAccessToken()
        }
    }

    /**
     * Token refresh interceptor for automatically refreshing expired tokens
     * Detects 401 / JWT expired responses and refreshes the token before retrying
     */
    fun createTokenRefreshInterceptor(): TokenRefreshInterceptor? {
        return if (sessionManager != null) {
            TokenRefreshInterceptor(sessionManager!!, authApiService)
        } else {
            null
        }
    }

    private val client =
            OkHttpClient.Builder()
                    .addInterceptor(loggingInterceptor)
                    .addInterceptor(authInterceptor)
                    .addInterceptor(createAuthorizationInterceptor())
                    .also { builder ->
                        // Add token refresh interceptor if session manager is available
                        createTokenRefreshInterceptor()?.let {
                            builder.addInterceptor(it)
                        }
                    }
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)
                    .build()

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

    // ========== Content API Services ==========

    val festivalHeaderApi: FestivalHeaderApi by lazy { retrofit.create(FestivalHeaderApi::class.java) }

    val contentHomeApi: ContentHomeApi by lazy { retrofit.create(ContentHomeApi::class.java) }

    val contentLineupApi: ContentLineupApi by lazy { retrofit.create(ContentLineupApi::class.java) }

    val contentArtistDetailApi: ContentArtistDetailApi by lazy { retrofit.create(ContentArtistDetailApi::class.java) }

    val contentStageScheduleApi: ContentStageScheduleApi by lazy { retrofit.create(ContentStageScheduleApi::class.java) }

    val contentMapApi: ContentMapApi by lazy { retrofit.create(ContentMapApi::class.java) }

    // ========== Experience API Services ==========

    val festivalExperienceApi: FestivalExperienceApi by lazy { retrofit.create(FestivalExperienceApi::class.java) }

    val appExperienceBundleApi: AppExperienceBundleApi by lazy { retrofit.create(AppExperienceBundleApi::class.java) }

    // ========== Offline Bundle API Service ==========

    val offlineBundleApi: OfflineBundleApi by lazy { retrofit.create(OfflineBundleApi::class.java) }

    // ========== Note: ContentRepository is created directly in repositories when needed ==========
    // No need to expose it here as it's only used internally

    // Supabase realtime client support removed. If you need to re-enable, re-add the dependency and
    // restore the client creation logic here.
}
