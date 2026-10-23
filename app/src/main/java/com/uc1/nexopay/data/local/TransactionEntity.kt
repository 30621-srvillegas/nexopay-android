package com.uc1.nexopay.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "transactions",
    indices = [Index(value = ["createdAt"])]
)
data class TransactionEntity(
    @PrimaryKey val id: String,
    val type: String,
    val amountCents: Long,
    val balanceAfterCents: Long,
    val counterpartyAccountId: String?,
    val contractCode: String?,
    val serviceName: String?,
    val description: String?,
    val referenceId: String?,
    val createdAt: Long
)

object TransactionType {
    const val DEPOSIT = "DEPOSIT"
    const val WITHDRAWAL = "WITHDRAWAL"
    const val TRANSFER_SENT = "TRANSFER_SENT"
    const val TRANSFER_RECEIVED = "TRANSFER_RECEIVED"
    const val SERVICE_PAYMENT = "SERVICE_PAYMENT"

    fun isIncome(type: String): Boolean =
        type == DEPOSIT || type == TRANSFER_RECEIVED
}
