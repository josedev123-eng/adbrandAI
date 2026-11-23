package com.adbrand.core.metricas;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

// HU 57: métricas del contenido para la app móvil. @SpringBootTest carga FiltroJwt, así se prueba con tokens reales.
// Usa la base local; cada prueba se deshace al terminar (@Transactional).
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class MetricasContenidoTest {

    private static final String URL = "/api/metricas/contenido";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private UsuarioRepository usuarios;

    @Autowired
    private ContenidoRepository contenidos;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private ServicioJwt jwt;

    private Usuario empresa;
    private Usuario otraEmpresa;

    @BeforeEach
    void preparar() {
        empresa = usuarios.saveAndFlush(nuevaCuenta("metricas@adbrand.pe"));
        otraEmpresa = usuarios.saveAndFlush(nuevaCuenta("otra.metricas@adbrand.pe"));
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
    private void anuncio(Usuario usuario, EstadoContenido estado, LocalDate creado) {
        Contenido contenido = new Contenido();
        contenido.setUsuarioId(usuario.getId());
        contenido.setTipo(Contenido.TIPO_ANUNCIO);
        contenido.setRedSocial(RedSocial.INSTAGRAM);
        contenido.setTono(Tono.CERCANO);
        contenido.setOferta("Oferta de prueba");
        contenido.setTexto("Texto de prueba");
        contenido.setEstado(estado);
        contenido = contenidos.saveAndFlush(contenido);
        jdbc.update("UPDATE contenido SET fecha_creacion = ? WHERE id = ?", creado.atTime(10, 0), contenido.getId());
        entityManager.clear();
    }

    private ResultActions metricas(Usuario usuario) throws Exception {
        return mvc.perform(get(URL).header("Authorization", "Bearer " + jwt.generar(usuario)));
    }

    @Test
    void cuentaLosAnunciosPorEstadoYElTotalEsLaSuma() throws Exception {
        anuncio(empresa, EstadoContenido.APROBADO, LocalDate.of(2026, 11, 3));
        anuncio(empresa, EstadoContenido.APROBADO, LocalDate.of(2026, 11, 10));
        anuncio(empresa, EstadoContenido.DUDOSO, LocalDate.of(2026, 11, 12));
        anuncio(empresa, EstadoContenido.RECHAZADO, LocalDate.of(2026, 11, 15));

        metricas(empresa)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(4))
                .andExpect(jsonPath("$.aprobados").value(2))
                .andExpect(jsonPath("$.enRevision").value(1))
                .andExpect(jsonPath("$.rechazados").value(1));
    }

    @Test
    void agrupaPorMesDelMasAntiguoAlMasReciente() throws Exception {
        anuncio(empresa, EstadoContenido.APROBADO, LocalDate.of(2026, 10, 20));
        anuncio(empresa, EstadoContenido.APROBADO, LocalDate.of(2026, 11, 3));
        anuncio(empresa, EstadoContenido.RECHAZADO, LocalDate.of(2026, 11, 15));

        metricas(empresa)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.porMes.length()").value(2))
                .andExpect(jsonPath("$.porMes[0].mes").value("2026-10"))
                .andExpect(jsonPath("$.porMes[0].cantidad").value(1))
                .andExpect(jsonPath("$.porMes[1].mes").value("2026-11"))
                .andExpect(jsonPath("$.porMes[1].cantidad").value(2));
    }

    @Test
    void sinContenidoTodoSaleEnCero() throws Exception {
        // Criterio 2: los tres estados vienen aunque no haya anuncios.
        metricas(empresa)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(0))
                .andExpect(jsonPath("$.aprobados").value(0))
                .andExpect(jsonPath("$.enRevision").value(0))
                .andExpect(jsonPath("$.rechazados").value(0))
                .andExpect(jsonPath("$.porMes.length()").value(0));
    }

    @Test
    void soloCuentaLosAnunciosDeLaPropiaEmpresa() throws Exception {
        anuncio(empresa, EstadoContenido.APROBADO, LocalDate.of(2026, 11, 3));
        anuncio(otraEmpresa, EstadoContenido.RECHAZADO, LocalDate.of(2026, 11, 4));
        anuncio(otraEmpresa, EstadoContenido.RECHAZADO, LocalDate.of(2026, 11, 5));

        metricas(empresa)
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.rechazados").value(0));
    }

    @Test
    void sinTokenPideIniciarSesion() throws Exception {
        mvc.perform(get(URL))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("NO_AUTENTICADO"));
    }
}
