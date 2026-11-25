package com.adbrand.core.contenido;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.adbrand.core.contenido.controller.OpcionesAnuncioController;
import com.adbrand.core.contenido.dto.AnuncioGeneradoResponse;
import com.adbrand.core.contenido.dto.GenerarAnuncioRequest;
import com.adbrand.core.contenido.dto.OpcionAnuncioResponse;
import com.adbrand.core.contenido.dto.OpcionElegidaResponse;
import com.adbrand.core.contenido.dto.OpcionesAnuncioResponse;
import com.adbrand.core.contenido.dto.RedSocial;
import com.adbrand.core.contenido.dto.VersionAnuncioResponse;
import com.adbrand.core.contenido.dto.VersionesAnuncioResponse;
import com.adbrand.core.contenido.entity.EstadoContenido;
import com.adbrand.core.contenido.service.OpcionesAnuncioService;
import com.adbrand.core.contenido.service.RetoqueAnuncioService;
import com.adbrand.core.negocio.entity.Tono;
import com.adbrand.core.shared.error.RecursoNoEncontradoException;
import com.adbrand.core.shared.error.SolicitudInvalidaException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

// HU 55: la app móvil pide una nueva versión del anuncio elegido desde el botón
// "Nueva versión" y compara la redacción anterior con la nueva.
@WebMvcTest(OpcionesAnuncioController.class)
class OpcionesAnuncioControllerTest {

    private static final String URL = "/api/contenido/anuncios/opciones";

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private OpcionesAnuncioService servicio;

    @MockitoBean
    private RetoqueAnuncioService retoques;

    // El diseño de las opciones se prueba aparte (DisenoAnuncioTest); aquí deja pasar la respuesta tal cual.
    @MockitoBean
    private com.adbrand.core.contenido.service.DisenadorOpciones disenos;

    @org.junit.jupiter.api.BeforeEach
    void sinDiseno() {
        when(disenos.conDisenos(any(), any())).thenAnswer(i -> i.getArgument(1));
        when(disenos.conDiseno(any(), any())).thenAnswer(i -> i.getArgument(1));
    }

    private static OpcionAnuncioResponse opcion(long id, int numero, boolean elegida) {
        return new OpcionAnuncioResponse(id, numero, "Foto realista", "Texto " + numero,
                "data:image/jpeg;base64,QUJD", "cloudflare", false, false, elegida);
    }

