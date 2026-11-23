package com.adbrand.ui.kit.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adbrand.core.network.Resultado
import com.adbrand.data.repository.DatosKitMarca
import com.adbrand.data.repository.KitMarcaRepository
import com.adbrand.data.repository.KitMarcaRepositoryRemoto
import com.adbrand.data.repository.PerfilRepository
import com.adbrand.data.repository.PerfilRepositoryRemoto
import com.adbrand.domain.model.KitMarca
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// Los mismos estilos que la web (adbrand-web/src/features/brandkit/models/brandkitApi.js).
val ESTILOS_VISUALES = listOf(
    "MINIMALISTA" to "Minimalista",
    "MODERNO" to "Moderno",
    "CLASICO" to "Clásico",
    "DIVERTIDO" to "Divertido",
    "ELEGANTE" to "Elegante",
    "TECNOLÓGICO" to "Tecnológico",
    "ARTESANAL" to "Artesanal",
    "CORPORATIVO" to "Corporativo"
)

// Lo que la empresa quiere que sienta su cliente (identidad de marca, igual que en la web). Hasta 3.
val SENTIMIENTOS = listOf("Confianza", "Alegría", "Cercanía", "Elegancia", "Calidez", "Energía", "Frescura", "Tradición")
const val TAMANO_MAXIMO_LOGO = 2 * 1024 * 1024

data class KitMarcaEstado(
    val cargando: Boolean = true,
    val kit: KitMarca? = null,
    // Cuestionario: nombre y rubro llegan solos del perfil de marca.
    val nombre: String = "",
    val rubro: String = "",
    val estilo: String = "",
    val colores: String = "",
    val sentimientos: List<String> = emptyList(),
    val diferencial: String = "",
    val esloganPropio: String = "",
    val generando: Boolean = false,
    val creandoOtro: Boolean = false,
    // Identidad: logos propuestos por la IA (data URL), guardando algo, y el eslogan que escribe a mano.
    val proponiendoLogos: Boolean = false,
    val logosPropuestos: List<String> = emptyList(),
    val guardandoIdentidad: Boolean = false,
    val esloganNuevo: String = "",
    val descargandoManual: Boolean = false,
    val error: String? = null,
    val exito: String? = null
) {
    val mostrarFormulario: Boolean get() = kit == null || creandoOtro
}

