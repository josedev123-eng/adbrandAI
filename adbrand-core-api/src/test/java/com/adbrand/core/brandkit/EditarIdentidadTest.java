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

// HU 31: guardar la identidad visual (tipografías y voz). @SpringBootTest carga FiltroJwt, así se prueba con tokens reales.
// Usa la base local; cada prueba se deshace al terminar (@Transactional).
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class EditarIdentidadTest {

    private static final String IDENTIDAD = """
            {"tipografiaPrimaria": " Georgia ", "tipografiaSecundaria": "Arial",
             "vozMarca": "Elegante y sobria, trata de usted al cliente."}""";

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
        duenio = usuarios.saveAndFlush(nuevaCuenta("duenio.identidad@adbrand.pe"));
        otraEmpresa = usuarios.saveAndFlush(nuevaCuenta("otra.identidad@adbrand.pe"));

        KitMarca nuevo = new KitMarca();
        nuevo.setUsuarioId(duenio.getId());
        nuevo.setNombreNegocio("Panadería Doña Rosa");
        nuevo.setSector("Panadería");
        nuevo.setEstiloVisual("MODERNO");
        nuevo.setLogoConcepto("Un pan con un sol");
        nuevo.setTipografiaPrimaria("Bricolage Grotesque");
        nuevo.setPaletaColores("#C62828 - Rojo horno");
        nuevo.setVozMarca("Cercana, usa tú.");
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

    private ResultActions guardarIdentidad(Usuario usuario, String cuerpo) throws Exception {
        return mvc.perform(put("/api/kit-marca/" + kit.getId() + "/identidad")
                .header("Authorization", "Bearer " + jwt.generar(usuario))
                .contentType(MediaType.APPLICATION_JSON).content(cuerpo));
    }

    @Test
    void guardaTipografiasYVozSinTocarElRestoDelKit() throws Exception {
        guardarIdentidad(duenio, IDENTIDAD)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipografiaPrimaria").value("Georgia"))
                .andExpect(jsonPath("$.tipografiaSecundaria").value("Arial"))
                .andExpect(jsonPath("$.vozMarca").value("Elegante y sobria, trata de usted al cliente."))
                .andExpect(jsonPath("$.paletaColores").value("#C62828 - Rojo horno"))
                .andExpect(jsonPath("$.logoConcepto").value("Un pan con un sol"));
    }

    @Test
    void laIdentidadQuedaComoVersionConSuVoz() throws Exception {
        guardarIdentidad(duenio, IDENTIDAD).andExpect(status().isOk());

        assertThat(versiones.findByKitMarcaIdOrderByFechaCreacionDescIdDesc(kit.getId())).hasSize(1);
        mvc.perform(get("/api/kit-marca/" + kit.getId() + "/versiones")
                        .header("Authorization", "Bearer " + jwt.generar(duenio)))
                .andExpect(jsonPath("$[0].tipografiaPrimaria").value("Georgia"))
                .andExpect(jsonPath("$[0].vozMarca").value("Elegante y sobria, trata de usted al cliente."));
    }

    @Test
    void laTipografiaSecundariaEsOpcional() throws Exception {
        guardarIdentidad(duenio, IDENTIDAD.replace("\"Arial\"", "\"  \""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipografiaSecundaria").isEmpty());
    }

    @Test
    void sinTipografiaPrincipalNiVozResponde400() throws Exception {
        guardarIdentidad(duenio, "{\"tipografiaPrimaria\": \"\", \"vozMarca\": \" \"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.tipografiaPrimaria").value("Elige la tipografía principal."))
                .andExpect(jsonPath("$.campos.vozMarca").value("Describe la voz de tu marca."));

        assertThat(versiones.findByKitMarcaIdOrderByFechaCreacionDescIdDesc(kit.getId())).isEmpty();
    }

    @Test
    void elKitDeOtraEmpresaResponde404() throws Exception {
        guardarIdentidad(otraEmpresa, IDENTIDAD)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("KIT_MARCA_NO_ENCONTRADO"));
    }

    @Test
    void losAnunciosNuevosTomanElKitEditadoMasReciente() throws Exception {
        guardarIdentidad(duenio, IDENTIDAD).andExpect(status().isOk());

        assertThat(kits.findFirstByUsuarioIdOrderByFechaActualizacionDescIdDesc(duenio.getId()))
                .get().extracting(KitMarca::getVozMarca)
                .isEqualTo("Elegante y sobria, trata de usted al cliente.");
        assertThat(kits.findFirstByUsuarioIdOrderByFechaActualizacionDescIdDesc(otraEmpresa.getId())).isEmpty();
    }
}
