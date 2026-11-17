package com.adbrand.core.auth.service;

import com.adbrand.core.auth.dto.LoginResponse;
import com.adbrand.core.auth.entity.CodigoVerificacionCorreo;
import com.adbrand.core.auth.entity.EstadoUsuario;
import com.adbrand.core.auth.entity.Usuario;
import com.adbrand.core.auth.repository.CodigoVerificacionCorreoRepository;
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

// Verificar el correo al registrarse en la web: un código de 6 dígitos que llega al correo.
// Hasta escribirlo, la cuenta no puede iniciar sesión (LoginService responde CORREO_NO_VERIFICADO).
@Service
public class VerificacionCorreoService {

    public static final String MENSAJE_REENVIO = "Si la cuenta está pendiente de verificar, te enviamos un código nuevo.";
    static final String MENSAJE_CODIGO_INVALIDO = "El código no es correcto o ya venció. Pide uno nuevo.";
    static final Duration VIGENCIA = Duration.ofMinutes(15);
    static final int INTENTOS_MAXIMOS = 5;
    // Para no llenar la bandeja: si ya se mandó un código hace menos de un minuto, no se manda otro.
    static final Duration ESPERA_REENVIO = Duration.ofSeconds(60);

    private static final Logger log = LoggerFactory.getLogger(VerificacionCorreoService.class);

    private final UsuarioRepository usuarios;
    private final CodigoVerificacionCorreoRepository codigos;
    private final PasswordEncoder codificador;
    private final ServicioCorreo correo;
    private final LoginService login;
    private final SecureRandom azar = new SecureRandom();

    public VerificacionCorreoService(UsuarioRepository usuarios, CodigoVerificacionCorreoRepository codigos,
                                     PasswordEncoder codificador, ServicioCorreo correo, LoginService login) {
        this.usuarios = usuarios;
        this.codigos = codigos;
        this.codificador = codificador;
        this.correo = correo;
        this.login = login;
    }

    // Manda un código nuevo y anula los anteriores. No hace nada si ya se mandó uno hace menos de un minuto.
    @Transactional
    public void enviarCodigo(Usuario usuario) {
        LocalDateTime ahora = LocalDateTime.now();
        List<CodigoVerificacionCorreo> pendientes =
                codigos.findByUsuarioIdAndUsadoFalseOrderByFechaCreacionDesc(usuario.getId());
        if (!pendientes.isEmpty() && pendientes.get(0).getFechaCreacion().isAfter(ahora.minus(ESPERA_REENVIO))) {
            return;
        }
        pendientes.forEach(pendiente -> pendiente.setUsado(true));

        String codigo = String.format("%06d", azar.nextInt(1_000_000));
        CodigoVerificacionCorreo registro = new CodigoVerificacionCorreo();
        registro.setUsuarioId(usuario.getId());
        registro.setCodigoHash(codificador.encode(codigo));
        registro.setFechaVencimiento(ahora.plus(VIGENCIA));
        codigos.saveAndFlush(registro);

        try {
            correo.enviar(usuario.getCorreo(), "Confirma tu correo en AdBrand.AI", """
                    Hola, %s:

                    Gracias por crear tu cuenta en AdBrand.AI.
                    Tu código para confirmar el correo es: %s

                    Vence en 15 minutos y sirve una sola vez.
                    Si no creaste esta cuenta, ignora este correo."""
                    .formatted(usuario.getNombre(), codigo));
        } catch (RuntimeException ex) {
            // Si el correo falla, la persona puede pedir otro código; el error queda en el log.
            log.error("No se pudo enviar el código de verificación a {}", usuario.getCorreo(), ex);
        }
    }

    // "Reenviar código": responde siempre igual, exista o no la cuenta, para no revelar qué correos están registrados.
    @Transactional
    public void reenviar(String correoEscrito) {
        usuarios.findByCorreo(RegistroService.normalizarCorreo(correoEscrito))
                .filter(usuario -> !usuario.isCorreoVerificado() && usuario.getEstado() == EstadoUsuario.ACTIVO)
                .ifPresent(this::enviarCodigo);
    }

    // Con el código correcto el correo queda verificado y la persona entra directo (recibe sus tokens).
    // noRollbackFor: el intento equivocado se cuenta aunque la respuesta sea un error.
    @Transactional(noRollbackFor = SolicitudInvalidaException.class)
    public LoginResponse verificar(String correoEscrito, String codigoEscrito) {
        Usuario usuario = usuarios.findByCorreo(RegistroService.normalizarCorreo(correoEscrito))
                .orElseThrow(this::codigoInvalido);
        if (usuario.isCorreoVerificado()) {
            throw new SolicitudInvalidaException("CORREO_YA_VERIFICADO", "Tu correo ya está verificado. Inicia sesión.");
        }

        LocalDateTime ahora = LocalDateTime.now();
        Optional<CodigoVerificacionCorreo> vigente = codigos
                .findByUsuarioIdAndUsadoFalseOrderByFechaCreacionDesc(usuario.getId()).stream()
                .findFirst()
                .filter(codigo -> !codigo.estaVencido(ahora) && codigo.getIntentos() < INTENTOS_MAXIMOS);
        if (vigente.isEmpty()) {
            throw codigoInvalido();
        }

        CodigoVerificacionCorreo codigo = vigente.get();
        if (!codificador.matches(codigoEscrito, codigo.getCodigoHash())) {
            codigo.setIntentos(codigo.getIntentos() + 1);
            codigos.saveAndFlush(codigo);
            throw codigoInvalido();
        }

        codigo.setUsado(true);
        usuario.setCorreoVerificado(true);
        return login.entregarSesion(usuario);
    }

    private SolicitudInvalidaException codigoInvalido() {
        return new SolicitudInvalidaException("CODIGO_INVALIDO", MENSAJE_CODIGO_INVALIDO);
    }
}
