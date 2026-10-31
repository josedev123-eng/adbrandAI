package com.adbrand.data.repository

import com.adbrand.core.network.Resultado
import com.adbrand.domain.model.Empresa

// HU 41: contrato para que el ViewModel se pruebe con datos falsos.
interface CuentaRepository {

    suspend fun obtener(): Resultado<Empresa>

    suspend fun guardar(empresa: Empresa): Resultado<Empresa>
}
