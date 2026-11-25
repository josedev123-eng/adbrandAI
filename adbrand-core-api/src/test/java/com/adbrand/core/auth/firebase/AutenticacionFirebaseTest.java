package com.adbrand.core.auth.firebase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.adbrand.core.auth.entity.EstadoUsuario;
import com.adbrand.core.auth.entity.Usuario;
import com.adbrand.core.auth.repository.UsuarioRepository;
import com.adbrand.core.auth.service.ServicioJwt;
import com.adbrand.core.shared.error.NoAutenticadoException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

// App móvil: entrar y registrarse con Firebase. Firebase se simula; lo que se prueba es cómo Spring une
// el usuario de Firebase con la empresa de siempre (la misma de la web). Usa la base local y se deshace al terminar.
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AutenticacionFirebaseTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private UsuarioRepository usuarios;

    @Autowired
    private ServicioJwt jwt;

    @MockitoBean
    private VerificadorTokenFirebase verificador;

    private Usuario cuentaWeb;

    @BeforeEach
    void preparar() {
        Usuario usuario = new Usuario();
        usuario.setNombre("Rosa");
        usuario.setNombreEmpresa("Panadería Rosa");
        usuario.setCorreo("rosa.firebase@hotmail.com");
        usuario.setContrasenaHash("$2a$10$hashDePruebaQueNoSeUsaParaEntrar.............");
        cuentaWeb = usuarios.saveAndFlush(usuario);
    }

    private void firebaseDice(String uid, String correo, boolean verificado) {
        when(verificador.verificar(anyString())).thenReturn(new UsuarioFirebase(uid, correo, verificado));
    }

    private ResultActions entrar() throws Exception {
        return mvc.perform(post("/api/auth/firebase").contentType(MediaType.APPLICATION_JSON)
                .content("{\"idToken\": \"token-de-firebase\"}"));
    }

    @Test
    void unaCuentaDeLaWebEntraConFirebaseSiElCorreoEstaVerificadoYQuedaUnida() throws Exception {
        firebaseDice("uid-rosa", "rosa.firebase@hotmail.com", true);

        entrar()
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.usuario.nombreEmpresa").value("Panadería Rosa"));

        assertThat(usuarios.findById(cuentaWeb.getId()).orElseThrow().getFirebaseUid()).isEqualTo("uid-rosa");
    }

    @Test
    void conElCorreoSinVerificarNoSeUneUnaCuentaExistente() throws Exception {
        // Así nadie entra a la cuenta de otro creando en Firebase un usuario con su correo.
        firebaseDice("uid-intruso", "rosa.firebase@hotmail.com", false);

        entrar()
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("CORREO_NO_VERIFICADO"));

        assertThat(usuarios.findById(cuentaWeb.getId()).orElseThrow().getFirebaseUid()).isNull();
    }

    @Test
    void siNoExisteLaCuentaPideRegistrarse() throws Exception {
        firebaseDice("uid-nuevo", "nadie@hotmail.com", true);

        entrar()
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("CUENTA_NO_ENCONTRADA"));
    }

    @Test
    void unTokenInvalidoNoEntra() throws Exception {
        when(verificador.verificar(anyString()))
                .thenThrow(new NoAutenticadoException("TOKEN_FIREBASE_INVALIDO", "No pudimos confirmar tu sesión."));

        entrar()
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("TOKEN_FIREBASE_INVALIDO"));
    }

    @Test
    void unaCuentaSuspendidaNoEntraNiConFirebase() throws Exception {
        cuentaWeb.setFirebaseUid("uid-rosa");
        cuentaWeb.setEstado(EstadoUsuario.INACTIVO);
        usuarios.saveAndFlush(cuentaWeb);
        firebaseDice("uid-rosa", "rosa.firebase@hotmail.com", true);

        entrar()
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("CUENTA_INACTIVA"));
    }

    @Test
    void elRegistroDesdeLaAppCreaLaEmpresaUnidaAFirebase() throws Exception {
        firebaseDice("uid-luis", "luis@outlook.com", false);

        mvc.perform(post("/api/auth/firebase/registro").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idToken\": \"t\", \"nombre\": \" Luis \", \"nombreEmpresa\": \"Ferretería Luis\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.correo").value("luis@outlook.com"))
                .andExpect(jsonPath("$.nombreEmpresa").value("Ferretería Luis"));

        Usuario creado = usuarios.findByFirebaseUid("uid-luis").orElseThrow();
        assertThat(creado.getNombre()).isEqualTo("Luis");
        // Hasta que confirme el correo, todavía no puede entrar.
        entrar().andExpect(status().isForbidden()).andExpect(jsonPath("$.codigo").value("CORREO_NO_VERIFICADO"));
    }

    @Test
    void noSeRegistraDosVecesElMismoCorreo() throws Exception {
        firebaseDice("uid-otro", "rosa.firebase@hotmail.com", true);

        mvc.perform(post("/api/auth/firebase/registro").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idToken\": \"t\", \"nombre\": \"Rosa\", \"nombreEmpresa\": \"Otra\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("CORREO_REPETIDO"));
    }

    @Test
    void conLaSesionDeSiempreSeUneElFirebaseRecienCreadoAunqueNoEsteVerificado() throws Exception {
        // Migración: entró con su contraseña de la web (prueba que la cuenta es suya) y luego se creó su Firebase.
        firebaseDice("uid-rosa", "rosa.firebase@hotmail.com", false);

        mvc.perform(post("/api/cuenta/firebase/vincular").header("Authorization", "Bearer " + jwt.generar(cuentaWeb))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"idToken\": \"t\"}"))
                .andExpect(status().isOk());

        assertThat(usuarios.findById(cuentaWeb.getId()).orElseThrow().getFirebaseUid()).isEqualTo("uid-rosa");
    }

    @Test
    void noSeUneUnFirebaseDeOtroCorreo() throws Exception {
        firebaseDice("uid-x", "otra@hotmail.com", true);

        mvc.perform(post("/api/cuenta/firebase/vincular").header("Authorization", "Bearer " + jwt.generar(cuentaWeb))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"idToken\": \"t\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("CORREO_DISTINTO"));
    }
}
