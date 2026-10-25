package com.adbrand.core.estado;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.adbrand.core.estado.controller.EstadoController;
import com.adbrand.core.estado.dto.EstadoIa;
import com.adbrand.core.estado.dto.EstadoResponse;
import com.adbrand.core.estado.service.EstadoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

// HU 18: el endpoint que consulta Django para el panel del estado del servidor.
@WebMvcTest(EstadoController.class)
class EstadoControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private EstadoService servicio;

    @Test
    void respondeElEstadoDelBackendYDeLaIa() throws Exception {
        when(servicio.verificar()).thenReturn(
                new EstadoResponse("ACTIVO", new EstadoIa("CAIDO", null, "La IA no respondió en 5 segundos")));

        mvc.perform(get("/api/estado"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.backend").value("ACTIVO"))
                .andExpect(jsonPath("$.ia.estado").value("CAIDO"))
                .andExpect(jsonPath("$.ia.detalle").value("La IA no respondió en 5 segundos"));
    }
}
