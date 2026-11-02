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
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

// HU 38: descargar el informe en PDF. @SpringBootTest carga FiltroJwt, así se prueba con tokens reales.
// Usa la base local; cada prueba se deshace al terminar (@Transactional).
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class InformePdfTest {

    private static final String URL = "/api/reportes/campanas/pdf";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private UsuarioRepository usuarios;

    @Autowired
    private ContenidoRepository contenidos;

    @Autowired
    private ServicioJwt jwt;

    private Usuario empresaA;
    private Usuario empresaB;

    @BeforeEach
    void crearCuentas() {
        empresaA = usuarios.saveAndFlush(nuevaCuenta("pdf.a@adbrand.pe", "Bodega PDF A"));
        empresaB = usuarios.saveAndFlush(nuevaCuenta("pdf.b@adbrand.pe", "Café PDF B"));
    }

    private Usuario nuevaCuenta(String correo, String empresa) {
        Usuario usuario = new Usuario();
        usuario.setNombre("Prueba");
        usuario.setNombreEmpresa(empresa);
        usuario.setCorreo(correo);
        usuario.setContrasenaHash("$2a$10$hashDePruebaQueNoSeUsaParaEntrar.............");
        return usuario;
    }

    private void anuncio(Usuario usuario, String oferta, RedSocial red, EstadoContenido estado) {
        Contenido contenido = new Contenido();
        contenido.setUsuarioId(usuario.getId());
        contenido.setTipo(Contenido.TIPO_ANUNCIO);
        contenido.setRedSocial(red);
        contenido.setTono(Tono.CERCANO);
        contenido.setOferta(oferta);
        contenido.setTexto("Texto de " + oferta);
        contenido.setEstado(estado);
        contenidos.saveAndFlush(contenido);
    }

    private byte[] descargar(Usuario usuario) throws Exception {
        return mvc.perform(get(URL).header("Authorization", "Bearer " + jwt.generar(usuario)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andReturn().getResponse().getContentAsByteArray();
    }

    @Test
    void descargaUnPdfConNombreDeArchivo() throws Exception {
        anuncio(empresaA, "2 panetones por S/ 35", RedSocial.INSTAGRAM, EstadoContenido.APROBADO);

        mvc.perform(get(URL).header("Authorization", "Bearer " + jwt.generar(empresaA)))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition",
                        "attachment; filename=\"informe-campanas-" + LocalDate.now() + ".pdf\""));
    }

    @Test
    void elPdfTraeLaEmpresaLaTablaYLosTotales() throws Exception {
        // Criterio 1
        anuncio(empresaA, "2 panetones por S/ 35", RedSocial.INSTAGRAM, EstadoContenido.APROBADO);
        anuncio(empresaA, "Té milagroso", RedSocial.FACEBOOK, EstadoContenido.DUDOSO);

        String texto = PlantillaInformePdfTest.texto(descargar(empresaA));

        assertThat(texto)
                .contains("Empresa: Bodega PDF A")
                .contains("2 panetones por S/ 35")
                .contains("Té milagroso")
                .contains("Instagram")
                .contains("Facebook")
                .contains("En revisión");
    }

    @Test
    void soloTraeLasCampanasDeLaPropiaEmpresa() throws Exception {
        anuncio(empresaA, "Promo de A", RedSocial.INSTAGRAM, EstadoContenido.APROBADO);
        anuncio(empresaB, "Promo de B", RedSocial.FACEBOOK, EstadoContenido.APROBADO);

        String texto = PlantillaInformePdfTest.texto(descargar(empresaB));

        assertThat(texto).contains("Café PDF B").contains("Promo de B").doesNotContain("Promo de A");
    }

    @Test
    void sinTokenPideIniciarSesion() throws Exception {
        mvc.perform(get(URL))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("NO_AUTENTICADO"));
    }
}
