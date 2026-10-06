package com.adbrand.core.contenido;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.adbrand.core.contenido.dto.AnuncioGeneradoResponse;
import com.adbrand.core.contenido.dto.GenerarAnuncioRequest;
import com.adbrand.core.contenido.dto.RedSocial;
import com.adbrand.core.contenido.entity.Contenido;
import com.adbrand.core.contenido.entity.EstadoContenido;
import com.adbrand.core.contenido.repository.ContenidoRepository;
import com.adbrand.core.contenido.service.AnuncioService;
import com.adbrand.core.contenido.service.PlantillaPromptAnuncio;
import com.adbrand.core.ia.service.IaNoDisponibleException;
import com.adbrand.core.ia.service.ServicioIa;
import com.adbrand.core.negocio.dto.PerfilNegocioResponse;
import com.adbrand.core.negocio.entity.PerfilNegocio;
import com.adbrand.core.negocio.entity.Tono;
import com.adbrand.core.negocio.repository.PerfilNegocioRepository;
import com.adbrand.core.negocio.service.PerfilNegocioService;
import com.adbrand.core.revision.dto.ResultadoRevision;
import com.adbrand.core.revision.service.FiltroContenido;
import com.adbrand.core.shared.error.RecursoNoEncontradoException;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@Import(AnuncioServiceIntegrationTest.TestConfig.class)
class AnuncioServiceIntegrationTest {

    @Configuration
    static class TestConfig {
        @Bean
        @Primary
        ServicioIa servicioIa() {
            var mock = org.mockito.Mockito.mock(ServicioIa.class);
            org.mockito.Mockito.when(mock.esSimulado()).thenReturn(true);
            java.util.concurrent.atomic.AtomicInteger counter = new java.util.concurrent.atomic.AtomicInteger(0);
            java.util.List<String> respuestas = java.util.List.of(
                "¡Primera versión del anuncio! Ven a visitarnos. #Promo",
                "¡Segunda versión mejorada! No te la pierdas. #Promo",
                "¡Tercera versión aún mejor! Aprovecha ya. #Promo",
                "¡Cuarta versión exclusiva! No te la pierdas. #Promo",
                "¡Quinta versión mejorada! Oferta limitada. #Promo"
            );
            org.mockito.Mockito.when(mock.generarTexto(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString()))
                .thenAnswer(invocation -> {
                    int idx = counter.getAndIncrement() % respuestas.size();
                    return respuestas.get(idx);
                });
            return mock;
        }
    }

    @Autowired
    AnuncioService service;

    @Autowired
    ContenidoRepository repo;

    @Autowired
    PerfilNegocioRepository perfilRepo;

    private long nextUserId = 1000;

    private long nextUserId() {
        return nextUserId++;
    }

    @Test
    void regenerar_creaNuevaAlternativaConMismosParametros() {
        PerfilNegocio perfil = new PerfilNegocio();
        long userId = nextUserId();
        perfil.setUsuarioId(userId);
        perfil.setNombreComercial("Test");
        perfil.setRubro("Test");
        perfil.setPublicoObjetivo("Público general");
        perfil.setTono(Tono.CERCANO);
        perfilRepo.save(perfil);

        GenerarAnuncioRequest req = new GenerarAnuncioRequest("Oferta test", com.adbrand.core.contenido.dto.RedSocial.INSTAGRAM);
        var primero = service.generar(userId, req);

        var segundo = service.regenerar(userId, primero.id());

        assertThat(segundo.id()).isNotEqualTo(primero.id());
        assertThat(segundo.texto()).isNotEqualTo(primero.texto());
        assertThat(segundo.oferta()).isEqualTo(primero.oferta());
        assertThat(segundo.redSocial()).isEqualTo(primero.redSocial());
    }

