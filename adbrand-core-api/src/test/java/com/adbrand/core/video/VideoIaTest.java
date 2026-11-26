package com.adbrand.core.video;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.adbrand.core.config.ImagenProperties;
import com.adbrand.core.config.VideoIaProperties;
import com.adbrand.core.contenido.service.VideoAnuncioService;
import com.adbrand.core.contenido.service.VideoAnuncioService.Guion;
import com.adbrand.core.ia.dto.ConsumoMesResponse;
import com.adbrand.core.ia.service.LimiteConsumoIa;
import com.adbrand.core.imagen.entity.ConsumoImagen;
import com.adbrand.core.imagen.repository.ConsumoImagenRepository;
import com.adbrand.core.previsualizacion.dto.ImagenAnuncioResponse;
import com.adbrand.core.previsualizacion.service.PrevisualizacionService;
import com.adbrand.core.shared.error.AccesoDenegadoException;
import com.adbrand.core.shared.error.DemasiadosIntentosException;
import com.adbrand.core.shared.error.SolicitudInvalidaException;
import com.adbrand.core.video.entity.VideoIa;
import com.adbrand.core.video.repository.VideoIaRepository;
import com.adbrand.core.video.service.ClienteVideoOpenRouter;
import com.adbrand.core.video.service.ComposicionVideoIa;
import com.adbrand.core.video.service.VideoIaService;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

// HU 66: video con IA. Solo Premium, 3 al mes, nunca más caro que el tope, y el costo queda en Consumo de IA.
// Ninguna prueba llama a OpenRouter de verdad.
class VideoIaTest {

    private static final VideoIaProperties CONFIG =
            new VideoIaProperties("google/veo-3.1-lite", "720p", 8, false, new BigDecimal("0.50"), 3, "Premium");

    private VideoIaRepository videos;
    private ClienteVideoOpenRouter openRouter;
    private VideoAnuncioService plantilla;
    private PrevisualizacionService previsualizacion;
    private LimiteConsumoIa limites;
    private ConsumoImagenRepository consumos;

    private static Guion guion() {
        BufferedImage foto = new BufferedImage(360, 640, BufferedImage.TYPE_INT_RGB);
        var g = foto.createGraphics();
        g.setColor(new Color(0x8B, 0x45, 0x13));
        g.fillRect(0, 0, 360, 640);
        g.dispose();
        return new Guion(foto, "Recargue su jornada con estilo", "Cafetería Don Pepe", "Tu dosis diaria", null,
                new Color(0x8B, 0x45, 0x13), "+51 987654321");
    }

    private VideoIaService servicio(boolean mock) {
        ImagenProperties imagen = new ImagenProperties(mock, null, null, "flux", "llave-de-prueba", "gemini", 90);
        return new VideoIaService(videos, openRouter, plantilla, previsualizacion, limites, consumos, CONFIG, imagen);
    }

    private void conPlan(String origen, String plan) {
        when(limites.consumoDelMes(7L)).thenReturn(new ConsumoMesResponse(0, 400000, 0, origen, plan, false, false));
    }

    @BeforeEach
    void preparar() {
        videos = mock(VideoIaRepository.class);
        openRouter = mock(ClienteVideoOpenRouter.class);
        plantilla = mock(VideoAnuncioService.class);
        previsualizacion = mock(PrevisualizacionService.class);
        limites = mock(LimiteConsumoIa.class);
        consumos = mock(ConsumoImagenRepository.class);
        when(videos.save(any(VideoIa.class))).thenAnswer(i -> i.getArgument(0));
        when(plantilla.guionDe(7L, 9L)).thenReturn(guion());
        when(openRouter.configurado()).thenReturn(true);
        when(previsualizacion.imagen(7L, 9L)).thenReturn(new ImagenAnuncioResponse(9L, "data:image/jpeg;base64,AAA"));
    }

    @Test
    void soloElPlanPremiumPuedeCrearVideosConIa() {
        conPlan(LimiteConsumoIa.PLAN, "Pro");

        assertThatThrownBy(() -> servicio(false).iniciar(7L, 9L)).isInstanceOf(AccesoDenegadoException.class)
                .hasMessageContaining("Premium");
        assertThat(servicio(false).disponibilidad(7L).motivo()).isEqualTo("PLAN");
        verify(openRouter, never()).crear(anyString(), anyString());
    }

