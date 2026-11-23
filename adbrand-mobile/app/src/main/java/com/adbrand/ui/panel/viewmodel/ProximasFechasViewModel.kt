package com.adbrand.ui.panel.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adbrand.core.network.Resultado
import com.adbrand.data.repository.MesContenidoRepository
import com.adbrand.data.repository.MesContenidoRepositoryRemoto
import com.adbrand.domain.model.FechaSugerida
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// HU 63: la tarjeta "Próximas fechas que venden" del Inicio. Si falla, simplemente no se muestra.
class ProximasFechasViewModel(
    private val repositorio: MesContenidoRepository = MesContenidoRepositoryRemoto()
) : ViewModel() {

    private val _fechas = MutableStateFlow<List<FechaSugerida>>(emptyList())
    val fechas: StateFlow<List<FechaSugerida>> = _fechas.asStateFlow()

    fun cargar() {
        viewModelScope.launch {
            val resultado = repositorio.proximas()
            _fechas.value = if (resultado is Resultado.Exito) resultado.dato else emptyList()
        }
    }
}
