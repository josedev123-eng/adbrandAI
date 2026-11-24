package com.adbrand.ui.metricas.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adbrand.core.network.Resultado
import com.adbrand.data.repository.MetricasRepository
import com.adbrand.data.repository.MetricasRepositoryRemoto
import com.adbrand.domain.model.MetricasContenido
import com.adbrand.domain.model.MetricasPublicaciones
import com.adbrand.domain.model.OpcionPeriodo
import com.adbrand.domain.model.PeriodoMetricas
import com.adbrand.domain.model.RangoFechas
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// HU 57: estado de la pantalla de métricas. HU 58: también las publicaciones del calendario.
// HU 60: el periodo elegido, las fechas que escribe el usuario en "Rango propio" (AAAA-MM-DD),
// el rango con el que se pidieron las métricas y el aviso si el rango propio no es válido.
data class MetricasEstado(
    val cargando: Boolean = false,
    val metricas: MetricasContenido = MetricasContenido(),
    val publicaciones: MetricasPublicaciones = MetricasPublicaciones(),
    val error: String? = null,
    val cargado: Boolean = false,
    val periodo: OpcionPeriodo = OpcionPeriodo.TODO,
    val desdeTexto: String = "",
    val hastaTexto: String = "",
    val rangoAplicado: RangoFechas = RangoFechas(null, null),
    val avisoPeriodo: String? = null
)

class MetricasViewModel(
    private val repositorio: MetricasRepository = MetricasRepositoryRemoto(),
    private val hoy: () -> LocalDate = { LocalDate.now() }
) : ViewModel() {

    private val _estado = MutableStateFlow(MetricasEstado())
    val estado: StateFlow<MetricasEstado> = _estado.asStateFlow()

    // Carga las dos métricas con el rango aplicado. Si alguna falla, se muestra su mensaje y se puede reintentar.
    fun cargar() {
        if (_estado.value.cargando) return
        val rango = _estado.value.rangoAplicado
        viewModelScope.launch {
            _estado.update { it.copy(cargando = true, error = null) }
            val contenido = repositorio.contenido(rango)
            val publicaciones = repositorio.publicaciones(rango)
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

    // HU 60, criterio 1: al elegir "Todo", "Este mes" o "Últimos 3 meses" las métricas se recalculan.
    // "Rango propio" solo muestra los campos; se recalcula al tocar "Aplicar".
    fun elegirPeriodo(opcion: OpcionPeriodo) {
        _estado.update { it.copy(periodo = opcion, avisoPeriodo = null) }
        if (opcion != OpcionPeriodo.RANGO) {
            _estado.update { it.copy(rangoAplicado = PeriodoMetricas.rango(opcion, hoy())) }
            cargar()
        }
    }

    fun cambiarDesde(texto: String) {
        _estado.update { it.copy(desdeTexto = texto, avisoPeriodo = null) }
    }

    fun cambiarHasta(texto: String) {
        _estado.update { it.copy(hastaTexto = texto, avisoPeriodo = null) }
    }

    // HU 60, criterio 2: con fechas mal escritas o el inicio después del fin, se avisa y no se aplica el filtro.
    fun aplicarRango() {
        val actual = _estado.value
        val problema = PeriodoMetricas.validar(actual.desdeTexto, actual.hastaTexto)
        if (problema != null) {
            _estado.update { it.copy(avisoPeriodo = problema) }
            return
        }
        _estado.update {
            it.copy(
                avisoPeriodo = null,
                rangoAplicado = PeriodoMetricas.rango(OpcionPeriodo.RANGO, hoy(), it.desdeTexto, it.hastaTexto)
            )
        }
        cargar()
    }
}
