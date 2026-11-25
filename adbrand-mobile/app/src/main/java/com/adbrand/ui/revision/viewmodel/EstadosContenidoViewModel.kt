package com.adbrand.ui.revision.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adbrand.core.network.Resultado
import com.adbrand.data.repository.EstadosContenidoRepository
import com.adbrand.data.repository.EstadosContenidoRepositoryRemoto
import com.adbrand.domain.model.ContenidoConEstado
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// HU 56: la sección "Estado de tus contenidos" de la pestaña Revisar.
// HU 63: para que la lista no se llene: "Todos" no muestra los cancelados (van en "Cancelados"), se ven de a 12
// con "Ver más" y hay buscador por texto o fecha (AAAA-MM-DD).
data class EstadosContenidoEstado(
    val cargando: Boolean = false,
    val cargado: Boolean = false,
    val contenidos: List<ContenidoConEstado> = emptyList(),
    val error: String? = null,
    val soloCancelados: Boolean = false,
    val busqueda: String = "",
    val mostrar: Int = POR_PAGINA,
    val procesandoId: Long? = null,
    val mensaje: String? = null
) {
    private val encontrados: List<ContenidoConEstado>
        get() {
            val palabra = sinTildes(busqueda.trim())
            return contenidos.filter { c ->
                palabra.isEmpty() || c.fecha.startsWith(busqueda.trim()) ||
                    sinTildes("${c.titular} ${c.oferta} ${c.texto}").contains(palabra)
            }
        }

    val filtrados: List<ContenidoConEstado>
        get() = encontrados.filter { it.cancelado == soloCancelados }

    val visibles: List<ContenidoConEstado> get() = filtrados.take(mostrar)
    val restantes: Int get() = (filtrados.size - mostrar).coerceAtLeast(0)
    val cantidadTodos: Int get() = encontrados.count { !it.cancelado }
    val cantidadCancelados: Int get() = encontrados.count { it.cancelado }

    companion object {
        const val POR_PAGINA = 12

        private fun sinTildes(texto: String): String =
            java.text.Normalizer.normalize(texto, java.text.Normalizer.Form.NFD)
                .replace(Regex("\\p{M}"), "")
                .lowercase()
    }
}

class EstadosContenidoViewModel(
    private val repositorio: EstadosContenidoRepository = EstadosContenidoRepositoryRemoto()
) : ViewModel() {

    private val _estado = MutableStateFlow(EstadosContenidoEstado())
    val estado: StateFlow<EstadosContenidoEstado> = _estado.asStateFlow()

    fun verCancelados(si: Boolean) = _estado.update { it.copy(soloCancelados = si, mostrar = EstadosContenidoEstado.POR_PAGINA) }

    fun buscar(texto: String) = _estado.update { it.copy(busqueda = texto, mostrar = EstadosContenidoEstado.POR_PAGINA) }

    fun verMas() = _estado.update { it.copy(mostrar = it.mostrar + EstadosContenidoEstado.POR_PAGINA) }

    // "Cancelar en AdBrand": pasa a "Cancelados" y sus fechas del calendario se cancelan.
    fun cancelar(id: Long) = ejecutar(id) {
        when (val r = repositorio.cancelar(id)) {
            is Resultado.Exito -> {
                cambiarEstado(id, "RECHAZADO")
                // Corto y claro: lo único que le queda por hacer al dueño.
                "Bórralo si ya lo subiste."
            }
            is Resultado.Error -> r.mensaje
        }
    }

    fun reactivar(id: Long) = ejecutar(id) {
        when (val r = repositorio.reactivar(id)) {
            is Resultado.Exito -> {
                cambiarEstado(id, "APROBADO")
                "Anuncio activado otra vez."
            }
            is Resultado.Error -> r.mensaje
        }
    }

    // Solo los cancelados. Sale de la lista.
    fun eliminar(id: Long) = ejecutar(id) {
        when (val r = repositorio.eliminar(id)) {
            is Resultado.Exito -> {
                _estado.update { e -> e.copy(contenidos = e.contenidos.filterNot { it.id == id }) }
                "Anuncio eliminado de tu lista."
            }
            is Resultado.Error -> r.mensaje
        }
    }

    private fun cambiarEstado(id: Long, nuevo: String) = _estado.update { e ->
        e.copy(contenidos = e.contenidos.map { if (it.id == id) it.copy(estadoActual = nuevo, motivo = "") else it })
    }

    private fun ejecutar(id: Long, accion: suspend () -> String) {
        if (_estado.value.procesandoId != null) return
        viewModelScope.launch {
            _estado.update { it.copy(procesandoId = id, mensaje = null) }
            val mensaje = accion()
            _estado.update { it.copy(procesandoId = null, mensaje = mensaje) }
        }
    }

    // Si falla, se queda la última lista que se pudo cargar y se muestra el mensaje.
    fun cargar() {
        if (_estado.value.cargando) return
        viewModelScope.launch {
            _estado.update { it.copy(cargando = true, error = null) }
            when (val resultado = repositorio.estados()) {
                is Resultado.Exito -> _estado.update {
                    it.copy(cargando = false, cargado = true, contenidos = resultado.dato)
                }

                is Resultado.Error -> _estado.update {
                    it.copy(cargando = false, cargado = true, error = resultado.mensaje)
                }
            }
        }
    }
}
