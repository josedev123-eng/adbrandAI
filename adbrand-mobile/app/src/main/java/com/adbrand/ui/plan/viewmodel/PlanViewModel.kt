package com.adbrand.ui.plan.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adbrand.core.network.Resultado
import com.adbrand.data.repository.PlanRepository
import com.adbrand.data.repository.PlanRepositoryRemoto
import com.adbrand.domain.model.MiSuscripcion
import com.adbrand.domain.model.PlanDisponible
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// HU 52: estado de la pantalla del plan: suscripción vigente y catálogo.
data class PlanEstado(
    val cargando: Boolean = false,
    val suscripcion: MiSuscripcion = MiSuscripcion(),
    val planes: List<PlanDisponible> = emptyList(),
    val error: String? = null,
    val cargado: Boolean = false
)

class PlanViewModel(
    private val repositorio: PlanRepository = PlanRepositoryRemoto()
) : ViewModel() {

    private val _estado = MutableStateFlow(PlanEstado())
    val estado: StateFlow<PlanEstado> = _estado.asStateFlow()

    fun cargar() {
        if (_estado.value.cargando) return
        viewModelScope.launch {
            _estado.update { it.copy(cargando = true, error = null) }
            val mia = repositorio.mia()
            val planes = repositorio.planes()
            when {
                mia is Resultado.Error -> _estado.update {
                    it.copy(cargando = false, cargado = true, error = mia.mensaje)
                }

                planes is Resultado.Error -> _estado.update {
                    it.copy(cargando = false, cargado = true, error = planes.mensaje)
                }

                mia is Resultado.Exito && planes is Resultado.Exito -> _estado.update {
                    it.copy(
                        cargando = false,
                        cargado = true,
                        suscripcion = mia.dato,
                        planes = planes.dato,
                        error = null
                    )
                }
            }
        }
    }
}
