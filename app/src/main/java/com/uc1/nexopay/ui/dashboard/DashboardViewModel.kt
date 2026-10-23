package com.uc1.nexopay.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uc1.nexopay.data.local.TransactionEntity
import com.uc1.nexopay.data.remote.SocketStatus
import com.uc1.nexopay.data.remote.WalletEvent
import com.uc1.nexopay.data.remote.WalletSocketManager
import com.uc1.nexopay.data.repository.WalletRepository
import com.uc1.nexopay.security.SessionManager
import com.uc1.nexopay.ui.common.ErrorMapper
import com.uc1.nexopay.ui.common.UiUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex

data class DashboardUiState(
    val owner: String = "",
    val accountId: String = "",
    val balanceCents: Long = 0L,
    val transactions: List<TransactionEntity> = emptyList(),
    val socketStatus: SocketStatus = SocketStatus.DISCONNECTED,
    val isLoading: Boolean = false,
    val isOperating: Boolean = false,
    val error: String? = null
)

sealed interface DashboardEvent {
    data class Message(val text: String) : DashboardEvent
    data object SessionExpired : DashboardEvent
}

class DashboardViewModel(
    private val repository: WalletRepository,
    private val sessionManager: SessionManager,
    private val socket: WalletSocketManager
) : ViewModel() {

    private val _state = MutableStateFlow(DashboardUiState())
    val state: StateFlow<DashboardUiState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<DashboardEvent>(extraBufferCapacity = 8)
    val events: SharedFlow<DashboardEvent> = _events.asSharedFlow()

    private val operationLock = Mutex()
    private var started = false

    init {
        val session = sessionManager.session.value
        _state.update {
            it.copy(
                owner = session?.name.orEmpty(),
                accountId = session?.accountId.orEmpty()
            )
        }

        repository.transactions
            .onEach { list -> _state.update { it.copy(transactions = list) } }
            .launchIn(viewModelScope)

        viewModelScope.launch {
            socket.status.collect { status ->
                _state.update { it.copy(socketStatus = status) }
            }
        }

        viewModelScope.launch {
            socket.events.collect { event -> handleSocketEvent(event) }
        }
    }

    fun onStart() {
        if (!started) {
            started = true
            socket.connect()
            viewModelScope.launch(Dispatchers.IO) { loadInitialData() }
        }
    }

    fun onStop() {
        started = false
        socket.disconnect()
    }

    fun refresh() {
        viewModelScope.launch(Dispatchers.IO) { loadInitialData() }
    }

    fun deposit(amountCents: Long) = runOperation(
        successMessage = "Depósito de ${UiUtils.formatCents(amountCents)} realizado."
    ) {
        repository.deposit(amountCents)
    }

    fun withdraw(amountCents: Long) = runOperation(
        successMessage = "Retiro de ${UiUtils.formatCents(amountCents)} realizado."
    ) {
        repository.withdraw(amountCents)
    }

    fun transfer(toAccountId: String, amountCents: Long) = runOperation(
        successMessage = "Transferencia de ${UiUtils.formatCents(amountCents)} enviada."
    ) {
        repository.transfer(toAccountId, amountCents)
    }

    fun payService(contractCode: String, amountCents: Long) = runOperation(
        successMessage = "Pago de servicio registrado."
    ) {
        repository.payService(contractCode, amountCents)
    }

    fun logout() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.logout()
            socket.disconnect()
            _events.emit(DashboardEvent.SessionExpired)
        }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }

    private suspend fun loadInitialData() {
        _state.update { it.copy(isLoading = true, error = null) }

        if (sessionManager.accessToken.isNullOrBlank()) {
            val refreshed = repository.refreshAccessToken()
            if (refreshed.isFailure) {
                if (sessionManager.session.value == null) {
                    handleSessionFailure()
                } else {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = refreshed.exceptionOrNull()
                                ?.let(ErrorMapper::humanMessage)
                                ?: "Sin conexión. Se muestra el historial local."
                        )
                    }
                }
                return
            }
        }

        repository.balance().onSuccess { response ->
            _state.update { it.copy(balanceCents = response.balanceCents) }
        }.onFailure { error ->
            if (sessionManager.session.value == null) {
                handleSessionFailure()
            } else {
                _state.update { it.copy(error = ErrorMapper.humanMessage(error)) }
            }
        }

        repository.syncTransactions().onFailure { error ->
            if (sessionManager.session.value == null) {
                handleSessionFailure()
            }
        }

        _state.update { it.copy(isLoading = false) }
    }

    private fun runOperation(
        successMessage: String,
        block: suspend () -> Result<com.uc1.nexopay.data.remote.dto.OperationResponse>
    ) {
        if (!operationLock.tryLock()) return

        _state.update { it.copy(isOperating = true, error = null) }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val result = block()
                result.onSuccess { response ->
                    _state.update {
                        it.copy(
                            balanceCents = response.balanceCents,
                            isOperating = false
                        )
                    }
                    _events.emit(DashboardEvent.Message(successMessage))
                }.onFailure { error ->
                    _state.update {
                        it.copy(
                            isOperating = false,
                            error = ErrorMapper.humanMessage(error)
                        )
                    }
                }
            } finally {
                operationLock.unlock()
                _state.update { it.copy(isOperating = false) }
            }
        }
    }

    private suspend fun handleSocketEvent(event: WalletEvent) {
        when (event) {
            is WalletEvent.Ready -> {
                _state.update { it.copy(balanceCents = event.balanceCents) }
            }

            is WalletEvent.BalanceChanged -> {
                _state.update { it.copy(balanceCents = event.balanceCents) }
            }

            is WalletEvent.TransferReceived -> {
                _state.update { it.copy(balanceCents = event.balanceCents) }
                _events.emit(
                    DashboardEvent.Message(
                        "Recibiste ${UiUtils.formatCents(event.transaction.amountCents)} " +
                            "de la cuenta ${event.fromAccountId}."
                    )
                )
            }

            is WalletEvent.Failed -> {
                if (sessionManager.session.value == null) {
                    _events.emit(DashboardEvent.SessionExpired)
                }
            }
        }
    }

    private suspend fun handleSessionFailure() {
        sessionManager.clear()
        socket.disconnect()
        _events.emit(DashboardEvent.SessionExpired)
    }
}
