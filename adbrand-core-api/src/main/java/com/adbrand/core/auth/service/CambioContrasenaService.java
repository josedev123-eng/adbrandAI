package com.adbrand.core.auth.service;

import com.adbrand.core.auth.entity.Usuario;
import com.adbrand.core.auth.repository.UsuarioRepository;
import com.adbrand.core.shared.error.NoAutenticadoException;
import com.adbrand.core.shared.error.SolicitudInvalidaException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Mi cuenta: la empresa conectada cambia su contraseña. Debe escribir la actual (si alguien deja la sesión
// abierta en otra computadora, no puede cambiarla sin saberla).
@Service
public class CambioContrasenaService {

    static final String MENSAJE_ACTUAL_INCORRECTA = "La contraseña actual no es correcta.";
    static final String MENSAJE_IGUAL = "La contraseña nueva debe ser distinta de la actual.";

    private final UsuarioRepository usuarios;
    private final PasswordEncoder codificador;

    public CambioContrasenaService(UsuarioRepository usuarios, PasswordEncoder codificador) {
        this.usuarios = usuarios;
        this.codificador = codificador;
    }

    @Transactional
    public void cambiar(Long usuarioId, String actual, String nueva) {
        Usuario usuario = usuarios.findById(usuarioId)
                .orElseThrow(() -> new NoAutenticadoException("NO_AUTENTICADO", "Inicia sesión para continuar."));

        if (!codificador.matches(actual, usuario.getContrasenaHash())) {
            throw new SolicitudInvalidaException("CONTRASENA_ACTUAL_INCORRECTA", MENSAJE_ACTUAL_INCORRECTA);
        }
        if (actual.equals(nueva)) {
            throw new SolicitudInvalidaException("CONTRASENA_IGUAL", MENSAJE_IGUAL);
        }
        usuario.setContrasenaHash(codificador.encode(nueva));
    }
}
