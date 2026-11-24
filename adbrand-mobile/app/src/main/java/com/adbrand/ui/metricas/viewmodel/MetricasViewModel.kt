package com.adbrand.ui.metricas.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adbrand.core.network.Resultado
import com.adbrand.data.repository.MetricasRepository
import com.adbrand.data.repository.MetricasRepositoryRemoto
import com.adbrand.domain.model.MetricasContenido
import com.adbrand.domain.model.MetricasPublicaciones
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// HU 57: estado de la pantalla de métricas. HU 58: también las publicaciones del calendario.
data class MetricasEstado(
    val cargando: Boolean = false,
    val metricas: MetricasContenido = MetricasContenido(),
    val publicaciones: MetricasPublicaciones = MetricasPublicaciones(),
    val error: String? = null,
    val cargado: Boolean = false
)

class MetricasViewModel(
    private val repositorio: MetricasRepository = MetricasRepositoryRemoto()
) : ViewModel() {

    private val _estado = MutableStateFlow(MetricasEstado())
    val estado: StateFlow<MetricasEstado> = _estado.asStateFlow()

    // Carga las dos métricas. Si alguna falla, se muestra su mensaje y se puede reintentar.
    fun cargar() {
        if (_estado.value.cargando) return
        viewModelScope.launch {
            _estado.update { it.copy(cargando = true, error = null) }
            val contenido = repositorio.contenido()
            val publicaciones = repositorio.publicaciones()
            _estado.update { actual ->
                val error = (contenido as? Resultado.Error)?.mensaje ?: (publicaciones as? Resultado.Error)?.mensaje
                actual.copy(
                    cargando = false,
                    cargado = true,
                    error = error,
                    metricas = (contenido as? Resultado.Exito)?.dato ?: actual.metricas,
                    publicaciones = (publicaciones as? Resultado.Exito)?.dato ?: actual.publicaciones
                )
            }
        }
    }
}
