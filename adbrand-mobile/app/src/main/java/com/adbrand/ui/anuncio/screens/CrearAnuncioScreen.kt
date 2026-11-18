package com.adbrand.ui.anuncio.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.adbrand.core.theme.AmbarOscuro
import com.adbrand.core.theme.AmbarSuave
import com.adbrand.core.theme.Blanco
import com.adbrand.core.theme.BordeSuave
import com.adbrand.core.theme.FondoClaro
import com.adbrand.core.theme.IndigoAdbrand
import com.adbrand.core.theme.IndigoClaro
import com.adbrand.core.theme.IndigoOscuro
import com.adbrand.core.theme.SuperficieBlanca
import com.adbrand.core.theme.TextoPrincipal
import com.adbrand.core.theme.TextoSecundario
import com.adbrand.core.theme.VioletaAdbrand
import com.adbrand.core.theme.VioletaClaro
import com.adbrand.domain.model.AnuncioGenerado
import com.adbrand.domain.model.OpcionAnuncio
import com.adbrand.domain.model.RedSocial
import com.adbrand.ui.anuncio.components.ImagenIa
import com.adbrand.ui.anuncio.viewmodel.CrearAnuncioEstado
import com.adbrand.ui.anuncio.viewmodel.CrearAnuncioViewModel
import com.adbrand.ui.anuncio.viewmodel.MAXIMO_INSTRUCCION
import com.adbrand.ui.anuncio.viewmodel.MAXIMO_OFERTA
import com.adbrand.ui.components.AvisoEstado
import com.adbrand.ui.components.BotonPrincipal
import com.adbrand.ui.components.BotonSecundario
import com.adbrand.ui.components.CampoAdbrand
import com.adbrand.ui.components.ChipSuave
import com.adbrand.ui.components.EncabezadoPantalla
import com.adbrand.ui.components.EntradaAnimada
import com.adbrand.ui.components.Esqueleto
import com.adbrand.ui.components.IconoDestacado
import com.adbrand.ui.components.PantallaScroll
import com.adbrand.ui.components.TarjetaSeccion
import com.adbrand.ui.components.TipoAviso

