package com.adbrand.core.contacto.service;

import com.adbrand.core.contacto.dto.ContactoDtos.EnviarMensajeRequest;
import com.adbrand.core.contacto.dto.ContactoDtos.MensajeEnviadoResponse;
import com.adbrand.core.contacto.entity.MensajeContacto;
import com.adbrand.core.contacto.repository.MensajeContactoRepository;
import com.adbrand.core.notificacion.service.ServicioCorreo;
import com.adbrand.core.shared.error.DemasiadosIntentosException;
import java.time.LocalDateTime;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Formulario de Contacto (soporte): guarda el mensaje para el panel de administración y avisa por correo al equipo.
// Si el correo falla, el mensaje igual queda guardado: no se le muestra un error a quien escribió.
@Service
public class ContactoService {

    static final int MAXIMO_POR_HORA = 5;
    static final Map<String, String> ASUNTOS = Map.of(
            "DUDA", "Tengo una duda",
            "PROBLEMA", "Algo no funciona",
            "PAGOS", "Pagos y planes",
            "SUGERENCIA", "Sugerencia",
            "OTRO", "Otro");

    private static final Logger log = LoggerFactory.getLogger(ContactoService.class);

    private final MensajeContactoRepository mensajes;
    private final ServicioCorreo correo;
    private final String correoSoporte;

    public ContactoService(MensajeContactoRepository mensajes, ServicioCorreo correo,
                           @Value("${contacto.correo-soporte:adbrandai.soporte@gmail.com}") String correoSoporte) {
        this.mensajes = mensajes;
        this.correo = correo;
        this.correoSoporte = correoSoporte;
    }

    @Transactional
    public MensajeEnviadoResponse enviar(Long usuarioId, EnviarMensajeRequest datos) {
        String correoCliente = datos.correo().trim();
        if (mensajes.countByCorreoIgnoreCaseAndFechaCreacionAfter(correoCliente, LocalDateTime.now().minusHours(1))
                >= MAXIMO_POR_HORA) {
            throw new DemasiadosIntentosException("DEMASIADOS_MENSAJES",
                    "Ya nos escribiste varias veces en la última hora. Te responderemos pronto.");
        }
        MensajeContacto guardado = mensajes.save(MensajeContacto.nuevo(usuarioId, datos.nombre().trim(), correoCliente,
                datos.asunto(), datos.mensaje().trim()));
        avisarAlEquipo(guardado);
        return new MensajeEnviadoResponse(guardado.getId(), guardado.getFechaCreacion());
    }

    private void avisarAlEquipo(MensajeContacto m) {
        try {
            correo.enviar(correoSoporte, "[Contacto] " + ASUNTOS.get(m.getAsunto()) + " - " + m.getNombre(),
                    """
                    Nuevo mensaje desde el formulario de Contacto de AdBrand.AI.

                    Nombre: %s
                    Correo: %s
                    Motivo: %s
                    %s
                    Mensaje:
                    %s

                    Respóndele a %s. También lo ves en el panel de administración (Mensajes de contacto).
                    """.formatted(m.getNombre(), m.getCorreo(), ASUNTOS.get(m.getAsunto()),
                            m.getUsuarioId() == null ? "Escribió sin iniciar sesión.\n" : "Empresa n.º " + m.getUsuarioId() + "\n",
                            m.getMensaje(), m.getCorreo()));
        } catch (RuntimeException ex) {
            log.warn("No se pudo avisar por correo del mensaje de contacto {}: {}", m.getId(), ex.getMessage());
        }
    }
}