    @Test
    void multiplesRegeneraciones_creanAlternativasDistintas() {
        PerfilNegocio perfil = new PerfilNegocio();
        long userId = nextUserId();
        perfil.setUsuarioId(userId);
        perfil.setNombreComercial("Test2");
        perfil.setRubro("Test2");
        perfil.setPublicoObjetivo("Público general");
        perfil.setTono(Tono.PROFESIONAL);
        perfilRepo.save(perfil);

        GenerarAnuncioRequest req = new GenerarAnuncioRequest("Oferta multiple", com.adbrand.core.contenido.dto.RedSocial.FACEBOOK);
        var original = service.generar(userId, req);

        var v1 = service.regenerar(userId, original.id());
        var v2 = service.regenerar(userId, original.id());
        var v3 = service.regenerar(userId, original.id());

        assertThat(v1.id()).isNotEqualTo(original.id());
        assertThat(v2.id()).isNotEqualTo(v1.id());
        assertThat(v3.id()).isNotEqualTo(v2.id());
        assertThat(v1.texto()).isNotEqualTo(v2.texto());
        assertThat(v2.texto()).isNotEqualTo(v3.texto());
    }
}

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@SpringBootTest
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@Import(AnuncioServiceIntegrationTest.TestConfig.class)
class AnuncioServiceIntegrationTest {

    @Configuration
    static class TestConfig {
        @Bean
        @Primary
        ServicioIa servicioIa() {
            var mock = org.mockito.Mockito.mock(ServicioIa.class);
            org.mockito.Mockito.when(mock.esSimulado()).thenReturn(true);
            java.util.concurrent.atomic.AtomicInteger counter = new java.util.concurrent.atomic.AtomicInteger(0);
            java.util.List<String> respuestas = java.util.List.of(
                "¡Primera versión del anuncio! Ven a visitarnos. #Promo",
                "¡Segunda versión mejorada! No te la pierdas. #Promo",
                "¡Tercera versión aún mejor! Aprovecha ya. #Promo",
                "¡Cuarta versión exclusiva! No te la pierdas. #Promo",
                "¡Quinta versión mejorada! Oferta limitada. #Promo"
            );
            org.mockito.Mockito.when(mock.generarTexto(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString()))
                .thenAnswer(invocation -> {
                    int idx = counter.getAndIncrement() % respuestas.size();
                    return respuestas.get(idx);
                });
            return mock;
        }
    }

    @Autowired
    AnuncioService service;

    @Autowired
    ContenidoRepository repo;

    @Autowired
    PerfilNegocioRepository perfilRepo;

    private long nextUserId = 1000;

    private long nextUserId() {
        return nextUserId++;
    }

    @Test
    void regenerar_creaNuevaAlternativaConMismosParametros() {
        PerfilNegocio perfil = new PerfilNegocio();
        long userId = nextUserId();
        perfil.setUsuarioId(userId);
        perfil.setNombreComercial("Test");
        perfil.setRubro("Test");
        perfil.setPublicoObjetivo("Público general");
        perfil.setTono(Tono.CERCANO);
        perfilRepo.save(perfil);

        GenerarAnuncioRequest req = new GenerarAnuncioRequest("Oferta test", com.adbrand.core.contenido.dto.RedSocial.INSTAGRAM);
        var primero = service.generar(userId, req);

        var segundo = service.regenerar(userId, primero.id());

        assertThat(segundo.id()).isNotEqualTo(primero.id());
        assertThat(segundo.texto()).isNotEqualTo(primero.texto());
        assertThat(segundo.oferta()).isEqualTo(primero.oferta());
        assertThat(segundo.redSocial()).isEqualTo(primero.redSocial());
    }

    @Test
    void multiplesRegeneraciones_creanAlternativasDistintas() {
        PerfilNegocio perfil = new PerfilNegocio();
        long userId = nextUserId();
        perfil.setUsuarioId(userId);
        perfil.setNombreComercial("Test2");
        perfil.setRubro("Test2");
        perfil.setPublicoObjetivo("Público general");
        perfil.setTono(Tono.PROFESIONAL);
        perfilRepo.save(perfil);

        GenerarAnuncioRequest req = new GenerarAnuncioRequest("Oferta multiple", com.adbrand.core.contenido.dto.RedSocial.FACEBOOK);
        var original = service.generar(userId, req);

        var v1 = service.regenerar(userId, original.id());
        var v2 = service.regenerar(userId, original.id());
        var v3 = service.regenerar(userId, original.id());

        assertThat(v1.id()).isNotEqualTo(original.id());
        assertThat(v2.id()).isNotEqualTo(v1.id());
        assertThat(v3.id()).isNotEqualTo(v2.id());
        assertThat(v1.texto()).isNotEqualTo(v2.texto());
        assertThat(v2.texto()).isNotEqualTo(v3.texto());
    }
}