// HU 10, 12 y 13 + IA de imágenes: la IA propone 4 anuncios con texto e imagen,
// el dueño elige uno y desde ahí puede copiarlo o regenerar el texto.
@Composable
fun CrearAnuncioScreen(
    alIrAPerfil: () -> Unit = {},
    viewModel: CrearAnuncioViewModel = viewModel()
) {
    val estado by viewModel.estado.collectAsState()
    val contexto = LocalContext.current
    val portapapeles = remember(contexto) {
        contexto.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    }

    PantallaScroll {
        Spacer(modifier = Modifier.height(14.dp))

        EntradaAnimada(indice = 0) {
            EncabezadoPantalla(
                titulo = "Crear anuncio",
                subtitulo = "Escribe tu oferta y la IA te propone 4 opciones con texto e imagen."
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        if (estado.faltaPerfil) {
            EntradaAnimada(indice = 1) {
                Column {
                    AvisoEstado(
                        texto = "Primero completa el perfil de tu negocio para que la IA sepa cómo escribir.",
                        tipo = TipoAviso.ERROR
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    BotonSecundario(
                        texto = "Completar mi perfil",
                        onClick = alIrAPerfil,
                        icono = Icons.Filled.Person
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                }
            }
        }

        estado.aviso?.let { mensaje ->
            EntradaAnimada(indice = 1) {
                Column {
                    AvisoEstado(texto = mensaje, tipo = TipoAviso.ERROR)
                    Spacer(modifier = Modifier.height(14.dp))
                }
            }
        }

        estado.mensajeExito?.let { mensaje ->
            EntradaAnimada(indice = 1) {
                Column {
                    AvisoEstado(texto = mensaje, tipo = TipoAviso.EXITO)
                    Spacer(modifier = Modifier.height(14.dp))
                }
            }
        }

        EntradaAnimada(indice = 2) {
            TarjetaSeccion(
                titulo = "Tu oferta",
                subtitulo = "¿Qué quieres anunciar?",
                icono = Icons.Filled.Edit
            ) {
                CampoAdbrand(
                    valor = estado.oferta,
                    alCambiar = viewModel::actualizarOferta,
                    etiqueta = "Oferta",
                    marcador = "2 panetones por S/ 35 hasta el 24 de diciembre",
                    ayuda = "${estado.oferta.length} / $MAXIMO_OFERTA",
                    error = estado.errorOferta,
                    unaLinea = false,
                    lineasMinimas = 4,
                    accionIme = ImeAction.Done
                )

                Text(
                    text = "Red social",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextoSecundario
                )

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    RedSocial.entries.forEach { red ->
                        TarjetaRedSocial(
                            red = red,
                            seleccionado = estado.redSocial == red,
                            modifier = Modifier.weight(1f),
                            alElegir = { viewModel.actualizarRedSocial(red) }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        EntradaAnimada(indice = 3) {
            BotonPrincipal(
                texto = if (estado.generando) "Creando 4 opciones..." else "Generar 4 opciones",
                onClick = viewModel::generar,
                cargando = estado.generando,
                habilitado = estado.eligiendoId == null,
                icono = Icons.Filled.AutoAwesome
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        when {
            estado.generando -> EntradaAnimada(indice = 4) {
                TarjetaOpcionesCargando()
                Spacer(modifier = Modifier.height(18.dp))
            }

            estado.hayOpciones -> EntradaAnimada(indice = 4) {
                TarjetaOpciones(
                    estado = estado,
                    alElegir = viewModel::elegir
                )
                Spacer(modifier = Modifier.height(18.dp))
            }
        }

        EntradaAnimada(indice = 5) {
            TarjetaResultado(
                estado = estado,
                alCopiar = {
                    estado.anuncio?.let { anuncio ->
                        portapapeles?.setPrimaryClip(
                            ClipData.newPlainText("Anuncio AdBrand", anuncio.texto)
                        )
                        viewModel.marcarCopiado()
                    }
                },
                alRegenerar = viewModel::regenerar,
                alNuevaVersion = viewModel::generar,
                alCambiarInstruccion = viewModel::actualizarInstruccion,
                alRetocar = viewModel::retocar,
                alUsarVersion = viewModel::volverAVersion
            )
        }

        Spacer(modifier = Modifier.height(28.dp))
    }
}

// ---------------------------------------------------------------------------
// Las 4 opciones propuestas
// ---------------------------------------------------------------------------

@Composable
private fun TarjetaOpciones(
    estado: CrearAnuncioEstado,
    alElegir: (Long) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(VioletaClaro),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.AutoAwesome,
                    contentDescription = null,
                    tint = VioletaAdbrand,
                    modifier = Modifier.size(19.dp)
                )
            }
            Column {
                Text(
                    text = "Elige tu opción favorita",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextoPrincipal
                )
                Text(
                    text = "Cada propuesta tiene su texto y su estilo de imagen.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextoSecundario
                )
            }
        }

        estado.opciones.forEach { opcion ->
            TarjetaOpcion(
                opcion = opcion,
                eligiendo = estado.eligiendoId == opcion.id,
                habilitado = estado.eligiendoId == null,
                alElegir = { alElegir(opcion.id) }
            )
        }
    }
}

@Composable
private fun TarjetaOpcion(
    opcion: OpcionAnuncio,
    eligiendo: Boolean,
    habilitado: Boolean,
    alElegir: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = SuperficieBlanca,
        border = BorderStroke(
            width = if (opcion.elegida) 1.5.dp else 1.dp,
            color = if (opcion.elegida) IndigoAdbrand else BordeSuave
        )
    ) {
        Column {
            Box {
                ImagenIa(
                    dataUrl = opcion.imagen,
                    etiqueta = "Opción ${opcion.numero}: ${opcion.estilo}",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(172.dp)
                )

                Row(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ChipSuave(
                        texto = "Opción ${opcion.numero}",
                        colorTexto = Blanco,
                        colorFondo = IndigoOscuro.copy(alpha = 0.82f)
                    )
                    ChipSuave(
                        texto = opcion.estilo,
                        colorTexto = IndigoOscuro,
                        colorFondo = SuperficieBlanca.copy(alpha = 0.92f)
                    )
                }

                if (opcion.elegida) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(10.dp)
                    ) {
                        ChipSuave(
                            texto = "Elegida",
                            colorTexto = Blanco,
                            colorFondo = IndigoAdbrand
                        )
                    }
                }

                if (eligiendo) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(IndigoOscuro.copy(alpha = 0.45f)),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(26.dp),
                            strokeWidth = 2.5.dp,
                            color = Blanco
                        )
                    }
                }
            }

            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = opcion.texto,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextoPrincipal
                )

                if (opcion.imagenSimulada || opcion.textoSimulado) {
                    ChipSuave(
                        texto = "Contenido de prueba",
                        colorTexto = AmbarOscuro,
                        colorFondo = AmbarSuave
                    )
                }

                if (opcion.elegida) {
                    BotonSecundario(
                        texto = "Elegida",
                        onClick = {},
                        icono = Icons.Filled.Check,
                        habilitado = false
                    )
                } else {
                    BotonPrincipal(
                        texto = if (eligiendo) "Guardando..." else "Elegir esta",
                        onClick = alElegir,
                        cargando = eligiendo,
                        habilitado = habilitado,
                        icono = Icons.Filled.Check
                    )
                }
            }
        }
    }
}

