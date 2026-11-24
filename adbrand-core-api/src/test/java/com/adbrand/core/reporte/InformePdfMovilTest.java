package com.adbrand.core.reporte;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
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
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

// HU 59: la app móvil reutiliza el PDF del informe (HU 38 y 40) tal como lo pide:
// con el token de la sesión y el periodo que se eligió en Métricas (HU 60).
// Usa la base local; cada prueba se deshace al terminar (@Transactional).
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class InformePdfMovilTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private UsuarioRepository usuarios;

    @Autowired
    private ContenidoRepository contenidos;

    @Autowired
    private ServicioJwt jwt;

    private Usuario empresa;

    @BeforeEach
    void preparar() {
        Usuario usuario = new Usuario();
        usuario.setNombre("Prueba");
        usuario.setNombreEmpresa("Bodega Móvil");
        usuario.setCorreo("pdf.movil@adbrand.pe");
        usuario.setContrasenaHash("$2a$10$hashDePruebaQueNoSeUsaParaEntrar.............");
        empresa = usuarios.saveAndFlush(usuario);

        Contenido contenido = new Contenido();
        contenido.setUsuarioId(empresa.getId());
        contenido.setTipo(Contenido.TIPO_ANUNCIO);
        contenido.setRedSocial(RedSocial.INSTAGRAM);
        contenido.setTono(Tono.CERCANO);
        contenido.setOferta("2 panetones por S/ 35");
        contenido.setTexto("Texto de prueba");
        contenido.setEstado(EstadoContenido.APROBADO);
        contenidos.saveAndFlush(contenido);
    }

    @Test
    void conElTokenYElPeriodoDevuelveUnPdfParaGuardar() throws Exception {
        String hoy = LocalDate.now().toString();
        byte[] pdf = mvc.perform(get("/api/reportes/campanas/pdf")
                        .param("desde", hoy).param("hasta", hoy)
                        .header("Authorization", "Bearer " + jwt.generar(empresa)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string("Content-Disposition",
                        "attachment; filename=\"informe-campanas-" + hoy + ".pdf\""))
                .andReturn().getResponse().getContentAsByteArray();

        // Un PDF de verdad, no vacío: empieza con "%PDF" y trae el anuncio del periodo.
        assertThat(pdf).isNotEmpty();
        assertThat(new String(pdf, 0, 4, StandardCharsets.US_ASCII)).isEqualTo("%PDF");
        assertThat(PlantillaInformePdfTest.texto(pdf)).contains("Bodega Móvil").contains("2 panetones por S/ 35");
    }

    @Test
    void conElPeriodoAlRevesRespondeElErrorEnJsonYNoUnPdf() throws Exception {
        mvc.perform(get("/api/reportes/campanas/pdf")
                        .param("desde", "2026-11-30").param("hasta", "2026-11-01")
                        .header("Authorization", "Bearer " + jwt.generar(empresa)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("FECHAS_INVALIDAS"));
    }
}
