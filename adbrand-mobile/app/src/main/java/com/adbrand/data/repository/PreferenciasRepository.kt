package com.adbrand.data.repository

import com.adbrand.core.network.Resultado
import com.adbrand.domain.model.CampoPreferencia
import com.adbrand.domain.model.PreferenciasAvisos

// HU 51: contrato para probar la pantalla de avisos sin red.
interface PreferenciasRepository {

    suspend fun obtener(): Resultado<PreferenciasAvisos>

    suspend fun actualizar(campo: CampoPreferencia, valor: Boolean): Resultado<PreferenciasAvisos>
}
