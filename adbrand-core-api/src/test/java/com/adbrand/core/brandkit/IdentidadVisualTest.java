package com.adbrand.core.brandkit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.adbrand.core.auth.entity.Usuario;
import com.adbrand.core.auth.repository.UsuarioRepository;
import com.adbrand.core.auth.service.ServicioJwt;
import com.adbrand.core.brandkit.dto.GenerarKitMarcaRequest;
import com.adbrand.core.brandkit.entity.KitMarca;
import com.adbrand.core.brandkit.repository.KitMarcaRepository;
import com.adbrand.core.brandkit.service.KitMarcaService;
import com.adbrand.core.imagen.dto.ImagenGenerada;
import com.adbrand.core.imagen.service.ServicioImagenes;
import java.util.Base64;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

// Identidad de marca (V70): eslogan propio o de la IA, logo subido o elegido de la IA, y manual en PDF.
// Usa la base local; cada prueba se deshace al terminar (@Transactional).
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class IdentidadVisualTest {

    // PNG de 1x1 píxel (válido).
    private static final String PNG = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg==";

    @Autowired private MockMvc mvc;
    @Autowired private UsuarioRepository usuarios;
    @Autowired private KitMarcaRepository kits;
    @Autowired private ServicioJwt jwt;

    @MockitoBean private ServicioImagenes imagenes;
    // Anotar el retoque usa su propia transacción y no ve la empresa de la prueba (que no se confirma).
    @MockitoBean private com.adbrand.core.imagen.service.ControlGastoImagenes gasto;

    private Usuario usuario;
    private KitMarca kit;

    @BeforeEach
    void preparar() {
        usuario = new Usuario();
        usuario.setNombre("Rosa Quispe");
        usuario.setNombreEmpresa("Panadería Rosa");
        usuario.setCorreo("rosa.identidad@adbrand.pe");
        usuario.setContrasenaHash("x");
        usuario = usuarios.saveAndFlush(usuario);

        kit = new KitMarca();
        kit.setUsuarioId(usuario.getId());
        kit.setNombreNegocio("Panadería Rosa");
        kit.setSector("Panadería y pastelería");
        kit.setEstiloVisual("ARTESANAL");
        kit.setPaletaColores("#8B4513 - Café - Calidez\n#F5DEB3 - Trigo - Cercanía");
        kit = kits.saveAndFlush(kit);
    }

    private String token() {
        return "Bearer " + jwt.generar(usuario);
    }

    @Test
    void laIaProponeTresLogosSinGuardarlos() throws Exception {
        when(imagenes.borrador(anyString())).thenReturn(new ImagenGenerada("image/png", PNG, "cloudflare", false));

        mvc.perform(post("/api/kit-marca/%d/logo/opciones".formatted(kit.getId())).header("Authorization", token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.opciones.length()").value(3))
                .andExpect(jsonPath("$.opciones[0]").value(org.hamcrest.Matchers.startsWith("data:image/png;base64,")));

        assertThat(kits.findById(kit.getId()).orElseThrow().getLogoImagen()).isNull();
    }

    @Test
    void guardaElLogoSubidoYLoDevuelveComoImagen() throws Exception {
        String cuerpo = "{\"imagen\": \"data:image/png;base64,%s\", \"origen\": \"SUBIDO\"}".formatted(PNG);

        mvc.perform(put("/api/kit-marca/%d/logo-imagen".formatted(kit.getId())).header("Authorization", token())
                        .contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.logoOrigen").value("SUBIDO"))
                .andExpect(jsonPath("$.logoImagen").value("data:image/png;base64," + PNG));
    }

    @Test
    void rechazaUnArchivoQueNoEsImagen() throws Exception {
        String texto = Base64.getEncoder().encodeToString("<script>alert(1)</script>".getBytes());
        String cuerpo = "{\"imagen\": \"data:image/png;base64,%s\", \"origen\": \"SUBIDO\"}".formatted(texto);

        mvc.perform(put("/api/kit-marca/%d/logo-imagen".formatted(kit.getId())).header("Authorization", token())
                        .contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("LOGO_INVALIDO"));
    }

    @Test
    void elKitDeOtraEmpresaNoSePuedeTocar() throws Exception {
        Usuario otro = new Usuario();
        otro.setNombre("Luis");
        otro.setNombreEmpresa("Bodega Luis");
        otro.setCorreo("luis.identidad@adbrand.pe");
        otro.setContrasenaHash("x");
        otro = usuarios.saveAndFlush(otro);

        mvc.perform(put("/api/kit-marca/%d/eslogan".formatted(kit.getId()))
                        .header("Authorization", "Bearer " + jwt.generar(otro))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"eslogan\": \"Hackeado\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void eligeElEsloganYGeneraElManualEnPdf() throws Exception {
        mvc.perform(put("/api/kit-marca/%d/eslogan".formatted(kit.getId())).header("Authorization", token())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"eslogan\": \"El sabor de tu barrio\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eslogan").value("El sabor de tu barrio"));

        MvcResult pdf = mvc.perform(get("/api/kit-marca/%d/manual".formatted(kit.getId())).header("Authorization", token()))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(pdf.getResponse().getContentType()).isEqualTo("application/pdf");
        assertThat(new String(pdf.getResponse().getContentAsByteArray(), 0, 4)).isEqualTo("%PDF");
    }

    @Test
    void siYaTieneEsloganSeRespetaYSiNoSeGuardanLasPropuestasDeLaIa() throws Exception {
        KitMarca conPropio = new KitMarca();
        KitMarcaService.guardarIdentidad(conPropio, new GenerarKitMarcaRequest("A", "B", null, null, "MODERNO",
                "Confianza", "Pan a las 6 am", "La casa del buen pan"), null);
        assertThat(conPropio.getEslogan()).isEqualTo("La casa del buen pan");

        KitMarca conIa = new KitMarca();
        KitMarcaService.guardarIdentidad(conIa, new GenerarKitMarcaRequest("A", "B", null, null, "MODERNO",
                "Alegría", null, null), new ObjectMapper().readTree("[\"Uno\", \"Dos\", \"Tres\", \"Cuatro\"]"));
        assertThat(conIa.getEslogan()).isEqualTo("Uno");
        assertThat(conIa.getEsloganOpciones()).isEqualTo("Uno\nDos\nTres");
        assertThat(conIa.getSentimiento()).isEqualTo("Alegría");
    }

    @Test
    void editarElLogoDevuelveUnaVersionNuevaSinGuardarla() throws Exception {
        kit.setLogoImagen(PNG);
        kit.setLogoImagenTipo("image/png");
        kits.saveAndFlush(kit);
        when(imagenes.finalORetoque(org.mockito.ArgumentMatchers.contains("taza más grande"), org.mockito.ArgumentMatchers.any()))
                .thenReturn(new ImagenGenerada("image/png", "TlVFVk8=", "openrouter", false));

        mvc.perform(post("/api/kit-marca/%d/logo/retocar".formatted(kit.getId())).header("Authorization", token())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"instruccion\": \"pon la taza más grande\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.imagen").value("data:image/png;base64,TlVFVk8="));

        assertThat(kits.findById(kit.getId()).orElseThrow().getLogoImagen()).isEqualTo(PNG);
    }

    @Test
    void sinLogoNoSePuedeEditar() throws Exception {
        mvc.perform(post("/api/kit-marca/%d/logo/retocar".formatted(kit.getId())).header("Authorization", token())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"instruccion\": \"más grande\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("SIN_LOGO"));
    }
}
