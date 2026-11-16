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

// HU 53 y 54: estado de la lista "por revisar".
// porRechazar: el contenido del que se está preguntando el motivo (la ventanita abierta).
data class RevisionEstado(
    val cargando: Boolean = false,
    val cargado: Boolean = false,
    val contenidos: List<ContenidoRevision> = emptyList(),
    val procesandoId: Long? = null,
    val error: String? = null,
    val aviso: String? = null,
    val porRechazar: ContenidoRevision? = null,
    val motivoRechazo: String = ""
)

const val MAXIMO_MOTIVO_RECHAZO = 300

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

    // HU 54: abre la ventanita para rechazar; el motivo es opcional.
    fun pedirRechazo(id: Long) {
        if (_estado.value.procesandoId != null) return
        val contenido = _estado.value.contenidos.find { it.id == id } ?: return
        _estado.update { it.copy(porRechazar = contenido, motivoRechazo = "", error = null, aviso = null) }
    }

    fun cambiarMotivo(texto: String) {
        _estado.update { it.copy(motivoRechazo = texto.take(MAXIMO_MOTIVO_RECHAZO)) }
    }

    fun cancelarRechazo() {
        _estado.update { it.copy(porRechazar = null, motivoRechazo = "") }
    }

    // Criterio 1: el contenido rechazado sale de la lista y ya no se puede programar.
    fun confirmarRechazo() {
        val contenido = _estado.value.porRechazar ?: return
        val motivo = _estado.value.motivoRechazo.trim().ifBlank { null }
        viewModelScope.launch {
            _estado.update { it.copy(porRechazar = null, procesandoId = contenido.id, error = null, aviso = null) }
            when (val resultado = repositorio.rechazar(contenido.id, motivo)) {
                is Resultado.Exito -> _estado.update { actual ->
                    actual.copy(
                        procesandoId = null,
                        motivoRechazo = "",
                        contenidos = actual.contenidos.filterNot { it.id == contenido.id },
                        aviso = "Contenido rechazado. No se publicará ni se podrá programar."
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
