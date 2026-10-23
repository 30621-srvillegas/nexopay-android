package com.uc1.nexopay.ui.common

import com.uc1.nexopay.data.local.TransactionEntity
import com.uc1.nexopay.data.local.TransactionType
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object UiUtils {

    private val locale = Locale("es", "PE")
    private val moneyFormat = NumberFormat.getNumberInstance(locale).apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }
    private val dateFormat = DateTimeFormatter
        .ofPattern("dd MMM yyyy · HH:mm", locale)
        .withZone(ZoneId.systemDefault())

    @Synchronized
    fun formatCents(cents: Long): String {
        val value = BigDecimal.valueOf(cents, 2)
        return "S/ ${moneyFormat.format(value)}"
    }

    fun parseAmountToCents(raw: String): Long? {
        val cleaned = raw
            .trim()
            .replace("S/", "", ignoreCase = true)
            .replace(" ", "")
        if (cleaned.isEmpty()) return null

        val normalized = normalizeAmount(cleaned) ?: return null
        val value = normalized.toBigDecimalOrNull() ?: return null
        if (value <= BigDecimal.ZERO) return null

        return runCatching {
            value.movePointRight(2)
                .setScale(0, RoundingMode.HALF_UP)
                .longValueExact()
        }.getOrNull()?.takeIf { it > 0 }
    }

    private fun normalizeAmount(value: String): String? {
        val lastComma = value.lastIndexOf(',')
        val lastDot = value.lastIndexOf('.')

        return when {
            lastComma >= 0 && lastDot >= 0 ->
                if (lastComma > lastDot) {
                    value.replace(".", "").replace(',', '.')
                } else {
                    value.replace(",", "")
                }

            lastComma >= 0 -> value.replace(',', '.')
            else -> value
        }
    }

    fun formatDate(epochMillis: Long): String =
        dateFormat.format(Instant.ofEpochMilli(epochMillis))

    fun transactionTitle(transaction: TransactionEntity): String = when (transaction.type) {
        TransactionType.DEPOSIT -> "Depósito"
        TransactionType.WITHDRAWAL -> "Retiro"
        TransactionType.TRANSFER_RECEIVED -> "Transferencia recibida"
        TransactionType.TRANSFER_SENT -> "Transferencia enviada"
        TransactionType.SERVICE_PAYMENT ->
            transaction.serviceName?.let { "Servicio: $it" } ?: "Servicio básico"
        else -> transaction.description ?: "Movimiento"
    }

    fun transactionSubtitle(transaction: TransactionEntity): String {
        val detail = when {
            transaction.counterpartyAccountId != null ->
                "Cuenta ${transaction.counterpartyAccountId}"
            transaction.contractCode != null ->
                "Contrato ${transaction.contractCode}"
            else -> transaction.description
                ?.takeIf { it.isNotBlank() && it != transactionTitle(transaction) }
                ?: "NexoPay"
        }
        return "${formatDate(transaction.createdAt)} · $detail"
    }

    fun signedAmount(transaction: TransactionEntity): String {
        val prefix = if (TransactionType.isIncome(transaction.type)) "+" else "−"
        return "$prefix ${formatCents(transaction.amountCents)}"
    }
}
