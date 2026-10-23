package com.uc1.nexopay.data.repository

import com.uc1.nexopay.data.local.AppDatabase
import com.uc1.nexopay.data.local.TransactionEntity
import com.uc1.nexopay.data.local.TransactionType
import com.uc1.nexopay.data.remote.NexoApi
import com.uc1.nexopay.data.remote.dto.*
import com.uc1.nexopay.security.SessionManager
import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import retrofit2.Response
import java.time.Instant
import java.time.format.DateTimeParseException
import java.util.UUID

class WalletRepository(
    private val api: NexoApi,
    private val database: AppDatabase,
    private val sessionManager: SessionManager
) {

    private val dao = database.transactionDao()

    val transactions: Flow<List<TransactionEntity>> = dao.observeRecent()

    suspend fun login(accountId: String, password: String): Result<LoginResponse> =
        call { api.login(LoginRequest(accountId.trim(), password)) }
            .onSuccess { response ->
                database.transactionDao().deleteAll()
                sessionManager.save(
                    accountId = response.account.id,
                    name = response.account.owner,
                    accessToken = response.accessToken,
                    refreshToken = response.refreshToken
                )
            }

    suspend fun restoreSession(): Result<Unit> = runCatching {
        if (!sessionManager.restore()) error("No hay sesión guardada")
    }

    suspend fun refreshAccessToken(): Result<Unit> = runCatching {
        val refreshToken = sessionManager.refreshToken
            ?: error("No hay refresh token")
        val response = api.refresh(RefreshRequest(refreshToken))

        if (!response.isSuccessful) {
            if (response.code() == 401 || response.code() == 403) {
                sessionManager.clear()
            }
            throw ApiException(response.code(), response.errorBody()?.string())
        }

        val body = response.body() ?: error("Respuesta de refresh vacía")
        sessionManager.updateAccessToken(body.accessToken)
        sessionManager.updateRefreshToken(body.refreshToken)
    }

    suspend fun logout() {
        val refreshToken = sessionManager.refreshToken
        if (refreshToken != null) {
            runCatching { api.logout(RefreshRequest(refreshToken)) }
        }
        sessionManager.clear()
        database.transactionDao().deleteAll()
    }

    suspend fun balance(): Result<BalanceResponse> =
        call { api.balance() }

    suspend fun syncTransactions(): Result<List<TransactionEntity>> =
        call { api.transactions(limit = 100) }
            .map { response ->
                val entities = response.transactions.map { it.toEntity() }
                database.withTransaction {
                    if (entities.isNotEmpty()) dao.upsertAll(entities)
                }
                entities
            }

    suspend fun deposit(amountCents: Long): Result<OperationResponse> =
        call { api.deposit(uuid(), DepositRequest(amountCents)) }
            .onSuccess { persist(it) }

    suspend fun withdraw(amountCents: Long): Result<OperationResponse> =
        call { api.withdraw(uuid(), WithdrawRequest(amountCents)) }
            .onSuccess { persist(it) }

    suspend fun transfer(
        toAccountId: String,
        amountCents: Long
    ): Result<OperationResponse> =
        call {
            api.transfer(
                uuid(),
                TransferRequest(toAccountId.trim(), amountCents)
            )
        }.onSuccess { persist(it) }

    suspend fun payService(
        contractCode: String,
        amountCents: Long
    ): Result<OperationResponse> =
        call {
            api.payService(
                uuid(),
                PayServiceRequest(contractCode.trim().uppercase(), amountCents)
            )
        }.onSuccess { persist(it) }

    suspend fun realtimeTicket(): Result<TicketResponse> =
        call { api.realtimeTicket() }

    suspend fun saveIncoming(transaction: TransactionDto): TransactionEntity {
        val entity = transaction.toEntity()
        dao.upsert(entity)
        return entity
    }

    private suspend fun persist(response: OperationResponse) {
        dao.upsert(response.transaction.toEntity())
    }

    private suspend fun <T> call(block: suspend () -> Response<T>): Result<T> =
        runCatching {
            val response = block()
            if (!response.isSuccessful) {
                throw ApiException(response.code(), response.errorBody()?.string())
            }
            response.body() ?: error("Respuesta vacía del servidor")
        }

    private fun uuid(): String = UUID.randomUUID().toString()

    private fun TransactionDto.toEntity(): TransactionEntity =
        TransactionEntity(
            id = id,
            type = type,
            amountCents = amountCents,
            balanceAfterCents = balanceCents,
            counterpartyAccountId = counterpartyAccountId,
            contractCode = contractCode,
            serviceName = serviceName,
            description = description,
            referenceId = referenceId,
            createdAt = parseInstant(createdAt)
        )

    private fun parseInstant(value: String): Long =
        try {
            Instant.parse(value).toEpochMilli()
        } catch (_: DateTimeParseException) {
            System.currentTimeMillis()
        }

    companion object {
        fun displayType(type: String): String = when (type) {
            TransactionType.DEPOSIT -> "Depósito"
            TransactionType.WITHDRAWAL -> "Retiro"
            TransactionType.TRANSFER_RECEIVED -> "Transferencia recibida"
            TransactionType.TRANSFER_SENT -> "Transferencia enviada"
            TransactionType.SERVICE_PAYMENT -> "Servicio básico"
            else -> "Movimiento"
        }

        fun isIncome(type: String): Boolean = TransactionType.isIncome(type)
    }
}

class ApiException(
    val statusCode: Int,
    val rawBody: String?
) : Exception("HTTP $statusCode")
