package com.adbrand.core.estado;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

// HU 18 + HU 22: Django consulta /api/estado sin token, así que debe pasar el FiltroJwt sin pedir sesión.
@SpringBootTest(properties = "ia.mock=true")
@AutoConfigureMockMvc
class EstadoPublicoTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void elEstadoNoPideToken() throws Exception {
        mvc.perform(get("/api/estado"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.backend").value("ACTIVO"));
    }

    @Test
    void lasDemasRutasSiguenPidiendoToken() throws Exception {
        mvc.perform(get("/api/negocio/perfil"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("NO_AUTENTICADO"));
    }
}
