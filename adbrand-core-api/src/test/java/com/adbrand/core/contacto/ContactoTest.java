package com.adbrand.core.contacto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.adbrand.core.contacto.repository.MensajeContactoRepository;
import com.adbrand.core.notificacion.service.ServicioCorreo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

// Formulario de Contacto: se envía sin sesión, se guarda y avisa por correo al equipo de soporte.
// Usa la base local; cada prueba se deshace al terminar (@Transactional).
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ContactoTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private MensajeContactoRepository mensajes;

    @MockitoBean
    private ServicioCorreo correo;

    private ResultActions enviar(String json) throws Exception {
        return mvc.perform(post("/api/contacto").contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private static String mensaje(String correoCliente) {
        return """
                {"nombre": "Rosa Quispe", "correo": "%s", "asunto": "PROBLEMA",
                 "mensaje": "No puedo descargar el video de mi anuncio."}""".formatted(correoCliente);
    }

    @Test
    void sinSesionSeGuardaYAvisaAlEquipo() throws Exception {
        long antes = mensajes.count();

        enviar(mensaje("rosa.contacto@adbrand.test")).andExpect(status().isCreated()).andExpect(jsonPath("$.id").isNumber());

        assertThat(mensajes.count()).isEqualTo(antes + 1);
        verify(correo).enviar(eq("adbrandai.soporte@gmail.com"), contains("Algo no funciona"), contains("rosa.contacto@adbrand.test"));
    }

    @Test
    void siElCorreoFallaElMensajeIgualQuedaGuardado() throws Exception {
        doThrow(new IllegalStateException("sin SMTP")).when(correo).enviar(anyString(), anyString(), anyString());

        enviar(mensaje("rosa.sinsmtp@adbrand.test")).andExpect(status().isCreated());
    }

    @Test
    void validaLosCampos() throws Exception {
        enviar("""
                {"nombre": "", "correo": "no-es-correo", "asunto": "SPAM", "mensaje": "hola"}""")
                .andExpect(status().isBadRequest());
    }

    @Test
    void noAceptaMasDeCincoMensajesPorHoraDelMismoCorreo() throws Exception {
        for (int i = 0; i < 5; i++) {
            enviar(mensaje("rosa.insiste@adbrand.test")).andExpect(status().isCreated());
        }
        enviar(mensaje("rosa.insiste@adbrand.test")).andExpect(status().isTooManyRequests());
    }
}
