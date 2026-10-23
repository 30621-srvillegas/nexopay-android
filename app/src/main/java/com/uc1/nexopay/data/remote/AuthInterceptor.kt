package com.uc1.nexopay.data.remote

import com.uc1.nexopay.security.SessionManager
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(
    private val sessionManager: SessionManager
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val path = original.url.encodedPath

        val isPublic = path.endsWith("/auth/login") ||
            path.endsWith("/auth/refresh")

        if (isPublic) return chain.proceed(original)

        val token = sessionManager.accessToken
        if (token.isNullOrBlank()) return chain.proceed(original)

        val request = original.newBuilder()
            .header("Authorization", "Bearer $token")
            .build()

        return chain.proceed(request)
    }
}