class KitMarcaViewModel(
    private val kits: KitMarcaRepository = KitMarcaRepositoryRemoto(),
    private val perfil: PerfilRepository = PerfilRepositoryRemoto()
) : ViewModel() {

    private val _estado = MutableStateFlow(KitMarcaEstado())
    val estado: StateFlow<KitMarcaEstado> = _estado.asStateFlow()

    fun cargar() {
        viewModelScope.launch {
            _estado.update { it.copy(cargando = true, error = null) }
            val kit = (kits.actual() as? Resultado.Exito)?.dato
            val datos = (perfil.obtener() as? Resultado.Exito)?.dato
            _estado.update {
                it.copy(
                    cargando = false,
                    kit = kit,
                    nombre = it.nombre.ifBlank { datos?.nombreComercial.orEmpty() },
                    rubro = it.rubro.ifBlank { datos?.rubro.orEmpty() }
                )
            }
        }
    }

    fun cambiarNombre(valor: String) = _estado.update { it.copy(nombre = valor.take(120), error = null) }
    fun cambiarRubro(valor: String) = _estado.update { it.copy(rubro = valor.take(80), error = null) }
    fun elegirEstilo(valor: String) = _estado.update { it.copy(estilo = valor, error = null) }
    fun cambiarColores(valor: String) = _estado.update { it.copy(colores = valor.take(300)) }
    fun cambiarDiferencial(valor: String) = _estado.update { it.copy(diferencial = valor.take(300)) }
    fun cambiarEsloganPropio(valor: String) = _estado.update { it.copy(esloganPropio = valor.take(150)) }
    fun cambiarEsloganNuevo(valor: String) = _estado.update { it.copy(esloganNuevo = valor.take(150)) }
    fun crearOtro() = _estado.update { it.copy(creandoOtro = true, exito = null) }
    fun cancelarOtro() = _estado.update { it.copy(creandoOtro = false, error = null) }
    fun cerrarMensajes() = _estado.update { it.copy(error = null, exito = null) }

    // Hasta 3 sentimientos: si ya hay 3, el nuevo reemplaza al más antiguo.
    fun alternarSentimiento(valor: String) = _estado.update {
        val actuales = it.sentimientos
        it.copy(sentimientos = if (valor in actuales) actuales - valor else (actuales + valor).takeLast(3))
    }

    fun generar() {
        val actual = _estado.value
        val falta = when {
            actual.nombre.isBlank() -> "Escribe el nombre de tu negocio."
            actual.rubro.isBlank() -> "Elige tu rubro."
            actual.estilo.isBlank() -> "Elige un estilo."
            else -> null
        }
        if (falta != null) {
            _estado.update { it.copy(error = falta) }
            return
        }
        if (actual.generando) return
        viewModelScope.launch {
            _estado.update { it.copy(generando = true, error = null, exito = null) }
            val datos = DatosKitMarca(
                nombreNegocio = actual.nombre,
                sector = actual.rubro,
                estiloVisual = actual.estilo,
                paleta = actual.colores,
                sentimiento = actual.sentimientos.joinToString(", "),
                diferencial = actual.diferencial,
                eslogan = actual.esloganPropio
            )
            when (val resultado = kits.generar(datos)) {
                is Resultado.Exito -> _estado.update {
                    it.copy(generando = false, kit = resultado.dato, creandoOtro = false, exito = "¡Listo! Tu kit de marca está creado.")
                }
                is Resultado.Error -> _estado.update { it.copy(generando = false, error = resultado.mensaje) }
            }
        }
    }

    fun usarEslogan(texto: String) {
        val kit = _estado.value.kit ?: return
        val eslogan = texto.trim()
        if (eslogan.isEmpty() || _estado.value.guardandoIdentidad) return
        viewModelScope.launch {
            _estado.update { it.copy(guardandoIdentidad = true, error = null, exito = null) }
            when (val resultado = kits.elegirEslogan(kit.id ?: return@launch, eslogan)) {
                is Resultado.Exito -> _estado.update {
                    it.copy(guardandoIdentidad = false, kit = resultado.dato, esloganNuevo = "", exito = "Eslogan guardado.")
                }
                is Resultado.Error -> _estado.update { it.copy(guardandoIdentidad = false, error = resultado.mensaje) }
            }
        }
    }

    fun proponerLogos() {
        val id = _estado.value.kit?.id ?: return
        if (_estado.value.proponiendoLogos) return
        viewModelScope.launch {
            _estado.update { it.copy(proponiendoLogos = true, logosPropuestos = emptyList(), error = null, exito = null) }
            when (val resultado = kits.proponerLogos(id)) {
                is Resultado.Exito -> _estado.update { it.copy(proponiendoLogos = false, logosPropuestos = resultado.dato) }
                is Resultado.Error -> _estado.update { it.copy(proponiendoLogos = false, error = resultado.mensaje) }
            }
        }
    }

    fun elegirLogo(dataUrl: String) = guardarLogo(dataUrl, "IA")

    // El logo que la empresa ya tenía, elegido de la galería. La pantalla lo lee y lo manda como data URL.
    fun subirLogo(tipo: String?, bytes: ByteArray?) {
        when {
            bytes == null -> _estado.update { it.copy(error = "No pudimos leer la imagen. Prueba con otra.") }
            tipo != "image/png" && tipo != "image/jpeg" -> _estado.update { it.copy(error = "Elige tu logo en PNG o JPG.") }
            bytes.size > TAMANO_MAXIMO_LOGO -> _estado.update { it.copy(error = "El logo puede pesar hasta 2 MB.") }
            else -> {
                val base64 = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
                guardarLogo("data:$tipo;base64,$base64", "SUBIDO")
            }
        }
    }

    private fun guardarLogo(dataUrl: String, origen: String) {
        val id = _estado.value.kit?.id ?: return
        viewModelScope.launch {
            _estado.update { it.copy(guardandoIdentidad = true, error = null, exito = null) }
            when (val resultado = kits.guardarLogo(id, dataUrl, origen)) {
                is Resultado.Exito -> _estado.update {
                    it.copy(
                        guardandoIdentidad = false,
                        kit = resultado.dato,
                        logosPropuestos = emptyList(),
                        exito = if (origen == "IA") "Logo elegido." else "Tu logo se guardó."
                    )
                }
                is Resultado.Error -> _estado.update { it.copy(guardandoIdentidad = false, error = resultado.mensaje) }
            }
        }
    }

    // Descarga el manual de marca en PDF; "alListo" lo guarda y lo abre (la pantalla tiene el contexto).
    fun descargarManual(alListo: (ByteArray) -> Unit) {
        val id = _estado.value.kit?.id ?: return
        if (_estado.value.descargandoManual) return
        viewModelScope.launch {
            _estado.update { it.copy(descargandoManual = true, error = null) }
            when (val resultado = kits.manualPdf(id)) {
                is Resultado.Exito -> {
                    _estado.update { it.copy(descargandoManual = false) }
                    alListo(resultado.dato)
                }
                is Resultado.Error -> _estado.update { it.copy(descargandoManual = false, error = resultado.mensaje) }
            }
        }
    }
}
