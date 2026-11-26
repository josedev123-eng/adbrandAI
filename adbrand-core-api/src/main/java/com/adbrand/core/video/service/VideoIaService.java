package com.adbrand.core.video.service;

import com.adbrand.core.config.ImagenProperties;
import com.adbrand.core.config.VideoIaProperties;
import com.adbrand.core.contenido.service.VideoAnuncioService;
import com.adbrand.core.contenido.service.VideoAnuncioService.Guion;
import com.adbrand.core.ia.dto.ConsumoMesResponse;
import com.adbrand.core.ia.service.LimiteConsumoIa;
import com.adbrand.core.imagen.entity.ConsumoImagen;
import com.adbrand.core.imagen.repository.ConsumoImagenRepository;
import com.adbrand.core.previsualizacion.service.PrevisualizacionService;
import com.adbrand.core.shared.error.AccesoDenegadoException;
import com.adbrand.core.shared.error.DemasiadosIntentosException;
import com.adbrand.core.shared.error.RecursoNoEncontradoException;
import com.adbrand.core.shared.error.SolicitudInvalidaException;
import com.adbrand.core.video.entity.VideoIa;
import com.adbrand.core.video.repository.VideoIaRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// HU 66: "Video con IA". La foto actual del anuncio (con sus retoques) se manda a OpenRouter para que la anime
// (Veo 3.1 Lite por defecto); cuando llega, se le pone el titular y el cierre de la marca.
// - Solo para el plan configurado (Premium) y hasta 3 por mes por cuenta (configurable).
// - Antes de pedirlo se revisa el precio del modelo: si pasaría el tope (US$0.50), no se pide.
// - El costo real se anota en consumo_imagen (tipo VIDEO) para verlo en "Consumo de IA".
// - Con AI_IMAGE_MOCK=true no se llama a OpenRouter: se entrega el video de plantilla (para probar sin gastar).
// Tarda 1 a 3 minutos: la web y la app preguntan cada pocos segundos en qué va (consultar).
@Service
public class VideoIaService {

    private static final Logger log = LoggerFactory.getLogger(VideoIaService.class);
    private static final ZoneId LIMA = ZoneId.of("America/Lima");
    static final String INSTRUCCION = "Animate this advertising photo into a short vertical video: a slow, smooth camera "
            + "push-in and gentle natural motion (steam, light, reflections, soft movement of the background). Keep the "
            + "product, its colors and the composition exactly as in the photo. Do not add any text, letters, numbers, "
            + "logos or watermarks. No people unless they are already in the photo.";

    public record Disponibilidad(boolean disponible, String motivo, long usados, int limite, String modelo,
                                 BigDecimal costoEstimado) {
    }

    public record EstadoVideo(Long id, String estado, Integer progreso, String error, BigDecimal costo, long usados,
                              int limite) {
    }

    private final VideoIaRepository videos;
    private final ClienteVideoOpenRouter openRouter;
    private final VideoAnuncioService plantilla;
    private final PrevisualizacionService previsualizacion;
    private final LimiteConsumoIa limites;
    private final ConsumoImagenRepository consumos;
    private final VideoIaProperties config;
    private final ImagenProperties imagenes;

    public VideoIaService(VideoIaRepository videos, ClienteVideoOpenRouter openRouter, VideoAnuncioService plantilla,
                          PrevisualizacionService previsualizacion, LimiteConsumoIa limites,
                          ConsumoImagenRepository consumos, VideoIaProperties config, ImagenProperties imagenes) {
        this.videos = videos;
        this.openRouter = openRouter;
        this.plantilla = plantilla;
        this.previsualizacion = previsualizacion;
        this.limites = limites;
        this.consumos = consumos;
        this.config = config;
        this.imagenes = imagenes;
    }

    // Para mostrar el botón: si está en su plan, cuántos lleva este mes y cuánto costaría.
    public Disponibilidad disponibilidad(Long usuarioId) {
        long usados = usadosDelMes(usuarioId);
        if (!tienePlan(usuarioId)) {
            return new Disponibilidad(false, "PLAN", usados, config.limiteMes(), config.modelo(), null);
        }
        if (usados >= config.limiteMes()) {
            return new Disponibilidad(false, "LIMITE", usados, config.limiteMes(), config.modelo(), null);
        }
        return new Disponibilidad(true, null, usados, config.limiteMes(), config.modelo(), costoEstimado());
    }

