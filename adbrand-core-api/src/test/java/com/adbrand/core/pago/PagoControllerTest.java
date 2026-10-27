package com.adbrand.core.pago;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.adbrand.core.pago.controller.PagoController;
import com.adbrand.core.pago.dto.PagoResponse;
import com.adbrand.core.pago.service.OperacionYapeService;
import com.adbrand.core.pago.service.PagoService;
import com.adbrand.core.shared.error.ConflictoException;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

// HU 26: el endpoint responde 201 con el pago, 400 si los datos están mal y 409 si ya hay plan activo.
@WebMvcTest(PagoController.class)
class PagoControllerTest {

    private static final String URL = "/api/pagos/tarjeta";
    private static final String CUERPO = """
            {"planId":1,"numeroTarjeta":"4111 1111 1111 1111","titular":"Ana Quispe",
             "vencimiento":"12/30","cvv":"123"}
            """;

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private PagoService servicio;

    @MockitoBean
    private OperacionYapeService yape;

    @Test
    void devuelve201ConElPagoAprobado() throws Exception {
        when(servicio.pagarConTarjeta(eq(1L), any())).thenReturn(
                new PagoResponse(5L, "Pro", new BigDecimal("59.90"), "TARJETA", "APROBADO", "OP-123", LocalDate.of(2026, 10, 21)));

        mvc.perform(post(URL).requestAttr("usuarioId", 1L).contentType(MediaType.APPLICATION_JSON).content(CUERPO))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.plan").value("Pro"))
                .andExpect(jsonPath("$.estado").value("APROBADO"));
    }

    @Test
    void sinNumeroDeTarjetaResponde400ConElCampo() throws Exception {
        mvc.perform(post(URL).requestAttr("usuarioId", 1L).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"planId\":1,\"numeroTarjeta\":\"\",\"titular\":\"Ana Quispe\",\"vencimiento\":\"12/30\",\"cvv\":\"123\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("DATOS_INVALIDOS"))
                .andExpect(jsonPath("$.campos.numeroTarjeta").value("Escribe el número de la tarjeta."));
    }

    @Test
    void siYaTieneElPlanResponde409() throws Exception {
        when(servicio.pagarConTarjeta(eq(1L), any()))
                .thenThrow(new ConflictoException("SUSCRIPCION_ACTIVA", "Ya tienes una suscripción activa al plan Pro."));

        mvc.perform(post(URL).requestAttr("usuarioId", 1L).contentType(MediaType.APPLICATION_JSON).content(CUERPO))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("SUSCRIPCION_ACTIVA"));
    }
}
