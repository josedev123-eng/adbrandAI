package com.adbrand.core.notificacion.service;

import com.adbrand.core.notificacion.dto.PreferenciaNotificacionRequest;
import com.adbrand.core.notificacion.dto.PreferenciaNotificacionResponse;
import com.adbrand.core.notificacion.entity.PreferenciaNotificacion;
import com.adbrand.core.notificacion.entity.TipoNotificacion;
import com.adbrand.core.notificacion.repository.PreferenciaNotificacionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// HU 51: lee y cambia qué avisos recibe una empresa. Sin fila guardada se
// devuelven los valores por defecto (todo activo), sin escribir en la base.
@Service
public class ServicioPreferenciasNotificacion {

    private final PreferenciaNotificacionRepository repositorio;

    public ServicioPreferenciasNotificacion(PreferenciaNotificacionRepository repositorio) {
        this.repositorio = repositorio;
    }

    @Transactional(readOnly = true)
    public PreferenciaNotificacionResponse obtener(Long usuarioId) {
        return PreferenciaNotificacionResponse.desde(cargar(usuarioId));
    }

    @Transactional
    public PreferenciaNotificacionResponse actualizar(
            Long usuarioId, PreferenciaNotificacionRequest cambios) {
        PreferenciaNotificacion preferencia = cargar(usuarioId);
        if (cambios.campanas() != null) {
            preferencia.setCampanas(cambios.campanas());
        }
        if (cambios.suscripciones() != null) {
            preferencia.setSuscripciones(cambios.suscripciones());
        }
        if (cambios.novedades() != null) {
            preferencia.setNovedades(cambios.novedades());
        }
        return PreferenciaNotificacionResponse.desde(repositorio.save(preferencia));
    }

    /** HU 51: filtro que aplica el envío antes de buscar los teléfonos. */
    @Transactional(readOnly = true)
    public boolean permite(Long usuarioId, TipoNotificacion tipo) {
        if (tipo == TipoNotificacion.SISTEMA) {
            return true;
        }
        return repositorio.findById(usuarioId)
                .map(preferencia -> preferencia.permite(tipo))
                .orElse(true);
    }

    private PreferenciaNotificacion cargar(Long usuarioId) {
        return repositorio.findById(usuarioId)
                .orElseGet(() -> new PreferenciaNotificacion(usuarioId));
    }
}
