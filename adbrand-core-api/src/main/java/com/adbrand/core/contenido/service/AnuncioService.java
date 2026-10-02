package com.adbrand.core.contenido.service;

import com.adbrand.core.contenido.dto.AnuncioGeneradoResponse;
import com.adbrand.core.contenido.dto.GenerarAnuncioRequest;
import com.adbrand.core.contenido.dto.PromptAnuncio;
import com.adbrand.core.ia.service.ServicioIa;
import com.adbrand.core.negocio.dto.PerfilNegocioResponse;
import com.adbrand.core.negocio.service.PerfilNegocioService;
import org.springframework.stereotype.Service;

// Junta las piezas de la HU 10: perfil del negocio + plantilla del prompt + IA.
@Service
public class AnuncioService {

    private final PerfilNegocioService perfilService;
    private final PlantillaPromptAnuncio plantilla;
    private final ServicioIa ia;

    public AnuncioService(PerfilNegocioService perfilService, PlantillaPromptAnuncio plantilla, ServicioIa ia) {
        this.perfilService = perfilService;
        this.plantilla = plantilla;
        this.ia = ia;
    }

    public AnuncioGeneradoResponse generar(Long usuarioId, GenerarAnuncioRequest datos) {
        // Criterio 1: sin perfil registrado no se puede generar (responde 404 PERFIL_NO_ENCONTRADO).
        PerfilNegocioResponse perfil = perfilService.obtener(usuarioId);
        PromptAnuncio prompt = plantilla.construir(perfil, datos.oferta(), datos.redSocial());
        String texto = ia.generarTexto(prompt.sistema(), prompt.usuario());
        return new AnuncioGeneradoResponse(texto, datos.redSocial(), perfil.tono(), ia.esSimulado());
    }
}