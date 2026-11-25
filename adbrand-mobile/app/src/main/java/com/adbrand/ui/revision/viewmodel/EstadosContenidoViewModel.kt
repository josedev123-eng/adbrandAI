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
data class EstadosContenidoEstado(
    val cargando: Boolean = false,
    val cargado: Boolean = false,
    val contenidos: List<ContenidoConEstado> = emptyList(),
    val error: String? = null
)

class EstadosContenidoViewModel(
    private val repositorio: EstadosContenidoRepository = EstadosContenidoRepositoryRemoto()
) : ViewModel() {

    private val _estado = MutableStateFlow(EstadosContenidoEstado())
    val estado: StateFlow<EstadosContenidoEstado> = _estado.asStateFlow()

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
