package com.adbrand.core.reporte;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.adbrand.core.auth.entity.Usuario;
import com.adbrand.core.auth.repository.UsuarioRepository;
import com.adbrand.core.auth.service.ServicioJwt;
import com.adbrand.core.calendario.entity.Campana;
import com.adbrand.core.calendario.entity.PublicacionProgramada;
import com.adbrand.core.calendario.entity.RedSocialPublicacion;
import com.adbrand.core.calendario.entity.TipoCampana;
import com.adbrand.core.calendario.repository.CampanaRepository;
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

// HU 40: filtrar el informe (y su PDF) por periodo y por campaña.
// Usa la base local; cada prueba se deshace al terminar (@Transactional).
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class InformeFiltrosTest {

    private static final String URL = "/api/reportes/campanas";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private UsuarioRepository usuarios;

    @Autowired
    private ContenidoRepository contenidos;

    @Autowired
    private CampanaRepository campanas;

    @Autowired
    private PublicacionProgramadaRepository publicaciones;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private ServicioJwt jwt;

    private Usuario duenio;
    private Usuario otraEmpresa;

    @BeforeEach
    void preparar() {
        duenio = usuarios.saveAndFlush(nuevaCuenta("duenio.filtros@adbrand.pe"));
        otraEmpresa = usuarios.saveAndFlush(nuevaCuenta("otra.filtros@adbrand.pe"));
    }

    private Usuario nuevaCuenta(String correo) {
        Usuario usuario = new Usuario();
        usuario.setNombre("Prueba");
        usuario.setNombreEmpresa("Empresa " + correo);
        usuario.setCorreo(correo);
        usuario.setContrasenaHash("$2a$10$hashDePruebaQueNoSeUsaParaEntrar.............");
        return usuario;
    }

    // fecha_creacion no se puede cambiar desde la entidad (updatable = false); se fija directo en la base.
    private Contenido anuncio(String oferta, LocalDate creado, EstadoContenido estado) {
        Contenido contenido = new Contenido();
        contenido.setUsuarioId(duenio.getId());
        contenido.setTipo("ANUNCIO");
        contenido.setRedSocial(RedSocial.INSTAGRAM);
        contenido.setTono(Tono.CERCANO);
        contenido.setOferta(oferta);
        contenido.setTexto("Texto de " + oferta);
        contenido.setEstado(estado);
        contenido = contenidos.saveAndFlush(contenido);
        jdbc.update("UPDATE contenido SET fecha_creacion = ? WHERE id = ?", creado.atTime(10, 0), contenido.getId());
        entityManager.clear();
        return contenido;
    }

    private Campana campana(Usuario usuario, String nombre) {
        Campana campana = new Campana();
        campana.setUsuarioId(usuario.getId());
        campana.setNombre(nombre);
        campana.setColor("#C62828");
        campana.setTipo(TipoCampana.CAMPANA);
        campana.setFechaInicio(LocalDate.of(2026, 12, 1));
        campana.setFechaFin(LocalDate.of(2026, 12, 25));
        return campanas.saveAndFlush(campana);
    }

    private void programarEnCampana(Contenido contenido, Campana campana) {
        PublicacionProgramada publicacion = new PublicacionProgramada();
        publicacion.setUsuarioId(duenio.getId());
        publicacion.setTitulo(contenido.getOferta());
        publicacion.setRedSocial(RedSocialPublicacion.INSTAGRAM);
        publicacion.setFecha(LocalDate.now().plusDays(7));
        publicacion.setHora(LocalTime.of(18, 0));
        publicacion.setCampanaId(campana.getId());
        publicacion.setContenidoId(contenido.getId());
        publicaciones.saveAndFlush(publicacion);
    }

    private ResultActions informe(String consulta) throws Exception {
        return mvc.perform(get(URL + consulta).header("Authorization", "Bearer " + jwt.generar(duenio)));
    }

    private void anunciosDeOctubreYNoviembre() {
        anuncio("Promo de octubre", LocalDate.of(2026, 10, 15), EstadoContenido.APROBADO);
        anuncio("Promo del 1 de noviembre", LocalDate.of(2026, 11, 1), EstadoContenido.RECHAZADO);
        anuncio("Promo del 30 de noviembre", LocalDate.of(2026, 11, 30), EstadoContenido.APROBADO);
    }

    @Test
    void sinFiltrosTraeTodosLosAnuncios() throws Exception {
        anunciosDeOctubreYNoviembre();

        informe("")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(3));
    }

    @Test
    void elPeriodoIncluyeLosDosDiasYDejaFueraLoDemas() throws Exception {
        anunciosDeOctubreYNoviembre();

        informe("?desde=2026-11-01&hasta=2026-11-30")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(2))
                .andExpect(jsonPath("$.aprobados").value(1))
                .andExpect(jsonPath("$.rechazados").value(1))
                .andExpect(jsonPath("$.campanas[0].oferta").value("Promo del 30 de noviembre"))
                .andExpect(jsonPath("$.campanas[1].oferta").value("Promo del 1 de noviembre"));

        // Con una sola fecha también filtra.
        informe("?hasta=2026-10-31")
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.campanas[0].oferta").value("Promo de octubre"));
    }

    @Test
    void laCampanaSoloDejaLosAnunciosProgramadosEnElla() throws Exception {
        Contenido enNavidad = anuncio("Panetones", LocalDate.of(2026, 11, 20), EstadoContenido.APROBADO);
        anuncio("Promo suelta", LocalDate.of(2026, 11, 21), EstadoContenido.APROBADO);
        Campana navidad = campana(duenio, "Navidad 2026");
        programarEnCampana(enNavidad, navidad);

        informe("?campanaId=" + navidad.getId())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.campanas[0].oferta").value("Panetones"));
    }

    @Test
    void conLasFechasAlRevesDa400YNoGeneraElInforme() throws Exception {
        informe("?desde=2026-11-30&hasta=2026-11-01")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("FECHAS_INVALIDAS"))
                .andExpect(jsonPath("$.mensaje").value("La fecha de inicio no puede ser posterior a la fecha de fin."));

        mvc.perform(get(URL + "/pdf?desde=2026-11-30&hasta=2026-11-01")
                        .header("Authorization", "Bearer " + jwt.generar(duenio)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("FECHAS_INVALIDAS"));
    }

    @Test
    void unaFechaMalEscritaDa400() throws Exception {
        informe("?desde=noviembre")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("FECHA_INVALIDA"));
    }

    @Test
    void laCampanaDeOtraEmpresaDa404() throws Exception {
        Campana ajena = campana(otraEmpresa, "De otra empresa");

        informe("?campanaId=" + ajena.getId())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("CAMPANA_NO_ENCONTRADA"));
    }

    @Test
    void elPdfConFiltrosTraeLosMismosAnunciosQueLaPantalla() throws Exception {
        anunciosDeOctubreYNoviembre();

        byte[] pdf = mvc.perform(get(URL + "/pdf?desde=2026-11-01&hasta=2026-11-30")
                        .header("Authorization", "Bearer " + jwt.generar(duenio)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();
        String texto = PlantillaInformePdfTest.texto(pdf);

        assertThat(texto)
                .contains("Promo del 1 de noviembre")
                .contains("Promo del 30 de noviembre")
                .doesNotContain("Promo de octubre");
    }
}