    @Test
    void generaLasCuatroOpciones() throws Exception {
        when(servicio.generar(eq(1L), any(GenerarAnuncioRequest.class))).thenReturn(new OpcionesAnuncioResponse(
                7L, "2x1 en pan de yema", RedSocial.INSTAGRAM,
                List.of(opcion(101, 1, false), opcion(102, 2, false), opcion(103, 3, false), opcion(104, 4, false))));

        mvc.perform(post(URL).requestAttr("usuarioId", 1L).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"oferta\": \"2x1 en pan de yema\", \"redSocial\": \"INSTAGRAM\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.generacionId").value(7))
                .andExpect(jsonPath("$.opciones.length()").value(4))
                .andExpect(jsonPath("$.opciones[1].texto").value("Texto 2"))
                .andExpect(jsonPath("$.opciones[1].imagen").value("data:image/jpeg;base64,QUJD"));
    }

    @Test
    void sinOfertaResponde400() throws Exception {
        mvc.perform(post(URL).requestAttr("usuarioId", 1L).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"oferta\": \"  \", \"redSocial\": \"INSTAGRAM\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void elegirDevuelveElAnuncioGuardado() throws Exception {
        when(servicio.elegir(1L, 102L)).thenReturn(new OpcionElegidaResponse(
                new AnuncioGeneradoResponse(50L, "Texto 2", RedSocial.INSTAGRAM, Tono.CERCANO, false,
                        EstadoContenido.APROBADO, null),
                opcion(102, 2, true)));

        mvc.perform(post(URL + "/102/elegir").requestAttr("usuarioId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.anuncio.id").value(50))
                .andExpect(jsonPath("$.anuncio.estado").value("APROBADO"))
                .andExpect(jsonPath("$.opcion.elegida").value(true));
    }

    @Test
    void elegirUnaOpcionAjenaResponde404() throws Exception {
        when(servicio.elegir(1L, 999L)).thenThrow(
                new RecursoNoEncontradoException("OPCION_NO_ENCONTRADA", "La opción de anuncio no existe."));

        mvc.perform(post(URL + "/999/elegir").requestAttr("usuarioId", 1L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("OPCION_NO_ENCONTRADA"));
    }

    private static VersionesAnuncioResponse dosVersiones() {
        return new VersionesAnuncioResponse(102L, 2, "data:image/png;base64,V2", 18, List.of(
                new VersionAnuncioResponse(1, null, "data:image/jpeg;base64,V1", "cloudflare", false, false),
                new VersionAnuncioResponse(2, "fondo más claro", "data:image/png;base64,V2", "openrouter", false, true)));
    }

    @Test
    void retocarDevuelveElHistorialConLaVersionNueva() throws Exception {
        when(retoques.retocar(1L, 102L, "fondo más claro")).thenReturn(dosVersiones());

        mvc.perform(post(URL + "/102/retocar").requestAttr("usuarioId", 1L).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"instruccion\": \"  fondo más claro  \"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.versionActual").value(2))
                .andExpect(jsonPath("$.imagenActual").value("data:image/png;base64,V2"))
                .andExpect(jsonPath("$.versiones[1].instruccion").value("fondo más claro"))
                .andExpect(jsonPath("$.versiones[1].actual").value(true))
                .andExpect(jsonPath("$.retoquesRestantes").value(18));
    }

    @Test
    void retocarSinInstruccionResponde400() throws Exception {
        mvc.perform(post(URL + "/102/retocar").requestAttr("usuarioId", 1L).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"instruccion\": \" \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.instruccion").value("Escribe qué quieres cambiar."));
    }

    @Test
    void retocarUnaOpcionNoElegidaResponde400() throws Exception {
        when(retoques.retocar(1L, 101L, "fondo")).thenThrow(
                new SolicitudInvalidaException("OPCION_NO_ELEIDA", "Primero elige esta opción para poder retocarla."));

        mvc.perform(post(URL + "/101/retocar").requestAttr("usuarioId", 1L).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"instruccion\": \"fondo\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("OPCION_NO_ELEIDA"));
    }

    // HU 55: "Nueva versión" sobre la opción elegida genera una redacción nueva del texto.
    @Test
    void regenerarDevuelveLaNuevaRedaccionDeLaOpcionElegida() throws Exception {
        when(servicio.regenerarTexto(1L, 102L)).thenReturn(new OpcionElegidaResponse(
                new AnuncioGeneradoResponse(50L, "Texto nuevo", RedSocial.INSTAGRAM, Tono.CERCANO, false,
                        EstadoContenido.APROBADO, null),
                new OpcionAnuncioResponse(102L, 2, "Ilustración", "Texto nuevo", "data:image/jpeg;base64,QUJD",
                        "cloudflare", false, false, true)));

        mvc.perform(post(URL + "/102/regenerar").requestAttr("usuarioId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.anuncio.id").value(50))
                .andExpect(jsonPath("$.anuncio.texto").value("Texto nuevo"))
                .andExpect(jsonPath("$.opcion.id").value(102))
                .andExpect(jsonPath("$.opcion.elegida").value(true));
    }

    @Test
    void regenerarUnaOpcionQueNoElegioResponde400() throws Exception {
        when(servicio.regenerarTexto(1L, 101L)).thenThrow(
                new SolicitudInvalidaException("OPCION_NO_ELEIDA", "Primero elige esta opción para pedir una nueva versión."));

        mvc.perform(post(URL + "/101/regenerar").requestAttr("usuarioId", 1L))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("OPCION_NO_ELEIDA"));
    }

    @Test
    void regenerarUnaOpcionAjenaResponde404() throws Exception {
        when(servicio.regenerarTexto(1L, 999L)).thenThrow(
                new RecursoNoEncontradoException("OPCION_NO_ENCONTRADA", "La opción de anuncio no existe."));

        mvc.perform(post(URL + "/999/regenerar").requestAttr("usuarioId", 1L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("OPCION_NO_ENCONTRADA"));
    }

    @Test
    void usarUnaVersionAnteriorLaDevuelveComoActual() throws Exception {
        when(retoques.usarVersion(1L, 102L, 1)).thenReturn(new VersionesAnuncioResponse(102L, 1,
                "data:image/jpeg;base64,V1", 18, dosVersiones().versiones()));

        mvc.perform(post(URL + "/102/versiones/1/usar").requestAttr("usuarioId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.versionActual").value(1))
                .andExpect(jsonPath("$.imagenActual").value("data:image/jpeg;base64,V1"));
    }
}
