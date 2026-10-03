package com.adbrand.core.contenido;

import com.adbrand.core.contenido.dto.GenerarAnuncioRequest;
import com.adbrand.core.contenido.entity.Contenido;
import com.adbrand.core.contenido.entity.EstadoContenido;
import com.adbrand.core.contenido.repository.ContenidoRepository;
import com.adbrand.core.negocio.entity.PerfilNegocio;
import com.adbrand.core.negocio.entity.Tono;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class AnuncioServiceTest {

    @Autowired
    AnuncioService service;

    @Autowired
    ContenidoRepository repo;

    @Autowired
    PerfilNegocioRepository perfilRepo;

    @Test
    void regenerar_creaNuevaAlternativaConMismosParametros() {
        // Given: perfil existente
        PerfilNegocio perfil = new PerfilNegocio();
        perfil.setUsuarioId(1L);
        perfil.setNombreComercial("Test");
        perfil.setRubro("Test");
        perfil.setTono(Tono.CERCANO);
        perfilRepo.save(perfil);

        // When: generar primer anuncio
        GenerarAnuncioRequest req = new GenerarAnuncioRequest("Oferta test", com.adbrand.core.contenido.dto.RedSocial.INSTAGRAM);
        var primero = service.generar(1L, req);

        // When: regenerar
        var segundo = service.regenerar(1L, primero.id());

        // Then: nueva alternativa distinta
        assertThat(segundo.id()).isNotEqualTo(primero.id());
        assertThat(segundo.texto()).isNotEqualTo(primero.texto());
        assertThat(segundo.oferta()).isEqualTo(primero.oferta());
        assertThat(segundo.redSocial()).isEqualTo(primero.redSocial());
    }

    @Test
    void multiplesRegeneraciones_creanAlternativasDistintas() {
        PerfilNegocio perfil = new PerfilNegocio();
        perfil.setUsuarioId(2L);
        perfil.setNombreComercial("Test2");
        perfil.setRubro("Test2");
        perfil.setTono(Tono.PROFESIONAL);
        perfilRepo.save(perfil);

        GenerarAnuncioRequest req = new GenerarAnuncioRequest("Oferta multiple", com.adbrand.core.contenido.dto.RedSocial.FACEBOOK);
        var original = service.generar(2L, req);

        var v1 = service.regenerar(2L, original.id());
        var v2 = service.regenerar(2L, original.id());
        var v3 = service.regenerar(2L, original.id());

        assertThat(v1.id()).isNotEqualTo(original.id());
        assertThat(v2.id()).isNotEqualTo(v1.id());
        assertThat(v3.id()).isNotEqualTo(v2.id());
        assertThat(v1.texto()).isNotEqualTo(v2.texto());
        assertThat(v2.texto()).isNotEqualTo(v3.texto());
    }
}