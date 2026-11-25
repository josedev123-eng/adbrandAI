package com.adbrand.core.auth.firebase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.adbrand.core.shared.error.NoAutenticadoException;
import io.jsonwebtoken.Jwts;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.time.Instant;
import java.util.Date;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

// App móvil: el token de Firebase solo vale si lo firmó Google para NUESTRO proyecto y no venció.
// Se prueba con llaves generadas aquí, sin conectarse a Google.
class VerificadorTokenFirebaseGoogleTest {

    private static final String PROYECTO = "adbrand-prueba";
    private static KeyPair llaves;
    private static KeyPair otrasLlaves;

    @BeforeAll
    static void crearLlaves() throws Exception {
        KeyPairGenerator generador = KeyPairGenerator.getInstance("RSA");
        generador.initialize(2048);
        llaves = generador.generateKeyPair();
        otrasLlaves = generador.generateKeyPair();
    }

    private final VerificadorTokenFirebaseGoogle verificador =
            new VerificadorTokenFirebaseGoogle(PROYECTO, kid -> "llave-1".equals(kid) ? llaves.getPublic() : null);

    private String token(KeyPair firmadoCon, String proyecto, Instant vence, Boolean verificado) {
        return Jwts.builder()
                .header().keyId("llave-1").and()
                .issuer("https://securetoken.google.com/" + proyecto)
                .audience().add(proyecto).and()
                .subject("uid-123")
                .claim("email", "Rosa@Hotmail.com")
                .claim("email_verified", verificado)
                .issuedAt(Date.from(vence.minusSeconds(3600)))
                .expiration(Date.from(vence))
                .signWith(firmadoCon.getPrivate(), Jwts.SIG.RS256)
                .compact();
    }

    @Test
    void unTokenValidoDevuelveElUidYElCorreo() {
        UsuarioFirebase usuario = verificador.verificar(token(llaves, PROYECTO, Instant.now().plusSeconds(600), true));

        assertThat(usuario.uid()).isEqualTo("uid-123");
        assertThat(usuario.correo()).isEqualTo("rosa@hotmail.com");
        assertThat(usuario.correoVerificado()).isTrue();
    }

    @Test
    void unTokenDeOtroProyectoNoSirve() {
        String ajeno = token(llaves, "otro-proyecto", Instant.now().plusSeconds(600), true);

        assertThatThrownBy(() -> verificador.verificar(ajeno))
                .isInstanceOf(NoAutenticadoException.class)
                .hasMessageContaining("Firebase");
    }

    @Test
    void unTokenVencidoNoSirve() {
        String vencido = token(llaves, PROYECTO, Instant.now().minusSeconds(3600), true);

        assertThatThrownBy(() -> verificador.verificar(vencido)).isInstanceOf(NoAutenticadoException.class);
    }

    @Test
    void unTokenFirmadoConOtraLlaveNoSirve() {
        String falso = token(otrasLlaves, PROYECTO, Instant.now().plusSeconds(600), true);

        assertThatThrownBy(() -> verificador.verificar(falso)).isInstanceOf(NoAutenticadoException.class);
    }

    @Test
    void textoQueNoEsUnTokenNoSirve() {
        assertThatThrownBy(() -> verificador.verificar("esto-no-es-un-token")).isInstanceOf(NoAutenticadoException.class);
    }

    @Test
    void sinElIdDelProyectoConfiguradoAvisaQueFaltaConfigurarlo() {
        VerificadorTokenFirebaseGoogle sinProyecto = new VerificadorTokenFirebaseGoogle("");

        assertThatThrownBy(() -> sinProyecto.verificar(token(llaves, PROYECTO, Instant.now().plusSeconds(600), true)))
                .isInstanceOf(NoAutenticadoException.class)
                .hasMessageContaining("FIREBASE_PROJECT_ID");
    }
}
