package com.adbrand.core.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.adbrand.core.auth.entity.CodigoRecuperacion;
import com.adbrand.core.auth.entity.Usuario;
import com.adbrand.core.auth.repository.CodigoRecuperacionRepository;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

// HU 24: recuperar la contraseña con un código. ServicioCorreo es un mock para leer el código enviado.
// Usa la base local; cada prueba se deshace al terminar (@Transactional).
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class RecuperacionTest {

    private static final String CORREO = "ana.prueba@adbrand.pe";
    private static final String CONTRASENA = "Prueba123";
    private static final String NUEVA = "Nueva2026x";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private UsuarioRepository usuarios;

    @Autowired
    private CodigoRecuperacionRepository codigos;

    @Autowired
    private PasswordEncoder codificador;

    @MockitoBean
    private ServicioCorreo correo;

    private Usuario usuario;

    @BeforeEach
    void crearCuenta() {
        usuario = new Usuario();
        usuario.setNombre("Ana Quispe");
        usuario.setNombreEmpresa("Florería Ana");
        usuario.setCorreo(CORREO);
        usuario.setContrasenaHash(codificador.encode(CONTRASENA));
        usuario = usuarios.saveAndFlush(usuario);
    }

    private ResultActions solicitar(String correoEscrito) throws Exception {
        return mvc.perform(post("/api/auth/recuperar/solicitar").contentType(MediaType.APPLICATION_JSON)
                .content("{\"correo\": \"%s\"}".formatted(correoEscrito)));
    }

    private ResultActions cambiar(String codigo, String contrasenaNueva) throws Exception {
        String cuerpo = "{\"correo\": \"%s\", \"codigo\": \"%s\", \"contrasenaNueva\": \"%s\"}"
                .formatted(CORREO, codigo, contrasenaNueva);
        return mvc.perform(post("/api/auth/recuperar/cambiar").contentType(MediaType.APPLICATION_JSON).content(cuerpo));
    }

    private ResultActions iniciarSesion(String contrasena) throws Exception {
        String cuerpo = "{\"correo\": \"%s\", \"contrasena\": \"%s\"}".formatted(CORREO, contrasena);
        return mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(cuerpo));
    }

    // Pide un código y lo saca del texto que se mandó al mock de correo.
    private String pedirCodigo() throws Exception {
        solicitar(CORREO).andExpect(status().isOk());
        ArgumentCaptor<String> texto = ArgumentCaptor.forClass(String.class);
        verify(correo).enviar(eq(CORREO), anyString(), texto.capture());
        Matcher codigo = Pattern.compile("\\b(\\d{6})\\b").matcher(texto.getValue());
        assertThat(codigo.find()).isTrue();
        return codigo.group(1);
    }

    @Test
    void codigoCorrectoCambiaLaContrasenaYLaNuevaSirveParaEntrar() throws Exception {
        String codigo = pedirCodigo();

        cambiar(codigo, NUEVA)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensaje").value("Tu contraseña se cambió, inicia sesión."));

        iniciarSesion(NUEVA).andExpect(status().isOk());
        iniciarSesion(CONTRASENA)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("CREDENCIALES_INVALIDAS"));
    }

    @Test
    void elCodigoSeGuardaConHashYNoEnTextoPlano() throws Exception {
        String codigo = pedirCodigo();

        CodigoRecuperacion guardado = codigos.findByUsuarioIdAndUsadoFalse(usuario.getId()).get(0);
        assertThat(guardado.getCodigoHash()).isNotEqualTo(codigo).startsWith("$2");
        assertThat(guardado.getFechaVencimiento()).isAfter(LocalDateTime.now().plusMinutes(14));
    }

    @Test
    void codigoUsadoNoSirveOtraVez() throws Exception {
        String codigo = pedirCodigo();
        cambiar(codigo, NUEVA).andExpect(status().isOk());

        cambiar(codigo, "OtraClave99")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("CODIGO_INVALIDO"))
                .andExpect(jsonPath("$.mensaje").value("El código no es válido o ya venció."));
    }

    @Test
    void codigoEquivocadoNoCambiaLaContrasena() throws Exception {
        String codigo = pedirCodigo();
        String equivocado = codigo.equals("000000") ? "111111" : "000000";

        cambiar(equivocado, NUEVA)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("CODIGO_INVALIDO"));

        iniciarSesion(CONTRASENA).andExpect(status().isOk());
    }

    @Test
    void codigoVencidoNoSirve() throws Exception {
        CodigoRecuperacion vencido = new CodigoRecuperacion();
        vencido.setUsuarioId(usuario.getId());
        vencido.setCodigoHash(codificador.encode("123456"));
        vencido.setFechaVencimiento(LocalDateTime.now().minusMinutes(1));
        codigos.saveAndFlush(vencido);

        cambiar("123456", NUEVA)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("CODIGO_INVALIDO"));

        iniciarSesion(CONTRASENA).andExpect(status().isOk());
    }

    @Test
    void pedirOtroCodigoDejaSinEfectoElAnterior() throws Exception {
        CodigoRecuperacion anterior = new CodigoRecuperacion();
        anterior.setUsuarioId(usuario.getId());
        anterior.setCodigoHash(codificador.encode("123456"));
        anterior.setFechaVencimiento(LocalDateTime.now().plusMinutes(10));
        codigos.saveAndFlush(anterior);

        solicitar(CORREO).andExpect(status().isOk());

        cambiar("123456", NUEVA)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("CODIGO_INVALIDO"));
    }

    @Test
    void correoNoRegistradoRespondeIgualYNoEnviaNada() throws Exception {
        solicitar("nadie.registrado@adbrand.pe")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensaje").value("Si el correo está registrado, te enviamos un código."));

        verify(correo, never()).enviar(anyString(), anyString(), anyString());
    }

    @Test
    void contrasenaNuevaDebilSeRechazaConElCampo() throws Exception {
        String codigo = pedirCodigo();

        cambiar(codigo, "debil")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.contrasenaNueva").exists());

        iniciarSesion(CONTRASENA).andExpect(status().isOk());
    }

    @Test
    void codigoQueNoTieneSeisDigitosSeRechaza() throws Exception {
        cambiar("12ab", NUEVA)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.codigo").value("El código tiene 6 dígitos."));
    }
}
