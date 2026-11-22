package com.adbrand.ui.avisos.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adbrand.core.network.Resultado
import com.adbrand.data.repository.PreferenciasRepository
import com.adbrand.data.repository.PreferenciasRepositoryRemoto
import com.adbrand.domain.model.CampoPreferencia
import com.adbrand.domain.model.PreferenciasAvisos
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// HU 51: estado de la pantalla de avisos. El interruptor cambia al instante y,
// si el servidor falla, vuelve a su valor anterior con un mensaje claro.
data class PreferenciasEstado(
    val cargando: Boolean = false,
    val cargado: Boolean = false,
    val guardando: Boolean = false,
    val preferencias: PreferenciasAvisos = PreferenciasAvisos(),
    val error: String? = null,
    val mensaje: String? = null
)

class PreferenciasViewModel(
    private val repositorio: PreferenciasRepository = PreferenciasRepositoryRemoto()
) : ViewModel() {

    private val _estado = MutableStateFlow(PreferenciasEstado())
    val estado: StateFlow<PreferenciasEstado> = _estado.asStateFlow()

    fun cargar() {
        if (_estado.value.cargando) return
        viewModelScope.launch {
            _estado.update { it.copy(cargando = true, error = null, mensaje = null) }
            when (val resultado = repositorio.obtener()) {
                is Resultado.Exito -> _estado.update {
                    it.copy(
                        cargando = false,
                        cargado = true,
                        preferencias = resultado.dato,
                        error = null
                    )
                }

                is Resultado.Error -> _estado.update {
                    it.copy(cargando = false, cargado = true, error = resultado.mensaje)
                }
            }
        }
    }

    // Optimista: se pinta el nuevo valor antes de esperar al servidor.
    fun alternar(campo: CampoPreferencia, valor: Boolean) {
        if (_estado.value.guardando) return
        val anterior = _estado.value.preferencias
        _estado.update {
            it.copy(
                preferencias = anterior.con(campo, valor),
                guardando = true,
                error = null,
                mensaje = null
            )
        }
        viewModelScope.launch {
            when (val resultado = repositorio.actualizar(campo, valor)) {
                is Resultado.Exito -> _estado.update {
                    it.copy(
                        preferencias = resultado.dato,
                        guardando = false,
                        mensaje = "Preferencia guardada."
                    )
                }

                is Resultado.Error -> _estado.update {
                    it.copy(
                        preferencias = anterior,
                        guardando = false,
                        error = resultado.mensaje
                    )
                }
            }
        }
    }

    fun reintentar() = cargar()
}
