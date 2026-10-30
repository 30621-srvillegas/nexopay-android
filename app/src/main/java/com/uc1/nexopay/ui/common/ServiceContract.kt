package com.uc1.nexopay.ui.common

object ServiceContract {
    /** Mantiene el formato aceptado por la API; no consulta la existencia del contrato. */
    fun build(prefix: String?, number: String): String? {
        if (prefix !in setOf("LUZ", "AGUA", "NET", "GAS")) return null
        val normalized = number.trim()
        if (!normalized.matches(Regex("^[0-9]{3,12}$"))) return null
        return "$prefix-$normalized"
    }
}
