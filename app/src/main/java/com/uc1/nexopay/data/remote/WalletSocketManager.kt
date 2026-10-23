package com.uc1.nexopay.data.remote

import android.util.Log
import com.uc1.nexopay.BuildConfig
import com.uc1.nexopay.data.local.TransactionEntity
import com.uc1.nexopay.data.remote.dto.WsBalanceData
import com.uc1.nexopay.data.remote.dto.WsEnvelope
import com.uc1.nexopay.data.remote.dto.WsReadyData
import com.uc1.nexopay.data.remote.dto.WsTransferData
import com.uc1.nexopay.data.repository.WalletRepository
import com.uc1.nexopay.security.SessionManager
import com.uc1.nexopay.ui.common.ErrorMapper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.decodeFromJsonElement
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import kotlin.random.Random

enum class SocketStatus {
    CONNECTING,
    CONNECTED,
    DISCONNECTED
}

sealed interface WalletEvent {
    data class Ready(val balanceCents: Long) : WalletEvent
    data class BalanceChanged(
        val balanceCents: Long,
        val reason: String?,
        val transaction: TransactionEntity?
    ) : WalletEvent
    data class TransferReceived(
        val transaction: TransactionEntity,
        val fromAccountId: String,
        val balanceCents: Long
    ) : WalletEvent
    data class Failed(val message: String) : WalletEvent
}

class WalletSocketManager(
    private val repository: WalletRepository,
    private val sessionManager: SessionManager,
    private val scope: CoroutineScope
) {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val client = OkHttpClient.Builder()
        .pingInterval(20, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    private val _status = MutableStateFlow(SocketStatus.DISCONNECTED)
    val status: StateFlow<SocketStatus> = _status.asStateFlow()

    private val _events = MutableSharedFlow<WalletEvent>(
        extraBufferCapacity = 16
    )
    val events: SharedFlow<WalletEvent> = _events.asSharedFlow()

    @Volatile
    private var webSocket: WebSocket? = null

    @Volatile
    private var opening = false

    @Volatile
    private var intentionalClose = false

    private var reconnectJob: Job? = null
    private var retryAttempt = 0

    fun connect() {
        if (sessionManager.session.value == null) return
        if (webSocket != null) return
        if (opening) return

        intentionalClose = false
        openSocket()
    }

    fun disconnect() {
        intentionalClose = true
        reconnectJob?.cancel()
        reconnectJob = null
        webSocket?.close(1000, "client_close")
        webSocket = null
        _status.value = SocketStatus.DISCONNECTED
    }

    private fun openSocket() {
        if (intentionalClose) return
        if (webSocket != null) return
        if (opening) return

        opening = true

        scope.launch {
            try {
                _status.value = SocketStatus.CONNECTING

                val ticket = repository.realtimeTicket().getOrElse { error ->
                    _status.value = SocketStatus.DISCONNECTED
                    _events.emit(WalletEvent.Failed(ErrorMapper.humanMessage(error)))
                    scheduleReconnect()
                    return@launch
                }

                if (intentionalClose) return@launch

                val encoded = URLEncoder.encode(ticket.ticket, "UTF-8")
                val request = Request.Builder()
                    .url("${BuildConfig.WS_URL}?ticket=$encoded")
                    .build()

                webSocket = client.newWebSocket(request, listener)
            } catch (error: Exception) {
                _status.value = SocketStatus.DISCONNECTED
                _events.emit(WalletEvent.Failed(ErrorMapper.humanMessage(error)))
                scheduleReconnect()
            } finally {
                opening = false
            }
        }
    }

    private val listener = object : WebSocketListener() {

        override fun onOpen(webSocket: WebSocket, response: Response) {
            retryAttempt = 0
            _status.value = SocketStatus.CONNECTED
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            scope.launch {
                handleMessage(text)
            }
        }

        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
            webSocket.close(code, reason)
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            this@WalletSocketManager.webSocket = null
            _status.value = SocketStatus.DISCONNECTED
            scheduleReconnect()
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            this@WalletSocketManager.webSocket = null
            _status.value = SocketStatus.DISCONNECTED

            scope.launch {
                _events.emit(WalletEvent.Failed(ErrorMapper.humanMessage(t)))
            }
            scheduleReconnect()
        }
    }

    private suspend fun handleMessage(text: String) {
        try {
            val envelope = json.decodeFromString<WsEnvelope>(text)

            when (envelope.event) {
                "connection.ready" -> {
                    val data = json.decodeFromJsonElement<WsReadyData>(envelope.data)
                    _events.emit(WalletEvent.Ready(data.balanceCents))
                }

                "balance.updated" -> {
                    val data = json.decodeFromJsonElement<WsBalanceData>(envelope.data)
                    val entity = data.transaction?.let {
                        repository.saveIncoming(it)
                    }
                    _events.emit(
                        WalletEvent.BalanceChanged(
                            balanceCents = data.balanceCents,
                            reason = data.reason,
                            transaction = entity
                        )
                    )
                }

                "transfer.received" -> {
                    val data = json.decodeFromJsonElement<WsTransferData>(envelope.data)
                    val entity = repository.saveIncoming(data.transaction)
                    _events.emit(
                        WalletEvent.TransferReceived(
                            transaction = entity,
                            fromAccountId = data.fromAccountId,
                            balanceCents = data.balanceCents
                        )
                    )
                }
            }
        } catch (error: Exception) {
            if (BuildConfig.DEBUG) {
                Log.w("NexoPaySocket", "Evento WS inválido: ${error.message}")
            }
        }
    }

    private fun scheduleReconnect() {
        if (intentionalClose) return
        if (sessionManager.session.value == null) return
        if (reconnectJob?.isActive == true) return

        val attempt = retryAttempt.coerceAtMost(5)
        retryAttempt++

        reconnectJob = scope.launch {
            val backoff = (1_000L shl attempt) + Random.nextLong(0, 600)
            delay(backoff)
            if (!intentionalClose && webSocket == null) {
                openSocket()
            }
        }
    }
}
