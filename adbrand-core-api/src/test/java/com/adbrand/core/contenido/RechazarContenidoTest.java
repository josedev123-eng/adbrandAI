package com.adbrand.core.contenido;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.adbrand.core.auth.entity.Usuario;
import com.adbrand.core.auth.repository.UsuarioRepository;
import com.adbrand.core.auth.service.ServicioJwt;
import com.adbrand.core.calendario.entity.PublicacionProgramada;
import com.adbrand.core.calendario.entity.RedSocialPublicacion;
import com.adbrand.core.calendario.repository.PublicacionProgramadaRepository;
import com.adbrand.core.contenido.dto.RedSocial;
import com.adbrand.core.contenido.entity.Contenido;
import com.adbrand.core.contenido.entity.DecisionDueno;
import com.adbrand.core.contenido.entity.EstadoContenido;
import com.adbrand.core.contenido.repository.ContenidoRepository;
import com.adbrand.core.negocio.entity.Tono;
import java.time.LocalDate;
import java.time.LocalTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

// HU 54: el dueño rechaza desde el móvil el contenido que no le gusta, con un motivo opcional.
// Usa la base local; cada prueba se deshace al terminar (@Transactional).
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class RechazarContenidoTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private UsuarioRepository usuarios;

    @Autowired
    private ContenidoRepository contenidos;

    @Autowired
    private PublicacionProgramadaRepository publicaciones;

    @Autowired
    private ServicioJwt jwt;

    private Usuario duenio;
    private Usuario otraEmpresa;

    @BeforeEach
    void preparar() {
        duenio = usuarios.saveAndFlush(nuevaCuenta("duenio.rechazar@adbrand.pe"));
        otraEmpresa = usuarios.saveAndFlush(nuevaCuenta("otra.rechazar@adbrand.pe"));
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

    private ResultActions rechazar(Usuario usuario, Contenido contenido, String cuerpo) throws Exception {
        var pedido = post("/api/contenido/" + contenido.getId() + "/rechazar").header("Authorization", token(usuario));
        if (cuerpo != null) {
            pedido = pedido.contentType(MediaType.APPLICATION_JSON).content(cuerpo);
        }
        return mvc.perform(pedido);
    }

    private Contenido recargar(Contenido contenido) {
        return contenidos.findById(contenido.getId()).orElseThrow();
    }

    @Test
    void alRechazarConMotivoQuedaRechazadoYSaleDeLaListaPorRevisar() throws Exception {
        Contenido contenido = anuncio(duenio, EstadoContenido.APROBADO);

        rechazar(duenio, contenido, "{\"motivo\": \"  El precio está mal  \"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.decisionDueno").value("RECHAZADO"))
                .andExpect(jsonPath("$.motivoDueno").value("El precio está mal"));

        Contenido guardado = recargar(contenido);
        assertThat(guardado.getDecisionDueno()).isEqualTo(DecisionDueno.RECHAZADO);
        assertThat(guardado.getFechaDecision()).isNotNull();
        mvc.perform(get("/api/contenido/por-revisar").header("Authorization", token(duenio)))
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void elMotivoEsOpcional() throws Exception {
        Contenido sinCuerpo = anuncio(duenio, EstadoContenido.APROBADO);
        Contenido motivoVacio = anuncio(duenio, EstadoContenido.DUDOSO);

        rechazar(duenio, sinCuerpo, null).andExpect(status().isOk());
        rechazar(duenio, motivoVacio, "{\"motivo\": \"   \"}").andExpect(status().isOk());

        assertThat(recargar(sinCuerpo).getMotivoDueno()).isNull();
        assertThat(recargar(motivoVacio).getMotivoDueno()).isNull();
        assertThat(recargar(motivoVacio).getDecisionDueno()).isEqualTo(DecisionDueno.RECHAZADO);
    }

    @Test
    void unMotivoDeMasDe300CaracteresNoSeAcepta() throws Exception {
        Contenido contenido = anuncio(duenio, EstadoContenido.APROBADO);

        rechazar(duenio, contenido, "{\"motivo\": \"" + "x".repeat(301) + "\"}")
                .andExpect(status().isBadRequest());

        assertThat(recargar(contenido).getDecisionDueno()).isEqualTo(DecisionDueno.PENDIENTE);
    }

    @Test
    void elContenidoDeOtraEmpresaNoSeRechaza() throws Exception {
        Contenido ajeno = anuncio(otraEmpresa, EstadoContenido.APROBADO);

        rechazar(duenio, ajeno, null)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("CONTENIDO_NO_ENCONTRADO"));

        assertThat(recargar(ajeno).getDecisionDueno()).isEqualTo(DecisionDueno.PENDIENTE);
    }

    @Test
    void loQueYaEstaProgramadoNoSeRechazaSinQuitarloDelCalendario() throws Exception {
        Contenido contenido = anuncio(duenio, EstadoContenido.APROBADO);
        PublicacionProgramada publicacion = new PublicacionProgramada();
        publicacion.setUsuarioId(duenio.getId());
        publicacion.setTitulo("Café 2x1");
        publicacion.setRedSocial(RedSocialPublicacion.INSTAGRAM);
        publicacion.setFecha(LocalDate.now().plusDays(3));
        publicacion.setHora(LocalTime.of(10, 0));
        publicacion.setContenidoId(contenido.getId());
        publicaciones.saveAndFlush(publicacion);

        rechazar(duenio, contenido, null)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("CONTENIDO_PROGRAMADO"));

        assertThat(recargar(contenido).getDecisionDueno()).isEqualTo(DecisionDueno.PENDIENTE);
    }

    @Test
    void loRechazadoPorElDuenoNoSeProgramaEnElCalendario() throws Exception {
        Contenido contenido = anuncio(duenio, EstadoContenido.APROBADO);
        rechazar(duenio, contenido, null).andExpect(status().isOk());

        String cuerpo = """
                {"titulo": "Café 2x1", "redSocial": "INSTAGRAM", "fecha": "%s", "hora": "10:00", "contenidoId": %d}
                """.formatted(LocalDate.now().plusDays(2), contenido.getId());
        mvc.perform(post("/api/calendario").header("Authorization", token(duenio))
                        .contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("CONTENIDO_RECHAZADO_POR_DUENO"));
    }

    @Test
    void despuesDeRechazarloYaNoSePuedeAprobar() throws Exception {
        Contenido contenido = anuncio(duenio, EstadoContenido.APROBADO);
        rechazar(duenio, contenido, null).andExpect(status().isOk());

        mvc.perform(post("/api/contenido/" + contenido.getId() + "/aprobar").header("Authorization", token(duenio)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("CONTENIDO_YA_RECHAZADO"));
    }
}
