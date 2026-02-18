package com.faster.festival.di

import com.faster.festival.BuildConfig
import com.faster.festival.data.remote.AuthApiService
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import io.github.jan_tennert.supabase.SupabaseClient
import io.github.jan_tennert.supabase.createSupabaseClient
import io.github.jan_tennert.supabase.realtime.Realtime
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
                "Please add VITE_SUPABASE_URL to local.properties file. " +
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

    // Supabase Realtime Client
    val supabaseClient: SupabaseClient by lazy {
        val supabaseUrl = BuildConfig.VITE_SUPABASE_URL
        val supabaseAnonKey = BuildConfig.VITE_SUPABASE_ANON_KEY

        if (supabaseUrl.isEmpty() || supabaseAnonKey.isEmpty()) {
            throw IllegalArgumentException(
                "Supabase credentials are not configured. " +
                "Please add VITE_SUPABASE_URL and VITE_SUPABASE_ANON_KEY to local.properties"
            )
        }

        createSupabaseClient(
            supabaseUrl = supabaseUrl,
            supabaseKey = supabaseAnonKey
        ) {
            install(Realtime)
        }
    }
}
