package com.adbrand.data.repository

import com.adbrand.core.network.Resultado

// HU 50: contrato para probar el registro del teléfono sin red.
interface NotificacionesRepository {

    suspend fun registrar(token: String, plataforma: String, nombreEquipo: String): Resultado<Unit>

    suspend fun quitar(token: String): Resultado<Unit>
}
