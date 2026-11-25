package com.adbrand.core.notificaciones

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// HU 50: puente entre el servicio de Firebase y la interfaz. El servicio corre
// fuera del hilo principal; aquí solo se publican estados que Compose observa.
object BuzonNotificacion {

    private val _aviso = MutableStateFlow<AvisoPush?>(null)
    val aviso: StateFlow<AvisoPush?> = _aviso.asStateFlow()

    private val _destino = MutableStateFlow<AvisoPush?>(null)
    val destino: StateFlow<AvisoPush?> = _destino.asStateFlow()

    // Llega un mensaje con la app abierta: banner arriba, sin tapar la pantalla.
    fun recibir(aviso: AvisoPush) {
        _aviso.value = aviso
    }

    // El usuario tocó la notificación o el banner: hay que navegar.
    fun abrir(aviso: AvisoPush) {
        _aviso.value = null
        _destino.value = aviso
    }

    fun abrirSiHaceFalta(intentAviso: AvisoPush?) {
        if (intentAviso != null) {
            _destino.value = intentAviso
        }
    }

    fun cerrarBanner() {
        _aviso.value = null
    }

    fun consumirDestino() {
        _destino.value = null
    }
}
