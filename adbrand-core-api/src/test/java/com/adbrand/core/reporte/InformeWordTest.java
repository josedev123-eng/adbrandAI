package com.adbrand.core.reporte;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
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
import com.jayway.jsonpath.JsonPath;
import jakarta.persistence.EntityManager;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

// HU 39: descargar el informe en Word. Criterio 2: el Word trae los mismos datos que la pantalla y el PDF.
// Usa la base local; cada prueba se deshace al terminar (@Transactional).
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class InformeWordTest {

    private static final String URL = "/api/reportes/campanas/docx";
    private static final String TIPO_DOCX = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";

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

    @BeforeEach
    void crearCuenta() {
        Usuario usuario = new Usuario();
        usuario.setNombre("Prueba");
        usuario.setNombreEmpresa("Panadería Word");
        usuario.setCorreo("word@adbrand.pe");
        usuario.setContrasenaHash("$2a$10$hashDePruebaQueNoSeUsaParaEntrar.............");
        empresa = usuarios.saveAndFlush(usuario);
    }

    // fecha_creacion no se puede cambiar desde la entidad (updatable = false); se fija directo en la base.
    private void anuncio(String oferta, RedSocial red, EstadoContenido estado, LocalDate creado) {
        Contenido contenido = new Contenido();
        contenido.setUsuarioId(empresa.getId());
        contenido.setTipo(Contenido.TIPO_ANUNCIO);
        contenido.setRedSocial(red);
        contenido.setTono(Tono.CERCANO);
        contenido.setOferta(oferta);
        contenido.setTexto("Texto de " + oferta);
        contenido.setEstado(estado);
        contenido = contenidos.saveAndFlush(contenido);
        jdbc.update("UPDATE contenido SET fecha_creacion = ? WHERE id = ?", creado.atTime(10, 0), contenido.getId());
        entityManager.clear();
    }

    private void tresAnuncios() {
        anuncio("2 panetones por S/ 35", RedSocial.INSTAGRAM, EstadoContenido.APROBADO, LocalDate.of(2026, 11, 20));
        anuncio("Cigarrillos importados", RedSocial.FACEBOOK, EstadoContenido.RECHAZADO, LocalDate.of(2026, 11, 5));
        anuncio("Té milagroso", RedSocial.INSTAGRAM, EstadoContenido.DUDOSO, LocalDate.of(2026, 10, 15));
    }

    private String bearer() {
        return "Bearer " + jwt.generar(empresa);
    }

    private byte[] descargar(String url) throws Exception {
        return mvc.perform(get(url).header("Authorization", bearer()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();
    }

    // Todo el texto del .docx, incluidas las celdas de las tablas.
    static String texto(byte[] docx) throws Exception {
        try (XWPFDocument documento = new XWPFDocument(new ByteArrayInputStream(docx));
             XWPFWordExtractor extractor = new XWPFWordExtractor(documento)) {
            return extractor.getText();
        }
    }

    // Sin espacios ni saltos de línea, para comparar sin depender de cómo se separan las celdas.
    private static String compacto(String texto) {
        return texto.replaceAll("\\s+", "");
    }

    @Test
    void seDescargaComoWordConElNombreDeHoy() throws Exception {
        mvc.perform(get(URL).header("Authorization", bearer()))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", TIPO_DOCX))
                .andExpect(header().string("Content-Disposition",
                        "attachment; filename=\"informe-campanas-" + LocalDate.now() + ".docx\""));
    }

    // Criterio 2: cada dato que muestra la pantalla (el JSON de /api/reportes/campanas) está en el Word.
    @Test
    void losDatosDelWordCoincidenConLosDeLaPantalla() throws Exception {
        tresAnuncios();

        String json = mvc.perform(get("/api/reportes/campanas").header("Authorization", bearer()))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        String word = compacto(texto(descargar(URL)));

        Map<String, String> redes = Map.of("INSTAGRAM", "Instagram", "FACEBOOK", "Facebook");
        Map<String, String> estados = Map.of("APROBADO", "Aprobado", "DUDOSO", "En revisión", "RECHAZADO", "Rechazado");
        DateTimeFormatter formato = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

        assertThat(word).contains(compacto("Empresa: " + JsonPath.read(json, "$.empresa")));
        assertThat(word).contains(compacto("Campañas Aprobadas En revisión Rechazadas "
                + JsonPath.read(json, "$.total") + " " + JsonPath.read(json, "$.aprobados") + " "
                + JsonPath.read(json, "$.dudosos") + " " + JsonPath.read(json, "$.rechazados")));

        List<Map<String, Object>> campanas = JsonPath.read(json, "$.campanas");
        assertThat(campanas).hasSize(3);
        StringBuilder filas = new StringBuilder();
        for (Map<String, Object> campana : campanas) {
            filas.append(LocalDateTime.parse((String) campana.get("fecha")).format(formato))
                    .append(redes.get(campana.get("redSocial")))
                    .append(campana.get("oferta"))
                    .append(estados.get(campana.get("estado")));
        }
        // Las mismas filas y en el mismo orden que la pantalla.
        assertThat(word).contains(compacto(filas.toString()));
    }

    @Test
    void conFiltrosElWordTraeLosMismosAnunciosQueElPdf() throws Exception {
        tresAnuncios();
        String filtro = "?desde=2026-11-01&hasta=2026-11-30";

        String word = texto(descargar(URL + filtro));
        String pdf = PlantillaInformePdfTest.texto(descargar("/api/reportes/campanas/pdf" + filtro));

        for (String oferta : List.of("2 panetones por S/ 35", "Cigarrillos importados")) {
            assertThat(word).contains(oferta);
            assertThat(pdf).contains(oferta);
        }
        assertThat(word).doesNotContain("Té milagroso").contains("Periodo: 01/11/2026 al 30/11/2026");
        assertThat(pdf).doesNotContain("Té milagroso");
    }

    @Test
    void sinCampanasElWordLoDice() throws Exception {
        assertThat(texto(descargar(URL)))
                .contains("Empresa: Panadería Word")
                .contains("No hay campañas para mostrar.");
    }

    @Test
    void conLasFechasAlRevesNoSeGenera() throws Exception {
        mvc.perform(get(URL + "?desde=2026-11-30&hasta=2026-11-01").header("Authorization", bearer()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("FECHAS_INVALIDAS"));
    }

    @Test
    void sinTokenPideIniciarSesion() throws Exception {
        mvc.perform(get(URL))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("NO_AUTENTICADO"));
    }
}
