package com.adbrand.ui.campana.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adbrand.core.network.Resultado
import com.adbrand.data.repository.CampanaRepository
import com.adbrand.data.repository.CampanaRepositoryRemoto
import com.adbrand.domain.model.Campana
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// HU 49: estado de la ficha que se abre desde el aviso de recordatorio.
data class DetalleCampanaEstado(
    val cargando: Boolean = false,
    val campana: Campana? = null,
    val error: String? = null,
    val noEncontrada: Boolean = false
)

class DetalleCampanaViewModel(
    private val repositorio: CampanaRepository = CampanaRepositoryRemoto()
) : ViewModel() {

    private val _estado = MutableStateFlow(DetalleCampanaEstado())
    val estado: StateFlow<DetalleCampanaEstado> = _estado.asStateFlow()

    private var idActual: Long? = null

    fun cargar(id: Long) {
        if (id <= 0) {
            _estado.update {
                it.copy(
                    cargando = false,
                    error = "No encontramos el aviso de esta campaña.",
                    noEncontrada = true
                )
            }
            return
        }
        if (_estado.value.cargando) return

        idActual = id
        viewModelScope.launch {
            _estado.update {
                it.copy(cargando = true, error = null, noEncontrada = false)
            }
            when (val resultado = repositorio.obtener(id)) {
                is Resultado.Exito -> _estado.update {
                    it.copy(cargando = false, campana = resultado.dato, error = null)
                }

                is Resultado.Error -> _estado.update {
                    it.copy(
                        cargando = false,
                        error = resultado.mensaje,
                        noEncontrada = resultado.codigo == 404
                    )
                }
            }
        }
    }

    fun reintentar() {
        idActual?.let(::cargar)
    }
}
