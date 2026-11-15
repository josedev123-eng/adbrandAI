package com.adbrand.ui.panel.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adbrand.core.network.Resultado
import com.adbrand.data.repository.InformeRepository
import com.adbrand.data.repository.InformeRepositoryRemoto
import com.adbrand.domain.model.CampanaInforme
import com.adbrand.domain.model.InformeCampanas
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// HU 37: estado del panel de consulta del usuario.
data class PanelEstado(
    val cargando: Boolean = false,
    val informe: InformeCampanas? = null,
    val error: String? = null,
    val cargado: Boolean = false,
    val filtro: String? = null
) {
    // Filtro local: no hace falta pedir otra vez al backend.
    val campanasVisibles: List<CampanaInforme>
        get() {
            val todas = informe?.campanas.orEmpty()
            val estadoBuscado = filtro ?: return todas
            return todas.filter { it.estado.equals(estadoBuscado, ignoreCase = true) }
        }
}

class PanelViewModel(
    private val repositorio: InformeRepository = InformeRepositoryRemoto()
) : ViewModel() {

    private val _estado = MutableStateFlow(PanelEstado())
    val estado: StateFlow<PanelEstado> = _estado.asStateFlow()

    fun cargar() {
        if (_estado.value.cargando) return
        viewModelScope.launch {
            _estado.update { it.copy(cargando = true, error = null) }
            when (val resultado = repositorio.obtener()) {
                is Resultado.Exito -> _estado.update {
                    it.copy(
                        cargando = false,
                        cargado = true,
                        informe = resultado.dato
                    )
                }

                is Resultado.Error -> _estado.update {
                    it.copy(cargando = false, cargado = true, error = resultado.mensaje)
                }
            }
        }
    }

    fun filtrar(estado: String?) {
        _estado.update { it.copy(filtro = estado) }
    }
}
