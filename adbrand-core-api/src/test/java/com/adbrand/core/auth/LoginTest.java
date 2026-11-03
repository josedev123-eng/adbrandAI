package com.adbrand.core.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.adbrand.core.auth.entity.EstadoUsuario;
import com.adbrand.core.auth.entity.Usuario;
import com.adbrand.core.auth.repository.UsuarioRepository;
import com.adbrand.core.auth.service.ServicioJwt;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

// HU 22: inicio de sesión con JWT y filtro de rutas. @SpringBootTest sí carga FiltroJwt.
// Usa la base local; cada prueba se deshace al terminar (@Transactional).
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class LoginTest {

    private static final String CORREO = "luis.prueba@adbrand.pe";
    private static final String CONTRASENA = "Prueba123";

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
        usuario.setNombre("Luis Rojas");
        usuario.setNombreEmpresa("Bodega San Martín");
        usuario.setCorreo(CORREO);
        usuario.setContrasenaHash(codificador.encode(CONTRASENA));
        usuario = usuarios.saveAndFlush(usuario);
    }

    private ResultActions iniciarSesion(String correo, String contrasena) throws Exception {
        String cuerpo = "{\"correo\": \"%s\", \"contrasena\": \"%s\"}".formatted(correo, contrasena);
        return mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(cuerpo));
    }

    private String tokenDe(MvcResult resultado) throws Exception {
        String json = resultado.getResponse().getContentAsString();
        return json.replaceAll("(?s).*\"token\":\"([^\"]+)\".*", "$1");
    }

    @Test
    void loginCorrectoDevuelveUnTokenQueServicioJwtPuedeLeer() throws Exception {
        MvcResult resultado = iniciarSesion(CORREO, CONTRASENA)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isString())
                .andExpect(jsonPath("$.usuario.id").value(usuario.getId()))
                .andExpect(jsonPath("$.usuario.nombreEmpresa").value("Bodega San Martín"))
                .andExpect(jsonPath("$.usuario.contrasenaHash").doesNotExist())
                .andReturn();

        assertThat(jwt.leerUsuarioId(tokenDe(resultado))).isEqualTo(usuario.getId());
    }

    @Test
    void elCorreoSeAceptaConMayusculasYEspacios() throws Exception {
        iniciarSesion("  LUIS.Prueba@AdBrand.pe ", CONTRASENA).andExpect(status().isOk());
    }

    @Test
    void loginGuardaElUltimoAcceso() throws Exception {
        iniciarSesion(CORREO, CONTRASENA).andExpect(status().isOk());

        assertThat(usuarios.findById(usuario.getId()).orElseThrow().getUltimoAcceso()).isNotNull();
    }

    @Test
    void contrasenaMalaYCorreoInexistenteDan401ConElMismoMensaje() throws Exception {
        iniciarSesion(CORREO, "Equivocada1")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("CREDENCIALES_INVALIDAS"))
                .andExpect(jsonPath("$.mensaje").value("Correo o contraseña incorrectos."));

        iniciarSesion("nadie@adbrand.pe", CONTRASENA)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("CREDENCIALES_INVALIDAS"))
                .andExpect(jsonPath("$.mensaje").value("Correo o contraseña incorrectos."));
    }

    @Test
    void cuentaInactivaDa403() throws Exception {
        usuario.setEstado(EstadoUsuario.INACTIVO);
        usuarios.saveAndFlush(usuario);

        iniciarSesion(CORREO, CONTRASENA)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("CUENTA_INACTIVA"));
    }

    @Test
    void cuentaInactivaConContrasenaMalaNoRevelaQueExiste() throws Exception {
        usuario.setEstado(EstadoUsuario.INACTIVO);
        usuarios.saveAndFlush(usuario);

        iniciarSesion(CORREO, "Equivocada1")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("CREDENCIALES_INVALIDAS"));
    }

    @Test
    void sinCorreoNiContrasenaDa400() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.correo").value("Ingresa tu correo."))
                .andExpect(jsonPath("$.campos.contrasena").value("Ingresa tu contraseña."));
    }

    @Test
    void rutaProtegidaSinTokenDa401() throws Exception {
        mvc.perform(get("/api/auth/sesion"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("NO_AUTENTICADO"))
                .andExpect(jsonPath("$.mensaje").value("Inicia sesión para continuar."));

        mvc.perform(get("/api/negocio/perfil")).andExpect(status().isUnauthorized());
    }

    @Test
    void conTokenAlteradoDa401() throws Exception {
        String token = jwt.generar(usuario);
        // Cambia un carácter de la firma: la firma ya no coincide.
        String alterado = token.substring(0, token.length() - 2) + (token.endsWith("A") ? "BB" : "AA");

        mvc.perform(get("/api/auth/sesion").header("Authorization", "Bearer " + alterado))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("NO_AUTENTICADO"));
    }

    @Test
    void conTokenVencidoDa401() throws Exception {
        Instant haceDosHoras = Instant.now().minus(Duration.ofHours(2));
        String vencido = jwt.generar(usuario.getId(), CORREO, haceDosHoras, haceDosHoras.plus(Duration.ofHours(1)));

        mvc.perform(get("/api/auth/sesion").header("Authorization", "Bearer " + vencido))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("NO_AUTENTICADO"));
    }

    @Test
    void conTokenSinPrefijoBearerDa401() throws Exception {
        mvc.perform(get("/api/auth/sesion").header("Authorization", jwt.generar(usuario)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void conTokenValidoEntraYDevuelveSuUsuario() throws Exception {
        MvcResult login = iniciarSesion(CORREO, CONTRASENA).andReturn();

        mvc.perform(get("/api/auth/sesion").header("Authorization", "Bearer " + tokenDe(login)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(usuario.getId()))
                .andExpect(jsonPath("$.correo").value(CORREO));
    }

    @Test
    void lasPeticionesOptionsNoPidenToken() throws Exception {
        mvc.perform(options("/api/negocio/perfil")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "PUT")
                        .header("Access-Control-Request-Headers", "authorization,content-type"))
                .andExpect(status().isOk());
    }
}
