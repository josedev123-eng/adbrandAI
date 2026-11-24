package com.adbrand.core.ia;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.adbrand.core.auth.entity.Usuario;
import com.adbrand.core.auth.repository.UsuarioRepository;
import com.adbrand.core.auth.service.ServicioJwt;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

// Una empresa nueva, sin plan pagado, empieza el mes en el plan gratuito sin consumo.
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ConsumoControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private UsuarioRepository usuarios;

    @Autowired
    private ServicioJwt jwt;

    @Test
    void unaEmpresaNuevaEstaEnElPlanGratuitoSinConsumo() throws Exception {
        Usuario usuario = new Usuario();
        usuario.setNombre("Prueba");
        usuario.setNombreEmpresa("Empresa consumo");
        usuario.setCorreo("consumo.mes@adbrand.pe");
        usuario.setContrasenaHash("$2a$10$hashDePruebaQueNoSeUsaParaEntrar.............");
        usuario = usuarios.saveAndFlush(usuario);

        mvc.perform(get("/api/consumo/mes").header("Authorization", "Bearer " + jwt.generar(usuario)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokensUsados").value(0))
                .andExpect(jsonPath("$.origen").value("GRATUITO"))
                .andExpect(jsonPath("$.agotado").value(false));
    }
}
