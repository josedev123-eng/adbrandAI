package com.adbrand.core.reporte;

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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

// HU 37: informe consolidado de campañas. @SpringBootTest carga FiltroJwt, así se prueba con tokens reales.
// Usa la base local; cada prueba se deshace al terminar (@Transactional).
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class InformeCampanasTest {

    private static final String URL = "/api/reportes/campanas";

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
        empresaA = usuarios.saveAndFlush(nuevaCuenta("informe.a@adbrand.pe", "Bodega Informe A"));
        empresaB = usuarios.saveAndFlush(nuevaCuenta("informe.b@adbrand.pe", "Café Informe B"));
    }

    private Usuario nuevaCuenta(String correo, String empresa) {
        Usuario usuario = new Usuario();
        usuario.setNombre("Prueba");
        usuario.setNombreEmpresa(empresa);
        usuario.setCorreo(correo);
        usuario.setContrasenaHash("$2a$10$hashDePruebaQueNoSeUsaParaEntrar.............");
        return usuario;
    }

    private Contenido anuncio(Usuario usuario, String oferta, RedSocial red, EstadoContenido estado) {
        Contenido contenido = new Contenido();
        contenido.setUsuarioId(usuario.getId());
        contenido.setTipo(Contenido.TIPO_ANUNCIO);
        contenido.setRedSocial(red);
        contenido.setTono(Tono.CERCANO);
        contenido.setOferta(oferta);
        contenido.setTexto("Texto de " + oferta);
        contenido.setEstado(estado);
        return contenidos.saveAndFlush(contenido);
    }

    private ResultActions verInforme(Usuario usuario) throws Exception {
        return mvc.perform(get(URL).header("Authorization", "Bearer " + jwt.generar(usuario)));
    }

    @Test
    void reuneLosAnunciosDeLaEmpresaConFechaRedOfertaYEstado() throws Exception {
        // Criterio 1
        anuncio(empresaA, "2 panetones por S/ 35", RedSocial.INSTAGRAM, EstadoContenido.APROBADO);
        anuncio(empresaA, "Cigarrillos importados", RedSocial.FACEBOOK, EstadoContenido.RECHAZADO);
        Contenido reciente = anuncio(empresaA, "Té milagroso", RedSocial.INSTAGRAM, EstadoContenido.DUDOSO);

        verInforme(empresaA)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.empresa").value("Bodega Informe A"))
                .andExpect(jsonPath("$.total").value(3))
                .andExpect(jsonPath("$.aprobados").value(1))
                .andExpect(jsonPath("$.dudosos").value(1))
                .andExpect(jsonPath("$.rechazados").value(1))
                // El más reciente primero.
                .andExpect(jsonPath("$.campanas[0].id").value(reciente.getId()))
                .andExpect(jsonPath("$.campanas[0].oferta").value("Té milagroso"))
                .andExpect(jsonPath("$.campanas[0].redSocial").value("INSTAGRAM"))
                .andExpect(jsonPath("$.campanas[0].estado").value("DUDOSO"))
                .andExpect(jsonPath("$.campanas[0].fecha").isNotEmpty());
    }

    @Test
    void sinCampanasElInformeVieneVacio() throws Exception {
        // Criterio 2: la web usa la lista vacía para avisar y no descargar un archivo vacío.
        verInforme(empresaA)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(0))
                .andExpect(jsonPath("$.campanas").isEmpty());
    }

    @Test
    void soloIncluyeLasCampanasDeLaPropiaEmpresa() throws Exception {
        anuncio(empresaA, "Promo de A", RedSocial.INSTAGRAM, EstadoContenido.APROBADO);
        anuncio(empresaB, "Promo de B", RedSocial.FACEBOOK, EstadoContenido.APROBADO);

        verInforme(empresaB)
                .andExpect(jsonPath("$.empresa").value("Café Informe B"))
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.campanas[0].oferta").value("Promo de B"));
    }

    @Test
    void sinTokenPideIniciarSesion() throws Exception {
        mvc.perform(get(URL))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("NO_AUTENTICADO"));
    }
}
