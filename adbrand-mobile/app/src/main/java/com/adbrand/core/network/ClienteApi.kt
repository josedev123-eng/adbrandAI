package com.adbrand.core.network

import com.adbrand.BuildConfig
import com.adbrand.core.session.GestorSesion
import com.adbrand.data.remote.RenovadorSesionRemoto
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

// HU 42: un cliente HTTP configurable para usar la PC desde el emulador o por USB en un móvil.
object ClienteApi {

    val URL_BASE: String = BuildConfig.API_BASE_URL
    private val JSON = "application/json; charset=utf-8".toMediaType()

    // Cliente sin sesión: lo usa la renovación para no entrar en un ciclo de 401.
    val clienteSinSesion: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    // Cliente de la app: agrega el Bearer y renueva la sesión automáticamente ante un 401 (HU 43).
    val cliente: OkHttpClient by lazy {
        clienteSinSesion.newBuilder()
            .addInterceptor(GestorSesion.interceptorToken())
            .authenticator(AutenticadorRenovacion(RenovadorSesionRemoto(), GestorSesion))
            .build()
    }

    fun url(ruta: String): String = URL_BASE + ruta.trimStart('/')

    // Ejecuta una petición y devuelve el cuerpo como texto. Los errores de red se convierten en Error.
    fun ejecutar(
        metodo: String,
        ruta: String,
        cuerpo: String? = null,
        clienteHttp: OkHttpClient = cliente
    ): Result<RespuestaHttp> = try {
        val constructor = Request.Builder().url(url(ruta))
        when (metodo.uppercase()) {
            "GET" -> constructor.get()
            "POST" -> constructor.post((cuerpo ?: "{}").toRequestBody(JSON))
            "PUT" -> constructor.put((cuerpo ?: "{}").toRequestBody(JSON))
            "DELETE" -> constructor.delete()
            else -> error("Método HTTP no soportado: $metodo")
        }
        clienteHttp.newCall(constructor.build()).execute().use { respuesta ->
            Result.success(RespuestaHttp(respuesta.code, respuesta.body?.string().orEmpty()))
        }
    } catch (error: IOException) {
        Result.failure(error)
    }

    suspend fun ejecutarAsync(
        metodo: String,
        ruta: String,
        cuerpo: String? = null,
        clienteHttp: OkHttpClient = cliente
    ): Result<RespuestaHttp> = withContext(Dispatchers.IO) {
        ejecutar(metodo, ruta, cuerpo, clienteHttp)
    }
}