@Composable
private fun TarjetaOpcionesCargando() {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(VioletaClaro),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.AutoAwesome,
                    contentDescription = null,
                    tint = VioletaAdbrand,
                    modifier = Modifier.size(19.dp)
                )
            }
            Column {
                Text(
                    text = "La IA está creando 4 propuestas",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextoPrincipal
                )
                Text(
                    text = "Escribe los textos y dibuja una imagen para cada una.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextoSecundario
                )
            }
        }

        repeat(2) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = SuperficieBlanca,
                border = BorderStroke(1.dp, BordeSuave)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Esqueleto(
                        modifier = Modifier.fillMaxWidth(),
                        altura = 150.dp,
                        forma = RoundedCornerShape(14.dp)
                    )
                    Esqueleto(modifier = Modifier.fillMaxWidth())
                    Esqueleto(modifier = Modifier.fillMaxWidth(0.72f))
                }
            }
        }

        Text(
            text = "La IA está escribiendo los textos y dibujando las imágenes...",
            style = MaterialTheme.typography.bodySmall,
            color = TextoSecundario,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

// ---------------------------------------------------------------------------
// Resultado
// ---------------------------------------------------------------------------

@Composable
private fun TarjetaResultado(
    estado: CrearAnuncioEstado,
    alCopiar: () -> Unit,
    alRegenerar: () -> Unit,
    alNuevaVersion: () -> Unit,
    alCambiarInstruccion: (String) -> Unit,
    alRetocar: () -> Unit,
    alUsarVersion: (Int) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = SuperficieBlanca,
        border = BorderStroke(1.dp, BordeSuave),
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Tu anuncio",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextoPrincipal
                )
                Spacer(modifier = Modifier.weight(1f))
                if (estado.anuncio != null) {
                    ChipSuave(texto = "Generado con IA")
                }
            }

            // Mientras regenera se conserva el texto anterior para no perder el contexto.
            val fase = when {
                estado.generando -> "cargando"
                estado.anuncio == null -> "vacio"
                else -> "anuncio"
            }

            AnimatedContent(
                targetState = fase,
                transitionSpec = {
                    (fadeIn(tween(260)) + scaleIn(initialScale = 0.97f)) togetherWith fadeOut(tween(120))
                },
                label = "resultado-anuncio"
            ) { etapa ->
                when (etapa) {
                    "cargando" -> EstadoEscribiendo()
                    "vacio" -> EstadoSinAnuncio(hayOpciones = estado.hayOpciones)
                    else -> estado.anuncio?.let { anuncio ->
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            estado.opcionElegida?.let { opcion ->
                                ImagenIa(
                                    dataUrl = opcion.imagen,
                                    etiqueta = "Imagen del anuncio (${opcion.estilo})",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(190.dp)
                                )

                                if (estado.retocando) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            strokeWidth = 2.dp,
                                            color = VioletaAdbrand
                                        )
                                        Spacer(modifier = Modifier.size(8.dp))
                                        Text(
                                            text = "La IA está retocando tu imagen...",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextoSecundario
                                        )
                                    }
                                }

                                FormularioRetoque(
                                    estado = estado,
                                    alCambiarInstruccion = alCambiarInstruccion,
                                    alRetocar = alRetocar
                                )

                                ListaVersiones(
                                    estado = estado,
                                    alUsarVersion = alUsarVersion
                                )
                            }
                            ContenidoAnuncio(
                                anuncio = anuncio,
                                copiado = estado.copiado,
                                regenerando = estado.regenerando,
                                alCopiar = alCopiar,
                                alRegenerar = alRegenerar,
                                alNuevaVersion = alNuevaVersion
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FormularioRetoque(
    estado: CrearAnuncioEstado,
    alCambiarInstruccion: (String) -> Unit,
    alRetocar: () -> Unit
) {
    val restantes = estado.historial?.retoquesRestantes
    val ayuda = when {
        estado.errorInstruccion != null -> null
        restantes != null -> "Te quedan $restantes retoques este mes."
        else -> "${estado.instruccion.length} / $MAXIMO_INSTRUCCION"
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "¿Qué quieres cambiar de la imagen?",
            style = MaterialTheme.typography.labelMedium,
            color = TextoSecundario
        )
        CampoAdbrand(
            valor = estado.instruccion,
            alCambiar = alCambiarInstruccion,
            etiqueta = "Retoque",
            marcador = "Ej.: pon el fondo más claro",
            ayuda = ayuda,
            error = estado.errorInstruccion,
            habilitado = !estado.retocando,
            unaLinea = true,
            accionIme = ImeAction.Done
        )
        BotonPrincipal(
            texto = if (estado.retocando) "Retocando..." else "Retocar imagen",
            onClick = alRetocar,
            cargando = estado.retocando,
            habilitado = estado.instruccion.isNotBlank(),
            icono = Icons.Filled.AutoAwesome
        )
    }
}

