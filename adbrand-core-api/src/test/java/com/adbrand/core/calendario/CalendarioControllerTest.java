package com.adbrand.core.calendario;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.adbrand.core.calendario.controller.CalendarioController;
import com.adbrand.core.calendario.dto.PublicacionResponse;
import com.adbrand.core.calendario.service.CalendarioService;
import com.adbrand.core.shared.error.SolicitudInvalidaException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

// HU 33: GET del mes, POST para programar, mes inválido y título vacío.
@WebMvcTest(CalendarioController.class)
class CalendarioControllerTest {

    private static final String URL = "/api/calendario";

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private CalendarioService servicio;

    private PublicacionResponse publicacion() {
        return new PublicacionResponse(3L, "Promoción de fin de semana", "INSTAGRAM",
                LocalDate.of(2026, 11, 14), LocalTime.of(18, 30), "PROGRAMADA");
    }

    @Test
    void devuelveLasPublicacionesDelMes() throws Exception {
        when(servicio.listar(1L, "2026-11")).thenReturn(List.of(publicacion()));

        mvc.perform(get(URL).requestAttr("usuarioId", 1L).param("mes", "2026-11"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].titulo").value("Promoción de fin de semana"))
                .andExpect(jsonPath("$[0].redSocial").value("INSTAGRAM"))
                .andExpect(jsonPath("$[0].fecha").value("2026-11-14"))
                .andExpect(jsonPath("$[0].estado").value("PROGRAMADA"));
    }

    @Test
    void unMesInvalidoResponde400() throws Exception {
        when(servicio.listar(1L, "noviembre"))
                .thenThrow(new SolicitudInvalidaException("MES_INVALIDO", "El mes debe tener el formato AAAA-MM."));

        mvc.perform(get(URL).requestAttr("usuarioId", 1L).param("mes", "noviembre"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("MES_INVALIDO"));
    }

    @Test
    void programaUnaPublicacionYDevuelve201() throws Exception {
        when(servicio.crear(eq(1L), any())).thenReturn(publicacion());

        mvc.perform(post(URL)
                        .requestAttr("usuarioId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"titulo":"Promoción de fin de semana","redSocial":"INSTAGRAM",
                                 "fecha":"2026-11-14","hora":"18:30:00"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(3))
                .andExpect(jsonPath("$.estado").value("PROGRAMADA"));
    }

    @Test
    void unTituloVacioResponde400ConElCampo() throws Exception {
        mvc.perform(post(URL)
                        .requestAttr("usuarioId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"titulo":"","redSocial":"INSTAGRAM","fecha":"2026-11-14","hora":"18:30:00"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("DATOS_INVALIDOS"))
                .andExpect(jsonPath("$.campos.titulo").value("Escribe un título para la publicación."));
    }
}
