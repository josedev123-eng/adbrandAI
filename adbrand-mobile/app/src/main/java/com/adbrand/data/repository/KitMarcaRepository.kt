package com.adbrand.data.repository

import com.adbrand.core.network.ClienteApi
import com.adbrand.core.network.Resultado
import com.adbrand.domain.model.KitMarca
import com.adbrand.domain.model.LogoMarca
import java.io.IOException
import org.json.JSONArray
import org.json.JSONObject

// Lo que la empresa responde en el cuestionario de identidad (V70). Todo opcional menos nombre, rubro y estilo.
data class DatosKitMarca(
    val nombreNegocio: String,
    val sector: String,
    val estiloVisual: String,
    val paleta: String = "",
    val sentimiento: String = "",
    val diferencial: String = "",
    val eslogan: String = ""
)

// Kit de marca desde el celular, con los mismos endpoints que la web:
// GET /api/kit-marca, POST /api/kit-marca/generar y, de la identidad de marca, logo/opciones, logo-imagen,
// eslogan y manual (PDF).
interface KitMarcaRepository {
    suspend fun actual(): Resultado<KitMarca?>
    suspend fun generar(datos: DatosKitMarca): Resultado<KitMarca>
    suspend fun proponerLogos(kitId: Long): Resultado<List<String>>
    suspend fun guardarLogo(kitId: Long, dataUrl: String, origen: String): Resultado<KitMarca>
    suspend fun elegirEslogan(kitId: Long, eslogan: String): Resultado<KitMarca>
    suspend fun manualPdf(kitId: Long): Resultado<ByteArray>
}

class KitMarcaRepositoryRemoto : KitMarcaRepository {

    override suspend fun actual(): Resultado<KitMarca?> {
        val respuesta = ClienteApi.ejecutarAsync("GET", "kit-marca").getOrElse { return AnuncioJson.errorDeRed(it) }
        if (!respuesta.esExitosa) return AnuncioJson.error(respuesta, "No se pudo cargar tu kit de marca.")
        return leer {
            val lista = JSONArray(respuesta.cuerpo)
            // El más reciente es el de id mayor.
            val kits = (0 until lista.length()).map { kitDesde(lista.getJSONObject(it)) }
            kits.maxByOrNull { it.id ?: 0L }
        }
    }

    override suspend fun generar(datos: DatosKitMarca): Resultado<KitMarca> {
        val cuerpo = JSONObject()
            .put("nombreNegocio", datos.nombreNegocio.trim())
            .put("sector", datos.sector.trim())
            .put("estiloVisual", datos.estiloVisual)
            .put("paletaSugerida", datos.paleta.trim())
            .put("sentimiento", datos.sentimiento.trim())
            .put("diferencial", datos.diferencial.trim())
            .put("eslogan", datos.eslogan.trim())
            .toString()
        val respuesta = ClienteApi.ejecutarAsync("POST", "kit-marca/generar", cuerpo, ClienteApi.clienteIa)
            .getOrElse { return AnuncioJson.errorDeRed(it) }
        if (!respuesta.esExitosa) return AnuncioJson.error(respuesta, "No se pudo generar tu kit de marca.")
        return leer { kitDesde(JSONObject(respuesta.cuerpo)) }
    }

    override suspend fun proponerLogos(kitId: Long): Resultado<List<String>> {
        val respuesta = ClienteApi.ejecutarAsync("POST", "kit-marca/$kitId/logo/opciones", "{}", ClienteApi.clienteIa)
            .getOrElse { return AnuncioJson.errorDeRed(it) }
        if (!respuesta.esExitosa) return AnuncioJson.error(respuesta, "No se pudieron crear los logos.")
        return leer {
            val opciones = JSONObject(respuesta.cuerpo).optJSONArray("opciones") ?: JSONArray()
            (0 until opciones.length()).map { opciones.getString(it) }
        }
    }

