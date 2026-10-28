package com.adbrand.core.brandkit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.adbrand.core.auth.entity.Usuario;
import com.adbrand.core.auth.repository.UsuarioRepository;
import com.adbrand.core.auth.service.ServicioJwt;
import com.adbrand.core.brandkit.entity.KitMarca;
import com.adbrand.core.brandkit.repository.KitMarcaRepository;
import com.adbrand.core.brandkit.repository.KitMarcaVersionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

// HU 29: guardar el logo diseñado en el editor. @SpringBootTest carga FiltroJwt, así se prueba con tokens reales.
// Usa la base local; cada prueba se deshace al terminar (@Transactional).
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class EditarLogoTest {

    private static final String LOGO = """
            {"texto": "DR", "forma": "CIRCULO", "colorFondo": "#c62828", "colorTexto": "#ffffff",
             "tipografia": "Bricolage Grotesque"}""";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private UsuarioRepository usuarios;

    @Autowired
    private KitMarcaRepository kits;

    @Autowired
    private KitMarcaVersionRepository versiones;

    @Autowired
    private ServicioJwt jwt;

    private Usuario duenio;
    private Usuario otraEmpresa;
    private KitMarca kit;

    @BeforeEach
    void preparar() {
        duenio = usuarios.saveAndFlush(nuevaCuenta("duenio.logo@adbrand.pe"));
        otraEmpresa = usuarios.saveAndFlush(nuevaCuenta("otra.logo@adbrand.pe"));

        KitMarca nuevo = new KitMarca();
        nuevo.setUsuarioId(duenio.getId());
        nuevo.setNombreNegocio("Panadería Doña Rosa");
        nuevo.setSector("Panadería");
        nuevo.setEstiloVisual("MODERNO");
        nuevo.setLogoConcepto("Un pan con un sol");
        nuevo.setTipografiaPrimaria("Bricolage Grotesque");
        nuevo.setPaletaColores("#C62828 - Rojo horno");
        kit = kits.saveAndFlush(nuevo);
    }

    private Usuario nuevaCuenta(String correo) {
        Usuario usuario = new Usuario();
        usuario.setNombre("Prueba");
        usuario.setNombreEmpresa("Empresa " + correo);
        usuario.setCorreo(correo);
        usuario.setContrasenaHash("$2a$10$hashDePruebaQueNoSeUsaParaEntrar.............");
        return usuario;
    }

    private ResultActions guardarLogo(Usuario usuario, Long kitId, String cuerpo) throws Exception {
        return mvc.perform(put("/api/kit-marca/" + kitId + "/logo")
                .header("Authorization", "Bearer " + jwt.generar(usuario))
                .contentType(MediaType.APPLICATION_JSON).content(cuerpo));
    }

    @Test
    void guardaElLogoYLoDevuelveConElKit() throws Exception {
        guardarLogo(duenio, kit.getId(), LOGO)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.logoDiseno.texto").value("DR"))
                .andExpect(jsonPath("$.logoDiseno.forma").value("CIRCULO"))
                .andExpect(jsonPath("$.logoDiseno.colorFondo").value("#C62828"))
                .andExpect(jsonPath("$.logoDiseno.colorTexto").value("#FFFFFF"))
                .andExpect(jsonPath("$.logoDiseno.tipografia").value("Bricolage Grotesque"))
                // El resto del kit no cambia.
                .andExpect(jsonPath("$.logoConcepto").value("Un pan con un sol"))
                .andExpect(jsonPath("$.paletaColores").value("#C62828 - Rojo horno"));

        // Al volver a pedir el kit, el logo sigue ahí y se puede volver a editar.
        mvc.perform(get("/api/kit-marca/" + kit.getId()).header("Authorization", "Bearer " + jwt.generar(duenio)))
                .andExpect(jsonPath("$.logoDiseno.texto").value("DR"));
    }

    @Test
    void cadaLogoGuardadoQuedaComoUnaVersionNueva() throws Exception {
        // Criterio 2: el logo queda como nueva versión del kit (historial de la HU 32).
        guardarLogo(duenio, kit.getId(), LOGO).andExpect(status().isOk());
        guardarLogo(duenio, kit.getId(), LOGO.replace("\"DR\"", "\"ROSA\"").replace("CIRCULO", "CUADRADO"))
                .andExpect(status().isOk());

        assertThat(versiones.findByKitMarcaIdOrderByFechaCreacionDescIdDesc(kit.getId())).hasSize(2);
        mvc.perform(get("/api/kit-marca/" + kit.getId() + "/versiones")
                        .header("Authorization", "Bearer " + jwt.generar(duenio)))
                .andExpect(jsonPath("$[0].logoDiseno.texto").value("ROSA"))
                .andExpect(jsonPath("$[0].logoDiseno.forma").value("CUADRADO"))
                .andExpect(jsonPath("$[1].logoDiseno.texto").value("DR"))
                .andExpect(jsonPath("$[0].logoConcepto").value("Un pan con un sol"));
    }

    @Test
    void unKitSinLogoDisenadoDevuelveLogoDisenoVacio() throws Exception {
        mvc.perform(get("/api/kit-marca/" + kit.getId()).header("Authorization", "Bearer " + jwt.generar(duenio)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.logoDiseno").isEmpty());
    }

    @Test
    void rechazaColoresYFormasInvalidas() throws Exception {
        guardarLogo(duenio, kit.getId(), LOGO.replace("#c62828", "rojo"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.colorFondo").value("Escribe el color como #RRGGBB."));

        guardarLogo(duenio, kit.getId(), LOGO.replace("\"DR\"", "\"\""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.texto").exists());

        assertThat(versiones.findByKitMarcaIdOrderByFechaCreacionDescIdDesc(kit.getId())).isEmpty();
    }

    @Test
    void elKitDeOtraEmpresaResponde404() throws Exception {
        guardarLogo(otraEmpresa, kit.getId(), LOGO)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("KIT_MARCA_NO_ENCONTRADO"));
    }

    @Test
    void editarElKitConservaElLogoDisenadoEnLaVersion() throws Exception {
        // La edición de la HU 32 no borra el logo diseñado: la versión lo guarda tal cual estaba.
        guardarLogo(duenio, kit.getId(), LOGO).andExpect(status().isOk());
        mvc.perform(put("/api/kit-marca/" + kit.getId()).header("Authorization", "Bearer " + jwt.generar(duenio))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"colores": [{"hex": "#2E7D32", "nombre": "Verde"}],
                                 "tipografiaPrimaria": "Montserrat", "logoConcepto": "Otro concepto"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.logoDiseno.texto").value("DR"));

        mvc.perform(get("/api/kit-marca/" + kit.getId() + "/versiones")
                        .header("Authorization", "Bearer " + jwt.generar(duenio)))
                .andExpect(jsonPath("$[0].logoConcepto").value("Otro concepto"))
                .andExpect(jsonPath("$[0].logoDiseno.texto").value("DR"));
    }
}
