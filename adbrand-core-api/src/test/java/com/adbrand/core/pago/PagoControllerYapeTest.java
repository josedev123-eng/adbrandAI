package com.adbrand.core.pago;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.adbrand.core.pago.controller.PagoController;
import com.adbrand.core.pago.dto.OperacionYapeResponse;
import com.adbrand.core.pago.dto.PagoResponse;
import com.adbrand.core.pago.service.OperacionYapeService;
import com.adbrand.core.pago.service.PagoService;
import com.adbrand.core.shared.error.RecursoNoEncontradoException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

// HU 27: endpoints de Yape (registro del QR, confirmación y pendientes).
@WebMvcTest(PagoController.class)
class PagoControllerYapeTest {

    private static final String REGISTRAR = "/api/pagos/yape";
    private static final String CONFIRMAR = "/api/pagos/yape/7/confirmar";
    private static final String PENDIENTES = "/api/pagos/yape/pendientes";

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private PagoService servicio;

    @MockitoBean
    private OperacionYapeService yape;

    private OperacionYapeResponse operacion() {
        return new OperacionYapeResponse(7L, "YPE-ABC12345", "Pro", new BigDecimal("59.90"),
                "PENDIENTE", "987654321", LocalDateTime.of(2026, 10, 23, 10, 0),
                "yape://pagar?codigo=YPE-ABC12345&monto=59.90&moneda=PEN");
    }

    @Test
    void registrarDevuelve201ConElQrPendiente() throws Exception {
        when(yape.registrar(eq(1L), any())).thenReturn(operacion());

        mvc.perform(post(REGISTRAR)
                        .requestAttr("usuarioId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"planId\":2,\"celular\":\"987654321\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.codigo").value("YPE-ABC12345"))
                .andExpect(jsonPath("$.estado").value("PENDIENTE"))
                .andExpect(jsonPath("$.contenidoQr").value(org.hamcrest.Matchers.containsString("yape://")));
    }

    @Test
    void confirmarDevuelveElPagoAprobado() throws Exception {
        when(yape.confirmar(1L, 7L)).thenReturn(new PagoResponse(5L, "Pro", new BigDecimal("59.90"),
                "YAPE", "APROBADO", "YPE-ABC12345", LocalDate.of(2026, 10, 23)));

        mvc.perform(post(CONFIRMAR).requestAttr("usuarioId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.metodo").value("YAPE"))
                .andExpect(jsonPath("$.referencia").value("YPE-ABC12345"));
    }

    @Test
    void confirmarUnaOperacionInexistenteResponde404() throws Exception {
        when(yape.confirmar(1L, 7L)).thenThrow(new RecursoNoEncontradoException(
                "OPERACION_NO_ENCONTRADA", "La operación de Yape no existe o no es tuya."));

        mvc.perform(post(CONFIRMAR).requestAttr("usuarioId", 1L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("OPERACION_NO_ENCONTRADA"));
    }

    @Test
    void pendientesDevuelveLaListaDelUsuario() throws Exception {
        when(yape.pendientes(1L)).thenReturn(List.of(operacion()));

        mvc.perform(get(PENDIENTES).requestAttr("usuarioId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].codigo").value("YPE-ABC12345"))
                .andExpect(jsonPath("$[0].plan").value("Pro"));
    }

    @Test
    void unCelularQueNoEmpiezaConNueveResponde400() throws Exception {
        mvc.perform(post(REGISTRAR)
                        .requestAttr("usuarioId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"planId\":2,\"celular\":\"123456789\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("DATOS_INVALIDOS"))
                .andExpect(jsonPath("$.campos.celular").exists());
    }
}
