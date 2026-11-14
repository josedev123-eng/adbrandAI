package com.adbrand.ui.revision.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adbrand.core.network.Resultado
import com.adbrand.data.repository.RevisionRepository
import com.adbrand.data.repository.RevisionRepositoryRemoto
import com.adbrand.domain.model.ContenidoRevision
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// HU 53: estado de la lista "por revisar".
data class RevisionEstado(
    val cargando: Boolean = false,
    val cargado: Boolean = false,
    val contenidos: List<ContenidoRevision> = emptyList(),
    val procesandoId: Long? = null,
    val error: String? = null,
    val aviso: String? = null
)

class RevisionViewModel(
    private val repositorio: RevisionRepository = RevisionRepositoryRemoto()
) : ViewModel() {

    private val _estado = MutableStateFlow(RevisionEstado())
    val estado: StateFlow<RevisionEstado> = _estado.asStateFlow()

    fun cargar() {
        if (_estado.value.cargando) return
        viewModelScope.launch {
            _estado.update { it.copy(cargando = true, error = null) }
            when (val resultado = repositorio.porRevisar()) {
                is Resultado.Exito -> _estado.update {
                    it.copy(cargando = false, cargado = true, contenidos = resultado.dato)
                }

                is Resultado.Error -> _estado.update {
                    it.copy(cargando = false, cargado = true, error = resultado.mensaje)
                }
            }
        }
    }

    // Criterio 1: un solo toque; el contenido aprobado sale de la lista y queda listo para el calendario.
    fun aprobar(id: Long) {
        val contenido = _estado.value.contenidos.find { it.id == id } ?: return
        // Criterio 2: lo rechazado por el moderador no se aprueba (la pantalla tampoco muestra el botón).
        if (!contenido.puedeAprobar || _estado.value.procesandoId != null) return
        viewModelScope.launch {
            _estado.update { it.copy(procesandoId = id, error = null, aviso = null) }
            when (val resultado = repositorio.aprobar(id)) {
                is Resultado.Exito -> _estado.update { actual ->
                    actual.copy(
                        procesandoId = null,
                        contenidos = actual.contenidos.filterNot { it.id == id },
                        aviso = "Contenido aprobado. Ya puedes programarlo en el calendario."
                    )
                }

                is Resultado.Error -> _estado.update {
                    it.copy(procesandoId = null, error = resultado.mensaje)
                }
            }
        }
    }

    fun cerrarAviso() {
        _estado.update { it.copy(aviso = null) }
    }
}