    @Test
    void hastaTresAlMes() {
        conPlan(LimiteConsumoIa.PLAN, "Premium");
        when(videos.countByUsuarioIdAndEstadoNotAndFechaCreacionGreaterThanEqual(eq(7L), eq(VideoIa.Estado.FALLIDO), any()))
                .thenReturn(3L);

        assertThatThrownBy(() -> servicio(false).iniciar(7L, 9L)).isInstanceOf(DemasiadosIntentosException.class)
                .hasMessageContaining("3 videos con IA");
    }

    @Test
    void nuncaPideUnVideoMasCaroQueElTopeNiSinSaberElPrecio() {
        conPlan(LimiteConsumoIa.PLAN, "Premium");
        when(openRouter.precioPorSegundo()).thenReturn(new BigDecimal("0.10"));
        assertThatThrownBy(() -> servicio(false).iniciar(7L, 9L)).isInstanceOf(SolicitudInvalidaException.class)
                .hasMessageContaining("0.80");

        when(openRouter.precioPorSegundo()).thenReturn(null);
        assertThatThrownBy(() -> servicio(false).iniciar(7L, 9L)).isInstanceOf(SolicitudInvalidaException.class)
                .hasMessageContaining("No pudimos confirmar el precio");
        verify(openRouter, never()).crear(anyString(), anyString());
    }

    @Test
    void conPremiumPideElVideoConLaFotoDelAnuncioYAlTerminarAnotaElCosto() {
        conPlan(LimiteConsumoIa.PLAN, "Premium");
        when(openRouter.precioPorSegundo()).thenReturn(new BigDecimal("0.03"));
        when(openRouter.crear(anyString(), eq("data:image/jpeg;base64,AAA"))).thenReturn("trabajo-1");

        var creado = servicio(false).iniciar(7L, 9L);
        assertThat(creado.estado()).isEqualTo("CREANDO");

        VideoIa guardado = VideoIa.nuevo(7L, 9L, "google/veo-3.1-lite");
        guardado.setTrabajoId("trabajo-1");
        when(videos.findByIdAndUsuarioId(1L, 7L)).thenReturn(Optional.of(guardado));
        when(openRouter.estado("trabajo-1"))
                .thenReturn(new ClienteVideoOpenRouter.EstadoTrabajo("completed", 100, new BigDecimal("0.24"), null));
        when(openRouter.descargar("trabajo-1")).thenReturn(VideoAnuncioService.codificar(guion()));

        var listo = servicio(false).consultar(7L, 1L);

        assertThat(listo.estado()).isEqualTo("LISTO");
        assertThat(listo.costo()).isEqualByComparingTo("0.24");
        verify(consumos).save(org.mockito.ArgumentMatchers.argThat(
                (ConsumoImagen c) -> c.getTipo() == ConsumoImagen.Tipo.VIDEO && c.getCosto().compareTo(new BigDecimal("0.24")) == 0));
    }

    @Test
    void enModoDePruebaEntregaElVideoDePlantillaSinGastar() {
        conPlan(LimiteConsumoIa.PLAN, "Premium");

        var listo = servicio(true).iniciar(7L, 9L);

        assertThat(listo.estado()).isEqualTo("LISTO");
        verify(openRouter, never()).crear(anyString(), anyString());
    }

    @Test
    void leeElPrecioDeLaResolucionYSinAudio() throws Exception {
        var skus = new ObjectMapper().readTree("""
                {"duration_seconds_with_audio":"0.08","duration_seconds_without_audio":"0.05",
                 "duration_seconds_with_audio_720p":"0.05","duration_seconds_without_audio_720p":"0.03"}""");

        assertThat(ClienteVideoOpenRouterAcceso.precioDe(skus, "720p", false)).isEqualByComparingTo("0.03");
        assertThat(ClienteVideoOpenRouterAcceso.precioDe(skus, "1080p", true)).isEqualByComparingTo("0.08");
    }

    @Test
    void alVideoDeLaIaLePoneElTitularYElCierreDeMarca() {
        byte[] deLaIa = VideoAnuncioService.codificar(guion());

        byte[] final_ = ComposicionVideoIa.componer(deLaIa, guion());

        assertThat(new String(Arrays.copyOfRange(final_, 4, 8), StandardCharsets.US_ASCII)).isEqualTo("ftyp");
        // 8 s de la IA + 3 s de marca: el archivo final es más largo que el de la IA.
        assertThat(final_.length).isGreaterThan(deLaIa.length / 2);
        assertThat(final_).isNotEqualTo(deLaIa);
    }
}
