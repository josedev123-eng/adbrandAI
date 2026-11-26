package com.adbrand.core.video.controller;

import com.adbrand.core.video.service.VideoIaService;
import com.adbrand.core.video.service.VideoIaService.Disponibilidad;
import com.adbrand.core.video.service.VideoIaService.EstadoVideo;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

// HU 66: video con IA de un anuncio. Se pide (tarda 1 a 3 minutos), se pregunta en qué va y se descarga.
@RestController
@RequestMapping("/api/contenido")
public class VideoIaController {

    private final VideoIaService servicio;

    public VideoIaController(VideoIaService servicio) {
        this.servicio = servicio;
    }

    // { disponible, motivo (PLAN o LIMITE), usados, limite, modelo, costoEstimado }
    @GetMapping("/video-ia/disponibilidad")
    public Disponibilidad disponibilidad(@RequestAttribute("usuarioId") Long usuarioId) {
        return servicio.disponibilidad(usuarioId);
    }

    @PostMapping("/anuncios/{id}/video-ia")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public EstadoVideo iniciar(@RequestAttribute("usuarioId") Long usuarioId, @PathVariable Long id) {
        return servicio.iniciar(usuarioId, id);
    }

    // { id, estado (CREANDO, LISTO o FALLIDO), progreso, error, costo, usados, limite }
    @GetMapping("/video-ia/{id}")
    public EstadoVideo estado(@RequestAttribute("usuarioId") Long usuarioId, @PathVariable Long id) {
        return servicio.consultar(usuarioId, id);
    }

    @GetMapping("/video-ia/{id}/archivo")
    public ResponseEntity<byte[]> archivo(@RequestAttribute("usuarioId") Long usuarioId, @PathVariable Long id) {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("video/mp4"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"video-ia-" + id + ".mp4\"")
                .body(servicio.archivo(usuarioId, id));
    }
}
