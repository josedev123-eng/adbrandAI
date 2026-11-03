package com.adbrand.core.auth.service;

import com.adbrand.core.auth.dto.RegistroRequest;
import com.adbrand.core.auth.dto.UsuarioResponse;
import com.adbrand.core.auth.entity.Usuario;
import com.adbrand.core.auth.repository.UsuarioRepository;
import com.adbrand.core.shared.error.ConflictoException;
import java.util.Locale;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// HU 21: crea la cuenta de una empresa con el correo único y la contraseña cifrada.
@Service
public class RegistroService {

    static final String CORREO_REPETIDO = "CORREO_REPETIDO";
    static final String MENSAJE_CORREO_REPETIDO = "Ese correo ya está registrado.";

    private final UsuarioRepository usuarios;
    private final PasswordEncoder codificador;

    public RegistroService(UsuarioRepository usuarios, PasswordEncoder codificador) {
        this.usuarios = usuarios;
        this.codificador = codificador;
    }

    // El correo se guarda sin espacios y en minúsculas: "Ana@Mail.com" y "ana@mail.com" son la misma cuenta.
    public static String normalizarCorreo(String correo) {
        return correo == null ? null : correo.trim().toLowerCase(Locale.ROOT);
    }

    @Transactional
    public UsuarioResponse registrar(RegistroRequest datos) {
        String correo = normalizarCorreo(datos.correo());
        if (usuarios.existsByCorreo(correo)) {
            throw new ConflictoException(CORREO_REPETIDO, MENSAJE_CORREO_REPETIDO);
        }

        Usuario usuario = new Usuario();
        usuario.setNombre(datos.nombre().trim());
        usuario.setNombreEmpresa(datos.nombreEmpresa().trim());
        usuario.setCorreo(correo);
        usuario.setContrasenaHash(codificador.encode(datos.contrasena()));

        try {
            return UsuarioResponse.desde(usuarios.saveAndFlush(usuario));
        } catch (DataIntegrityViolationException ex) {
            // Dos registros con el mismo correo al mismo tiempo: la base lo rechaza por el UNIQUE.
            throw new ConflictoException(CORREO_REPETIDO, MENSAJE_CORREO_REPETIDO);
        }
    }
}
