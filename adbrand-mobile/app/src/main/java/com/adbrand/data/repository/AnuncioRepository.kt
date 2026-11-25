package com.adbrand.data.repository

import com.adbrand.core.network.Resultado
import com.adbrand.domain.model.AnuncioGenerado
import com.adbrand.domain.model.RedSocial

// HU 10 y 12: contrato para probar la pantalla sin red.
interface AnuncioRepository {

    suspend fun generar(oferta: String, redSocial: RedSocial): Resultado<AnuncioGenerado>

    suspend fun regenerar(id: Long): Resultado<AnuncioGenerado>
}
