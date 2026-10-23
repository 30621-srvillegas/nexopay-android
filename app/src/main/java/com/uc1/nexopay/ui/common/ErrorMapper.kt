package com.uc1.nexopay.ui.common

import com.uc1.nexopay.data.repository.ApiException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

object ErrorMapper {

    private val json = Json { ignoreUnknownKeys = true }

    fun humanMessage(error: Throwable): String = when (error) {
        is ApiException -> fromApi(error)
        is UnknownHostException -> "No se pudo conectar. Revisa Internet o la URL del backend."
        is SocketTimeoutException -> "El servidor tardó demasiado. Inténtalo otra vez."
        is IOException -> "Sin conexión. Revisa tu red."
        is HttpException -> "Error del servidor (${error.code()})."
        else -> error.message ?: "Ocurrió un error inesperado."
    }

    private fun fromApi(error: ApiException): String {
        val body = error.rawBody.orEmpty()
        val code = runCatching {
            json.parseToJsonElement(body)
                .jsonObject["error"]
                ?.jsonObject
                ?.get("code")
                ?.jsonPrimitive
                ?.content
        }.getOrNull()

        return when (code) {
            "invalid_credentials" -> "Cuenta o contraseña incorrectos."
            "account_locked" -> "Cuenta bloqueada por intentos fallidos. Espera 15 minutos."
            "insufficient_funds" -> "Saldo insuficiente para esta operación."
            "invalid_amount" -> "El monto no es válido."
            "invalid_contract" -> "El código de contrato no es válido."
            "idempotency_conflict" -> "La operación ya fue procesada con otros datos."
            "rate_limited" -> "Demasiadas solicitudes. Espera unos segundos."
            "unauthorized" -> "Tu sesión expiró. Inicia sesión otra vez."
            else -> when (error.statusCode) {
                400, 422 -> "Revisa los datos e inténtalo de nuevo."
                401 -> "Tu sesión expiró. Inicia sesión otra vez."
                403 -> "No tienes permiso para esta operación."
                404 -> "No encontramos la cuenta o el recurso."
                409 -> "La operación entra en conflicto con otra. Reintenta."
                429 -> "Demasiadas solicitudes. Espera unos segundos."
                in 500..599 -> "El servidor falló. Inténtalo más tarde."
                else -> "No se pudo completar la operación."
            }
        }
    }
}
