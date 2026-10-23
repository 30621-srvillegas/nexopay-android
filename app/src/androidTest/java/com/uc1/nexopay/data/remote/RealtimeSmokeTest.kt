package com.uc1.nexopay.data.remote

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.uc1.nexopay.BuildConfig
import com.uc1.nexopay.data.local.DatabaseProvider
import com.uc1.nexopay.data.remote.dto.RefreshRequest
import com.uc1.nexopay.data.repository.WalletRepository
import com.uc1.nexopay.security.SecureStore
import com.uc1.nexopay.security.SessionManager
import java.net.URLEncoder
import java.util.UUID
import kotlin.coroutines.resume
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RealtimeSmokeTest {

    private val json = Json { ignoreUnknownKeys = true }
    private val http = OkHttpClient()
    private val mediaType = "application/json".toMediaType()

    @Test
    fun socketReceivesReadyAndTransfer() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val secureStore = SecureStore(context)
        val session = SessionManager(secureStore).apply { clear() }
        val api = ApiFactory.create(session) {}
        val database = DatabaseProvider.get(context, secureStore)
        val repository = WalletRepository(api, database, session)

        val login = repository.login("10020001", "Nexo#2026").getOrThrow()
        assertTrue(login.accessToken.isNotBlank())

        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val socket = WalletSocketManager(repository, session, scope)

        val ready = CompletableDeferred<Long>()
        val received = CompletableDeferred<WalletEvent.TransferReceived>()

        val collector = scope.launch(start = CoroutineStart.UNDISPATCHED) {
            socket.events.collect { event ->
                when (event) {
                    is WalletEvent.Ready -> ready.complete(event.balanceCents)
                    is WalletEvent.TransferReceived -> received.complete(event)
                    else -> Unit
                }
            }
        }

        try {
            socket.connect()
            val initialBalance = withTimeout(15_000) { ready.await() }
            assertEquals(SocketStatus.CONNECTED, socket.status.value)

            val luisToken = rawLogin("10020002")
            rawTransfer(luisToken, toAccountId = "10020001", amountCents = 123)

            val event = withTimeout(15_000) { received.await() }
            assertEquals(123L, event.transaction.amountCents)
            assertEquals("TRANSFER_RECEIVED", event.transaction.type)
            assertEquals("10020002", event.fromAccountId)
            assertEquals(initialBalance + 123, event.balanceCents)

            val stored = database.transactionDao().getRecent()
            assertTrue(
                "transacción WS no persistida en Room",
                stored.any { it.id == event.transaction.id }
            )

            rawTransfer(login.accessToken, toAccountId = "10020002", amountCents = 123)

            val ticket = api.realtimeTicket()
            assertTrue("ticket HTTP ${ticket.code()}", ticket.isSuccessful)
            val raw = ticket.body()!!.ticket
            assertTrue(raw.isNotBlank())

            val url = "${BuildConfig.WS_URL}?ticket=${URLEncoder.encode(raw, "UTF-8")}"
            assertEquals(1000, openSocket(url))
            assertEquals(4401, openSocket(url))
        } finally {
            socket.disconnect()
            collector.cancel()
            scope.cancel()
            runCatching { api.logout(RefreshRequest(session.refreshToken ?: "")) }
            session.clear()
        }
    }

    private fun rawLogin(accountId: String): String {
        val (code, body) = postJson(
            path = "api/v1/auth/login",
            body = """{"accountId":"$accountId","password":"Nexo#2026"}"""
        )
        assertEquals("login $accountId HTTP $code: $body", 200, code)
        return json.parseToJsonElement(body)
            .jsonObject["accessToken"]!!
            .jsonPrimitive
            .content
    }

    private fun rawTransfer(token: String, toAccountId: String, amountCents: Long) {
        val (code, body) = postJson(
            path = "api/v1/operations/transfer",
            body = """{"toAccountId":"$toAccountId","amountCents":$amountCents}""",
            token = token,
            idempotencyKey = UUID.randomUUID().toString()
        )
        assertEquals("transfer HTTP $code: $body", 200, code)
    }

    private fun postJson(
        path: String,
        body: String,
        token: String? = null,
        idempotencyKey: String? = null
    ): Pair<Int, String> {
        val builder = Request.Builder()
            .url(BuildConfig.API_BASE_URL + path)
            .post(body.toRequestBody(mediaType))
        token?.let { builder.header("Authorization", "Bearer $it") }
        idempotencyKey?.let { builder.header("Idempotency-Key", it) }

        http.newCall(builder.build()).execute().use { response ->
            return response.code to response.body?.string().orEmpty()
        }
    }

    private suspend fun openSocket(url: String): Int = withTimeout(10_000) {
        suspendCancellableCoroutine { continuation ->
            val webSocket = OkHttpClient().newWebSocket(
                Request.Builder().url(url).build(),
                object : WebSocketListener() {
                    override fun onOpen(webSocket: WebSocket, response: Response) {
                        webSocket.close(1000, "test_close")
                    }

                    override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                        webSocket.close(code, reason)
                    }

                    override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                        if (continuation.isActive) continuation.resume(code)
                    }

                    override fun onFailure(
                        webSocket: WebSocket,
                        t: Throwable,
                        response: Response?
                    ) {
                        if (continuation.isActive) continuation.resume(response?.code ?: -1)
                    }
                }
            )
            continuation.invokeOnCancellation { webSocket.cancel() }
        }
    }
}