@Composable
private fun ListaVersiones(
    estado: CrearAnuncioEstado,
    alUsarVersion: (Int) -> Unit
) {
    val historial = estado.historial ?: return
    if (historial.versiones.size <= 1) return

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Versiones de la imagen",
            style = MaterialTheme.typography.labelMedium,
            color = TextoSecundario
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            historial.versiones.forEach { version ->
                val seleccionada = version.actual
                Column(
                    modifier = Modifier
                        .width(96.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(
                            enabled = !seleccionada && !estado.retocando,
                            onClick = { alUsarVersion(version.numero) }
                        )
                        .padding(4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    ImagenIa(
                        dataUrl = version.imagen,
                        etiqueta = "Versión ${version.numero}",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp)
                    )
                    Text(
                        text = if (seleccionada) "v${version.numero} · actual" else "v${version.numero}",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (seleccionada) IndigoAdbrand else TextoSecundario,
                        fontWeight = if (seleccionada) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }
}

@Composable
private fun ContenidoAnuncio(
    anuncio: AnuncioGenerado,
    copiado: Boolean,
    regenerando: Boolean,
    alCopiar: () -> Unit,
    alRegenerar: () -> Unit,
    alNuevaVersion: () -> Unit
) {
    if (anuncio.enRevision) {
        AvisoRevision(anuncio.motivoRevision)
    }

    Crossfade(targetState = anuncio.texto, label = "texto-anuncio") { texto ->
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(FondoClaro)
                .padding(16.dp)
        ) {
            Text(
                text = texto,
                style = MaterialTheme.typography.bodyLarge,
                color = TextoPrincipal
            )
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ChipSuave(texto = "Tono ${anuncio.tono.etiqueta.lowercase()}")
        ChipSuave(texto = anuncio.redSocial.etiqueta)
        if (anuncio.simulado) {
            ChipSuave(
                texto = "Simulado",
                colorTexto = AmbarOscuro,
                colorFondo = AmbarSuave
            )
        }
    }

    // HU 13: un anuncio en revisión no se puede copiar todavía.
    if (!anuncio.enRevision) {
        BotonPrincipal(
            texto = if (copiado) "¡Copiado!" else "Copiar texto",
            onClick = alCopiar,
            habilitado = !regenerando,
            icono = if (copiado) Icons.Filled.Check else Icons.Filled.Share
        )
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        BotonSecundario(
            texto = "Regenerar",
            onClick = alRegenerar,
            modifier = Modifier.weight(1f),
            icono = Icons.Filled.Refresh,
            habilitado = !regenerando
        )
        BotonSecundario(
            texto = "Nueva versión",
            onClick = alNuevaVersion,
            modifier = Modifier.weight(1f),
            icono = Icons.Filled.Add,
            habilitado = !regenerando
        )
    }

    if (regenerando) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                strokeWidth = 2.dp,
                color = IndigoAdbrand
            )
            Spacer(modifier = Modifier.size(8.dp))
            Text(
                text = "Regenerando tu anuncio...",
                style = MaterialTheme.typography.bodySmall,
                color = TextoSecundario
            )
        }
    }
}

