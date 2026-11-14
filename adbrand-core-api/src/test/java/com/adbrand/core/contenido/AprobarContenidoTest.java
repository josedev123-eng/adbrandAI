package com.adbrand.core.contenido;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.adbrand.core.auth.entity.Usuario;
import com.adbrand.core.auth.repository.UsuarioRepository;
import com.adbrand.core.auth.service.ServicioJwt;
import com.adbrand.core.contenido.dto.RedSocial;
import com.adbrand.core.contenido.entity.Contenido;
import com.adbrand.core.contenido.entity.DecisionDueno;
import com.adbrand.core.contenido.entity.EstadoContenido;
import com.adbrand.core.contenido.repository.ContenidoRepository;
import com.adbrand.core.negocio.entity.Tono;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

// HU 53: el dueño aprueba su contenido con un botón desde el móvil.
// Usa la base local; cada prueba se deshace al terminar (@Transactional).
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AprobarContenidoTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private UsuarioRepository usuarios;

    @Autowired
    private ContenidoRepository contenidos;

    @Autowired
    private ServicioJwt jwt;

    private Usuario duenio;
    private Usuario otraEmpresa;

    @BeforeEach
    void preparar() {
        duenio = usuarios.saveAndFlush(nuevaCuenta("duenio.aprobar@adbrand.pe"));
        otraEmpresa = usuarios.saveAndFlush(nuevaCuenta("otra.aprobar@adbrand.pe"));
    }

    private Usuario nuevaCuenta(String correo) {
        Usuario usuario = new Usuario();
        usuario.setNombre("Prueba");
        usuario.setNombreEmpresa("Empresa " + correo);
        usuario.setCorreo(correo);
        usuario.setContrasenaHash("$2a$10$hashDePruebaQueNoSeUsaParaEntrar.............");
        return usuario;
    }

    private Contenido anuncio(Usuario usuario, EstadoContenido estado) {
        Contenido contenido = new Contenido();
        contenido.setUsuarioId(usuario.getId());
        contenido.setTipo("ANUNCIO");
        contenido.setRedSocial(RedSocial.INSTAGRAM);
        contenido.setTono(Tono.CERCANO);
        contenido.setOferta("2x1 en café los viernes");
        contenido.setTexto("Ven con un amigo y llévate dos cafés por el precio de uno.");
        contenido.setEstado(estado);
        return contenidos.saveAndFlush(contenido);
    }

    private String token(Usuario usuario) {
        return "Bearer " + jwt.generar(usuario);
    }

    private ResultActions aprobar(Usuario usuario, Contenido contenido) throws Exception {
        return mvc.perform(post("/api/contenido/" + contenido.getId() + "/aprobar").header("Authorization", token(usuario)));
    }

    private DecisionDueno decisionDe(Contenido contenido) {
        return contenidos.findById(contenido.getId()).orElseThrow().getDecisionDueno();
    }

    @Test
    void laListaPorRevisarTraeSoloLoPendienteDeLaPropiaEmpresa() throws Exception {
        Contenido pendiente = anuncio(duenio, EstadoContenido.APROBADO);
        Contenido rechazadoPorModerador = anuncio(duenio, EstadoContenido.RECHAZADO);
        Contenido yaAprobado = anuncio(duenio, EstadoContenido.APROBADO);
        yaAprobado.setDecisionDueno(DecisionDueno.APROBADO);
        contenidos.saveAndFlush(yaAprobado);
        anuncio(otraEmpresa, EstadoContenido.APROBADO);

        mvc.perform(get("/api/contenido/por-revisar").header("Authorization", token(duenio)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(rechazadoPorModerador.getId()))
                .andExpect(jsonPath("$[0].puedeAprobar").value(false))
                .andExpect(jsonPath("$[1].id").value(pendiente.getId()))
                .andExpect(jsonPath("$[1].puedeAprobar").value(true))
                .andExpect(jsonPath("$[1].decisionDueno").value("PENDIENTE"));
    }

    @Test
    void alAprobarPasaAAprobadoYSaleDeLaListaPorRevisar() throws Exception {
        Contenido contenido = anuncio(duenio, EstadoContenido.APROBADO);

        aprobar(duenio, contenido)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.decisionDueno").value("APROBADO"));

        assertThat(decisionDe(contenido)).isEqualTo(DecisionDueno.APROBADO);
        mvc.perform(get("/api/contenido/por-revisar").header("Authorization", token(duenio)))
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void elContenidoDeOtraEmpresaNoSeAprueba() throws Exception {
        Contenido ajeno = anuncio(otraEmpresa, EstadoContenido.APROBADO);

        aprobar(duenio, ajeno)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("CONTENIDO_NO_ENCONTRADO"));

        assertThat(decisionDe(ajeno)).isEqualTo(DecisionDueno.PENDIENTE);
    }

    @Test
    void loRechazadoPorElModeradorNoSeAprueba() throws Exception {
        Contenido rechazado = anuncio(duenio, EstadoContenido.RECHAZADO);

        aprobar(duenio, rechazado)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("CONTENIDO_RECHAZADO"));

        assertThat(decisionDe(rechazado)).isEqualTo(DecisionDueno.PENDIENTE);
    }

    @Test
    void loQueRevisaElModeradorTodaviaNoSeAprueba() throws Exception {
        Contenido dudoso = anuncio(duenio, EstadoContenido.DUDOSO);

        aprobar(duenio, dudoso)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("CONTENIDO_EN_REVISION"));
    }
}
