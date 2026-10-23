package com.uc1.nexopay.data.remote

import com.uc1.nexopay.data.remote.dto.RefreshRequest
import com.uc1.nexopay.security.SessionManager
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route

class TokenAuthenticator(
    private val sessionManager: SessionManager,
    private val refreshApi: NexoApi,
    private val onSessionExpired: () -> Unit
) : Authenticator {

    private val refreshLock = Any()

    override fun authenticate(route: Route?, response: Response): Request? {
        if (response.request.header("X-Refresh-Retry") != null) return null

        val path = response.request.url.encodedPath
        if (path.endsWith("/auth/login") || path.endsWith("/auth/refresh")) return null

        val failedToken = response.request.header("Authorization")
            ?.removePrefix("Bearer ")
            ?.trim()

        synchronized(refreshLock) {
            val current = sessionManager.accessToken
            if (!current.isNullOrBlank() && current != failedToken) {
                return response.request.newBuilder()
                    .header("Authorization", "Bearer $current")
                    .header("X-Refresh-Retry", "1")
                    .build()
            }

            val refreshToken = sessionManager.refreshToken ?: run {
                onSessionExpired()
                return null
            }

            val result = runBlocking {
                runCatching {
                    refreshApi.refresh(RefreshRequest(refreshToken))
                }.getOrNull()
            }

            val body = result?.body()
            if (result?.isSuccessful != true || body == null) {
                if (result?.code() == 401 || result?.code() == 403) {
                    sessionManager.clear()
                    onSessionExpired()
                }
                return null
            }

            sessionManager.updateAccessToken(body.accessToken)
            sessionManager.updateRefreshToken(body.refreshToken)

            return response.request.newBuilder()
                .header("Authorization", "Bearer ${body.accessToken}")
                .header("X-Refresh-Retry", "1")
                .build()
        }
    }
}
