package com.uc1.nexopay.data.remote

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.uc1.nexopay.BuildConfig
import com.uc1.nexopay.security.SessionManager
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

object ApiFactory {

    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        coerceInputValues = true
    }

    private val contentType = "application/json".toMediaType()

    fun create(
        sessionManager: SessionManager,
        onSessionExpired: () -> Unit
    ): NexoApi {
        val baseClient = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(25, TimeUnit.SECONDS)
            .writeTimeout(25, TimeUnit.SECONDS)
            .callTimeout(35, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .apply {
                if (BuildConfig.DEBUG) {
                    addInterceptor(
                        HttpLoggingInterceptor().apply {
                            level = HttpLoggingInterceptor.Level.BASIC
                        }
                    )
                }
            }
            .build()

        val refreshRetrofit = Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(baseClient)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()

        val refreshApi = refreshRetrofit.create(NexoApi::class.java)

        val authenticatedClient = baseClient.newBuilder()
            .addInterceptor(AuthInterceptor(sessionManager))
            .authenticator(
                TokenAuthenticator(
                    sessionManager = sessionManager,
                    refreshApi = refreshApi,
                    onSessionExpired = onSessionExpired
                )
            )
            .build()

        return Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(authenticatedClient)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
            .create(NexoApi::class.java)
    }
}
