package com.adbrand.core.auth.firebase;

import com.adbrand.core.shared.error.NoAutenticadoException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.LocatorAdapter;
import io.jsonwebtoken.ProtectedHeader;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.PublicKey;
import java.security.cert.CertificateFactory;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

// Verifica los ID tokens de Firebase como indica la documentación de Firebase, sin el SDK de administración
// y sin guardar ninguna clave secreta en el proyecto:
// - la firma (RS256) con las llaves públicas que Google publica para Firebase (se guardan mientras Google lo permite);
// - "aud" = el ID de nuestro proyecto e "iss" = https://securetoken.google.com/<proyecto>;
// - que no esté vencido, que "sub" (el uid) no esté vacío y que traiga el correo.
@Component
public class VerificadorTokenFirebaseGoogle implements VerificadorTokenFirebase {

    static final String URL_LLAVES =
            "https://www.googleapis.com/robot/v1/metadata/x509/securetoken@system.gserviceaccount.com";
    private static final Pattern MAX_AGE = Pattern.compile("max-age=(\\d+)");

    private final String proyecto;
    private final Function<String, PublicKey> llavePorId;

    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private final ObjectMapper json = new ObjectMapper();
    private Map<String, PublicKey> llaves = Map.of();
    private Instant llavesVencen = Instant.EPOCH;

    @Autowired
    public VerificadorTokenFirebaseGoogle(@Value("${firebase.project-id:}") String proyecto) {
        this.proyecto = proyecto == null ? "" : proyecto.trim();
        this.llavePorId = this::llaveDeGoogle;
    }

    // Para las pruebas: las llaves vienen de la prueba, no de Google.
    VerificadorTokenFirebaseGoogle(String proyecto, Function<String, PublicKey> llavePorId) {
        this.proyecto = proyecto;
        this.llavePorId = llavePorId;
    }

    @Override
    public UsuarioFirebase verificar(String idToken) {
        if (proyecto.isBlank()) {
            throw new NoAutenticadoException("FIREBASE_SIN_CONFIGURAR",
                    "El inicio de sesión con Firebase no está configurado en el servidor (falta FIREBASE_PROJECT_ID).");
        }
        try {
            Claims datos = Jwts.parser()
                    .keyLocator(new LocatorAdapter<Key>() {
                        @Override
                        protected Key locate(ProtectedHeader cabecera) {
                            if (!"RS256".equals(cabecera.getAlgorithm())) {
                                throw new JwtException("Algoritmo no permitido");
                            }
                            return llavePorId.apply(cabecera.getKeyId());
                        }
                    })
                    .requireIssuer("https://securetoken.google.com/" + proyecto)
                    .requireAudience(proyecto)
                    .clockSkewSeconds(60)
                    .build()
                    .parseSignedClaims(idToken)
                    .getPayload();

            String uid = datos.getSubject();
            String correo = datos.get("email", String.class);
            if (uid == null || uid.isBlank() || correo == null || correo.isBlank()) {
                throw new JwtException("Falta el uid o el correo");
            }
            boolean verificado = Boolean.TRUE.equals(datos.get("email_verified", Boolean.class));
            return new UsuarioFirebase(uid, correo.trim().toLowerCase(), verificado);
        } catch (JwtException | IllegalArgumentException ex) {
            throw new NoAutenticadoException("TOKEN_FIREBASE_INVALIDO",
                    "No pudimos confirmar tu sesión de Firebase. Vuelve a iniciar sesión.");
        }
    }

    // Llaves públicas de Google para Firebase, por "kid". Se piden de nuevo cuando vencen (Cache-Control: max-age).
    private synchronized PublicKey llaveDeGoogle(String kid) {
        if (Instant.now().isAfter(llavesVencen) || !llaves.containsKey(kid)) {
            descargarLlaves();
        }
        PublicKey llave = llaves.get(kid);
        if (llave == null) {
            throw new JwtException("Llave desconocida");
        }
        return llave;
    }

    private void descargarLlaves() {
        try {
            HttpResponse<String> respuesta = http.send(
                    HttpRequest.newBuilder(URI.create(URL_LLAVES)).timeout(Duration.ofSeconds(5)).GET().build(),
                    HttpResponse.BodyHandlers.ofString());
            JsonNode cuerpo = json.readTree(respuesta.body());
            CertificateFactory fabrica = CertificateFactory.getInstance("X.509");
            Map<String, PublicKey> nuevas = new HashMap<>();
            cuerpo.properties().forEach(entrada -> {
                try {
                    byte[] pem = entrada.getValue().asText().getBytes(StandardCharsets.UTF_8);
                    nuevas.put(entrada.getKey(), fabrica.generateCertificate(new ByteArrayInputStream(pem)).getPublicKey());
                } catch (Exception ex) {
                    // Un certificado que no se puede leer simplemente no se usa.
                }
            });
            llaves = Map.copyOf(nuevas);
            long segundos = respuesta.headers().firstValue("cache-control")
                    .map(MAX_AGE::matcher).filter(Matcher::find).map(m -> Long.parseLong(m.group(1))).orElse(3600L);
            llavesVencen = Instant.now().plusSeconds(segundos);
        } catch (Exception ex) {
            throw new JwtException("No se pudieron obtener las llaves de Google", ex);
        }
    }
}
