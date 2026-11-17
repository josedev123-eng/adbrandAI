package com.adbrand.core.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.adbrand.core.auth.entity.CodigoVerificacionCorreo;
import com.adbrand.core.auth.entity.Usuario;
import com.adbrand.core.auth.repository.CodigoVerificacionCorreoRepository;
import com.adbrand.core.auth.repository.UsuarioRepository;
import com.adbrand.core.notificacion.service.ServicioCorreo;
import java.time.LocalDateTime;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

// Verificar el correo al registrarse en la web (V56): código de 6 dígitos, la cuenta no entra hasta escribirlo.
// Usa la base local; cada prueba se deshace al terminar (@Transactional).
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class VerificacionCorreoTest {

    private static final String CORREO = "rosa.verifica@adbrand.pe";
    private static final String CONTRASENA = "Prueba123";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private UsuarioRepository usuarios;

    @Autowired
    private CodigoVerificacionCorreoRepository codigos;

    @MockitoBean
    private ServicioCorreo correo;

    @BeforeEach
    void registrarse() throws Exception {
        String cuerpo = "{\"nombre\": \"Rosa Quispe\", \"nombreEmpresa\": \"Panadería Rosa\", \"correo\": \"%s\", \"contrasena\": \"%s\"}"
                .formatted(CORREO, CONTRASENA);
        mvc.perform(post("/api/auth/registro").contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isCreated());
    }

    private String codigoDelCorreo() {
        ArgumentCaptor<String> texto = ArgumentCaptor.forClass(String.class);
        verify(correo).enviar(eq(CORREO), anyString(), texto.capture());
        Matcher codigo = Pattern.compile("\\b(\\d{6})\\b").matcher(texto.getValue());
        assertThat(codigo.find()).isTrue();
        return codigo.group(1);
    }

    private ResultActions verificar(String codigo) throws Exception {
        return mvc.perform(post("/api/auth/verificar-correo").contentType(MediaType.APPLICATION_JSON)
                .content("{\"correo\": \"%s\", \"codigo\": \"%s\"}".formatted(CORREO, codigo)));
    }

    private ResultActions iniciarSesion() throws Exception {
        return mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"correo\": \"%s\", \"contrasena\": \"%s\"}".formatted(CORREO, CONTRASENA)));
    }

    private String otroCodigo(String codigo) {
        return codigo.equals("000000") ? "111111" : "000000";
    }

    @Test
    void alRegistrarseLlegaUnCodigoYLaCuentaQuedaSinVerificar() {
        codigoDelCorreo();

        assertThat(usuarios.findByCorreo(CORREO).orElseThrow().isCorreoVerificado()).isFalse();
    }

    @Test
    void sinVerificarNoPuedeIniciarSesion() throws Exception {
        iniciarSesion()
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("CORREO_NO_VERIFICADO"));
    }

    @Test
    void conElCodigoCorrectoQuedaVerificadaYEntraDirecto() throws Exception {
        verificar(codigoDelCorreo())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isString())
                .andExpect(jsonPath("$.usuario.correo").value(CORREO));

        assertThat(usuarios.findByCorreo(CORREO).orElseThrow().isCorreoVerificado()).isTrue();
        iniciarSesion().andExpect(status().isOk());
    }

    @Test
    void elCodigoSeGuardaConHash() {
        String codigo = codigoDelCorreo();
        Usuario usuario = usuarios.findByCorreo(CORREO).orElseThrow();

        CodigoVerificacionCorreo guardado = codigos
                .findByUsuarioIdAndUsadoFalseOrderByFechaCreacionDesc(usuario.getId()).get(0);
        assertThat(guardado.getCodigoHash()).isNotEqualTo(codigo);
    }

    @Test
    void codigoEquivocadoNoVerifica() throws Exception {
        String codigo = codigoDelCorreo();

        verificar(otroCodigo(codigo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("CODIGO_INVALIDO"));
        assertThat(usuarios.findByCorreo(CORREO).orElseThrow().isCorreoVerificado()).isFalse();
    }

    @Test
    void tras5CodigosEquivocadosNiElCorrectoSirve() throws Exception {
        String codigo = codigoDelCorreo();
        for (int i = 0; i < 5; i++) {
            verificar(otroCodigo(codigo)).andExpect(status().isBadRequest());
        }

        verificar(codigo).andExpect(status().isBadRequest());
    }

    @Test
    void codigoVencidoNoSirve() throws Exception {
        String codigo = codigoDelCorreo();
        Usuario usuario = usuarios.findByCorreo(CORREO).orElseThrow();
        CodigoVerificacionCorreo guardado = codigos
                .findByUsuarioIdAndUsadoFalseOrderByFechaCreacionDesc(usuario.getId()).get(0);
        guardado.setFechaVencimiento(LocalDateTime.now().minusMinutes(1));
        codigos.saveAndFlush(guardado);

        verificar(codigo).andExpect(status().isBadRequest());
    }

    @Test
    void reenviarAntesDeUnMinutoNoMandaOtroCorreo() throws Exception {
        mvc.perform(post("/api/auth/verificar-correo/reenviar").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\": \"%s\"}".formatted(CORREO)))
                .andExpect(status().isOk());

        verify(correo, times(1)).enviar(eq(CORREO), anyString(), anyString());
    }

    @Test
    void reenviarAUnCorreoQueNoExisteRespondeIgualYNoManda() throws Exception {
        mvc.perform(post("/api/auth/verificar-correo/reenviar").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\": \"nadie.verifica@adbrand.pe\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensaje").isString());

        verify(correo, never()).enviar(eq("nadie.verifica@adbrand.pe"), anyString(), anyString());
    }

    @Test
    void lasCuentasQueYaExistianNoTienenQueVerificar() throws Exception {
        Usuario antigua = new Usuario();
        antigua.setNombre("Luis");
        antigua.setNombreEmpresa("Bodega");
        antigua.setCorreo("luis.antiguo@adbrand.pe");
        antigua.setContrasenaHash("x");

        assertThat(usuarios.saveAndFlush(antigua).isCorreoVerificado()).isTrue();
    }
}
