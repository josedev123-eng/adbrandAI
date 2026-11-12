package com.adbrand.core.imagen.service;

import com.adbrand.core.config.ImagenProperties;
import com.adbrand.core.imagen.dto.ImagenGenerada;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

// Único punto de entrada para pedir imágenes. Decide qué IA usa cada paso:
// - borrador: la IA gratis (Cloudflare), para las 3-4 opciones de un anuncio.
// - final / retoque: la IA pagada (OpenRouter), solo sobre la opción que eligió el usuario.
// Con ia.imagen.mock=true (por defecto) todo sale del generador simulado y no se gasta nada.
// Si la IA elegida falla, se usa la siguiente: OpenRouter → Cloudflare → simulado, así nunca se queda sin imagen.
@Service
public class ServicioImagenes {

    public static final String CLOUDFLARE = "cloudflare";
    public static final String OPENROUTER = "openrouter";

    private static final Logger log = LoggerFactory.getLogger(ServicioImagenes.class);

    private final ImagenProperties propiedades;
    private final List<GeneradorImagen> generadores;
    private final GeneradorImagenSimulado simulado;

    public ServicioImagenes(ImagenProperties propiedades, List<GeneradorImagen> generadores,
                            GeneradorImagenSimulado simulado) {
        this.propiedades = propiedades;
        this.generadores = generadores;
        this.simulado = simulado;
    }

    public boolean esSimulado() {
        return propiedades.mock();
    }

    // Imagen rápida y gratis para una de las opciones del anuncio.
    public ImagenGenerada borrador(String descripcion) {
        return generarConRespaldo(descripcion, null, List.of(CLOUDFLARE));
    }

    // Imagen de buena calidad. Si viene imagenBase, se retoca esa imagen en vez de empezar de cero.
    public ImagenGenerada finalORetoque(String descripcion, ImagenGenerada imagenBase) {
        return generarConRespaldo(descripcion, imagenBase, List.of(OPENROUTER, CLOUDFLARE));
    }

    private ImagenGenerada generarConRespaldo(String descripcion, ImagenGenerada imagenBase, List<String> orden) {
        if (propiedades.mock()) {
            return simulado.generar(descripcion, imagenBase);
        }
        for (String nombre : orden) {
            Optional<GeneradorImagen> generador = buscar(nombre);
            if (generador.isEmpty()) {
                continue;
            }
            try {
                return generador.get().generar(descripcion, imagenBase);
            } catch (RuntimeException ex) {
                log.warn("La IA de imágenes {} falló, se prueba con la siguiente: {}", nombre, ex.getMessage());
            }
        }
        log.warn("Ninguna IA de imágenes respondió; se usa la imagen de prueba.");
        return simulado.generar(descripcion, imagenBase);
    }

    private Optional<GeneradorImagen> buscar(String nombre) {
        return generadores.stream().filter(generador -> generador.nombre().equals(nombre)).findFirst();
    }
}
