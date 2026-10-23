package com.uc1.nexopay.security

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class Session(
    val accountId: String,
    val name: String,
    val accessToken: String,
    val refreshToken: String
)

class SessionManager(private val secureStore: SecureStore) {

    private val _session = MutableStateFlow<Session?>(null)
    val session: StateFlow<Session?> = _session.asStateFlow()

    val accessToken: String?
        get() = _session.value?.accessToken

    val refreshToken: String?
        get() = secureStore.get(KEY_REFRESH)

    val accountId: String?
        get() = _session.value?.accountId

    fun restore(): Boolean {
        val refresh = secureStore.get(KEY_REFRESH) ?: return false
        val accountId = secureStore.get(KEY_ACCOUNT_ID) ?: return false
        val name = secureStore.get(KEY_NAME) ?: "Usuario"

        _session.value = Session(
            accountId = accountId,
            name = name,
            accessToken = "",
            refreshToken = refresh
        )
        return true
    }

    fun save(
        accountId: String,
        name: String,
        accessToken: String,
        refreshToken: String
    ) {
        secureStore.put(KEY_REFRESH, refreshToken)
        secureStore.put(KEY_ACCOUNT_ID, accountId)
        secureStore.put(KEY_NAME, name)
        _session.value = Session(accountId, name, accessToken, refreshToken)
    }

    fun updateAccessToken(accessToken: String) {
        _session.update { current ->
            current?.copy(accessToken = accessToken)
        }
    }

    fun updateRefreshToken(refreshToken: String) {
        secureStore.put(KEY_REFRESH, refreshToken)
        _session.update { current ->
            current?.copy(refreshToken = refreshToken)
        }
    }

    fun clear() {
        _session.value = null
        secureStore.remove(KEY_REFRESH)
        secureStore.remove(KEY_ACCOUNT_ID)
        secureStore.remove(KEY_NAME)
    }

    private companion object {
        const val KEY_REFRESH = "refresh_token"
        const val KEY_ACCOUNT_ID = "account_id"
        const val KEY_NAME = "account_name"
    }
}
