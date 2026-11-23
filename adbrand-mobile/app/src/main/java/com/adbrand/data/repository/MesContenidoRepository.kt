package com.adbrand.data.repository

import com.adbrand.core.network.ClienteApi
import com.adbrand.core.network.Resultado
import com.adbrand.domain.model.FechaSugerida
import java.time.LocalDate
import org.json.JSONObject

// HU 63: "Tu mes de contenido" (GET /api/calendario/mes-de-contenido). Fechas peruanas con idea según el rubro.
interface MesContenidoRepository {
    // Las próximas fechas que vienen (de este mes y del siguiente), sin las que ya pasaron.
    suspend fun proximas(cantidad: Int = 3): Resultado<List<FechaSugerida>>
}

class MesContenidoRepositoryRemoto : MesContenidoRepository {

    override suspend fun proximas(cantidad: Int): Resultado<List<FechaSugerida>> {
        val hoy = LocalDate.now()
        val todas = mutableListOf<FechaSugerida>()
        for (dia in listOf(hoy, hoy.plusMonths(1))) {
            val respuesta = ClienteApi.ejecutarAsync("GET", "calendario/mes-de-contenido?anio=${dia.year}&mes=${dia.monthValue}")
                .getOrElse { return AnuncioJson.errorDeRed(it) }
            if (!respuesta.esExitosa) return AnuncioJson.error(respuesta, "No se pudieron cargar las próximas fechas.")
            val lista = runCatching { JSONObject(respuesta.cuerpo).getJSONArray("fechas") }.getOrNull() ?: continue
            for (i in 0 until lista.length()) {
                val f = lista.getJSONObject(i)
                todas += FechaSugerida(
                    fecha = LocalDate.parse(f.getString("fecha")),
                    nombre = f.getString("nombre"),
                    idea = f.getString("idea"),
                    diasFaltan = f.getLong("diasFaltan")
                )
            }
        }
        return Resultado.Exito(
            todas.filter { it.diasFaltan >= 0 }.distinctBy { it.fecha to it.nombre }.sortedBy { it.fecha }.take(cantidad)
        )
    }
}
