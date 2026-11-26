package com.adbrand.domain.model

// IA de imágenes y opciones (rama de Jose): una de las 4 propuestas de anuncio.
// "imagen" llega como data URL lista para mostrarse; los indicadores "simulado"
// avisan cuando el contenido vino del modo de prueba y no de la IA real.
data class OpcionAnuncio(
    val id: Long = 0,
    val numero: Int = 0,
    val estilo: String = "",
    val texto: String = "",
    val imagen: String = "",
    val proveedorImagen: String = "",
    val imagenSimulada: Boolean = false,
    val textoSimulado: Boolean = false,
    val elegida: Boolean = false,
    // Ajuste 14: el anuncio ya diseñado (foto + titular + precio + botón + marca). null si no llegó.
    val diseno: String? = null,
    // HU 67: titular corto (3 a 6 palabras) y enfoque de la opción (Precio, Antojo, Confianza, Urgencia).
    val titular: String? = null,
    val angulo: String? = null
)

// Las 4 opciones que devuelve "Generar → Elegir".
data class OpcionesAnuncio(
    val generacionId: Long? = null,
    val oferta: String = "",
    val redSocial: RedSocial = RedSocial.INSTAGRAM,
    val opciones: List<OpcionAnuncio> = emptyList(),
    // HU 67: por qué la foto se usó tal cual cuando se pidió una escena (plan gratis, tope o falla); null si no.
    val avisoFoto: String? = null
)

// Al elegir una opción se guarda el anuncio (ya pasa por el filtro de la HU 13).
data class OpcionElegida(
    val anuncio: AnuncioGenerado = AnuncioGenerado(),
    val opcion: OpcionAnuncio = OpcionAnuncio()
)

// Una versión del historial de retoques: v1 es la imagen original.
data class VersionAnuncio(
    val numero: Int = 1,
    val instruccion: String = "",
    val imagen: String = "",
    val proveedorImagen: String = "",
    val imagenSimulada: Boolean = false,
    val actual: Boolean = false
)

// Historial completo de la opción elegida. retoquesRestantes es null en modo prueba.
data class VersionesAnuncio(
    val opcionId: Long = 0,
    val versionActual: Int = 1,
    val imagenActual: String = "",
    val retoquesRestantes: Int? = null,
    val versiones: List<VersionAnuncio> = emptyList()
)
