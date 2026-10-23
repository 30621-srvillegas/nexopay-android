package com.uc1.nexopay.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LoginRequest(
    val accountId: String,
    val password: String
)

@Serializable
data class LoginResponse(
    val accessToken: String,
    val refreshToken: String,
    val tokenType: String = "Bearer",
    val expiresInSeconds: Int = 0,
    val account: AccountDto
)

@Serializable
data class RefreshRequest(
    val refreshToken: String
)

@Serializable
data class RefreshResponse(
    val accessToken: String,
    val refreshToken: String,
    val tokenType: String = "Bearer",
    val expiresInSeconds: Int = 0
)

@Serializable
data class AccountDto(
    val id: String,
    val owner: String,
    val currency: String = "PEN",
    val balanceCents: Long,
    val balance: String
)

@Serializable
data class BalanceResponse(
    val balanceCents: Long,
    val balance: String
)

@Serializable
data class TransactionDto(
    val id: String,
    val type: String,
    val amountCents: Long,
    val amount: String,
    val balanceCents: Long,
    val balance: String,
    val counterpartyAccountId: String? = null,
    val contractCode: String? = null,
    val serviceName: String? = null,
    val description: String? = null,
    val referenceId: String? = null,
    val createdAt: String
)

@Serializable
data class TransactionsResponse(
    val transactions: List<TransactionDto>
)

@Serializable
data class DepositRequest(val amountCents: Long)

@Serializable
data class WithdrawRequest(val amountCents: Long)

@Serializable
data class TransferRequest(
    val toAccountId: String,
    val amountCents: Long
)

@Serializable
data class PayServiceRequest(
    val contractCode: String,
    val amountCents: Long
)

@Serializable
data class OperationResponse(
    val transaction: TransactionDto,
    val balanceCents: Long,
    val balance: String,
    val toAccountId: String? = null,
    val referenceId: String? = null,
    val idempotentReplay: Boolean = false
)

@Serializable
data class TicketResponse(
    val ticket: String,
    val expiresInSeconds: Int = 30
)

@Serializable
data class ApiErrorEnvelope(
    val error: ApiErrorBody
)

@Serializable
data class ApiErrorBody(
    val code: String,
    val message: String,
    val details: ApiErrorDetails? = null,
    val requestId: String? = null
)

@Serializable
data class ApiErrorDetails(
    val balanceCents: Long? = null,
    val field: String? = null,
    val retryAfterSeconds: Int? = null
)

@Serializable
data class HealthResponse(
    val status: String,
    val timestamp: String? = null
)
