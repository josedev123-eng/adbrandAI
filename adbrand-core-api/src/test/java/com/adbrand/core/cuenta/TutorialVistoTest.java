package com.adbrand.core.cuenta;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.adbrand.core.auth.entity.Usuario;
import com.adbrand.core.auth.repository.UsuarioRepository;
import com.adbrand.core.auth.service.ServicioJwt;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

// Tutorial guiado: una cuenta nueva no lo vio; al terminarlo queda guardado en la base (V46).
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class TutorialVistoTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private UsuarioRepository usuarios;

    @Autowired
    private ServicioJwt jwt;

    private Usuario empresa;

    @BeforeEach
    void crearCuenta() {
        Usuario usuario = new Usuario();
        usuario.setNombre("Prueba");
        usuario.setNombreEmpresa("Bodega Tutorial");
        usuario.setCorreo("tutorial@adbrand.pe");
        usuario.setContrasenaHash("$2a$10$hashDePruebaQueNoSeUsaParaEntrar.............");
        empresa = usuarios.saveAndFlush(usuario);
    }

    @Test
    void unaCuentaNuevaNoVioElTutorialYAlMarcarloQuedaGuardado() throws Exception {
        String token = "Bearer " + jwt.generar(empresa);

        mvc.perform(get("/api/auth/sesion").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tutorialVisto").value(false));

        mvc.perform(post("/api/cuenta/tutorial-visto").header("Authorization", token))
                .andExpect(status().isNoContent());

        assertThat(usuarios.findById(empresa.getId()).orElseThrow().isTutorialVisto()).isTrue();
        mvc.perform(get("/api/auth/sesion").header("Authorization", token))
                .andExpect(jsonPath("$.tutorialVisto").value(true));
    }

    @Test
    void sinSesionNoSePuedeMarcar() throws Exception {
        mvc.perform(post("/api/cuenta/tutorial-visto")).andExpect(status().isUnauthorized());
    }
}