    override suspend fun guardarLogo(kitId: Long, dataUrl: String, origen: String): Resultado<KitMarca> {
        val cuerpo = JSONObject().put("imagen", dataUrl).put("origen", origen).toString()
        val respuesta = ClienteApi.ejecutarAsync("PUT", "kit-marca/$kitId/logo-imagen", cuerpo)
            .getOrElse { return AnuncioJson.errorDeRed(it) }
        if (!respuesta.esExitosa) return AnuncioJson.error(respuesta, "No se pudo guardar el logo.")
        return leer { kitDesde(JSONObject(respuesta.cuerpo)) }
    }

    override suspend fun elegirEslogan(kitId: Long, eslogan: String): Resultado<KitMarca> {
        val cuerpo = JSONObject().put("eslogan", eslogan.trim()).toString()
        val respuesta = ClienteApi.ejecutarAsync("PUT", "kit-marca/$kitId/eslogan", cuerpo)
            .getOrElse { return AnuncioJson.errorDeRed(it) }
        if (!respuesta.esExitosa) return AnuncioJson.error(respuesta, "No se pudo guardar el eslogan.")
        return leer { kitDesde(JSONObject(respuesta.cuerpo)) }
    }

    override suspend fun manualPdf(kitId: Long): Resultado<ByteArray> {
        val archivo = ClienteApi.descargarAsync("kit-marca/$kitId/manual").getOrElse {
            return Resultado.Error(
                if (it is IOException) "No se pudo conectar con el servidor. Revisa tu conexión." else "Ocurrió un error inesperado."
            )
        }
        if (!archivo.esExitosa) {
            val mensaje = runCatching { JSONObject(archivo.texto).optString("mensaje") }.getOrNull()
            return Resultado.Error(mensaje?.takeIf { it.isNotBlank() } ?: "No se pudo descargar el manual.", archivo.codigo)
        }
        return Resultado.Exito(archivo.bytes)
    }

    private inline fun <T> leer(bloque: () -> T): Resultado<T> =
        runCatching { Resultado.Exito(bloque()) }
            .getOrElse { Resultado.Error("La respuesta del servidor no tiene el formato esperado.") }

    private fun kitDesde(json: JSONObject): KitMarca {
        val logo = json.optJSONObject("logoDiseno")?.let { diseno ->
            LogoMarca(
                texto = AnuncioJson.texto(diseno, "texto"),
                forma = AnuncioJson.texto(diseno, "forma").ifBlank { "REDONDEADO" },
                colorFondo = AnuncioJson.texto(diseno, "colorFondo").ifBlank { "#4F46E5" },
                colorTexto = AnuncioJson.texto(diseno, "colorTexto").ifBlank { "#FFFFFF" },
                tipografia = AnuncioJson.texto(diseno, "tipografia")
            )
        }
        val opciones = json.optJSONArray("esloganOpciones")?.let { lista ->
            (0 until lista.length()).map { lista.getString(it) }
        }.orEmpty()
        return KitMarca(
            id = json.optLong("id").takeIf { it > 0 },
            nombreNegocio = AnuncioJson.texto(json, "nombreNegocio"),
            sector = AnuncioJson.texto(json, "sector"),
            logoConcepto = AnuncioJson.texto(json, "logoConcepto"),
            tipografiaPrimaria = AnuncioJson.texto(json, "tipografiaPrimaria"),
            tipografiaSecundaria = AnuncioJson.texto(json, "tipografiaSecundaria"),
            paletaColores = AnuncioJson.texto(json, "paletaColores"),
            vozMarca = AnuncioJson.texto(json, "vozMarca"),
            logoDiseno = logo,
            eslogan = AnuncioJson.texto(json, "eslogan"),
            esloganOpciones = opciones,
            logoImagen = AnuncioJson.texto(json, "logoImagen").ifBlank { null },
            logoOrigen = AnuncioJson.texto(json, "logoOrigen").ifBlank { null }
        )
    }
}
