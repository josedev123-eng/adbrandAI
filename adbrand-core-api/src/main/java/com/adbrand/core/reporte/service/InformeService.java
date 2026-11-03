package com.adbrand.core.reporte.service;

import com.adbrand.core.auth.entity.Usuario;
import com.adbrand.core.auth.repository.UsuarioRepository;
import com.adbrand.core.contenido.entity.EstadoContenido;
import com.adbrand.core.contenido.repository.ContenidoRepository;
import com.adbrand.core.reporte.dto.CampanaInformeResponse;
import com.adbrand.core.reporte.dto.InformeCampanasResponse;
import com.adbrand.core.shared.error.RecursoNoEncontradoException;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// HU 37: arma el informe consolidado con todos los anuncios de la empresa que inició sesión.
@Service
public class InformeService {

    private final ContenidoRepository contenidos;
    private final UsuarioRepository usuarios;

    public InformeService(ContenidoRepository contenidos, UsuarioRepository usuarios) {
        this.contenidos = contenidos;
        this.usuarios = usuarios;
    }

    @Transactional(readOnly = true)
    public InformeCampanasResponse campanas(Long usuarioId) {
        Usuario usuario = usuarios.findById(usuarioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("USUARIO_NO_ENCONTRADO", "La cuenta no existe."));

        // HU 37, criterio 1: cada anuncio con su fecha, red social, oferta y estado, del más reciente al más antiguo.
        List<CampanaInformeResponse> campanas = contenidos.findByUsuarioIdOrderByFechaCreacionDescIdDesc(usuarioId)
                .stream()
                .map(CampanaInformeResponse::desde)
                .toList();

        return new InformeCampanasResponse(
                usuario.getNombreEmpresa(),
                campanas.size(),
                contar(campanas, EstadoContenido.APROBADO),
                contar(campanas, EstadoContenido.DUDOSO),
                contar(campanas, EstadoContenido.RECHAZADO),
                campanas);
    }

    private static long contar(List<CampanaInformeResponse> campanas, EstadoContenido estado) {
        return campanas.stream().filter(campana -> campana.estado() == estado).count();
    }
}
