package com.adbrand.core.cuenta;

import static org.assertj.core.api.Assertions.assertThat;
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
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

// Mi cuenta: cambiar la contraseña escribiendo la actual. Usa la base local; cada prueba se deshace (@Transactional).
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CambiarContrasenaTest {

    private static final String ACTUAL = "Prueba123";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private UsuarioRepository usuarios;

    @Autowired
    private PasswordEncoder codificador;

    @Autowired
    private ServicioJwt jwt;

    private Usuario usuario;

    @BeforeEach
    void crearCuenta() {
        usuario = new Usuario();
        usuario.setNombre("Ana Quispe");
        usuario.setNombreEmpresa("Florería Ana");
        usuario.setCorreo("ana.cambia@adbrand.pe");
        usuario.setContrasenaHash(codificador.encode(ACTUAL));
        usuario = usuarios.saveAndFlush(usuario);
    }

    private ResultActions cambiar(String actual, String nueva, boolean conToken) throws Exception {
        var pedido = post("/api/cuenta/contrasena").contentType(MediaType.APPLICATION_JSON)
                .content("{\"actual\": \"%s\", \"nueva\": \"%s\"}".formatted(actual, nueva));
        if (conToken) {
            pedido = pedido.header("Authorization", "Bearer " + jwt.generar(usuario));
        }
        return mvc.perform(pedido);
    }

    @Test
    void conLaActualCorrectaSeCambia() throws Exception {
        cambiar(ACTUAL, "NuevaClave1", true)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensaje").value("Tu contraseña se cambió."));

        String hash = usuarios.findById(usuario.getId()).orElseThrow().getContrasenaHash();
        assertThat(codificador.matches("NuevaClave1", hash)).isTrue();
    }

    @Test
    void conLaActualEquivocadaNoSeCambia() throws Exception {
        cambiar("Otra12345", "NuevaClave1", true)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("CONTRASENA_ACTUAL_INCORRECTA"));

        String hash = usuarios.findById(usuario.getId()).orElseThrow().getContrasenaHash();
        assertThat(codificador.matches(ACTUAL, hash)).isTrue();
    }

    @Test
    void laNuevaDebeSerSeguraYDistinta() throws Exception {
        cambiar(ACTUAL, "debil", true).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.nueva").exists());
        cambiar(ACTUAL, ACTUAL, true).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("CONTRASENA_IGUAL"));
    }

    @Test
    void sinSesionNoSePuede() throws Exception {
        cambiar(ACTUAL, "NuevaClave1", false).andExpect(status().isUnauthorized());
    }
}
