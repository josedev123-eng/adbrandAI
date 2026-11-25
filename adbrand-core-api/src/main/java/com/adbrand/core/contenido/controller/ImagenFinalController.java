package com.adbrand.core.contenido.controller;

import com.adbrand.core.contenido.service.ImagenFinalService;
import com.adbrand.core.previsualizacion.dto.FormatoAnuncio;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// HU 61: la imagen final del anuncio (JPG) con el logo, los colores y el eslogan del kit de marca.
// ?formato=cuadrado|vertical|horizontal (cuadrado si no se manda).
@RestController
@RequestMapping("/api/contenido/anuncios")
public class ImagenFinalController {

    private final ImagenFinalService imagenes;

    public ImagenFinalController(ImagenFinalService imagenes) {
        this.imagenes = imagenes;
    }

    @GetMapping("/{id}/imagen-final")
    public ResponseEntity<byte[]> imagenFinal(@RequestAttribute("usuarioId") Long usuarioId, @PathVariable Long id,
                                              @RequestParam(required = false) String formato) {
        FormatoAnuncio elegido = FormatoAnuncio.desde(formato);
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"anuncio-" + id + "-" + elegido.name().toLowerCase() + ".jpg\"")
                .body(imagenes.generar(usuarioId, id, elegido));
    }
}
