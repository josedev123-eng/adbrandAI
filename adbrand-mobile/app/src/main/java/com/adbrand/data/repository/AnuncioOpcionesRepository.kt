package com.adbrand.data.repository

import com.adbrand.core.network.Resultado
import com.adbrand.domain.model.OpcionElegida
import com.adbrand.domain.model.OpcionesAnuncio
import com.adbrand.domain.model.RedSocial
import com.adbrand.domain.model.VersionesAnuncio

// IA de imágenes y opciones (rama de Jose): contrato para probar la pantalla sin red.
interface AnuncioOpcionesRepository {

    suspend fun generar(oferta: String, redSocial: RedSocial): Resultado<OpcionesAnuncio>

    // HU 67: con la foto del producto (data URL JPG o PNG). Por defecto la ignora (para las pruebas sin red).
    suspend fun generar(oferta: String, redSocial: RedSocial, foto: String?): Resultado<OpcionesAnuncio> =
        generar(oferta, redSocial)

    suspend fun elegir(opcionId: Long): Resultado<OpcionElegida>

    suspend fun retocar(opcionId: Long, instruccion: String): Resultado<VersionesAnuncio>

    suspend fun versiones(opcionId: Long): Resultado<VersionesAnuncio>

    suspend fun usarVersion(opcionId: Long, numero: Int): Resultado<VersionesAnuncio>
}
