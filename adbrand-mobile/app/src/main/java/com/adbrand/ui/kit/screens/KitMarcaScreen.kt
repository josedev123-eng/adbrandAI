package com.adbrand.ui.kit.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.adbrand.core.theme.BordeSuave
import com.adbrand.core.theme.IndigoAdbrand
import com.adbrand.core.theme.IndigoClaro
import com.adbrand.core.theme.SuperficieBlanca
import com.adbrand.core.theme.TextoPrincipal
import com.adbrand.core.theme.TextoSecundario
import com.adbrand.core.theme.VioletaAdbrand
import com.adbrand.core.theme.VioletaClaro
import com.adbrand.domain.model.KitMarca
import com.adbrand.ui.anuncio.components.ImagenIa
import com.adbrand.ui.components.AvisoEstado
import com.adbrand.ui.components.BotonPrincipal
import com.adbrand.ui.components.BotonSecundario
import com.adbrand.ui.components.CampoAdbrand
import com.adbrand.ui.components.EncabezadoPantalla
import com.adbrand.ui.components.PantallaScroll
import com.adbrand.ui.components.TipoAviso
import com.adbrand.ui.kit.viewmodel.ESTILOS_VISUALES
import com.adbrand.ui.kit.viewmodel.KitMarcaEstado
import com.adbrand.ui.kit.viewmodel.KitMarcaViewModel
import com.adbrand.ui.kit.viewmodel.SENTIMIENTOS
import com.adbrand.ui.metricas.screens.abrirPdf
import com.adbrand.ui.metricas.screens.compartirPdf
import com.adbrand.ui.perfil.screens.SelectorRubro
import com.adbrand.ui.previsualizacion.screens.LogoMarca
import com.adbrand.ui.previsualizacion.screens.coloresDe
import java.io.File

// Kit de marca desde el celular (igual que en la web): la IA crea logo, colores, letras y eslogan a partir de un
// cuestionario corto. Después, "Tu identidad": elegir eslogan, subir tu logo o elegir uno de la IA, y el manual en PDF.
@Composable
fun KitMarcaScreen(viewModel: KitMarcaViewModel = viewModel()) {
    val estado by viewModel.estado.collectAsState()

    LaunchedEffect(Unit) { viewModel.cargar() }

    PantallaScroll(espaciado = 14.dp) {
        Spacer(modifier = Modifier.height(6.dp))
        EncabezadoPantalla(
            titulo = "Kit de marca",
            subtitulo = "Tu logo, tu eslogan y tus colores. Tus anuncios los usan solos."
        )

        estado.exito?.let { AvisoEstado(it, tipo = TipoAviso.EXITO) }
        if (!estado.mostrarFormulario) estado.error?.let { AvisoEstado(it, tipo = TipoAviso.ERROR) }

        when {
            estado.cargando -> Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = IndigoAdbrand)
            }
            !estado.mostrarFormulario && estado.kit != null -> {
                TuIdentidad(estado, viewModel)
                TarjetaKit(estado.kit!!)
                BotonSecundario(texto = "Crear otro kit", onClick = viewModel::crearOtro, icono = Icons.Filled.AutoAwesome)
            }
            else -> Formulario(viewModel)
        }

        Spacer(modifier = Modifier.height(26.dp))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Formulario(viewModel: KitMarcaViewModel) {
    val estado by viewModel.estado.collectAsState()

    Tarjeta {
        Paso(1, "Tu negocio")
        CampoAdbrand(
            valor = estado.nombre,
            alCambiar = viewModel::cambiarNombre,
            etiqueta = "Nombre del negocio",
            icono = Icons.Filled.Store,
            marcador = "Ej.: Panadería Doña Rosa"
        )
        SelectorRubro(valor = estado.rubro, alCambiar = viewModel::cambiarRubro, error = null)

        Paso(2, "Lo que quieres transmitir")
        Text("¿Qué quieres que sienta tu cliente? (hasta 3)", style = MaterialTheme.typography.labelLarge, color = TextoPrincipal)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SENTIMIENTOS.forEach { sentimiento ->
                Chip(sentimiento, sentimiento in estado.sentimientos) { viewModel.alternarSentimiento(sentimiento) }
            }
        }
        CampoAdbrand(
            valor = estado.diferencial,
            alCambiar = viewModel::cambiarDiferencial,
            etiqueta = "¿Qué te hace diferente? (opcional)",
            marcador = "Ej.: pan recién horneado a las 6 am"
        )
        CampoAdbrand(
            valor = estado.esloganPropio,
            alCambiar = viewModel::cambiarEsloganPropio,
            etiqueta = "¿Ya tienes eslogan? (opcional)",
            ayuda = "Si lo dejas vacío, la IA te propone 3.",
            marcador = "Ej.: La casa del buen pan"
        )

        Paso(3, "¿Cómo quieres que se vea?")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ESTILOS_VISUALES.forEach { (valor, nombre) ->
                Chip(nombre, estado.estilo == valor) { viewModel.elegirEstilo(valor) }
            }
        }
        CampoAdbrand(
            valor = estado.colores,
            alCambiar = viewModel::cambiarColores,
            etiqueta = "Colores que te gustan (opcional)",
            icono = Icons.Filled.Palette,
            marcador = "Ej.: verde y dorado"
        )

        estado.error?.let { AvisoEstado(it, tipo = TipoAviso.ERROR) }
        if (estado.generando) {
            AvisoEstado("La IA está creando tu kit. Suele tardar 1 o 2 minutos; no cierres la app.")
        }
        BotonPrincipal(
            texto = if (estado.generando) "Creando tu kit…" else "Crear mi kit con IA",
            onClick = viewModel::generar,
            cargando = estado.generando,
            icono = Icons.Filled.AutoAwesome
        )
        if (estado.kit != null && !estado.generando) {
            TextButton(onClick = viewModel::cancelarOtro, modifier = Modifier.fillMaxWidth()) {
                Text("Volver a mi kit actual")
            }
        }
    }
}

