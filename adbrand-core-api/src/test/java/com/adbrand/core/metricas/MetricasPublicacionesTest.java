package com.adbrand.core.metricas;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.adbrand.core.auth.entity.Usuario;
import com.adbrand.core.auth.repository.UsuarioRepository;
import com.adbrand.core.auth.service.ServicioJwt;
import com.adbrand.core.calendario.entity.Campana;
import com.adbrand.core.calendario.entity.EstadoPublicacion;
import com.adbrand.core.calendario.entity.PublicacionProgramada;
import com.adbrand.core.calendario.entity.RedSocialPublicacion;
import com.adbrand.core.calendario.entity.TipoCampana;
import com.adbrand.core.calendario.repository.CampanaRepository;
import com.adbrand.core.calendario.repository.PublicacionProgramadaRepository;
import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

// HU 58: métricas de las publicaciones del calendario. Criterio 2: los números coinciden con el calendario de la web.
// Las fechas son relativas a hoy (el mes siguiente), así la prueba no deja de pasar con el tiempo.
// Usa la base local; cada prueba se deshace al terminar (@Transactional).
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class MetricasPublicacionesTest {

    private static final String URL = "/api/metricas/publicaciones";
    private static final YearMonth MES = YearMonth.now().plusMonths(1);

    @Autowired
    private MockMvc mvc;

    @Autowired
    private UsuarioRepository usuarios;

    @Autowired
    private PublicacionProgramadaRepository publicaciones;

    @Autowired
    private CampanaRepository campanas;

    @Autowired
    private ServicioJwt jwt;

    private Usuario empresa;
    private Usuario otraEmpresa;

    @BeforeEach
    void preparar() {
        empresa = usuarios.saveAndFlush(nuevaCuenta("metricas.pub@adbrand.pe"));
        otraEmpresa = usuarios.saveAndFlush(nuevaCuenta("otra.metricas.pub@adbrand.pe"));
    }

    private Usuario nuevaCuenta(String correo) {
        Usuario usuario = new Usuario();
        usuario.setNombre("Prueba");
        usuario.setNombreEmpresa("Empresa " + correo);
        usuario.setCorreo(correo);
        usuario.setContrasenaHash("$2a$10$hashDePruebaQueNoSeUsaParaEntrar.............");
        return usuario;
    }

    private PublicacionProgramada publicacion(Usuario usuario, String titulo, RedSocialPublicacion red,
                                              EstadoPublicacion estado, LocalDate fecha, Long campanaId) {
        PublicacionProgramada publicacion = new PublicacionProgramada();
        publicacion.setUsuarioId(usuario.getId());
        publicacion.setTitulo(titulo);
        publicacion.setRedSocial(red);
        publicacion.setFecha(fecha);
        publicacion.setHora(LocalTime.of(18, 0));
        publicacion.setEstado(estado);
        publicacion.setCampanaId(campanaId);
        return publicaciones.saveAndFlush(publicacion);
    }

    private Campana navidad() {
        Campana campana = new Campana();
        campana.setUsuarioId(empresa.getId());
        campana.setNombre("Navidad");
        campana.setColor("#C62828");
        campana.setTipo(TipoCampana.CAMPANA);
        campana.setFechaInicio(MES.atDay(1));
        campana.setFechaFin(MES.atEndOfMonth());
        return campanas.saveAndFlush(campana);
    }

    // Instagram: 2 programadas y 1 publicada. Facebook: 1 programada y 1 cancelada.
    private void calendarioDePrueba(Long campanaId) {
        publicacion(empresa, "Promo 1", RedSocialPublicacion.INSTAGRAM, EstadoPublicacion.PROGRAMADA, MES.atDay(10), campanaId);
        publicacion(empresa, "Promo 2", RedSocialPublicacion.INSTAGRAM, EstadoPublicacion.PROGRAMADA, MES.atDay(5), null);
        publicacion(empresa, "Ya salió", RedSocialPublicacion.INSTAGRAM, EstadoPublicacion.PUBLICADA, MES.atDay(2), null);
        publicacion(empresa, "Promo 3", RedSocialPublicacion.FACEBOOK, EstadoPublicacion.PROGRAMADA, MES.atDay(20), null);
        publicacion(empresa, "Se anuló", RedSocialPublicacion.FACEBOOK, EstadoPublicacion.CANCELADA, MES.atDay(15), null);
    }

    private String bearer(Usuario usuario) {
        return "Bearer " + jwt.generar(usuario);
    }

    private ResultActions metricas(Usuario usuario) throws Exception {
        return mvc.perform(get(URL).header("Authorization", bearer(usuario)));
    }

    @Test
    void cuentaLasPublicacionesPorRedYPorEstado() throws Exception {
        calendarioDePrueba(null);

        metricas(empresa)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.programadas").value(3))
                .andExpect(jsonPath("$.publicadas").value(1))
                .andExpect(jsonPath("$.canceladas").value(1))
                .andExpect(jsonPath("$.porRed[0].redSocial").value("INSTAGRAM"))
                .andExpect(jsonPath("$.porRed[0].programadas").value(2))
                .andExpect(jsonPath("$.porRed[0].publicadas").value(1))
                .andExpect(jsonPath("$.porRed[1].redSocial").value("FACEBOOK"))
                .andExpect(jsonPath("$.porRed[1].programadas").value(1))
                .andExpect(jsonPath("$.porRed[1].publicadas").value(0));
    }

    // Criterio 2: lo que cuenta la app es lo mismo que muestra GET /api/calendario de la web.
    @Test
    void losNumerosCoincidenConElCalendarioDeLaWeb() throws Exception {
        calendarioDePrueba(null);

        String calendario = mvc.perform(get("/api/calendario").param("mes", MES.toString())
                        .header("Authorization", bearer(empresa)))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        List<Map<String, Object>> delMes = JsonPath.read(calendario, "$");

        String json = metricas(empresa).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        for (String red : List.of("INSTAGRAM", "FACEBOOK")) {
            for (String estado : List.of("PROGRAMADA", "PUBLICADA")) {
                long enLaWeb = delMes.stream()
                        .filter(p -> red.equals(p.get("redSocial")) && estado.equals(p.get("estado")))
                        .count();
                String campo = estado.equals("PROGRAMADA") ? "programadas" : "publicadas";
                List<Integer> enLaApp = JsonPath.read(json, "$.porRed[?(@.redSocial == '" + red + "')]." + campo);
                assertThat(enLaApp.get(0).longValue()).as(red + " " + estado).isEqualTo(enLaWeb);
            }
        }
    }

    @Test
    void lasProximasSoloTraenProgramadasDesdeHoyEnOrdenYConSuCampana() throws Exception {
        Campana navidad = navidad();
        calendarioDePrueba(navidad.getId());
        // Una programada que ya pasó no es "próxima", aunque siga contando en las métricas.
        publicacion(empresa, "Atrasada", RedSocialPublicacion.INSTAGRAM, EstadoPublicacion.PROGRAMADA,
                LocalDate.now().minusDays(3), null);

        metricas(empresa)
                .andExpect(jsonPath("$.programadas").value(4))
                .andExpect(jsonPath("$.proximas.length()").value(3))
                .andExpect(jsonPath("$.proximas[0].titulo").value("Promo 2"))
                .andExpect(jsonPath("$.proximas[1].titulo").value("Promo 1"))
                .andExpect(jsonPath("$.proximas[1].campanaNombre").value("Navidad"))
                .andExpect(jsonPath("$.proximas[1].campanaColor").value("#C62828"))
                .andExpect(jsonPath("$.proximas[2].titulo").value("Promo 3"))
                .andExpect(jsonPath("$.proximas[2].campanaNombre").doesNotExist());
    }

    @Test
    void sinPublicacionesLasDosRedesSalenEnCero() throws Exception {
        metricas(empresa)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.programadas").value(0))
                .andExpect(jsonPath("$.porRed.length()").value(2))
                .andExpect(jsonPath("$.porRed[0].programadas").value(0))
                .andExpect(jsonPath("$.porRed[1].programadas").value(0))
                .andExpect(jsonPath("$.proximas.length()").value(0));
    }

    @Test
    void soloCuentaLasPublicacionesDeLaPropiaEmpresa() throws Exception {
        publicacion(otraEmpresa, "De otra", RedSocialPublicacion.INSTAGRAM, EstadoPublicacion.PROGRAMADA, MES.atDay(3), null);

        metricas(empresa)
                .andExpect(jsonPath("$.programadas").value(0))
                .andExpect(jsonPath("$.proximas.length()").value(0));
    }

    @Test
    void sinTokenPideIniciarSesion() throws Exception {
        mvc.perform(get(URL))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("NO_AUTENTICADO"));
    }
}
