package com.adbrand.core.previsualizacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import com.adbrand.core.previsualizacion.dto.FormatoAnuncio;
import com.adbrand.core.previsualizacion.repository.AnuncioCompartidoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

// HU 47: cada vez que se comparte un anuncio desde la app queda registrado (tabla anuncio_compartido, V50).
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AnuncioCompartidoTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private UsuarioRepository usuarios;

    @Autowired
    private ContenidoRepository contenidos;

    @Autowired
    private AnuncioCompartidoRepository compartidos;

    @Autowired
    private ServicioJwt jwt;

    private Usuario duenio;
    private Usuario otraEmpresa;
    private Contenido anuncio;

    @BeforeEach
    void preparar() {
        duenio = usuarios.saveAndFlush(cuenta("duenio.compartir@adbrand.pe"));
        otraEmpresa = usuarios.saveAndFlush(cuenta("otra.compartir@adbrand.pe"));
        Contenido contenido = new Contenido();
        contenido.setUsuarioId(duenio.getId());
        contenido.setTipo("ANUNCIO");
        contenido.setRedSocial(RedSocial.INSTAGRAM);
        contenido.setTono(Tono.CERCANO);
        contenido.setOferta("2x1 en café");
        contenido.setTexto("Ven con un amigo y llévate dos cafés por el precio de uno.");
        contenido.setEstado(EstadoContenido.APROBADO);
        anuncio = contenidos.saveAndFlush(contenido);
    }

    private Usuario cuenta(String correo) {
        Usuario usuario = new Usuario();
        usuario.setNombre("Prueba");
        usuario.setNombreEmpresa("Empresa " + correo);
        usuario.setCorreo(correo);
        usuario.setContrasenaHash("$2a$10$hashDePruebaQueNoSeUsaParaEntrar.............");
        return usuario;
    }

    private String url() {
        return "/api/previsualizacion/anuncios/" + anuncio.getId() + "/compartido";
    }

    @Test
    void alCompartirQuedaRegistradoConSuFormatoYSeCuentan() throws Exception {
        String token = "Bearer " + jwt.generar(duenio);

        mvc.perform(post(url()).header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"formato\": \"vertical\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.formato").value("VERTICAL"))
                .andExpect(jsonPath("$.vecesCompartido").value(1))
                .andExpect(jsonPath("$.fecha").isNotEmpty());

        // Sin cuerpo: cuadrado.
        mvc.perform(post(url()).header("Authorization", token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.formato").value("CUADRADO"))
                .andExpect(jsonPath("$.vecesCompartido").value(2));

        assertThat(compartidos.findAll())
                .filteredOn(registro -> registro.getContenidoId().equals(anuncio.getId()))
                .extracting(registro -> registro.getFormato())
                .containsExactlyInAnyOrder(FormatoAnuncio.VERTICAL, FormatoAnuncio.CUADRADO);
    }

    @Test
    void elAnuncioDeOtraEmpresaNoSeRegistra() throws Exception {
        mvc.perform(post(url()).header("Authorization", "Bearer " + jwt.generar(otraEmpresa)))
                .andExpect(status().isNotFound());

        assertThat(compartidos.countByContenidoId(anuncio.getId())).isZero();
    }

    @Test
    void unFormatoInvalidoDevuelveUnMensajeClaro() throws Exception {
        mvc.perform(post(url()).header("Authorization", "Bearer " + jwt.generar(duenio))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"formato\": \"panoramico\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("FORMATO_INVALIDO"));
    }

    @Test
    void sinSesionNoSePuede() throws Exception {
        mvc.perform(post(url())).andExpect(status().isUnauthorized());
    }
}
