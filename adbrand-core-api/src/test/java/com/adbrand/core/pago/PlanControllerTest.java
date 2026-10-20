package com.adbrand.core.pago;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.adbrand.core.pago.controller.PlanController;
import com.adbrand.core.pago.dto.PlanResponse;
import com.adbrand.core.pago.service.PlanService;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

// HU 25: la web recibe los planes con su precio y su límite de tokens.
@WebMvcTest(PlanController.class)
class PlanControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private PlanService servicio;

    @Test
    void devuelveLosPlanesDelMasBaratoAlMasCaro() throws Exception {
        when(servicio.listar()).thenReturn(List.of(
                new PlanResponse(1L, "Básico", new BigDecimal("29.90"), 50000, "Para empezar."),
                new PlanResponse(2L, "Pro", new BigDecimal("59.90"), 150000, "Para crecer."),
                new PlanResponse(3L, "Premium", new BigDecimal("99.90"), 400000, "Sin límites prácticos.")));

        mvc.perform(get("/api/planes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].nombre").value("Básico"))
                .andExpect(jsonPath("$[0].precio").value(29.90))
                .andExpect(jsonPath("$[0].limiteTokens").value(50000))
                .andExpect(jsonPath("$[2].nombre").value("Premium"));
    }
}
