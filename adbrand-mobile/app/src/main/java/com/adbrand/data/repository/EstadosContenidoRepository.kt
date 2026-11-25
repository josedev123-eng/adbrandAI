package com.adbrand.data.repository

import com.adbrand.core.network.Resultado
import com.adbrand.domain.model.ContenidoConEstado

// HU 56: contrato para probar la lista de estados sin red.
interface EstadosContenidoRepository {

    suspend fun estados(): Resultado<List<ContenidoConEstado>>
}
