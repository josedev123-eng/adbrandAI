package com.adbrand.core.auth.service;

import com.adbrand.core.auth.dto.CambiarContrasenaRequest;
import com.adbrand.core.auth.entity.CodigoRecuperacion;
import com.adbrand.core.auth.entity.EstadoUsuario;
import com.adbrand.core.auth.entity.Usuario;
import com.adbrand.core.auth.repository.CodigoRecuperacionRepository;
import com.adbrand.core.auth.repository.UsuarioRepository;
import com.adbrand.core.notificacion.service.ServicioCorreo;
import com.adbrand.core.shared.error.SolicitudInvalidaException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// HU 24: recuperar la contraseña con un código de 6 dígitos enviado al correo.
@Service
public class RecuperacionService {

    public static final String MENSAJE_SOLICITUD = "Si el correo está registrado, te enviamos un código.";
    public static final String MENSAJE_CAMBIADA = "Tu contraseña se cambió, inicia sesión.";
    static final String MENSAJE_CODIGO_INVALIDO = "El código no es válido o ya venció.";
    static final Duration VIGENCIA = Duration.ofMinutes(15);

    private static final Logger log = LoggerFactory.getLogger(RecuperacionService.class);

    private final UsuarioRepository usuarios;
    private final CodigoRecuperacionRepository codigos;
    private final PasswordEncoder codificador;
    private final ServicioCorreo correo;
    private final SecureRandom azar = new SecureRandom();

    public RecuperacionService(UsuarioRepository usuarios, CodigoRecuperacionRepository codigos,
                               PasswordEncoder codificador, ServicioCorreo correo) {
        this.usuarios = usuarios;
        this.codigos = codigos;
        this.codificador = codificador;
        this.correo = correo;
    }

    // Siempre termina igual, exista o no el correo: la respuesta no revela qué correos están registrados.
    @Transactional
    public void solicitarCodigo(String correoEscrito) {
        Optional<Usuario> encontrado = usuarios.findByCorreo(RegistroService.normalizarCorreo(correoEscrito));
        if (encontrado.isEmpty() || encontrado.get().getEstado() != EstadoUsuario.ACTIVO) {
            return;
        }
        Usuario usuario = encontrado.get();

        // Un código nuevo deja sin efecto los anteriores que no se usaron.
        invalidarCodigos(usuario.getId());

        String codigo = String.format("%06d", azar.nextInt(1_000_000));
        CodigoRecuperacion registro = new CodigoRecuperacion();
        registro.setUsuarioId(usuario.getId());
        registro.setCodigoHash(codificador.encode(codigo));
        registro.setFechaVencimiento(LocalDateTime.now().plus(VIGENCIA));
        codigos.save(registro);

        try {
            correo.enviar(usuario.getCorreo(), "Tu código para recuperar la contraseña de AdBrand.AI", """
                    Hola, %s:

                    Tu código para cambiar la contraseña es: %s

                    Vence en 15 minutos y sirve una sola vez.
                    Si no lo pediste, ignora este correo: tu contraseña sigue igual."""
                    .formatted(usuario.getNombre(), codigo));
        } catch (RuntimeException ex) {
            // Si el correo falla, la respuesta sigue siendo la misma; el error queda en el log.
            log.error("No se pudo enviar el código de recuperación a {}", usuario.getCorreo(), ex);
        }
    }

    @Transactional
    public void cambiarContrasena(CambiarContrasenaRequest datos) {
        Usuario usuario = usuarios.findByCorreo(RegistroService.normalizarCorreo(datos.correo()))
                .orElseThrow(this::codigoInvalido);

        LocalDateTime ahora = LocalDateTime.now();
        CodigoRecuperacion valido = codigos.findByUsuarioIdAndUsadoFalse(usuario.getId()).stream()
                .filter(codigo -> !codigo.estaVencido(ahora))
                .filter(codigo -> codificador.matches(datos.codigo(), codigo.getCodigoHash()))
                .findFirst()
                .orElseThrow(this::codigoInvalido);

        usuario.setContrasenaHash(codificador.encode(datos.contrasenaNueva()));
        valido.setUsado(true);
        invalidarCodigos(usuario.getId());
    }

    private void invalidarCodigos(Long usuarioId) {
        List<CodigoRecuperacion> pendientes = codigos.findByUsuarioIdAndUsadoFalse(usuarioId);
        pendientes.forEach(codigo -> codigo.setUsado(true));
    }

    private SolicitudInvalidaException codigoInvalido() {
        return new SolicitudInvalidaException("CODIGO_INVALIDO", MENSAJE_CODIGO_INVALIDO);
    }
}
