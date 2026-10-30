package com.adbrand.core.auth.service;

import com.adbrand.core.auth.entity.EstadoUsuario;
import com.adbrand.core.auth.entity.TokenRenovacion;
import com.adbrand.core.auth.entity.Usuario;
import com.adbrand.core.auth.repository.TokenRenovacionRepository;
import com.adbrand.core.auth.repository.UsuarioRepository;
import com.adbrand.core.shared.error.AccesoDenegadoException;
import com.adbrand.core.shared.error.NoAutenticadoException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// HU 43: crea, rota y revoca los tokens de renovación de la app móvil.
// El token original nunca se guarda: en la tabla solo queda su hash SHA-256.
@Service
public class TokenRenovacionService {

    static final String MENSAJE_INVALIDO = "Tu sesión venció. Vuelve a iniciar sesión.";

    private final TokenRenovacionRepository tokens;
    private final UsuarioRepository usuarios;
    private final ServicioJwt jwt;
    private final SecureRandom aleatorio = new SecureRandom();
    private final long diasVigencia;

    public TokenRenovacionService(TokenRenovacionRepository tokens, UsuarioRepository usuarios,
                                  ServicioJwt jwt,
                                  @Value("${jwt.renovacion.dias:30}") long diasVigencia) {
        this.tokens = tokens;
        this.usuarios = usuarios;
        this.jwt = jwt;
        this.diasVigencia = diasVigencia;
    }

    // Devuelve el token en claro una sola vez; la base guarda el hash.
    @Transactional
    public String crear(Long usuarioId) {
        byte[] bytes = new byte[32];
        aleatorio.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        TokenRenovacion fila = new TokenRenovacion();
        fila.setUsuarioId(usuarioId);
        fila.setTokenHash(hash(token));
        fila.setFechaVencimiento(LocalDateTime.now().plusDays(diasVigencia));
        fila.setRevocado(false);
        tokens.save(fila);
        return token;
    }

    // Valida el token, revoca el anterior y entrega un par nuevo (rotación).
    @Transactional
    public Renovacion renovar(String token) {
        TokenRenovacion fila = buscarVigente(token);
        Usuario usuario = usuarios.findById(fila.getUsuarioId())
                .filter(encontrado -> encontrado.getEstado() == EstadoUsuario.ACTIVO)
                .orElseThrow(() -> new AccesoDenegadoException(
                        "CUENTA_INACTIVA", LoginService.MENSAJE_CUENTA_INACTIVA));

        fila.setRevocado(true);
        tokens.save(fila);

        return new Renovacion(jwt.generar(usuario), crear(usuario.getId()));
    }

    // Cerrar sesión es idempotente: si el token ya no existe o estaba revocado, no falla.
    @Transactional
    public void revocar(String token) {
        if (token == null || token.isBlank()) {
            return;
        }
        tokens.findByTokenHash(hash(token)).ifPresent(fila -> {
            fila.setRevocado(true);
            tokens.save(fila);
        });
    }

    private TokenRenovacion buscarVigente(String token) {
        if (token == null || token.isBlank()) {
            throw new NoAutenticadoException("TOKEN_RENOVACION_INVALIDO", MENSAJE_INVALIDO);
        }
        return tokens.findByTokenHash(hash(token))
                .filter(fila -> !fila.isRevocado())
                .filter(fila -> fila.getFechaVencimiento().isAfter(LocalDateTime.now()))
                .orElseThrow(() -> new NoAutenticadoException("TOKEN_RENOVACION_INVALIDO", MENSAJE_INVALIDO));
    }

    public static String hash(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 no está disponible", ex);
        }
    }

    // Lo que devuelve una renovación correcta.
    public record Renovacion(String token, String tokenRenovacion) {
    }
}
