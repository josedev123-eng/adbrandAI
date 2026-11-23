package com.adbrand.ui.metricas.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adbrand.core.network.Resultado
import com.adbrand.data.repository.MetricasRepository
import com.adbrand.data.repository.MetricasRepositoryRemoto
import com.adbrand.domain.model.MetricasContenido
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// HU 57: estado de la pantalla de métricas.
data class MetricasEstado(
    val cargando: Boolean = false,
    val metricas: MetricasContenido = MetricasContenido(),
    val error: String? = null,
    val cargado: Boolean = false
)

class MetricasViewModel(
    private val repositorio: MetricasRepository = MetricasRepositoryRemoto()
) : ViewModel() {

    private val _estado = MutableStateFlow(MetricasEstado())
    val estado: StateFlow<MetricasEstado> = _estado.asStateFlow()

    fun cargar() {
        if (_estado.value.cargando) return
        viewModelScope.launch {
            _estado.update { it.copy(cargando = true, error = null) }
            when (val resultado = repositorio.contenido()) {
                is Resultado.Exito -> _estado.update {
                    it.copy(cargando = false, cargado = true, metricas = resultado.dato)
                }

                is Resultado.Error -> _estado.update {
                    it.copy(cargando = false, cargado = true, error = resultado.mensaje)
                }
            }
        }
    }
}
