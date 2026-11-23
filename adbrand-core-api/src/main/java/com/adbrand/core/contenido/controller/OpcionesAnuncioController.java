package com.adbrand.core.contenido.controller;

import com.adbrand.core.contenido.dto.GenerarAnuncioRequest;
import com.adbrand.core.contenido.dto.OpcionElegidaResponse;
import com.adbrand.core.contenido.dto.OpcionesAnuncioResponse;
import com.adbrand.core.contenido.dto.RetocarImagenRequest;
import com.adbrand.core.contenido.dto.VersionesAnuncioResponse;
import com.adbrand.core.contenido.service.OpcionesAnuncioService;
import com.adbrand.core.contenido.service.RetoqueAnuncioService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// "Generar → Elegir": 4 opciones de anuncio (texto + imagen) y elegir una. El usuario sale del token (FiltroJwt).
@RestController
@RequestMapping("/api/contenido/anuncios/opciones")
public class OpcionesAnuncioController {

    private final OpcionesAnuncioService servicio;
    private final RetoqueAnuncioService retoques;

    public OpcionesAnuncioController(OpcionesAnuncioService servicio, RetoqueAnuncioService retoques) {
        this.servicio = servicio;
        this.retoques = retoques;
    }

    @PostMapping
    public OpcionesAnuncioResponse generar(@RequestAttribute("usuarioId") Long usuarioId,
                                           @Valid @RequestBody GenerarAnuncioRequest datos) {
        return servicio.generar(usuarioId, datos);
    }

    @PostMapping("/{id}/elegir")
    public OpcionElegidaResponse elegir(@RequestAttribute("usuarioId") Long usuarioId, @PathVariable Long id) {
        return servicio.elegir(usuarioId, id);
    }

    // "Retocar con prompts cortos": edita la imagen de la opción elegida y guarda una versión nueva.
    @PostMapping("/{id}/retocar")
    public VersionesAnuncioResponse retocar(@RequestAttribute("usuarioId") Long usuarioId, @PathVariable Long id,
                                            @Valid @RequestBody RetocarImagenRequest datos) {
        return retoques.retocar(usuarioId, id, datos.instruccion());
    }

    // HU 55: "Nueva versión" sobre la opción elegida: una redacción nueva del texto
    // y la versión del anuncio asociada a la opción se actualizan juntas.
    @PostMapping("/{id}/regenerar")
    public OpcionElegidaResponse regenerar(@RequestAttribute("usuarioId") Long usuarioId, @PathVariable Long id) {
        return servicio.regenerarTexto(usuarioId, id);
    }

    @GetMapping("/{id}/versiones")
    public VersionesAnuncioResponse versiones(@RequestAttribute("usuarioId") Long usuarioId, @PathVariable Long id) {
        return retoques.versiones(usuarioId, id);
    }

    // Deshacer: volver a usar una versión anterior de la imagen.
    @PostMapping("/{id}/versiones/{numero}/usar")
    public VersionesAnuncioResponse usarVersion(@RequestAttribute("usuarioId") Long usuarioId, @PathVariable Long id,
                                                @PathVariable int numero) {
        return retoques.usarVersion(usuarioId, id, numero);
    }
}
