package com.adbrand.data.remote

import com.adbrand.core.network.ClienteApi
import com.adbrand.core.network.RespuestaHttp
import com.adbrand.domain.model.Empresa
import org.json.JSONObject

// HU 41: GET y PUT /api/cuenta/empresa. El usuario sale del Bearer, no de la URL.
object CuentaApi {

    fun obtener(): Result<RespuestaHttp> = ClienteApi.ejecutar("GET", "cuenta/empresa")

    fun guardar(empresa: Empresa): Result<RespuestaHttp> {
        val cuerpo = JSONObject()
            .put("ruc", empresa.ruc)
            .put("razonSocial", empresa.razonSocial)
            .put("telefono", if (empresa.telefono.isBlank()) JSONObject.NULL else empresa.telefono)
            .put("direccion", empresa.direccion)
            .toString()
        return ClienteApi.ejecutar("PUT", "cuenta/empresa", cuerpo)
    }
}
