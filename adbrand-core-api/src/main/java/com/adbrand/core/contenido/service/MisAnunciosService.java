package com.adbrand.core.contenido.service;

import com.adbrand.core.contenido.dto.MiAnuncioResponse;
import com.adbrand.core.contenido.entity.Contenido;
import com.adbrand.core.contenido.repository.ContenidoRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// "Mis anuncios": el dueño ve el estado de cada anuncio y se entera de lo que decidió el moderador.
@Service
public class MisAnunciosService {

    private final ContenidoRepository contenidos;

    public MisAnunciosService(ContenidoRepository contenidos) {
        this.contenidos = contenidos;
    }

    @Transactional(readOnly = true)
    public List<MiAnuncioResponse> listar(Long usuarioId) {
        return contenidos.findByUsuarioIdOrderByFechaCreacionDescIdDesc(usuarioId).stream()
                .map(MiAnuncioResponse::desde)
                .toList();
    }

    // Cuántas decisiones del moderador todavía no vio (el número del menú).
    @Transactional(readOnly = true)
    public long avisosPendientes(Long usuarioId) {
        return contenidos.countByUsuarioIdAndAvisoPendienteTrue(usuarioId);
    }

    // Al abrir "Mis anuncios" las decisiones quedan vistas y el número del menú vuelve a cero.
    @Transactional
    public void marcarVistos(Long usuarioId) {
        List<Contenido> pendientes = contenidos.findByUsuarioIdAndAvisoPendienteTrue(usuarioId);
        pendientes.forEach(contenido -> contenido.setAvisoPendiente(false));
        contenidos.saveAll(pendientes);
    }
}
