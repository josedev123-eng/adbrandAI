package com.adbrand.core.auth.service;

import com.adbrand.core.auth.dto.LoginRequest;
import com.adbrand.core.auth.dto.LoginResponse;
import com.adbrand.core.auth.dto.UsuarioResponse;
import com.adbrand.core.auth.entity.EstadoUsuario;
import com.adbrand.core.auth.entity.Usuario;
import com.adbrand.core.auth.repository.UsuarioRepository;
import com.adbrand.core.shared.error.AccesoDenegadoException;
import com.adbrand.core.shared.error.NoAutenticadoException;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// HU 22: inicio de sesión del usuario PYME y consulta de la sesión actual.
@Service
public class LoginService {

    static final String MENSAJE_CREDENCIALES = "Correo o contraseña incorrectos.";
    static final String MENSAJE_CUENTA_INACTIVA = "Tu cuenta está desactivada. Escribe a soporte para activarla.";

    private final UsuarioRepository usuarios;
    private final PasswordEncoder codificador;
    private final ServicioJwt jwt;

    // Hash de una contraseña que nadie usa: si el correo no existe se compara igual,
    // así la respuesta tarda lo mismo y no revela qué correos están registrados.
    private final String hashDeRelleno;

    public LoginService(UsuarioRepository usuarios, PasswordEncoder codificador, ServicioJwt jwt) {
        this.usuarios = usuarios;
        this.codificador = codificador;
        this.jwt = jwt;
        this.hashDeRelleno = codificador.encode("cuenta-que-no-existe");
    }

    @Transactional
    public LoginResponse iniciarSesion(LoginRequest datos) {
        Optional<Usuario> encontrado = usuarios.findByCorreo(RegistroService.normalizarCorreo(datos.correo()));
        String hash = encontrado.map(Usuario::getContrasenaHash).orElse(hashDeRelleno);
        boolean contrasenaCorrecta = codificador.matches(datos.contrasena(), hash);

        // El mismo mensaje si el correo no existe o si la contraseña está mal.
        if (encontrado.isEmpty() || !contrasenaCorrecta) {
            throw new NoAutenticadoException("CREDENCIALES_INVALIDAS", MENSAJE_CREDENCIALES);
        }

        Usuario usuario = encontrado.get();
        if (usuario.getEstado() == EstadoUsuario.INACTIVO) {
            throw new AccesoDenegadoException("CUENTA_INACTIVA", MENSAJE_CUENTA_INACTIVA);
        }

        usuario.setUltimoAcceso(LocalDateTime.now());
        return new LoginResponse(jwt.generar(usuario), UsuarioResponse.desde(usuario));
    }

    // El usuario del token. Si la cuenta ya no existe o se desactivó, la sesión deja de servir.
    @Transactional(readOnly = true)
    public UsuarioResponse sesion(Long usuarioId) {
        Usuario usuario = usuarios.findById(usuarioId)
                .filter(u -> u.getEstado() == EstadoUsuario.ACTIVO)
                .orElseThrow(() -> new NoAutenticadoException("NO_AUTENTICADO", "Inicia sesión para continuar."));
        return UsuarioResponse.desde(usuario);
    }
}
