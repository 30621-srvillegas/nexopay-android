package com.uc1.nexopay.ui.common

import com.uc1.nexopay.data.repository.ApiException
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.IOException

class ErrorMapperTest {

    @Test
    fun mapsInsufficientFunds() {
        val error = ApiException(
            422,
            """{"error":{"code":"insufficient_funds","message":"Saldo insuficiente"}}"""
        )
        assertEquals(
            "Saldo insuficiente para esta operación.",
            ErrorMapper.humanMessage(error)
        )
    }

    @Test
    fun mapsLockedAccount() {
        val error = ApiException(
            423,
            """{"error":{"code":"account_locked","message":"Cuenta bloqueada"}}"""
        )
        assertEquals(
            "Cuenta bloqueada por intentos fallidos. Espera 15 minutos.",
            ErrorMapper.humanMessage(error)
        )
    }

    @Test
    fun mapsNetworkError() {
        assertEquals(
            "Sin conexión. Revisa tu red.",
            ErrorMapper.humanMessage(IOException("timeout"))
        )
    }
}
