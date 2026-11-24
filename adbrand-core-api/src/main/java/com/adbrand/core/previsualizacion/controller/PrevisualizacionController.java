package com.adbrand.core.previsualizacion.controller;

import com.adbrand.core.previsualizacion.dto.PrevisualizacionResponse;
import com.adbrand.core.previsualizacion.service.PrevisualizacionService;
import com.adbrand.core.previsualizacion.dto.CompartidoDtos;
import com.adbrand.core.previsualizacion.dto.FormatoAnuncio;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import com.adbrand.core.previsualizacion.dto.ImagenAnuncioResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// HU 45: la empresa sale del token; no se recibe un id por la URL.
@RestController
@RequestMapping("/api/previsualizacion")
public class PrevisualizacionController {

    private final PrevisualizacionService servicio;

    public PrevisualizacionController(PrevisualizacionService servicio) {
        this.servicio = servicio;
    }

    // HU 46: ?formato=cuadrado|vertical|horizontal (cuadrado si no se manda).
    @GetMapping
    public PrevisualizacionResponse obtener(@RequestAttribute("usuarioId") Long usuarioId,
                                            @RequestParam(required = false) String formato) {
        return servicio.obtener(usuarioId, FormatoAnuncio.desde(formato));
    }

    // HU 46: la imagen de un anuncio para su vista previa como publicación. Va aparte porque pesa:
    // la app la pide solo cuando se abre ese anuncio.
    // HU 47: la app avisa que el anuncio se compartió (WhatsApp, correo, etc.). { "formato": "VERTICAL" } opcional.
    @PostMapping("/anuncios/{contenidoId}/compartido")
    @ResponseStatus(HttpStatus.CREATED)
    public CompartidoDtos.CompartidoResponse compartido(@RequestAttribute("usuarioId") Long usuarioId,
                                                        @PathVariable Long contenidoId,
                                                        @RequestBody(required = false) CompartidoDtos.CompartidoRequest datos) {
        return servicio.registrarCompartido(usuarioId, contenidoId, datos == null ? null : datos.formato());
    }

    @GetMapping("/anuncios/{contenidoId}/imagen")
    public ImagenAnuncioResponse imagen(@RequestAttribute("usuarioId") Long usuarioId, @PathVariable Long contenidoId) {
        return servicio.imagen(usuarioId, contenidoId);
    }
}
