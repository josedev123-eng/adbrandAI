package com.adbrand.core.auth.service;

import com.adbrand.core.auth.dto.LoginRequest;
import com.adbrand.core.auth.dto.LoginResponse;
import com.adbrand.core.auth.dto.UsuarioResponse;
import com.adbrand.core.auth.entity.EstadoUsuario;
import com.adbrand.core.auth.entity.Usuario;
import com.adbrand.core.auth.repository.UsuarioRepository;
import com.adbrand.core.shared.error.AccesoDenegadoException;
import com.adbrand.core.shared.error.DemasiadosIntentosException;
import com.adbrand.core.shared.error.NoAutenticadoException;
import java.time.Duration;
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
    // Bloqueo por intentos fallidos (V54).
    static final int INTENTOS_MAXIMOS = 5;
    static final Duration DURACION_BLOQUEO = Duration.ofMinutes(15);

    private final UsuarioRepository usuarios;
    private final PasswordEncoder codificador;
    private final ServicioJwt jwt;
    private final TokenRenovacionService renovaciones;

    // Hash de una contraseña que nadie usa: si el correo no existe se compara igual,
    // así la respuesta tarda lo mismo y no revela qué correos están registrados.
    private final String hashDeRelleno;

    public LoginService(UsuarioRepository usuarios, PasswordEncoder codificador, ServicioJwt jwt,
                        TokenRenovacionService renovaciones) {
        this.usuarios = usuarios;
        this.codificador = codificador;
        this.jwt = jwt;
        this.renovaciones = renovaciones;
        this.hashDeRelleno = codificador.encode("cuenta-que-no-existe");
    }

    // noRollbackFor: el intento fallido se guarda aunque la respuesta sea un error.
    @Transactional(noRollbackFor = {NoAutenticadoException.class, DemasiadosIntentosException.class})
    public LoginResponse iniciarSesion(LoginRequest datos) {
        Optional<Usuario> encontrado = usuarios.findByCorreo(RegistroService.normalizarCorreo(datos.correo()));
        String hash = encontrado.map(Usuario::getContrasenaHash).orElse(hashDeRelleno);
        boolean contrasenaCorrecta = codificador.matches(datos.contrasena(), hash);
        LocalDateTime ahora = LocalDateTime.now();

        // Bloqueada: no entra ni con la contraseña correcta hasta que pasen los 15 minutos.
        if (encontrado.isPresent() && encontrado.get().estaBloqueado(ahora)) {
            throw bloqueada(encontrado.get(), ahora);
        }

        // El mismo mensaje si el correo no existe o si la contraseña está mal.
        if (encontrado.isEmpty() || !contrasenaCorrecta) {
            encontrado.ifPresent(usuario -> registrarFallo(usuario, ahora));
            throw new NoAutenticadoException("CREDENCIALES_INVALIDAS", MENSAJE_CREDENCIALES);
        }

        Usuario usuario = encontrado.get();
        usuario.setIntentosFallidos(0);
        usuario.setBloqueadoHasta(null);
        return entregarSesion(usuario);
    }

    // Suma un intento fallido; al quinto seguido bloquea la cuenta 15 minutos y avisa.
    private void registrarFallo(Usuario usuario, LocalDateTime ahora) {
        int intentos = usuario.getIntentosFallidos() + 1;
        if (intentos >= INTENTOS_MAXIMOS) {
            usuario.setIntentosFallidos(0);
            usuario.setBloqueadoHasta(ahora.plus(DURACION_BLOQUEO));
            usuarios.saveAndFlush(usuario);
            throw bloqueada(usuario, ahora);
        }
        usuario.setIntentosFallidos(intentos);
        usuarios.saveAndFlush(usuario);
    }

    private DemasiadosIntentosException bloqueada(Usuario usuario, LocalDateTime ahora) {
        long minutos = Math.max(1, (Duration.between(ahora, usuario.getBloqueadoHasta()).getSeconds() + 59) / 60);
        return new DemasiadosIntentosException("CUENTA_BLOQUEADA",
                "Escribiste mal la contraseña %d veces seguidas. Por seguridad, la cuenta se bloqueó; intenta de nuevo en %d %s o cambia tu contraseña con \"¿Olvidaste tu contraseña?\"."
                        .formatted(INTENTOS_MAXIMOS, minutos, minutos == 1 ? "minuto" : "minutos"));
    }

    // Revisa que la cuenta esté activa y entrega los tokens. La usa también el inicio de sesión con Firebase (app móvil).
    @Transactional
    public LoginResponse entregarSesion(Usuario usuario) {
        if (usuario.getEstado() == EstadoUsuario.INACTIVO) {
            // Si la suspendió el administrador, se explica el motivo (V45).
            String mensaje = usuario.getFechaSuspension() == null
                    ? MENSAJE_CUENTA_INACTIVA
                    : com.adbrand.core.auth.config.MensajesCuenta.suspendida(usuario.getMotivoSuspension());
            throw new AccesoDenegadoException("CUENTA_INACTIVA", mensaje);
        }

        usuario.setUltimoAcceso(LocalDateTime.now());
        // HU 43: además del JWT de acceso se entrega un token de renovación de larga duración.
        return new LoginResponse(jwt.generar(usuario), renovaciones.crear(usuario.getId()),
                UsuarioResponse.desde(usuario));
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
