package com.adbrand.core.notificacion;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.adbrand.core.notificacion.controller.PreferenciaNotificacionController;
import com.adbrand.core.notificacion.dto.PreferenciaNotificacionResponse;
import com.adbrand.core.notificacion.service.ServicioPreferenciasNotificacion;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

// HU 51: la app consulta y cambia sus preferencias de avisos.
@WebMvcTest(PreferenciaNotificacionController.class)
class PreferenciaNotificacionControllerTest {

    private static final String URL = "/api/notificaciones/preferencias";

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private ServicioPreferenciasNotificacion servicio;

    @Test
    void devuelveLasPreferenciasDeLaSesion() throws Exception {
        when(servicio.obtener(1L)).thenReturn(
                new PreferenciaNotificacionResponse(true, true, true, true, LocalDateTime.now()));

        mvc.perform(get(URL).requestAttr("usuarioId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.campanas").value(true))
                .andExpect(jsonPath("$.todasActivas").value(true));
    }

    @Test
    void cambiaSoloElInterruptorEnviado() throws Exception {
        when(servicio.actualizar(eq(1L), any())).thenReturn(
                new PreferenciaNotificacionResponse(false, true, true, false, LocalDateTime.now()));

        mvc.perform(patch(URL)
                        .requestAttr("usuarioId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"campanas": false}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.campanas").value(false))
                .andExpect(jsonPath("$.suscripciones").value(true));

        verify(servicio).actualizar(eq(1L), any());
    }
}
