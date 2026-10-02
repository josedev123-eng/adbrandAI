package com.adbrand.core.negocio.service;

import com.adbrand.core.negocio.dto.PerfilNegocioRequest;
import com.adbrand.core.negocio.dto.PerfilNegocioResponse;
import com.adbrand.core.negocio.entity.PerfilNegocio;
import com.adbrand.core.negocio.repository.PerfilNegocioRepository;
import com.adbrand.core.shared.error.RecursoNoEncontradoException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Service: aquí va la lógica. El controller solo recibe y responde.
@Service
public class PerfilNegocioService {

    private final PerfilNegocioRepository repositorio;

    public PerfilNegocioService(PerfilNegocioRepository repositorio) {
        this.repositorio = repositorio;
    }

    @Transactional(readOnly = true)
    public PerfilNegocioResponse obtener(Long usuarioId) {
        PerfilNegocio perfil = repositorio.findByUsuarioId(usuarioId)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "PERFIL_NO_ENCONTRADO", "Todavía no registraste el perfil de tu negocio."));
        return PerfilNegocioResponse.desde(perfil);
    }

    // Si el usuario ya tiene perfil se actualiza; si no, se crea. Así cada usuario tiene uno solo.
    @Transactional
    public PerfilNegocioResponse guardar(Long usuarioId, PerfilNegocioRequest datos) {
        PerfilNegocio perfil = repositorio.findByUsuarioId(usuarioId).orElseGet(PerfilNegocio::new);
        perfil.setUsuarioId(usuarioId);
        perfil.setNombreComercial(datos.nombreComercial().trim());
        perfil.setRubro(datos.rubro().trim());
        perfil.setPublicoObjetivo(datos.publicoObjetivo().trim());
        perfil.setTono(datos.tono());
        perfil.setDescripcion(datos.descripcion() == null || datos.descripcion().isBlank()
                ? null
                : datos.descripcion().trim());
        return PerfilNegocioResponse.desde(repositorio.saveAndFlush(perfil));
    }
}