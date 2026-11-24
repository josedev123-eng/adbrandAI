package com.adbrand.core.imagen.service;

import com.adbrand.core.imagen.dto.ImagenGenerada;

// Contrato común de todas las IA de imágenes (patrón Strategy): cada una sabe hacer una imagen
// a partir de una descripción y, si la IA lo permite, partiendo de una imagen anterior (para retocarla).
// ServicioImagenes decide cuál usar; el resto del proyecto no sabe qué IA hay detrás.
public interface GeneradorImagen {

    // Nombre corto con el que se guarda quién hizo la imagen: "simulado", "cloudflare", "openrouter".
    String nombre();

    // imagenBase puede ser null: entonces se crea desde cero.
    ImagenGenerada generar(String descripcion, ImagenGenerada imagenBase);
}
