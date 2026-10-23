package com.uc1.nexopay.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uc1.nexopay.di.AppContainer
import com.uc1.nexopay.ui.common.ErrorMapper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LoginUiState(
    val isLoading: Boolean = false,
    val isRestoring: Boolean = true,
    val lastAccount: String = "",
    val error: String? = null
)

sealed interface LoginEvent {
    data object Success : LoginEvent
    data class Message(val text: String) : LoginEvent
}

class LoginViewModel(
    private val container: AppContainer
) : ViewModel() {

    private val _state = MutableStateFlow(LoginUiState())
    val state: StateFlow<LoginUiState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<LoginEvent>(extraBufferCapacity = 4)
    val events: SharedFlow<LoginEvent> = _events.asSharedFlow()

    private var restoreAttempted = false

    init {
        viewModelScope.launch {
            container.settingsStore.lastAccount.collect { account ->
                _state.update { it.copy(lastAccount = account.orEmpty()) }
            }
        }
    }

    fun restoreSession() {
        if (restoreAttempted) return
        restoreAttempted = true

        viewModelScope.launch(Dispatchers.IO) {
            val restored = container.repository.restoreSession().isSuccess
            if (!restored) {
                _state.update { it.copy(isRestoring = false) }
                return@launch
            }

            val refreshed = container.repository.refreshAccessToken()
            if (refreshed.isSuccess || container.sessionManager.session.value != null) {
                _state.update { it.copy(isRestoring = false) }
                _events.emit(LoginEvent.Success)
            } else {
                container.repository.logout()
                _state.update { it.copy(isRestoring = false) }
            }
        }
    }

    fun login(accountId: String, password: String) {
        if (_state.value.isLoading) return

        if (accountId.isBlank()) {
            _state.update { it.copy(error = "Ingresa tu número de cuenta.") }
            return
        }
        if (password.isBlank()) {
            _state.update { it.copy(error = "Ingresa tu contraseña.") }
            return
        }

        _state.update { it.copy(isLoading = true, error = null) }

        viewModelScope.launch(Dispatchers.IO) {
            val result = container.repository.login(accountId, password)

            result.onSuccess { response ->
                container.settingsStore.setLastAccount(response.account.id)
                _state.update { it.copy(isLoading = false, isRestoring = false) }
                _events.emit(LoginEvent.Success)
            }.onFailure { error ->
                _state.update {
                    it.copy(
                        isLoading = false,
                        isRestoring = false,
                        error = ErrorMapper.humanMessage(error)
                    )
                }
            }
        }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }
}
