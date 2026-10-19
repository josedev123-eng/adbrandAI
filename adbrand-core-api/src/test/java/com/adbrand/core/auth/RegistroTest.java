package com.adbrand.core.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.adbrand.core.auth.entity.Usuario;
import com.adbrand.core.auth.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

// HU 21: registro de la empresa. Usa la base local, pero cada prueba se deshace al terminar (@Transactional).
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class RegistroTest {

    private static final String URL = "/api/auth/registro";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private UsuarioRepository usuarios;

    @Autowired
    private PasswordEncoder codificador;

    private ResultActions registrar(String nombre, String empresa, String correo, String contrasena) throws Exception {
        String cuerpo = """
                {"nombre": "%s", "nombreEmpresa": "%s", "correo": "%s", "contrasena": "%s"}"""
                .formatted(nombre, empresa, correo, contrasena);
        return mvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(cuerpo));
    }

    @Test
    void registroCorrectoCreaLaCuentaYResponde201() throws Exception {
        registrar("Rosa Quispe", "Panadería Doña Rosa", "rosa.prueba@adbrand.pe", "Prueba123")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.nombre").value("Rosa Quispe"))
                .andExpect(jsonPath("$.nombreEmpresa").value("Panadería Doña Rosa"))
                .andExpect(jsonPath("$.correo").value("rosa.prueba@adbrand.pe"))
                .andExpect(jsonPath("$.contrasena").doesNotExist())
                .andExpect(jsonPath("$.contrasenaHash").doesNotExist());

        assertThat(usuarios.existsByCorreo("rosa.prueba@adbrand.pe")).isTrue();
    }

    @Test
    void seGuardaElHashYNoLaContrasena() throws Exception {
        registrar("Rosa Quispe", "Panadería Doña Rosa", "rosa.prueba@adbrand.pe", "Prueba123")
                .andExpect(status().isCreated());

        Usuario guardado = usuarios.findByCorreo("rosa.prueba@adbrand.pe").orElseThrow();
        assertThat(guardado.getContrasenaHash()).isNotEqualTo("Prueba123");
        assertThat(codificador.matches("Prueba123", guardado.getContrasenaHash())).isTrue();
    }

    @Test
    void elCorreoSeGuardaEnMinusculasYSinEspacios() throws Exception {
        registrar("Rosa Quispe", "Panadería Doña Rosa", "  Rosa.Prueba@AdBrand.PE ", "Prueba123")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.correo").value("rosa.prueba@adbrand.pe"));
    }

    @Test
    void correoRepetidoResponde409YNoCreaOtraCuenta() throws Exception {
        registrar("Rosa Quispe", "Panadería Doña Rosa", "rosa.prueba@adbrand.pe", "Prueba123")
                .andExpect(status().isCreated());
        long antes = usuarios.count();

        registrar("Otra Persona", "Otra Empresa", "rosa.prueba@adbrand.pe", "Prueba456")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("CORREO_REPETIDO"))
                .andExpect(jsonPath("$.mensaje").value("Ese correo ya está registrado."));

        assertThat(usuarios.count()).isEqualTo(antes);
    }

    @Test
    void correoRepetidoConOtrasMayusculasTambienResponde409() throws Exception {
        registrar("Rosa Quispe", "Panadería Doña Rosa", "rosa.prueba@adbrand.pe", "Prueba123")
                .andExpect(status().isCreated());

        registrar("Otra Persona", "Otra Empresa", "ROSA.Prueba@adbrand.PE", "Prueba456")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("CORREO_REPETIDO"));
    }

    @Test
    void correoInvalidoResponde400() throws Exception {
        registrar("Rosa Quispe", "Panadería Doña Rosa", "no-es-correo", "Prueba123")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("DATOS_INVALIDOS"))
                .andExpect(jsonPath("$.campos.correo").value("Ingresa un correo válido."));
    }

    @Test
    void camposVaciosResponden400ConCadaCampo() throws Exception {
        long antes = usuarios.count();

        mvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.nombre").value("Ingresa tu nombre."))
                .andExpect(jsonPath("$.campos.nombreEmpresa").value("Ingresa el nombre de tu empresa."))
                .andExpect(jsonPath("$.campos.correo").value("Ingresa tu correo."))
                .andExpect(jsonPath("$.campos.contrasena").value("Ingresa una contraseña."));

        assertThat(usuarios.count()).isEqualTo(antes);
    }

    @Test
    void contrasenaDebilResponde400() throws Exception {
        // Sin mayúscula, sin número, con espacio y demasiado corta: ninguna se acepta.
        for (String debil : new String[] {"prueba123", "Pruebaaaa", "Prueba 123"}) {
            registrar("Rosa Quispe", "Panadería Doña Rosa", "rosa.prueba@adbrand.pe", debil)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.campos.contrasena")
                            .value("Debe tener una mayúscula, una minúscula y un número, sin espacios."));
        }

        registrar("Rosa Quispe", "Panadería Doña Rosa", "rosa.prueba@adbrand.pe", "Pr1")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.contrasena").exists());

        assertThat(usuarios.existsByCorreo("rosa.prueba@adbrand.pe")).isFalse();
    }
}
