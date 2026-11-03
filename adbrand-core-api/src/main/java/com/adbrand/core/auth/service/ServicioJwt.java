package com.adbrand.core.auth.service;

import com.adbrand.core.auth.entity.Usuario;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

// HU 22: crea y lee los tokens JWT del usuario PYME. Firma con jwt.secreto (HS256).
@Service
public class ServicioJwt {

    private static final String CLAIM_CORREO = "correo";

    private final SecretKey clave;
    private final Duration duracion;

    public ServicioJwt(@Value("${jwt.secreto}") String secreto, @Value("${jwt.horas:8}") long horas) {
        // Keys.hmacShaKeyFor exige al menos 32 bytes; con un secreto corto Spring no arranca y lo dice.
        this.clave = Keys.hmacShaKeyFor(secreto.getBytes(StandardCharsets.UTF_8));
        this.duracion = Duration.ofHours(horas);
    }

    // Token para el usuario que acaba de iniciar sesión: su id va en "sub" y vence a las jwt.horas.
    public String generar(Usuario usuario) {
        Instant ahora = Instant.now();
        return generar(usuario.getId(), usuario.getCorreo(), ahora, ahora.plus(duracion));
    }

    public String generar(Long usuarioId, String correo, Instant emitido, Instant vence) {
        return Jwts.builder()
                .subject(String.valueOf(usuarioId))
                .claim(CLAIM_CORREO, correo)
                .issuedAt(Date.from(emitido))
                .expiration(Date.from(vence))
                .signWith(clave)
                .compact();
    }

    // Devuelve el id del usuario si el token tiene una firma válida y no venció.
    public Long leerUsuarioId(String token) {
        try {
            String sujeto = Jwts.parser().verifyWith(clave).build()
                    .parseSignedClaims(token)
                    .getPayload()
                    .getSubject();
            return Long.valueOf(sujeto);
        } catch (JwtException | IllegalArgumentException ex) {
            throw new TokenInvalidoException(ex);
        }
    }

    // El token está alterado, vencido o mal escrito.
    public static class TokenInvalidoException extends RuntimeException {
        TokenInvalidoException(Throwable causa) {
            super("Token inválido", causa);
        }
    }
}
