package com.adbrand.ui.previsualizacion.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adbrand.core.network.Resultado
import com.adbrand.data.repository.PrevisualizacionRepository
import com.adbrand.data.repository.PrevisualizacionRepositoryRemoto
import com.adbrand.domain.model.Anuncio
import com.adbrand.domain.model.KitMarca
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// HU 45: estado de la previsualización.
data class PrevisualizacionEstado(
    val cargando: Boolean = false,
    val kit: KitMarca? = null,
    val anuncios: List<Anuncio> = emptyList(),
    val error: String? = null,
    val cargado: Boolean = false
)

class PrevisualizacionViewModel(
    private val repositorio: PrevisualizacionRepository = PrevisualizacionRepositoryRemoto()
) : ViewModel() {

    private val _estado = MutableStateFlow(PrevisualizacionEstado())
    val estado: StateFlow<PrevisualizacionEstado> = _estado.asStateFlow()

    fun cargar() {
        if (_estado.value.cargando) return
        viewModelScope.launch {
            _estado.update { it.copy(cargando = true, error = null) }
            when (val resultado = repositorio.obtener()) {
                is Resultado.Exito -> _estado.update {
                    it.copy(
                        cargando = false,
                        cargado = true,
                        kit = resultado.dato.kit,
                        anuncios = resultado.dato.anuncios
                    )
                }

                is Resultado.Error -> _estado.update {
                    it.copy(cargando = false, cargado = true, error = resultado.mensaje)
                }
            }
        }
    }
}
