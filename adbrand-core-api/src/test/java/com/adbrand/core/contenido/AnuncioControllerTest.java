package com.adbrand.core.contenido;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.adbrand.core.contenido.controller.AnuncioController;
import com.adbrand.core.contenido.dto.AnuncioGeneradoResponse;
import com.adbrand.core.contenido.dto.RedSocial;
import com.adbrand.core.contenido.entity.EstadoContenido;
import com.adbrand.core.contenido.service.AnuncioService;
import com.adbrand.core.ia.service.IaNoDisponibleException;
import com.adbrand.core.negocio.entity.Tono;
import com.adbrand.core.shared.error.RecursoNoEncontradoException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

// Prueba el endpoint como lo usaría la web: qué responde en cada caso (sin base de datos ni IA).
@WebMvcTest(AnuncioController.class)
class AnuncioControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private AnuncioService servicio;

    private static final String URL = "/api/contenido/anuncios";

    @Test
    void devuelveElAnuncioListoParaCopiar() throws Exception {
        when(servicio.generar(eq(1L), any())).thenReturn(
                new AnuncioGeneradoResponse(7L, "¡2x1 hoy!", RedSocial.INSTAGRAM, Tono.DIVERTIDO, "2x1 en tortas", false,
                        EstadoContenido.APROBADO, null));

        mvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"oferta\":\"2x1 en tortas\",\"redSocial\":\"INSTAGRAM\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.texto").value("¡2x1 hoy!"))
                .andExpect(jsonPath("$.tono").value("DIVERTIDO"))
                .andExpect(jsonPath("$.estado").value("APROBADO"));
    }

    @Test
    void sinOfertaResponde400ConElCampo() throws Exception {
        mvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content("{\"oferta\":\"  \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("DATOS_INVALIDOS"))
                .andExpect(jsonPath("$.campos.oferta").value("Escribe la oferta que quieres anunciar."))
                .andExpect(jsonPath("$.campos.redSocial").value("Selecciona la red social."));
    }

    @Test
    void ofertaDeMasDe300CaracteresResponde400() throws Exception {
        String larga = "a".repeat(301);
        mvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"oferta\":\"" + larga + "\",\"redSocial\":\"FACEBOOK\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.oferta").value("Máximo 300 caracteres."));
    }

    @Test
    void sinPerfilResponde404() throws Exception {
        when(servicio.generar(eq(1L), any())).thenThrow(
                new RecursoNoEncontradoException("PERFIL_NO_ENCONTRADO", "Todavía no registraste el perfil de tu negocio."));

        mvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"oferta\":\"Promo\",\"redSocial\":\"INSTAGRAM\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("PERFIL_NO_ENCONTRADO"));
    }

    @Test
    void siLaIaFallaResponde503ConMensajeClaro() throws Exception {
        when(servicio.generar(eq(1L), any())).thenThrow(new IaNoDisponibleException("caída", null));

        mvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"oferta\":\"Promo\",\"redSocial\":\"INSTAGRAM\"}"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.codigo").value("IA_NO_DISPONIBLE"))
                .andExpect(jsonPath("$.mensaje").value(
                        "El servicio de IA no responde en este momento. Intenta de nuevo en unos minutos."));
    }
}