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
import com.adbrand.core.contenido.entity.EstadoContenido;
import com.adbrand.core.contenido.repository.ContenidoRepository;
import com.adbrand.core.negocio.entity.Tono;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

// "Mis anuncios": el dueño ve el estado de sus anuncios y se entera de lo que decidió el moderador.
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class MisAnunciosTest {

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
        duenio = usuarios.saveAndFlush(nuevaCuenta("duenio.misanuncios@adbrand.pe"));
        otraEmpresa = usuarios.saveAndFlush(nuevaCuenta("otra.misanuncios@adbrand.pe"));
    }

    private Usuario nuevaCuenta(String correo) {
        Usuario usuario = new Usuario();
        usuario.setNombre("Prueba");
        usuario.setNombreEmpresa("Empresa " + correo);
        usuario.setCorreo(correo);
        usuario.setContrasenaHash("$2a$10$hashDePruebaQueNoSeUsaParaEntrar.............");
        return usuario;
    }

    // Simula lo que hace Django al moderar (HU 15): cambia el estado, guarda el moderador y deja el aviso.
    private Contenido anuncio(Usuario usuario, EstadoContenido estado, String motivo, boolean moderadoConAviso) {
        Contenido contenido = new Contenido();
        contenido.setUsuarioId(usuario.getId());
        contenido.setTipo("ANUNCIO");
        contenido.setRedSocial(RedSocial.INSTAGRAM);
        contenido.setTono(Tono.CERCANO);
        contenido.setOferta("2x1 en café los viernes");
        contenido.setTexto("Ven con un amigo.");
        contenido.setEstado(estado);
        contenido.setMotivoRevision(motivo);
        if (moderadoConAviso) {
            contenido.setModeradorId(7L);
            contenido.setAvisoPendiente(true);
        }
        return contenidos.saveAndFlush(contenido);
    }

    private String token(Usuario usuario) {
        return "Bearer " + jwt.generar(usuario);
    }

    @Test
    void listaSoloLosAnunciosPropiosConSuEstadoYElMotivoDelRechazo() throws Exception {
        anuncio(duenio, EstadoContenido.APROBADO, null, false);
        Contenido rechazado = anuncio(duenio, EstadoContenido.RECHAZADO, "Publicidad de tabaco.", true);
        anuncio(otraEmpresa, EstadoContenido.APROBADO, null, false);

        mvc.perform(get("/api/contenido/mis-anuncios").header("Authorization", token(duenio)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(rechazado.getId()))
                .andExpect(jsonPath("$[0].estado").value("RECHAZADO"))
                .andExpect(jsonPath("$[0].motivo").value("Publicidad de tabaco."))
                .andExpect(jsonPath("$[0].moderado").value(true))
                .andExpect(jsonPath("$[0].avisoPendiente").value(true));
    }

    @Test
    void elNumeroDeAvisosCuentaLasDecisionesNoVistasYAlAbrirlasVuelveACero() throws Exception {
        anuncio(duenio, EstadoContenido.APROBADO, null, true);
        anuncio(duenio, EstadoContenido.RECHAZADO, "Lenguaje ofensivo.", true);
        anuncio(otraEmpresa, EstadoContenido.RECHAZADO, "Otro.", true);

        mvc.perform(get("/api/contenido/avisos").header("Authorization", token(duenio)))
                .andExpect(jsonPath("$.pendientes").value(2));

        mvc.perform(post("/api/contenido/avisos/vistos").header("Authorization", token(duenio)))
                .andExpect(status().isNoContent());

        mvc.perform(get("/api/contenido/avisos").header("Authorization", token(duenio)))
                .andExpect(jsonPath("$.pendientes").value(0));
        // Los avisos de la otra empresa no se tocan.
        assertThat(contenidos.countByUsuarioIdAndAvisoPendienteTrue(otraEmpresa.getId())).isEqualTo(1);
    }

    @Test
    void sinSesionNoSeVeNada() throws Exception {
        mvc.perform(get("/api/contenido/mis-anuncios")).andExpect(status().isUnauthorized());
    }
}
