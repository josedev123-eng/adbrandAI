package com.adbrand.core.cuenta.service;

import com.adbrand.core.auth.entity.Usuario;
import com.adbrand.core.auth.repository.UsuarioRepository;
import com.adbrand.core.cuenta.dto.EmpresaRequest;
import com.adbrand.core.cuenta.dto.EmpresaResponse;
import com.adbrand.core.shared.error.ConflictoException;
import com.adbrand.core.shared.error.NoAutenticadoException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// HU 23: ver y editar los datos de la empresa. Siempre con el usuario del token, nunca con un id de la URL.
@Service
public class CuentaService {

    static final String RUC_REPETIDO = "RUC_REPETIDO";
    static final String MENSAJE_RUC_REPETIDO = "Ese RUC ya está registrado en otra cuenta.";

    private final UsuarioRepository usuarios;

    public CuentaService(UsuarioRepository usuarios) {
        this.usuarios = usuarios;
    }

    @Transactional(readOnly = true)
    public EmpresaResponse obtener(Long usuarioId) {
        return EmpresaResponse.desde(buscar(usuarioId));
    }

    @Transactional
    public EmpresaResponse guardar(Long usuarioId, EmpresaRequest datos) {
        Usuario usuario = buscar(usuarioId);
        if (usuarios.existsByRucAndIdNot(datos.ruc(), usuarioId)) {
            throw new ConflictoException(RUC_REPETIDO, MENSAJE_RUC_REPETIDO);
        }

        usuario.setRuc(datos.ruc());
        usuario.setRazonSocial(datos.razonSocial());
        usuario.setTelefono(datos.telefono());
        usuario.setDireccion(datos.direccion());

        try {
            return EmpresaResponse.desde(usuarios.saveAndFlush(usuario));
        } catch (DataIntegrityViolationException ex) {
            // Dos cuentas guardando el mismo RUC al mismo tiempo: la base lo rechaza por el UNIQUE.
            throw new ConflictoException(RUC_REPETIDO, MENSAJE_RUC_REPETIDO);
        }
    }

    // Si la cuenta ya no existe, el token no sirve.
    private Usuario buscar(Long usuarioId) {
        return usuarios.findById(usuarioId)
                .orElseThrow(() -> new NoAutenticadoException("NO_AUTENTICADO", "Inicia sesión para continuar."));
    }
}
