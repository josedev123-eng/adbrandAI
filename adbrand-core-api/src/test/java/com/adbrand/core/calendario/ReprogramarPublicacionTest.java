package com.adbrand.core.calendario;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.adbrand.core.auth.entity.Usuario;
import com.adbrand.core.auth.repository.UsuarioRepository;
import com.adbrand.core.auth.service.ServicioJwt;
import com.adbrand.core.calendario.entity.EstadoPublicacion;
import com.adbrand.core.calendario.entity.PublicacionProgramada;
import com.adbrand.core.calendario.entity.RedSocialPublicacion;
import com.adbrand.core.calendario.repository.PublicacionProgramadaRepository;
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

// HU 36: mover una publicación a otro día al arrastrarla en el calendario.
// Las fechas son relativas a hoy, así la prueba no deja de pasar con el tiempo.
// Usa la base local; cada prueba se deshace al terminar (@Transactional).
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ReprogramarPublicacionTest {

    private static final LocalDate EN_UNA_SEMANA = LocalDate.now().plusDays(7);
    private static final LocalDate EN_DOS_SEMANAS = LocalDate.now().plusDays(14);

    @Autowired
    private MockMvc mvc;

    @Autowired
    private UsuarioRepository usuarios;

    @Autowired
    private PublicacionProgramadaRepository publicaciones;

    @Autowired
    private ServicioJwt jwt;

    private Usuario duenio;
    private Usuario otraEmpresa;

    @BeforeEach
    void preparar() {
        duenio = usuarios.saveAndFlush(nuevaCuenta("duenio.mover@adbrand.pe"));
        otraEmpresa = usuarios.saveAndFlush(nuevaCuenta("otra.mover@adbrand.pe"));
    }

    private Usuario nuevaCuenta(String correo) {
        Usuario usuario = new Usuario();
        usuario.setNombre("Prueba");
        usuario.setNombreEmpresa("Empresa " + correo);
        usuario.setCorreo(correo);
        usuario.setContrasenaHash("$2a$10$hashDePruebaQueNoSeUsaParaEntrar.............");
        return usuario;
    }

    private PublicacionProgramada publicacion(Usuario usuario, EstadoPublicacion estado) {
        PublicacionProgramada publicacion = new PublicacionProgramada();
        publicacion.setUsuarioId(usuario.getId());
        publicacion.setTitulo("Oferta del viernes");
        publicacion.setRedSocial(RedSocialPublicacion.INSTAGRAM);
        publicacion.setFecha(EN_UNA_SEMANA);
        publicacion.setHora(LocalTime.of(18, 30));
        publicacion.setEstado(estado);
        return publicaciones.saveAndFlush(publicacion);
    }

    private ResultActions mover(Usuario usuario, Long id, String cuerpo) throws Exception {
        return mvc.perform(patch("/api/calendario/" + id + "/fecha")
                .header("Authorization", "Bearer " + jwt.generar(usuario))
                .contentType(MediaType.APPLICATION_JSON).content(cuerpo));
    }

    private LocalDate fechaGuardada(Long id) {
        return publicaciones.findById(id).orElseThrow().getFecha();
    }

    @Test
    void moverAUnDiaValidoGuardaLaFechaNuevaYMantieneLaHora() throws Exception {
        PublicacionProgramada programada = publicacion(duenio, EstadoPublicacion.PROGRAMADA);

        mover(duenio, programada.getId(), "{\"fecha\": \"" + EN_DOS_SEMANAS + "\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fecha").value(EN_DOS_SEMANAS.toString()))
                .andExpect(jsonPath("$.hora").value("18:30:00"))
                .andExpect(jsonPath("$.titulo").value("Oferta del viernes"));

        assertThat(fechaGuardada(programada.getId())).isEqualTo(EN_DOS_SEMANAS);
    }

    @Test
    void moverAUnDiaQueYaPasoDa400YSeQuedaEnSuDia() throws Exception {
        PublicacionProgramada programada = publicacion(duenio, EstadoPublicacion.PROGRAMADA);

        mover(duenio, programada.getId(), "{\"fecha\": \"" + LocalDate.now().minusDays(1) + "\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("FECHA_PASADA"))
                .andExpect(jsonPath("$.mensaje").value("No puedes mover una publicación a un día que ya pasó."));

        assertThat(fechaGuardada(programada.getId())).isEqualTo(EN_UNA_SEMANA);
    }

    @Test
    void laPublicacionDeOtraEmpresaDa404YNoSeMueve() throws Exception {
        PublicacionProgramada ajena = publicacion(otraEmpresa, EstadoPublicacion.PROGRAMADA);

        mover(duenio, ajena.getId(), "{\"fecha\": \"" + EN_DOS_SEMANAS + "\"}")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("PUBLICACION_NO_ENCONTRADA"));

        assertThat(fechaGuardada(ajena.getId())).isEqualTo(EN_UNA_SEMANA);
    }

    @Test
    void unaPublicacionYaPublicadaOCanceladaNoSeMueve() throws Exception {
        for (EstadoPublicacion estado : new EstadoPublicacion[] {EstadoPublicacion.PUBLICADA, EstadoPublicacion.CANCELADA}) {
            PublicacionProgramada cerrada = publicacion(duenio, estado);

            mover(duenio, cerrada.getId(), "{\"fecha\": \"" + EN_DOS_SEMANAS + "\"}")
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.codigo").value("PUBLICACION_NO_MOVIBLE"));

            assertThat(fechaGuardada(cerrada.getId())).isEqualTo(EN_UNA_SEMANA);
        }
    }

    @Test
    void sinFechaDa400ConElMensajeDelCampo() throws Exception {
        PublicacionProgramada programada = publicacion(duenio, EstadoPublicacion.PROGRAMADA);

        mover(duenio, programada.getId(), "{}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.fecha").value("Selecciona la fecha."));
    }
}