// "Tu identidad": eslogan, logo (el tuyo o uno de la IA) y el manual de marca en PDF.
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TuIdentidad(estado: KitMarcaEstado, viewModel: KitMarcaViewModel) {
    val kit = estado.kit ?: return
    val contexto = LocalContext.current
    val elegirFoto = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            val tipo = contexto.contentResolver.getType(uri)
            val bytes = runCatching { contexto.contentResolver.openInputStream(uri)?.use { it.readBytes() } }.getOrNull()
            viewModel.subirLogo(tipo, bytes)
        }
    }

    Tarjeta {
        Text("Tu identidad", style = MaterialTheme.typography.titleLarge, color = TextoPrincipal)

        // Eslogan.
        Text("Eslogan", style = MaterialTheme.typography.labelLarge, color = TextoSecundario)
        if (kit.eslogan.isNotBlank()) {
            Text(
                "“${kit.eslogan}”",
                style = MaterialTheme.typography.titleMedium,
                fontStyle = FontStyle.Italic,
                color = IndigoAdbrand
            )
        }
        if (kit.esloganOpciones.size > 1) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                kit.esloganOpciones.forEach { opcion ->
                    Chip(opcion, opcion == kit.eslogan) { if (opcion != kit.eslogan) viewModel.usarEslogan(opcion) }
                }
            }
        }
        CampoAdbrand(
            valor = estado.esloganNuevo,
            alCambiar = viewModel::cambiarEsloganNuevo,
            etiqueta = "¿Prefieres el tuyo?",
            marcador = "Ej.: La casa del buen pan"
        )
        if (estado.esloganNuevo.isNotBlank()) {
            BotonSecundario(texto = "Usar este eslogan", onClick = { viewModel.usarEslogan(estado.esloganNuevo) })
        }

        // Logo.
        Text("Logo", style = MaterialTheme.typography.labelLarge, color = TextoSecundario)
        kit.logoImagen?.let { logo ->
            ImagenIa(dataUrl = logo, etiqueta = "Logo de ${kit.nombreNegocio}", modifier = Modifier.size(120.dp))
        } ?: Text(
            "Sube el logo que ya tienes o deja que la IA te proponga 3.",
            style = MaterialTheme.typography.bodySmall,
            color = TextoSecundario
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BotonSecundario(
                texto = "Subir mi logo",
                onClick = { elegirFoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                icono = Icons.Filled.PhotoLibrary,
                habilitado = !estado.guardandoIdentidad,
                modifier = Modifier.weight(1f)
            )
            BotonSecundario(
                texto = if (estado.proponiendoLogos) "Creando…" else "3 logos con IA",
                onClick = viewModel::proponerLogos,
                icono = Icons.Filled.AutoAwesome,
                habilitado = !estado.proponiendoLogos && !estado.guardandoIdentidad,
                modifier = Modifier.weight(1f)
            )
        }
        if (estado.proponiendoLogos) {
            AvisoEstado("La IA está dibujando 3 logos para tu marca…")
        }
        if (estado.logosPropuestos.isNotEmpty()) {
            Text("Toca el que más te guste:", style = MaterialTheme.typography.bodySmall, color = TextoSecundario)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                estado.logosPropuestos.forEachIndexed { indice, logo ->
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        ImagenIa(
                            dataUrl = logo,
                            etiqueta = "Propuesta de logo ${indice + 1}",
                            modifier = Modifier.fillMaxWidth().aspectRatio(1f)
                        )
                        BotonPrincipal(
                            texto = "Elegir",
                            onClick = { viewModel.elegirLogo(logo) },
                            habilitado = !estado.guardandoIdentidad
                        )
                    }
                }
            }
        }

        // Manual de marca.
        BotonPrincipal(
            texto = if (estado.descargandoManual) "Preparando…" else "Manual de marca (PDF)",
            onClick = {
                viewModel.descargarManual { bytes ->
                    val archivo = File(contexto.filesDir, "manual-de-marca.pdf").apply { writeBytes(bytes) }
                    if (!abrirPdf(contexto, archivo)) compartirPdf(contexto, archivo)
                }
            },
            cargando = estado.descargandoManual,
            icono = Icons.Filled.Download
        )
        Text(
            "Para tus tarjetas, volantes, stickers o letrero: llévalo a tu imprenta.",
            style = MaterialTheme.typography.bodySmall,
            color = TextoSecundario
        )
    }
}

