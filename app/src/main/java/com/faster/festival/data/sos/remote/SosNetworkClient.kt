package com.faster.festival.data.sos.remote

import com.faster.festival.BuildConfig
import com.faster.festival.data.local.EncryptedSessionManager
import com.faster.festival.data.remote.AuthApiService
import com.faster.festival.data.remote.TokenRefreshInterceptor
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

/**
 * Builds the two Retrofit clients the SOS flow needs.
 *
 * Per Pinch SOS Frontend Implementation Guide §"Frontend Features And Endpoints":
 *
 *   • Project 1 (`VITE_SUPABASE_URL` + `VITE_SUPABASE_ANON_KEY`) hosts:
 *       sos-register-device, sos-verify-attestation, account-profile.
 *     The app config row that lists known `app_id`/`app_version` lives here.
 *   • Project 2 (`PROJECT2_SOS_URL` + `PROJECT2_SOS_ANON_KEY`) hosts:
 *       pinch-ingest, pinch-alert-status.
 *
 * Both projects accept the SAME Project 1 user JWT in `Authorization`. They
 * differ only in the `apikey` header and the base URL.
 *
 * The Retrofit interface ([SosApiService]) is identical for both surfaces —
 * relative paths don't collide, so we instantiate it twice.
 */
object SosNetworkClient {

    fun createProject1(
        sessionManager: EncryptedSessionManager,
        getAuthApiService: () -> AuthApiService
    ): SosApiService =
        project1Retrofit(sessionManager, getAuthApiService).create(SosApiService::class.java)

    fun createProject2(
        sessionManager: EncryptedSessionManager,
        getAuthApiService: () -> AuthApiService
    ): SosApiService =
        project2Retrofit(sessionManager, getAuthApiService).create(SosApiService::class.java)

    /**
     * Project 1 Retrofit instance — exposed so additional service interfaces
     * (e.g. anything that needs the Project 1 base URL + signed-domain
     * interceptor chain) can be `.create()`'d without duplicating the OkHttp
     * client. Today only [SosApiService] uses it; the canonical Project 1
     * surface for the rest of the app is the main [com.faster.festival.di.NetworkModule].
     */
    fun project1Retrofit(
        sessionManager: EncryptedSessionManager,
        getAuthApiService: () -> AuthApiService
    ): retrofit2.Retrofit = build(
        sessionManager = sessionManager,
        getAuthApiService = getAuthApiService,
        baseUrl = BuildConfig.VITE_SUPABASE_URL,
        apikey = BuildConfig.VITE_SUPABASE_ANON_KEY,
        tagForLogs = "Sos.P1"
    )

    /**
     * Project 2 Retrofit instance — exposed so [com.faster.festival.data.remote.Project2ApiService]
     * shares the same OkHttp client / interceptor chain as [SosApiService].
     * Calling this twice intentionally yields two distinct Retrofit instances;
     * callers are expected to memoize via DI (see [com.faster.festival.di.SosModule]).
     */
    fun project2Retrofit(
        sessionManager: EncryptedSessionManager,
        getAuthApiService: () -> AuthApiService
    ): retrofit2.Retrofit = build(
        sessionManager = sessionManager,
        getAuthApiService = getAuthApiService,
        baseUrl = BuildConfig.PROJECT2_SOS_URL,
        apikey = BuildConfig.PROJECT2_SOS_ANON_KEY,
        tagForLogs = "Sos.P2"
    )

    private fun build(
        sessionManager: EncryptedSessionManager,
        getAuthApiService: () -> AuthApiService,
        baseUrl: String,
        apikey: String,
        tagForLogs: String
    ): retrofit2.Retrofit {
        require(baseUrl.startsWith("http")) {
            "$tagForLogs base URL is not configured (got: '$baseUrl') — set it in .env"
        }
        require(apikey.isNotBlank()) {
            "$tagForLogs apikey is not configured — set it in .env"
        }

        val logging = HttpLoggingInterceptor().apply {
            // BODY in debug so 4xx error bodies (signature_mismatch,
            // attestation_not_verified, etc.) are visible in logcat. Headers
            // that leak secrets are explicitly redacted.
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY
            else HttpLoggingInterceptor.Level.NONE
            redactHeader("Authorization")
            redactHeader("apikey")
            redactHeader("x-device-signature")
        }

        val apiKeyAndAuth = okhttp3.Interceptor { chain ->
            val original = chain.request()
            val builder = original.newBuilder().header("apikey", apikey)
            val token = sessionManager.getAccessToken()
            if (!token.isNullOrBlank()) {
                builder.header("Authorization", "Bearer $token")
            }
            chain.proceed(builder.build())
        }

        // Token refresh — reuse the same interceptor the main NetworkModule
        // uses for the rest of the app. On 401, it suspends, calls
        // /auth/v1/token?grant_type=refresh_token, swaps in the new access
        // token, retries the original request exactly once. Without this,
        // every poll after the access-token TTL elapses returns 401 and the
        // polling loop just spams warnings.
        val tokenRefresh = TokenRefreshInterceptor(sessionManager, getAuthApiService)

        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .addInterceptor(apiKeyAndAuth)
            // Refresh MUST be last so it sees the 401 and retries with the
            // freshly-injected Bearer.
            .addInterceptor(tokenRefresh)
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(8, TimeUnit.SECONDS)
            .build()

        val json = Json {
            ignoreUnknownKeys = true
            isLenient = true
            encodeDefaults = true
            explicitNulls = true
        }

        return Retrofit.Builder()
            .baseUrl(baseUrl.trimEnd('/') + "/")
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
    }
}
