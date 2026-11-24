package com.adbrand.core.imagen;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.adbrand.core.imagen.controller.ImagenController;
import com.adbrand.core.imagen.dto.ImagenGenerada;
import com.adbrand.core.imagen.service.ServicioImagenes;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

// POST /api/imagenes/borrador: devuelve la imagen lista para <img src> y valida la descripción.
@WebMvcTest(ImagenController.class)
class ImagenControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private ServicioImagenes servicio;

    @Test
    void devuelveLaImagenComoDataUrl() throws Exception {
        when(servicio.borrador("Pan de yema")).thenReturn(new ImagenGenerada("image/svg+xml", "PHN2Zz4=", "simulado", true));

        mvc.perform(post("/api/imagenes/borrador")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"descripcion\": \"  Pan de yema  \"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.imagen").value("data:image/svg+xml;base64,PHN2Zz4="))
                .andExpect(jsonPath("$.proveedor").value("simulado"))
                .andExpect(jsonPath("$.simulado").value(true));
    }

    @Test
    void sinDescripcionResponde400() throws Exception {
        mvc.perform(post("/api/imagenes/borrador")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"descripcion\": \"   \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.descripcion").value("Describe la imagen que quieres."));
    }
}
