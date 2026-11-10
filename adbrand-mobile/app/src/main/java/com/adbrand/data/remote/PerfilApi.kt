package com.adbrand.data.remote

import com.adbrand.core.network.ClienteApi
import com.adbrand.core.network.RespuestaHttp
import com.adbrand.domain.model.PerfilNegocio
import org.json.JSONObject

// HU 44: GET y PUT /api/negocio/perfil.
object PerfilApi {

    suspend fun obtener(): Result<RespuestaHttp> = ClienteApi.ejecutarAsync("GET", "negocio/perfil")

    suspend fun guardar(perfil: PerfilNegocio): Result<RespuestaHttp> {
        val cuerpo = JSONObject()
            .put("nombreComercial", perfil.nombreComercial)
            .put("rubro", perfil.rubro)
            .put("publicoObjetivo", perfil.publicoObjetivo)
            .put("tono", perfil.tono.name)
            .put("descripcion", if (perfil.descripcion.isBlank()) JSONObject.NULL else perfil.descripcion)
            .toString()
        return ClienteApi.ejecutarAsync("PUT", "negocio/perfil", cuerpo)
    }
}
