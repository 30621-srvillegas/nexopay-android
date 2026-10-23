package com.uc1.nexopay.data.remote

import com.uc1.nexopay.data.remote.dto.*
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query

interface NexoApi {

    @POST("api/v1/auth/login")
    suspend fun login(@Body body: LoginRequest): Response<LoginResponse>

    @POST("api/v1/auth/refresh")
    suspend fun refresh(@Body body: RefreshRequest): Response<RefreshResponse>

    @POST("api/v1/auth/logout")
    suspend fun logout(@Body body: RefreshRequest): Response<Unit>

    @GET("api/v1/auth/me")
    suspend fun me(): Response<AccountDto>

    @GET("api/v1/operations/balance")
    suspend fun balance(): Response<BalanceResponse>

    @GET("api/v1/operations/transactions")
    suspend fun transactions(
        @Query("limit") limit: Int = 100
    ): Response<TransactionsResponse>

    @POST("api/v1/operations/deposit")
    suspend fun deposit(
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body body: DepositRequest
    ): Response<OperationResponse>

    @POST("api/v1/operations/withdraw")
    suspend fun withdraw(
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body body: WithdrawRequest
    ): Response<OperationResponse>

    @POST("api/v1/operations/transfer")
    suspend fun transfer(
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body body: TransferRequest
    ): Response<OperationResponse>

    @POST("api/v1/operations/pay-service")
    suspend fun payService(
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body body: PayServiceRequest
    ): Response<OperationResponse>

    @POST("api/v1/realtime/ticket")
    suspend fun realtimeTicket(): Response<TicketResponse>
}
