package com.adbrand.core.pago;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.adbrand.core.pago.controller.ComprobanteController;
import com.adbrand.core.pago.dto.ComprobanteResponse;
import com.adbrand.core.pago.service.ComprobanteService;
import com.adbrand.core.shared.error.RecursoNoEncontradoException;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

// HU 28: GET /api/pagos/{id}/comprobante devuelve el número y el detalle del pago.
@WebMvcTest(ComprobanteController.class)
class ComprobanteControllerTest {

    private static final String URL = "/api/pagos/5/comprobante";

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private ComprobanteService servicio;

    @Test
    void devuelveElComprobanteDelPago() throws Exception {
        when(servicio.obtener(1L, 5L)).thenReturn(new ComprobanteResponse("ADB-000005", 5L,
                "Dulce Perú", "ana@dulceperu.pe", "Pro", new BigDecimal("59.90"), "TARJETA",
                "OP-ABC123", LocalDate.of(2026, 10, 25), true));

        mvc.perform(get(URL).requestAttr("usuarioId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.numeroComprobante").value("ADB-000005"))
                .andExpect(jsonPath("$.pagoId").value(5))
                .andExpect(jsonPath("$.plan").value("Pro"))
                .andExpect(jsonPath("$.monto").value(59.90))
                .andExpect(jsonPath("$.correoEnviado").value(true));
    }

    @Test
    void unPagoInexistenteResponde404() throws Exception {
        when(servicio.obtener(1L, 5L)).thenThrow(new RecursoNoEncontradoException(
                "PAGO_NO_ENCONTRADO", "El pago no existe o no es tuyo."));

        mvc.perform(get(URL).requestAttr("usuarioId", 1L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("PAGO_NO_ENCONTRADO"));
    }
}
