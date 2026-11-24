package com.adbrand.core.metricas;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.adbrand.core.auth.entity.Usuario;
import com.adbrand.core.auth.repository.UsuarioRepository;
import com.adbrand.core.auth.service.ServicioJwt;
import com.adbrand.core.calendario.entity.EstadoPublicacion;
import com.adbrand.core.calendario.entity.PublicacionProgramada;
import com.adbrand.core.calendario.entity.RedSocialPublicacion;
import com.adbrand.core.calendario.repository.PublicacionProgramadaRepository;
import com.adbrand.core.contenido.dto.RedSocial;
import com.adbrand.core.contenido.entity.Contenido;
import com.adbrand.core.contenido.entity.EstadoContenido;
import com.adbrand.core.contenido.repository.ContenidoRepository;
import com.adbrand.core.negocio.entity.Tono;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.time.LocalTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

// HU 60: las métricas filtradas por periodo (desde/hasta). Criterio 2: fechas inválidas.
// Usa la base local; cada prueba se deshace al terminar (@Transactional).
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class MetricasPeriodoTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private UsuarioRepository usuarios;

    @Autowired
    private ContenidoRepository contenidos;

    @Autowired
    private PublicacionProgramadaRepository publicaciones;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private ServicioJwt jwt;

    private Usuario empresa;

    @BeforeEach
    void preparar() {
        Usuario usuario = new Usuario();
        usuario.setNombre("Prueba");
        usuario.setNombreEmpresa("Empresa periodo");
        usuario.setCorreo("metricas.periodo@adbrand.pe");
        usuario.setContrasenaHash("$2a$10$hashDePruebaQueNoSeUsaParaEntrar.............");
        empresa = usuarios.saveAndFlush(usuario);
    }

    // fecha_creacion no se puede cambiar desde la entidad (updatable = false); se fija directo en la base.
    private void anuncio(EstadoContenido estado, LocalDate creado) {
        Contenido contenido = new Contenido();
        contenido.setUsuarioId(empresa.getId());
        contenido.setTipo(Contenido.TIPO_ANUNCIO);
        contenido.setRedSocial(RedSocial.INSTAGRAM);
        contenido.setTono(Tono.CERCANO);
        contenido.setOferta("Oferta");
        contenido.setTexto("Texto");
        contenido.setEstado(estado);
        contenido = contenidos.saveAndFlush(contenido);
        jdbc.update("UPDATE contenido SET fecha_creacion = ? WHERE id = ?", creado.atTime(23, 30), contenido.getId());
        entityManager.clear();
    }

    private void publicacion(RedSocialPublicacion red, LocalDate fecha) {
        PublicacionProgramada publicacion = new PublicacionProgramada();
        publicacion.setUsuarioId(empresa.getId());
        publicacion.setTitulo("Publicación");
        publicacion.setRedSocial(red);
        publicacion.setFecha(fecha);
        publicacion.setHora(LocalTime.of(18, 0));
        publicacion.setEstado(EstadoPublicacion.PROGRAMADA);
        publicaciones.saveAndFlush(publicacion);
    }

    private ResultActions pedir(String ruta) throws Exception {
        return mvc.perform(get(ruta).header("Authorization", "Bearer " + jwt.generar(empresa)));
    }

    @Test
    void elPeriodoDejaFueraLosAnunciosDeOtrosMesesEIncluyeLosDosDias() throws Exception {
        anuncio(EstadoContenido.APROBADO, LocalDate.of(2026, 10, 31));
        // A las 23:30 del primer y del último día: los dos días completos cuentan.
        anuncio(EstadoContenido.APROBADO, LocalDate.of(2026, 11, 1));
        anuncio(EstadoContenido.RECHAZADO, LocalDate.of(2026, 11, 30));
        anuncio(EstadoContenido.APROBADO, LocalDate.of(2026, 12, 1));

        pedir("/api/metricas/contenido?desde=2026-11-01&hasta=2026-11-30")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(2))
                .andExpect(jsonPath("$.aprobados").value(1))
                .andExpect(jsonPath("$.rechazados").value(1))
                .andExpect(jsonPath("$.porMes.length()").value(1))
                .andExpect(jsonPath("$.porMes[0].mes").value("2026-11"));

        // Sin filtro cuenta todo, como en la HU 57.
        pedir("/api/metricas/contenido").andExpect(jsonPath("$.total").value(4));
    }

    @Test
    void elPeriodoTambienFiltraLasPublicacionesPorSuFecha() throws Exception {
        publicacion(RedSocialPublicacion.INSTAGRAM, LocalDate.of(2026, 11, 30));
        publicacion(RedSocialPublicacion.INSTAGRAM, LocalDate.of(2026, 12, 1));
        publicacion(RedSocialPublicacion.FACEBOOK, LocalDate.of(2026, 12, 31));
        publicacion(RedSocialPublicacion.FACEBOOK, LocalDate.of(2027, 1, 1));

        pedir("/api/metricas/publicaciones?desde=2026-12-01&hasta=2026-12-31")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.programadas").value(2))
                .andExpect(jsonPath("$.porRed[0].programadas").value(1))
                .andExpect(jsonPath("$.porRed[1].programadas").value(1));

        pedir("/api/metricas/publicaciones").andExpect(jsonPath("$.programadas").value(4));
    }

    @Test
    void conUnaSolaFechaElOtroLadoQuedaAbierto() throws Exception {
        anuncio(EstadoContenido.APROBADO, LocalDate.of(2026, 9, 15));
        anuncio(EstadoContenido.APROBADO, LocalDate.of(2026, 11, 15));

        pedir("/api/metricas/contenido?desde=2026-10-01").andExpect(jsonPath("$.total").value(1));
        pedir("/api/metricas/contenido?hasta=2026-10-01").andExpect(jsonPath("$.total").value(1));
    }

    @Test
    void conElInicioDespuesDelFinNoSeAplicaElFiltro() throws Exception {
        for (String ruta : new String[] {"/api/metricas/contenido", "/api/metricas/publicaciones"}) {
            pedir(ruta + "?desde=2026-11-30&hasta=2026-11-01")
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.codigo").value("FECHAS_INVALIDAS"))
                    .andExpect(jsonPath("$.mensaje").value("La fecha de inicio no puede ser posterior a la fecha de fin."));
        }
    }

    @Test
    void unaFechaMalEscritaDa400() throws Exception {
        for (String ruta : new String[] {"/api/metricas/contenido", "/api/metricas/publicaciones"}) {
            pedir(ruta + "?desde=noviembre")
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.codigo").value("FECHA_INVALIDA"));
        }
    }
}
