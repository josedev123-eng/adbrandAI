package com.adbrand.core.contenido;

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
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

// HU 56: GET /api/contenido/estados devuelve el estado actual de cada contenido de la empresa.
// Usa la base local; cada prueba se deshace al terminar (@Transactional).
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class EstadoActualTest {

    private static final String URL = "/api/contenido/estados";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private UsuarioRepository usuarios;

    @Autowired
    private ContenidoRepository contenidos;

    @Autowired
    private ServicioJwt jwt;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private EntityManager entityManager;

    private Usuario duenio;
    private Usuario otraEmpresa;

    @BeforeEach
    void preparar() {
        duenio = usuarios.saveAndFlush(nuevaCuenta("duenio.estados@adbrand.pe"));
        otraEmpresa = usuarios.saveAndFlush(nuevaCuenta("otra.estados@adbrand.pe"));
    }

    private Usuario nuevaCuenta(String correo) {
        Usuario usuario = new Usuario();
        usuario.setNombre("Karim Sovero");
        usuario.setNombreEmpresa("Empresa " + correo);
        usuario.setCorreo(correo);
        usuario.setContrasenaHash("$2a$10$hashDePruebaQueNoSeUsaParaEntrar.............");
        return usuario;
    }

    private Contenido anuncio(Usuario usuario, String oferta, EstadoContenido estado, DecisionDueno decision) {
        Contenido contenido = new Contenido();
        contenido.setUsuarioId(usuario.getId());
        contenido.setTipo("ANUNCIO");
        contenido.setRedSocial(RedSocial.INSTAGRAM);
        contenido.setTono(Tono.CERCANO);
        contenido.setOferta(oferta);
        contenido.setTexto("Texto de prueba para " + oferta);
        contenido.setEstado(estado);
        contenido.setDecisionDueno(decision);
        return contenidos.saveAndFlush(contenido);
    }

    // Deja la última modificación en una fecha pasada, así se ve qué contenido cambió después.
    private void cambiadoHaceDias(Contenido contenido, int dias) {
        jdbc.update("UPDATE contenido SET fecha_actualizacion = NOW() - make_interval(days => ?) WHERE id = ?",
                dias, contenido.getId());
        entityManager.clear();
    }

    private String token(Usuario usuario) {
        return "Bearer " + jwt.generar(usuario);
    }

    private ResultActions estados(Usuario usuario) throws Exception {
        return mvc.perform(get(URL).header("Authorization", token(usuario)));
    }

    @Test
    void cadaContenidoMuestraSuEstadoActualYSuMotivo() throws Exception {
        Contenido pendiente = anuncio(duenio, "Pendiente", EstadoContenido.APROBADO, DecisionDueno.PENDIENTE);
        Contenido aprobado = anuncio(duenio, "Aprobado", EstadoContenido.APROBADO, DecisionDueno.APROBADO);
        Contenido enRevision = anuncio(duenio, "En revisión", EstadoContenido.DUDOSO, DecisionDueno.PENDIENTE);
        Contenido delModerador = anuncio(duenio, "Moderador", EstadoContenido.RECHAZADO, DecisionDueno.PENDIENTE);
        delModerador.setMotivoRevision("Usa palabras no permitidas.");
        Contenido rechazado = anuncio(duenio, "Rechazado", EstadoContenido.APROBADO, DecisionDueno.RECHAZADO);
        rechazado.setMotivoDueno("El precio está mal.");
        contenidos.saveAllAndFlush(java.util.List.of(delModerador, rechazado));
        // Del más antiguo al más nuevo, en el mismo orden en que se crearon.
        cambiadoHaceDias(pendiente, 5);
        cambiadoHaceDias(aprobado, 4);
        cambiadoHaceDias(enRevision, 3);
        cambiadoHaceDias(delModerador, 2);
        cambiadoHaceDias(rechazado, 1);

        estados(duenio)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(5))
                .andExpect(jsonPath("$[0].oferta").value("Rechazado"))
                .andExpect(jsonPath("$[0].estadoActual").value("RECHAZADO"))
                .andExpect(jsonPath("$[0].motivo").value("El precio está mal."))
                .andExpect(jsonPath("$[1].estadoActual").value("RECHAZADO_MODERADOR"))
                .andExpect(jsonPath("$[1].motivo").value("Usa palabras no permitidas."))
                .andExpect(jsonPath("$[2].estadoActual").value("EN_REVISION"))
                .andExpect(jsonPath("$[3].estadoActual").value("APROBADO"))
                .andExpect(jsonPath("$[3].motivo").doesNotExist())
                .andExpect(jsonPath("$[4].estadoActual").value("PENDIENTE"))
                .andExpect(jsonPath("$[4].redSocial").value("INSTAGRAM"));
    }

    @Test
    void despuesDeAprobarElContenidoSaleArribaComoAprobado() throws Exception {
        Contenido primero = anuncio(duenio, "Primero", EstadoContenido.APROBADO, DecisionDueno.PENDIENTE);
        Contenido segundo = anuncio(duenio, "Segundo", EstadoContenido.APROBADO, DecisionDueno.PENDIENTE);
        cambiadoHaceDias(primero, 2);
        cambiadoHaceDias(segundo, 1);

        mvc.perform(post("/api/contenido/" + primero.getId() + "/aprobar").header("Authorization", token(duenio)))
                .andExpect(status().isOk());

        estados(duenio)
                .andExpect(jsonPath("$[0].id").value(primero.getId()))
                .andExpect(jsonPath("$[0].estadoActual").value("APROBADO"))
                .andExpect(jsonPath("$[1].estadoActual").value("PENDIENTE"));
    }

    @Test
    void despuesDeRechazarSeVeElMotivoQueEscribioElDuenio() throws Exception {
        Contenido primero = anuncio(duenio, "Primero", EstadoContenido.APROBADO, DecisionDueno.PENDIENTE);
        Contenido segundo = anuncio(duenio, "Segundo", EstadoContenido.APROBADO, DecisionDueno.PENDIENTE);
        cambiadoHaceDias(primero, 2);
        cambiadoHaceDias(segundo, 1);

        mvc.perform(post("/api/contenido/" + primero.getId() + "/rechazar")
                        .header("Authorization", token(duenio))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"motivo\": \"No me gusta la foto\"}"))
                .andExpect(status().isOk());

        estados(duenio)
                .andExpect(jsonPath("$[0].id").value(primero.getId()))
                .andExpect(jsonPath("$[0].estadoActual").value("RECHAZADO"))
                .andExpect(jsonPath("$[0].motivo").value("No me gusta la foto"));
    }

    @Test
    void soloMuestraLosContenidosDeLaPropiaEmpresa() throws Exception {
        anuncio(duenio, "Mío", EstadoContenido.APROBADO, DecisionDueno.PENDIENTE);
        anuncio(otraEmpresa, "De otra empresa", EstadoContenido.APROBADO, DecisionDueno.APROBADO);

        estados(duenio)
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].oferta").value("Mío"));
        estados(otraEmpresa)
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].oferta").value("De otra empresa"));
    }

    @Test
    void sinContenidoDevuelveUnaListaVacia() throws Exception {
        estados(duenio)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void sinTokenPideIniciarSesion() throws Exception {
        mvc.perform(get(URL))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("NO_AUTENTICADO"));
    }
}
