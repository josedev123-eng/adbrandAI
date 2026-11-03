package com.adbrand.core.calendario;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.adbrand.core.auth.entity.Usuario;
import com.adbrand.core.auth.repository.UsuarioRepository;
import com.adbrand.core.auth.service.ServicioJwt;
import com.adbrand.core.calendario.repository.CampanaRepository;
import com.adbrand.core.calendario.repository.PublicacionProgramadaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

// HU 35: campañas y eventos especiales. @SpringBootTest carga FiltroJwt, así se prueba con tokens reales.
// Usa la base local; cada prueba se deshace al terminar (@Transactional).
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CampanaTest {

    private static final String NAVIDAD = """
            {"nombre": "  Navidad 2026  ", "color": "#c62828", "tipo": "CAMPANA",
             "fechaInicio": "2026-12-01", "fechaFin": "2026-12-25"}""";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private UsuarioRepository usuarios;

    @Autowired
    private CampanaRepository campanas;

    @Autowired
    private PublicacionProgramadaRepository publicaciones;

    @Autowired
    private ServicioJwt jwt;

    private Usuario duenio;
    private Usuario otraEmpresa;

    @BeforeEach
    void preparar() {
        duenio = usuarios.saveAndFlush(nuevaCuenta("duenio.campana@adbrand.pe"));
        otraEmpresa = usuarios.saveAndFlush(nuevaCuenta("otra.campana@adbrand.pe"));
    }

    private Usuario nuevaCuenta(String correo) {
        Usuario usuario = new Usuario();
        usuario.setNombre("Prueba");
        usuario.setNombreEmpresa("Empresa " + correo);
        usuario.setCorreo(correo);
        usuario.setContrasenaHash("$2a$10$hashDePruebaQueNoSeUsaParaEntrar.............");
        return usuario;
    }

    private String bearer(Usuario usuario) {
        return "Bearer " + jwt.generar(usuario);
    }

    private ResultActions crearCampana(Usuario usuario, String cuerpo) throws Exception {
        return mvc.perform(post("/api/campanas").header("Authorization", bearer(usuario))
                .contentType(MediaType.APPLICATION_JSON).content(cuerpo));
    }

    private ResultActions programar(Usuario usuario, String titulo, String fecha, Long campanaId) throws Exception {
        String campana = campanaId == null ? "" : ", \"campanaId\": " + campanaId;
        return mvc.perform(post("/api/calendario").header("Authorization", bearer(usuario))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"titulo": "%s", "redSocial": "INSTAGRAM", "fecha": "%s", "hora": "18:00"%s}"""
                        .formatted(titulo, fecha, campana)));
    }

    private Long idDe(ResultActions resultado) throws Exception {
        String json = resultado.andReturn().getResponse().getContentAsString();
        return Long.valueOf(json.replaceAll(".*?\"id\":(\\d+).*", "$1"));
    }

    @Test
    void crearGuardaLaCampanaConElNombreLimpioYElColorEnMayusculas() throws Exception {
        crearCampana(duenio, NAVIDAD)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nombre").value("Navidad 2026"))
                .andExpect(jsonPath("$.color").value("#C62828"))
                .andExpect(jsonPath("$.tipo").value("CAMPANA"))
                .andExpect(jsonPath("$.fechaInicio").value("2026-12-01"))
                .andExpect(jsonPath("$.fechaFin").value("2026-12-25"));

        assertThat(campanas.findByUsuarioIdOrderByFechaInicioAscIdAsc(duenio.getId())).hasSize(1);
    }

    @Test
    void laListaSoloTraeLasCampanasDeMiEmpresaOrdenadasPorFecha() throws Exception {
        crearCampana(duenio, NAVIDAD).andExpect(status().isCreated());
        crearCampana(duenio, NAVIDAD.replace("Navidad 2026", "Día de la Madre")
                .replace("2026-12-01", "2026-05-01").replace("2026-12-25", "2026-05-10")
                .replace("CAMPANA", "EVENTO")).andExpect(status().isCreated());
        crearCampana(otraEmpresa, NAVIDAD.replace("Navidad 2026", "De otra empresa")).andExpect(status().isCreated());

        mvc.perform(get("/api/campanas").header("Authorization", bearer(duenio)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].nombre").value("Día de la Madre"))
                .andExpect(jsonPath("$[0].tipo").value("EVENTO"))
                .andExpect(jsonPath("$[1].nombre").value("Navidad 2026"));
    }

    @Test
    void fechaDeFinAntesDeLaDeInicioDa400YNoGuarda() throws Exception {
        crearCampana(duenio, NAVIDAD.replace("2026-12-25", "2026-11-30"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("FECHAS_INVALIDAS"));

        assertThat(campanas.findByUsuarioIdOrderByFechaInicioAscIdAsc(duenio.getId())).isEmpty();
    }

    @Test
    void colorMalEscritoONombreVacioDa400ConElMensajeDeCadaCampo() throws Exception {
        for (String color : new String[] {"rojo", "#FFF", "C62828", "#12345G"}) {
            crearCampana(duenio, NAVIDAD.replace("#c62828", color).replace("  Navidad 2026  ", " "))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.campos.color").value("Escribe el color como #RRGGBB."))
                    .andExpect(jsonPath("$.campos.nombre").value("Escribe el nombre de la campaña."));
        }

        assertThat(campanas.findByUsuarioIdOrderByFechaInicioAscIdAsc(duenio.getId())).isEmpty();
    }

    @Test
    void laPublicacionDeUnaCampanaLlevaSuNombreYColor() throws Exception {
        Long navidad = idDe(crearCampana(duenio, NAVIDAD));

        programar(duenio, "Oferta de Navidad", "2026-12-20", navidad)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.campanaId").value(navidad))
                .andExpect(jsonPath("$.campanaNombre").value("Navidad 2026"))
                .andExpect(jsonPath("$.campanaColor").value("#C62828"));

        programar(duenio, "Post sin campaña", "2026-12-21", null)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.campanaId").doesNotExist())
                .andExpect(jsonPath("$.campanaColor").doesNotExist());
    }

    @Test
    void elFiltroPorCampanaSoloMuestraSusPublicaciones() throws Exception {
        Long navidad = idDe(crearCampana(duenio, NAVIDAD));
        programar(duenio, "Oferta de Navidad", "2026-12-20", navidad).andExpect(status().isCreated());
        programar(duenio, "Post sin campaña", "2026-12-21", null).andExpect(status().isCreated());

        mvc.perform(get("/api/calendario").param("mes", "2026-12").header("Authorization", bearer(duenio)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].campanaColor").value("#C62828"));

        mvc.perform(get("/api/calendario").param("mes", "2026-12").param("campanaId", navidad.toString())
                        .header("Authorization", bearer(duenio)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].titulo").value("Oferta de Navidad"));
    }

    @Test
    void laCampanaDeOtraEmpresaDa404AlFiltrarYAlProgramar() throws Exception {
        Long ajena = idDe(crearCampana(otraEmpresa, NAVIDAD));

        mvc.perform(get("/api/calendario").param("mes", "2026-12").param("campanaId", ajena.toString())
                        .header("Authorization", bearer(duenio)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("CAMPANA_NO_ENCONTRADA"));

        programar(duenio, "Intento", "2026-12-20", ajena)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("CAMPANA_NO_ENCONTRADA"));

        assertThat(publicaciones.findByUsuarioIdAndFechaBetweenOrderByFechaAscHoraAsc(duenio.getId(),
                java.time.LocalDate.of(2026, 12, 1), java.time.LocalDate.of(2026, 12, 31))).isEmpty();
    }

    @Test
    void sinTokenDa401() throws Exception {
        mvc.perform(get("/api/campanas"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("NO_AUTENTICADO"));

        mvc.perform(post("/api/campanas").contentType(MediaType.APPLICATION_JSON).content(NAVIDAD))
                .andExpect(status().isUnauthorized());
    }
}
