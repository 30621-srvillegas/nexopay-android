package com.uc1.nexopay.data.remote.dto

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class WsEnvelope(
    val v: Int = 1,
    val event: String,
    val data: JsonObject,
    val ts: String? = null
)

@Serializable
data class WsReadyData(
    val accountId: String,
    val balanceCents: Long,
    val balance: String? = null
)

@Serializable
data class WsBalanceData(
    val balanceCents: Long,
    val balance: String? = null,
    val reason: String? = null,
    val transaction: TransactionDto? = null
)

@Serializable
data class WsTransferData(
    val transaction: TransactionDto,
    val fromAccountId: String,
    val balanceCents: Long,
    val balance: String? = null
)
