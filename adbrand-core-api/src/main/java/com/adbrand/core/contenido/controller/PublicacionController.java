package com.adbrand.core.contenido.controller;

import com.adbrand.core.contenido.dto.PublicacionResponse;
import com.adbrand.core.contenido.service.PublicacionService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// Listo para publicar: texto con hashtags y enlace "Pedir por WhatsApp" de un anuncio de la empresa que inició sesión.
@RestController
@RequestMapping("/api/contenido/anuncios")
public class PublicacionController {

    private final PublicacionService publicacion;

    public PublicacionController(PublicacionService publicacion) {
        this.publicacion = publicacion;
    }

    @GetMapping("/{id}/publicar")
    public PublicacionResponse preparar(@RequestAttribute("usuarioId") Long usuarioId, @PathVariable Long id) {
        return publicacion.preparar(usuarioId, id);
    }
}
