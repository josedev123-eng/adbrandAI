package com.adbrand.core.notificacion;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.adbrand.core.notificacion.controller.NotificacionController;
import com.adbrand.core.notificacion.dto.DispositivoResponse;
import com.adbrand.core.notificacion.service.ServicioDispositivos;
import com.adbrand.core.notificacion.service.ServicioNotificaciones;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

// HU 50: registro del token, validación y baja del dispositivo.
@WebMvcTest(NotificacionController.class)
class NotificacionControllerTest {

    private static final String URL = "/api/notificaciones/dispositivos";

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private ServicioDispositivos servicio;

    @MockitoBean
    private ServicioNotificaciones notificaciones;

    @Test
    void registraElDispositivoYDevuelve201() throws Exception {
        when(servicio.registrar(eq(1L), any())).thenReturn(
                new DispositivoResponse(5L, "android", "Pixel 6", true, LocalDateTime.now()));

        mvc.perform(post(URL)
                        .requestAttr("usuarioId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fcmToken":"token-abc","platform":"android","deviceName":"Pixel 6"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.platform").value("android"))
                .andExpect(jsonPath("$.activo").value(true));
    }

    @Test
    void unTokenVacioResponde400ConElCampoMarcado() throws Exception {
        mvc.perform(post(URL)
                        .requestAttr("usuarioId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fcmToken":"  ","platform":"android"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("DATOS_INVALIDOS"))
                .andExpect(jsonPath("$.campos.fcmToken").exists());
    }

    @Test
    void unaPlataformaDesconocidaResponde400() throws Exception {
        mvc.perform(post(URL)
                        .requestAttr("usuarioId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fcmToken":"token-abc","platform":"windows"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.platform").exists());
    }

    @Test
    void eliminaElDispositivoYDevuelve204() throws Exception {
        mvc.perform(delete(URL + "/token-abc").requestAttr("usuarioId", 1L))
                .andExpect(status().isNoContent());
    }

    @Test
    void elAvisoDePruebaSeEnviaSoloAlUsuarioDeLaSesion() throws Exception {
        when(notificaciones.enviarAUsuario(eq(1L), any(), any(), any())).thenReturn(2);

        mvc.perform(post("/api/notificaciones/prueba")
                        .requestAttr("usuarioId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tipo":"system_alert"}
                                """))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.enviados").value(2));

        verify(notificaciones).enviarAUsuario(eq(1L), any(), any(), any());
    }
}
