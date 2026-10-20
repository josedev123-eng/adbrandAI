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

// HU 32: editar el kit de marca a mano. @SpringBootTest carga FiltroJwt, así se prueba con tokens reales.
// Usa la base local; cada prueba se deshace al terminar (@Transactional).
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class EditarKitMarcaTest {

    private static final String CAMBIOS = """
            {"colores": [{"hex": "#c62828", "nombre": "Rojo oferta - Urgencia"}, {"hex": "#263238", "nombre": ""}],
             "tipografiaPrimaria": "Montserrat", "tipografiaSecundaria": "Lato",
             "logoConcepto": "Una canasta roja con una hoja"}""";

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
        duenio = usuarios.saveAndFlush(nuevaCuenta("duenio.kit@adbrand.pe"));
        otraEmpresa = usuarios.saveAndFlush(nuevaCuenta("otra.kit@adbrand.pe"));

        KitMarca nuevo = new KitMarca();
        nuevo.setUsuarioId(duenio.getId());
        nuevo.setNombreNegocio("Bodega Prueba");
        nuevo.setSector("Bodega y minimarket");
        nuevo.setEstiloVisual("MODERNO");
        nuevo.setLogoConcepto("Una canasta con una hoja verde");
        nuevo.setTipografiaPrimaria("Bricolage Grotesque");
        nuevo.setTipografiaSecundaria("Plus Jakarta Sans");
        nuevo.setPaletaColores("#2E7D32 - Verde bodega - Confianza\n#F9A825 - Amarillo sol");
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

    private String bearer(Usuario usuario) {
        return "Bearer " + jwt.generar(usuario);
    }

    private ResultActions editar(Usuario usuario, Long kitId, String cuerpo) throws Exception {
        return mvc.perform(put("/api/kit-marca/" + kitId).header("Authorization", bearer(usuario))
                .contentType(MediaType.APPLICATION_JSON).content(cuerpo));
    }

    @Test
    void editarGuardaUnaVersionYElKitQuedaConLoNuevo() throws Exception {
        editar(duenio, kit.getId(), CAMBIOS)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipografiaPrimaria").value("Montserrat"))
                .andExpect(jsonPath("$.tipografiaSecundaria").value("Lato"))
                .andExpect(jsonPath("$.logoConcepto").value("Una canasta roja con una hoja"))
                .andExpect(jsonPath("$.paletaColores").value("#C62828 - Rojo oferta - Urgencia\n#263238"))
                // Lo que no se edita se queda igual.
                .andExpect(jsonPath("$.vozMarca").value("Cercana, usa tú."));

        assertThat(versiones.findByKitMarcaIdOrderByFechaCreacionDescIdDesc(kit.getId()))
                .hasSize(1)
                .first()
                .satisfies(version -> assertThat(version.getTipografiaPrimaria()).isEqualTo("Montserrat"));
    }

    @Test
    void alPedirElKitOtraVezSaleLoUltimoGuardado() throws Exception {
        editar(duenio, kit.getId(), CAMBIOS).andExpect(status().isOk());
        editar(duenio, kit.getId(), CAMBIOS.replace("Montserrat", "Poppins")).andExpect(status().isOk());

        mvc.perform(get("/api/kit-marca/" + kit.getId()).header("Authorization", bearer(duenio)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipografiaPrimaria").value("Poppins"));
    }

    @Test
    void lasVersionesSeListanLaMasRecientePrimero() throws Exception {
        editar(duenio, kit.getId(), CAMBIOS).andExpect(status().isOk());
        editar(duenio, kit.getId(), CAMBIOS.replace("Montserrat", "Poppins")).andExpect(status().isOk());

        mvc.perform(get("/api/kit-marca/" + kit.getId() + "/versiones").header("Authorization", bearer(duenio)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].tipografiaPrimaria").value("Poppins"))
                .andExpect(jsonPath("$[1].tipografiaPrimaria").value("Montserrat"));
    }

    @Test
    void elKitDeOtraEmpresaDa404AlVerAlEditarYAlPedirVersiones() throws Exception {
        mvc.perform(get("/api/kit-marca/" + kit.getId()).header("Authorization", bearer(otraEmpresa)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("KIT_MARCA_NO_ENCONTRADO"));

        editar(otraEmpresa, kit.getId(), CAMBIOS)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("KIT_MARCA_NO_ENCONTRADO"));

        mvc.perform(get("/api/kit-marca/" + kit.getId() + "/versiones").header("Authorization", bearer(otraEmpresa)))
                .andExpect(status().isNotFound());

        assertThat(kits.findById(kit.getId()).orElseThrow().getTipografiaPrimaria()).isEqualTo("Bricolage Grotesque");
        assertThat(versiones.findByKitMarcaIdOrderByFechaCreacionDescIdDesc(kit.getId())).isEmpty();
    }

    @Test
    void kitQueNoExisteDa404() throws Exception {
        editar(duenio, 999_999_999L, CAMBIOS).andExpect(status().isNotFound());
    }

    @Test
    void colorMalEscritoDa400YNoGuardaNada() throws Exception {
        for (String color : new String[] {"#12345G", "C62828", "#FFF", "verde"}) {
            editar(duenio, kit.getId(), CAMBIOS.replace("#c62828", color))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.campos['colores[0].hex']").value("Escribe el color como #RRGGBB."));
        }

        assertThat(versiones.findByKitMarcaIdOrderByFechaCreacionDescIdDesc(kit.getId())).isEmpty();
    }

    @Test
    void sinColoresOSinTipografiaPrincipalDa400() throws Exception {
        editar(duenio, kit.getId(), """
                {"colores": [], "tipografiaPrimaria": " ", "logoConcepto": "Algo"}""")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.colores").value("Agrega al menos un color."))
                .andExpect(jsonPath("$.campos.tipografiaPrimaria").value("Ingresa la tipografía principal."));
    }

    @Test
    void sinTokenDa401() throws Exception {
        mvc.perform(put("/api/kit-marca/" + kit.getId()).contentType(MediaType.APPLICATION_JSON).content(CAMBIOS))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("NO_AUTENTICADO"));
    }
}
