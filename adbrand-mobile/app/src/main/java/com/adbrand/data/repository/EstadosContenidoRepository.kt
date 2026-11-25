package com.adbrand.data.repository

import com.adbrand.core.network.Resultado
import com.adbrand.domain.model.ContenidoConEstado

// HU 56: contrato para probar la lista de estados sin red.
interface EstadosContenidoRepository {

    suspend fun estados(): Resultado<List<ContenidoConEstado>>

    // HU 63: cancelar en AdBrand, volver a activar y eliminar (solo cancelados). Por defecto fallan, así las
    // pruebas con datos falsos no tienen que implementarlas.
    suspend fun cancelar(id: Long): Resultado<Int> = Resultado.Error("No disponible")
    suspend fun reactivar(id: Long): Resultado<Unit> = Resultado.Error("No disponible")
    suspend fun eliminar(id: Long): Resultado<Unit> = Resultado.Error("No disponible")
}
