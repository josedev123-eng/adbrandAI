package com.adbrand.core.notificacion.push;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.AndroidConfig;
import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.MulticastMessage;
import com.google.firebase.messaging.Notification;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// HU 50: envío real con Firebase Cloud Messaging (HTTP v1).
// Las credenciales NUNCA van en el código: se leen de variables de entorno o de
// un archivo de cuenta de servicio fuera del repositorio.
public class EnviadorPushFirebase implements EnviadorPush {

    private static final Logger log = LoggerFactory.getLogger(EnviadorPushFirebase.class);
    private static final String NOMBRE_APP = "adbrand-push";

    private final String rutaCredenciales;
    private final String proyecto;
    private FirebaseApp app;

    public EnviadorPushFirebase(String rutaCredenciales, String proyecto) {
        this.rutaCredenciales = rutaCredenciales;
        this.proyecto = proyecto;
    }

    // La app se crea una sola vez, aunque el servicio sea singleton: Firebase lo exige.
    private synchronized FirebaseApp app() throws IOException {
        if (app != null) {
            return app;
        }
        FirebaseOptions.Builder opciones = FirebaseOptions.builder().setCredentials(credenciales());
        if (proyecto != null && !proyecto.isBlank()) {
            opciones.setProjectId(proyecto);
        }
        app = FirebaseApp.getApps().stream()
                .filter(existente -> NOMBRE_APP.equals(existente.getName()))
                .findFirst()
                .orElseGet(() -> FirebaseApp.initializeApp(opciones.build(), NOMBRE_APP));
        return app;
    }

    private GoogleCredentials credenciales() throws IOException {
        if (rutaCredenciales != null && !rutaCredenciales.isBlank()) {
            try (FileInputStream archivo = new FileInputStream(rutaCredenciales)) {
                return GoogleCredentials.fromStream(archivo);
            }
        }
        // En Cloud Run / App Engine toma la cuenta de servicio del entorno.
        return GoogleCredentials.getApplicationDefault();
    }

    @Override
    public ResultadoEnvio enviar(List<String> tokens, String titulo, String cuerpo, Map<String, String> datos) {
        if (tokens == null || tokens.isEmpty()) {
            return new ResultadoEnvio(0, List.of());
        }
        try {
            MulticastMessage.Builder mensaje = MulticastMessage.builder()
                    .addAllTokens(tokens)
                    .setNotification(Notification.builder().setTitle(titulo).setBody(cuerpo).build())
                    .setAndroidConfig(AndroidConfig.builder()
                            .setPriority(AndroidConfig.Priority.HIGH)
                            .setTtl(86_400_000L)
                            .build());
            if (datos != null && !datos.isEmpty()) {
                mensaje.putAllData(datos);
            }

            BatchResponse respuesta = FirebaseMessaging.getInstance(app()).sendEachForMulticast(mensaje.build());
            List<String> invalidos = new ArrayList<>();
            for (int i = 0; i < respuesta.getResponses().size(); i++) {
                if (!respuesta.getResponses().get(i).isSuccessful()) {
                    FirebaseMessagingException error = respuesta.getResponses().get(i).getException();
                    MessagingErrorCode codigo = error == null ? null : error.getMessagingErrorCode();
                    if (codigo == MessagingErrorCode.UNREGISTERED || codigo == MessagingErrorCode.INVALID_ARGUMENT) {
                        invalidos.add(tokens.get(i));
                    } else if (error != null) {
                        log.warn("No se pudo enviar a un dispositivo: {}", error.getMessage());
                    }
                }
            }
            return new ResultadoEnvio(respuesta.getSuccessCount(), invalidos);
        } catch (IOException error) {
            log.error("Firebase no tiene credenciales válidas: {}", error.getMessage());
            return new ResultadoEnvio(0, List.of());
        } catch (FirebaseMessagingException error) {
            log.error("Firebase rechazó el envío: {}", error.getMessage());
            return new ResultadoEnvio(0, List.of());
        } catch (IllegalStateException error) {
            log.error("Firebase no está inicializado: {}", error.getMessage());
            return new ResultadoEnvio(0, List.of());
        }
    }
}
