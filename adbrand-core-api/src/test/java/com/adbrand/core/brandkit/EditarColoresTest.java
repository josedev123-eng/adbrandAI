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

// HU 30: guardar solo los colores del kit. @SpringBootTest carga FiltroJwt, así se prueba con tokens reales.
// Usa la base local; cada prueba se deshace al terminar (@Transactional).
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class EditarColoresTest {

    private static final String COLORES = """
            {"colores": [{"hex": "#c62828", "nombre": "Rojo horno"}, {"hex": "#FFF3E0", "nombre": "Crema"},
                         {"hex": "#263238", "nombre": ""}]}""";

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
        duenio = usuarios.saveAndFlush(nuevaCuenta("duenio.colores@adbrand.pe"));
        otraEmpresa = usuarios.saveAndFlush(nuevaCuenta("otra.colores@adbrand.pe"));

        KitMarca nuevo = new KitMarca();
        nuevo.setUsuarioId(duenio.getId());
        nuevo.setNombreNegocio("Panadería Doña Rosa");
        nuevo.setSector("Panadería");
        nuevo.setEstiloVisual("MODERNO");
        nuevo.setLogoConcepto("Un pan con un sol");
        nuevo.setTipografiaPrimaria("Bricolage Grotesque");
        nuevo.setPaletaColores("#2E7D32 - Verde");
        nuevo.setLogoDiseno("""
                {"texto":"DR","forma":"CIRCULO","colorFondo":"#2E7D32","colorTexto":"#FFFFFF","tipografia":"Arial"}""");
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

    private ResultActions guardarColores(Usuario usuario, String cuerpo) throws Exception {
        return mvc.perform(put("/api/kit-marca/" + kit.getId() + "/colores")
                .header("Authorization", "Bearer " + jwt.generar(usuario))
                .contentType(MediaType.APPLICATION_JSON).content(cuerpo));
    }

    @Test
    void guardaLaPaletaComoVersionNuevaSinTocarElRestoDelKit() throws Exception {
        guardarColores(duenio, COLORES)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paletaColores").value("#C62828 - Rojo horno\n#FFF3E0 - Crema\n#263238"))
                .andExpect(jsonPath("$.tipografiaPrimaria").value("Bricolage Grotesque"))
                .andExpect(jsonPath("$.logoConcepto").value("Un pan con un sol"))
                .andExpect(jsonPath("$.logoDiseno.texto").value("DR"));

        assertThat(versiones.findByKitMarcaIdOrderByFechaCreacionDescIdDesc(kit.getId())).hasSize(1);
        mvc.perform(get("/api/kit-marca/" + kit.getId() + "/versiones")
                        .header("Authorization", "Bearer " + jwt.generar(duenio)))
                .andExpect(jsonPath("$[0].paletaColores").value("#C62828 - Rojo horno\n#FFF3E0 - Crema\n#263238"))
                .andExpect(jsonPath("$[0].logoDiseno.texto").value("DR"));
    }

    @Test
    void unColorInvalidoNoSeGuardaYDiceCualEs() throws Exception {
        // Criterio 2
        guardarColores(duenio, COLORES.replace("#FFF3E0", "crema"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos['colores[1].hex']").value("Escribe el color como #RRGGBB."));

        assertThat(kits.findById(kit.getId()).orElseThrow().getPaletaColores()).isEqualTo("#2E7D32 - Verde");
        assertThat(versiones.findByKitMarcaIdOrderByFechaCreacionDescIdDesc(kit.getId())).isEmpty();
    }

    @Test
    void unColorRepetidoNoSeGuarda() throws Exception {
        guardarColores(duenio, COLORES.replace("#263238", "#C62828"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("COLOR_REPETIDO"))
                .andExpect(jsonPath("$.mensaje").value("El color #C62828 está repetido."));

        assertThat(versiones.findByKitMarcaIdOrderByFechaCreacionDescIdDesc(kit.getId())).isEmpty();
    }

    @Test
    void sinColoresOMasDeOchoResponde400() throws Exception {
        guardarColores(duenio, "{\"colores\": []}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.colores").value("Agrega al menos un color."));

        StringBuilder nueve = new StringBuilder("{\"colores\": [");
        for (int i = 1; i <= 9; i++) {
            nueve.append(i > 1 ? "," : "").append("{\"hex\": \"#00000").append(i).append("\"}");
        }
        guardarColores(duenio, nueve.append("]}").toString())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.colores").value("Máximo 8 colores."));
    }

    @Test
    void elKitDeOtraEmpresaResponde404() throws Exception {
        guardarColores(otraEmpresa, COLORES)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("KIT_MARCA_NO_ENCONTRADO"));
    }
}
