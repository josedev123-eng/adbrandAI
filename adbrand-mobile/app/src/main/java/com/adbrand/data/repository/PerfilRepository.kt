package com.adbrand.data.repository

import com.adbrand.core.network.Resultado
import com.adbrand.domain.model.PerfilNegocio

// HU 44: contrato para probar el ViewModel sin red.
interface PerfilRepository {

    suspend fun obtener(): Resultado<PerfilNegocio>

    suspend fun guardar(perfil: PerfilNegocio): Resultado<PerfilNegocio>
}
