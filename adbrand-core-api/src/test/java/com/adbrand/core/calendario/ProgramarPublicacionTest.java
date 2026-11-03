package com.adbrand.core.calendario;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.adbrand.core.auth.entity.Usuario;
import com.adbrand.core.auth.repository.UsuarioRepository;
import com.adbrand.core.auth.service.ServicioJwt;
import com.adbrand.core.calendario.repository.PublicacionProgramadaRepository;
import com.adbrand.core.contenido.dto.RedSocial;
import com.adbrand.core.contenido.entity.Contenido;
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

// HU 34: programar una publicación con un anuncio aprobado, sin fechas pasadas ni anuncios rechazados.
// Las fechas son relativas a hoy, así la prueba no deja de pasar con el tiempo.
// Usa la base local; cada prueba se deshace al terminar (@Transactional).
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ProgramarPublicacionTest {

    private static final LocalDate EN_UNA_SEMANA = LocalDate.now().plusDays(7);

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
        duenio = usuarios.saveAndFlush(nuevaCuenta("duenio.programar@adbrand.pe"));
        otraEmpresa = usuarios.saveAndFlush(nuevaCuenta("otra.programar@adbrand.pe"));
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

    private ResultActions programar(Usuario usuario, LocalDate fecha, LocalTime hora, Long contenidoId) throws Exception {
        String anuncio = contenidoId == null ? "" : ", \"contenidoId\": " + contenidoId;
        return mvc.perform(post("/api/calendario").header("Authorization", "Bearer " + jwt.generar(usuario))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"titulo": "Oferta del viernes", "redSocial": "INSTAGRAM", "fecha": "%s", "hora": "%s"%s}"""
                        .formatted(fecha, hora, anuncio)));
    }

    private long publicacionesDe(Usuario usuario) {
        return publicaciones.findAll().stream().filter(p -> p.getUsuarioId().equals(usuario.getId())).count();
    }

    @Test
    void unAnuncioAprobadoSeProgramaYElCalendarioMuestraSuOferta() throws Exception {
        Contenido aprobado = anuncio(duenio, EstadoContenido.APROBADO);

        programar(duenio, EN_UNA_SEMANA, LocalTime.of(18, 0), aprobado.getId())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("PROGRAMADA"))
                .andExpect(jsonPath("$.contenidoId").value(aprobado.getId()))
                .andExpect(jsonPath("$.contenidoOferta").value("2x1 en café los viernes"));

        String mes = EN_UNA_SEMANA.toString().substring(0, 7);
        mvc.perform(get("/api/calendario").param("mes", mes).header("Authorization", "Bearer " + jwt.generar(duenio)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].contenidoOferta").value("2x1 en café los viernes"));
    }

    @Test
    void unDiaQueYaPasoDa400YNoGuarda() throws Exception {
        programar(duenio, LocalDate.now().minusDays(1), LocalTime.of(18, 0), null)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("FECHA_PASADA"))
                .andExpect(jsonPath("$.mensaje").value("No puedes programar en una fecha u hora que ya pasó."));

        assertThat(publicacionesDe(duenio)).isZero();
    }

    @Test
    void hoyAUnaHoraQueYaPasoTambienDa400() throws Exception {
        // Un minuto atrás; si la prueba corre justo a medianoche, se usa ayer, que también ya pasó.
        LocalTime haceUnMinuto = LocalTime.now().minusMinutes(1);
        LocalDate dia = haceUnMinuto.isAfter(LocalTime.now()) ? LocalDate.now().minusDays(1) : LocalDate.now();

        programar(duenio, dia, haceUnMinuto.withSecond(0).withNano(0), null)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("FECHA_PASADA"));
    }

    @Test
    void unAnuncioRechazadoNoSePrograma() throws Exception {
        Contenido rechazado = anuncio(duenio, EstadoContenido.RECHAZADO);

        programar(duenio, EN_UNA_SEMANA, LocalTime.of(18, 0), rechazado.getId())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("CONTENIDO_RECHAZADO"))
                .andExpect(jsonPath("$.mensaje").value("Este anuncio fue rechazado y no se puede programar."));

        assertThat(publicacionesDe(duenio)).isZero();
    }

    @Test
    void unAnuncioEnRevisionTodaviaNoSePrograma() throws Exception {
        Contenido dudoso = anuncio(duenio, EstadoContenido.DUDOSO);

        programar(duenio, EN_UNA_SEMANA, LocalTime.of(18, 0), dudoso.getId())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("CONTENIDO_NO_APROBADO"));

        assertThat(publicacionesDe(duenio)).isZero();
    }

    @Test
    void elAnuncioDeOtraEmpresaOUnoQueNoExisteDa404() throws Exception {
        Contenido ajeno = anuncio(otraEmpresa, EstadoContenido.APROBADO);

        programar(duenio, EN_UNA_SEMANA, LocalTime.of(18, 0), ajeno.getId())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("CONTENIDO_NO_ENCONTRADO"));

        programar(duenio, EN_UNA_SEMANA, LocalTime.of(18, 0), 999_999_999L)
                .andExpect(status().isNotFound());

        assertThat(publicacionesDe(duenio)).isZero();
    }
}
