package com.adbrand.core.imagen.controller;

import com.adbrand.core.imagen.dto.ImagenResponse;
import com.adbrand.core.imagen.dto.PedirImagenRequest;
import com.adbrand.core.imagen.service.ServicioImagenes;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// Pide una imagen borrador a partir de una descripción. Pide token como todo /api.
// Es la base del flujo "Generar opciones → Elegir → Retocar" del anuncio.
@RestController
@RequestMapping("/api/imagenes")
public class ImagenController {

    private final ServicioImagenes servicio;

    public ImagenController(ServicioImagenes servicio) {
        this.servicio = servicio;
    }

    @PostMapping("/borrador")
    public ImagenResponse borrador(@Valid @RequestBody PedirImagenRequest datos) {
        return ImagenResponse.desde(servicio.borrador(datos.descripcion()));
    }
}
