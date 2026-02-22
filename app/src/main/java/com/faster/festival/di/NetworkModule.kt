package com.faster.festival.di

import com.faster.festival.BuildConfig
import com.faster.festival.data.remote.AuthApiService
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

    private val client =
            OkHttpClient.Builder()
                    .addInterceptor(loggingInterceptor)
                    .addInterceptor(authInterceptor)
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

    val authApiService: AuthApiService by lazy { retrofit.create(AuthApiService::class.java) }

    // Supabase realtime client support removed. If you need to re-enable, re-add the dependency and
    // restore the client creation logic here.
}