@Composable
private fun Chip(texto: String, seleccionado: Boolean, alTocar: () -> Unit) {
    FilterChip(
        selected = seleccionado,
        onClick = alTocar,
        label = { Text(texto) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = IndigoClaro,
            selectedLabelColor = IndigoAdbrand
        )
    )
}

@Composable
private fun Tarjeta(contenido: @Composable () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = SuperficieBlanca,
        border = BorderStroke(1.dp, BordeSuave)
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) { contenido() }
    }
}

@Composable
private fun Paso(numero: Int, titulo: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(26.dp)
                .clip(CircleShape)
                .background(VioletaClaro),
            contentAlignment = Alignment.Center
        ) {
            Text("$numero", style = MaterialTheme.typography.labelLarge, color = VioletaAdbrand)
        }
        Text(titulo, style = MaterialTheme.typography.titleMedium, color = TextoPrincipal)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TarjetaKit(kit: KitMarca) {
    val colores = coloresDe(kit.paletaColores)
    Tarjeta {
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
            LogoMarca(kit.logoDiseno, kit.nombreNegocio, colores.firstOrNull() ?: IndigoAdbrand, imagen = kit.logoImagen)
            Column {
                Text(kit.nombreNegocio, style = MaterialTheme.typography.titleLarge, color = TextoPrincipal)
                Text(kit.sector, style = MaterialTheme.typography.bodySmall, color = TextoSecundario)
            }
        }
        if (colores.isNotEmpty()) {
            Text("Tus colores", style = MaterialTheme.typography.labelLarge, color = TextoPrincipal)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                colores.forEach { color ->
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(color)
                    )
                }
            }
        }
        if (kit.tipografiaPrimaria.isNotBlank()) {
            Text("Tus letras", style = MaterialTheme.typography.labelLarge, color = TextoPrincipal)
            Text(
                listOf(kit.tipografiaPrimaria, kit.tipografiaSecundaria).filter { it.isNotBlank() }.joinToString(" · "),
                style = MaterialTheme.typography.bodyMedium,
                color = TextoSecundario
            )
        }
        if (kit.vozMarca.isNotBlank()) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                androidx.compose.material3.Icon(Icons.Filled.Star, contentDescription = null, tint = VioletaAdbrand, modifier = Modifier.size(16.dp))
                Text("Cómo habla tu marca", style = MaterialTheme.typography.labelLarge, color = TextoPrincipal)
            }
            Text(kit.vozMarca, style = MaterialTheme.typography.bodyMedium, color = TextoSecundario)
        }
    }
}