@Composable
private fun AvisoRevision(motivo: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = AmbarSuave
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = Icons.Filled.Warning,
                contentDescription = null,
                tint = AmbarOscuro,
                modifier = Modifier.size(20.dp)
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Tu anuncio quedó en revisión",
                    style = MaterialTheme.typography.labelLarge,
                    color = AmbarOscuro,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "El filtro automático encontró algo que un moderador debe revisar antes de que puedas publicarlo.",
                    style = MaterialTheme.typography.bodySmall,
                    color = AmbarOscuro
                )
                if (motivo.isNotBlank()) {
                    Text(
                        text = "Motivo: $motivo",
                        style = MaterialTheme.typography.bodySmall,
                        color = AmbarOscuro,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun EstadoEscribiendo() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 26.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        CircularProgressIndicator(color = IndigoAdbrand)
        Text(
            text = "La IA está preparando tus propuestas...",
            style = MaterialTheme.typography.bodyMedium,
            color = TextoSecundario
        )
    }
}

@Composable
private fun EstadoSinAnuncio(hayOpciones: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        IconoDestacado(icono = Icons.Filled.AutoAwesome)
        Text(
            text = if (hayOpciones) "Elige una de las 4 opciones" else "Aquí aparecerá tu anuncio",
            style = MaterialTheme.typography.titleMedium,
            color = TextoPrincipal
        )
        Text(
            text = if (hayOpciones) {
                "Cuando elijas, verás aquí el anuncio con su imagen y su texto listo para copiar."
            } else {
                "Escribe tu oferta, genera 4 propuestas y quédate con la que más te guste."
            },
            style = MaterialTheme.typography.bodySmall,
            color = TextoSecundario,
            textAlign = TextAlign.Center
        )
    }
}

// ---------------------------------------------------------------------------
// Selector de red social
// ---------------------------------------------------------------------------

@Composable
private fun RowScope.TarjetaRedSocial(
    red: RedSocial,
    seleccionado: Boolean,
    modifier: Modifier = Modifier,
    alElegir: () -> Unit
) {
    Surface(
        modifier = modifier.clickable(onClick = alElegir),
        shape = RoundedCornerShape(16.dp),
        color = if (seleccionado) IndigoClaro else SuperficieBlanca,
        border = BorderStroke(
            width = if (seleccionado) 1.5.dp else 1.dp,
            color = if (seleccionado) IndigoAdbrand else BordeSuave
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (seleccionado) IndigoAdbrand else IndigoClaro),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = red.iniciales,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (seleccionado) Blanco else IndigoOscuro,
                    fontWeight = FontWeight.ExtraBold
                )
            }
            Text(
                text = red.etiqueta,
                style = MaterialTheme.typography.labelLarge,
                color = if (seleccionado) IndigoOscuro else TextoPrincipal
            )
            Spacer(modifier = Modifier.weight(1f))
            if (seleccionado) {
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(IndigoAdbrand),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = null,
                        tint = Blanco,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }
    }
}
