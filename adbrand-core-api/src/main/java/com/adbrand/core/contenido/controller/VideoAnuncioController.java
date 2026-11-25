package com.adbrand.core.contenido.controller;

import com.adbrand.core.contenido.service.VideoAnuncioService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// HU 65: video corto (MP4) de un anuncio de la empresa que inició sesión, para Reels, TikTok o Estados de WhatsApp.
@RestController
@RequestMapping("/api/contenido/anuncios")
public class VideoAnuncioController {

    private final VideoAnuncioService videos;

    public VideoAnuncioController(VideoAnuncioService videos) {
        this.videos = videos;
    }

    @GetMapping("/{id}/video")
    public ResponseEntity<byte[]> video(@RequestAttribute("usuarioId") Long usuarioId, @PathVariable Long id) {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("video/mp4"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"anuncio-" + id + ".mp4\"")
                .body(videos.generar(usuarioId, id));
    }
}