    @Transactional
    public EstadoVideo iniciar(Long usuarioId, Long contenidoId) {
        if (!tienePlan(usuarioId)) {
            throw new AccesoDenegadoException("PLAN_REQUERIDO",
                    "El video con IA es del plan " + config.plan() + ". Elige ese plan para usarlo.");
        }
        long usados = usadosDelMes(usuarioId);
        if (usados >= config.limiteMes()) {
            throw new DemasiadosIntentosException("LIMITE_VIDEOS_IA",
                    "Ya usaste tus " + config.limiteMes() + " videos con IA de este mes. Se renuevan el día 1.");
        }
        Guion guion = plantilla.guionDe(usuarioId, contenidoId);
        VideoIa video = videos.save(VideoIa.nuevo(usuarioId, contenidoId, imagenes.mock() ? "simulado" : config.modelo()));

        if (imagenes.mock() || !openRouter.configurado()) {
            // Modo de prueba: el video de plantilla, sin gastar.
            video.listo(VideoAnuncioService.codificar(guion), BigDecimal.ZERO);
            consumos.save(ConsumoImagen.video(usuarioId, "simulado", true, BigDecimal.ZERO));
            return respuesta(videos.save(video), usados + 1);
        }

        BigDecimal estimado = costoEstimado();
        if (estimado == null) {
            // Sin precio confirmado no se pide: así nunca se cobra más del tope.
            video.fallido("No se pudo confirmar el precio del modelo.", null);
            videos.save(video);
            throw new SolicitudInvalidaException("COSTO_DESCONOCIDO",
                    "No pudimos confirmar el precio del video con IA. Inténtalo en unos minutos.");
        }
        if (estimado.compareTo(config.costoMaximo()) > 0) {
            video.fallido("El modelo cuesta más que el tope configurado.", null);
            videos.save(video);
            throw new SolicitudInvalidaException("COSTO_MAXIMO",
                    "El video costaría US$" + estimado + " y el tope es US$" + config.costoMaximo()
                            + ". Elige un modelo más barato en AI_VIDEO_MODEL.");
        }
        String foto = previsualizacion.imagen(usuarioId, contenidoId).imagen();
        try {
            video.setTrabajoId(openRouter.crear(INSTRUCCION, foto));
        } catch (RuntimeException ex) {
            video.fallido(ex.getMessage(), null);
            videos.save(video);
            throw new SolicitudInvalidaException("VIDEO_IA_NO_DISPONIBLE",
                    "No se pudo pedir el video a la IA. Inténtalo en unos minutos.");
        }
        return respuesta(videos.save(video), usados + 1);
    }

    // Pregunta a OpenRouter en qué va. Si terminó, lo descarga, le pone el titular y la marca, y anota el costo.
    @Transactional
    public EstadoVideo consultar(Long usuarioId, Long videoId) {
        VideoIa video = buscar(usuarioId, videoId);
        if (video.getEstado() == VideoIa.Estado.CREANDO && video.getTrabajoId() != null) {
            ClienteVideoOpenRouter.EstadoTrabajo trabajo = openRouter.estado(video.getTrabajoId());
            if (trabajo.completado()) {
                byte[] crudo = openRouter.descargar(video.getTrabajoId());
                Guion guion = plantilla.guionDe(usuarioId, video.getContenidoId());
                video.listo(ComposicionVideoIa.componer(crudo, guion), trabajo.costo());
                consumos.save(ConsumoImagen.video(usuarioId, "openrouter", false, trabajo.costo()));
            } else if (trabajo.terminado()) {
                log.warn("El video con IA {} no salió: {}", videoId, trabajo.error());
                video.fallido("La IA no pudo crear el video. Inténtalo otra vez.", trabajo.costo());
                if (trabajo.costo() != null && trabajo.costo().signum() > 0) {
                    consumos.save(ConsumoImagen.video(usuarioId, "openrouter", false, trabajo.costo()));
                }
            } else {
                video.setProgreso(trabajo.progreso());
            }
            videos.save(video);
        }
        return respuesta(video, usadosDelMes(usuarioId));
    }

    @Transactional(readOnly = true)
    public byte[] archivo(Long usuarioId, Long videoId) {
        VideoIa video = buscar(usuarioId, videoId);
        if (video.getEstado() != VideoIa.Estado.LISTO || video.getVideo() == null) {
            throw new SolicitudInvalidaException("VIDEO_NO_LISTO", "El video todavía no está listo.");
        }
        return video.getVideo();
    }

    private boolean tienePlan(Long usuarioId) {
        ConsumoMesResponse consumo = limites.consumoDelMes(usuarioId);
        return LimiteConsumoIa.PLAN.equals(consumo.origen()) && consumo.plan() != null
                && consumo.plan().equalsIgnoreCase(config.plan());
    }

    private long usadosDelMes(Long usuarioId) {
        return videos.countByUsuarioIdAndEstadoNotAndFechaCreacionGreaterThanEqual(usuarioId, VideoIa.Estado.FALLIDO,
                LocalDate.now(LIMA).withDayOfMonth(1).atStartOfDay());
    }

    // Precio por segundo del modelo (de OpenRouter) x duración. null si no se pudo saber.
    BigDecimal costoEstimado() {
        if (imagenes.mock()) {
            return BigDecimal.ZERO;
        }
        try {
            BigDecimal porSegundo = openRouter.precioPorSegundo();
            return porSegundo == null ? null : porSegundo.multiply(BigDecimal.valueOf(config.duracionSegundos()));
        } catch (RuntimeException ex) {
            log.warn("No se pudo leer el precio del modelo de video: {}", ex.getMessage());
            return null;
        }
    }

    private VideoIa buscar(Long usuarioId, Long videoId) {
        return videos.findByIdAndUsuarioId(videoId, usuarioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("VIDEO_NO_ENCONTRADO", "Ese video no existe."));
    }

    private EstadoVideo respuesta(VideoIa video, long usados) {
        return new EstadoVideo(video.getId(), video.getEstado().name(), video.getProgreso(), video.getError(),
                video.getCosto(), usados, config.limiteMes());
    }
}
