package com.adbrand.core.contenido.service;

import com.adbrand.core.contenido.dto.AnuncioGeneradoResponse;
import com.adbrand.core.contenido.dto.GenerarAnuncioRequest;
import com.adbrand.core.contenido.dto.PromptAnuncio;
import com.adbrand.core.contenido.entity.Contenido;
import com.adbrand.core.contenido.entity.EstadoContenido;
import com.adbrand.core.contenido.repository.ContenidoRepository;
import com.adbrand.core.ia.service.ServicioIa;
import com.adbrand.core.negocio.dto.PerfilNegocioResponse;
import com.adbrand.core.negocio.service.PerfilNegocioService;
import com.adbrand.core.revision.dto.ResultadoRevision;
import com.adbrand.core.revision.service.FiltroContenido;
import org.springframework.stereotype.Service;

// Junta las piezas de la HU 10 (perfil + plantilla + IA) y de la HU 13 (filtro + guardar con estado).
@Service
public class AnuncioService {

    private final PerfilNegocioService perfilService;
    private final PlantillaPromptAnuncio plantilla;
    private final ServicioIa ia;
    private final FiltroContenido filtro;
    private final ContenidoRepository contenidos;

    public AnuncioService(PerfilNegocioService perfilService, PlantillaPromptAnuncio plantilla, ServicioIa ia,
            FiltroContenido filtro, ContenidoRepository contenidos) {
        this.perfilService = perfilService;
        this.plantilla = plantilla;
        this.ia = ia;
        this.filtro = filtro;
        this.contenidos = contenidos;
    }

    public AnuncioGeneradoResponse generar(Long usuarioId, GenerarAnuncioRequest datos) {
        // HU 10, criterio 1: sin perfil registrado no se puede generar (responde 404 PERFIL_NO_ENCONTRADO).
        PerfilNegocioResponse perfil = perfilService.obtener(usuarioId);
        PromptAnuncio prompt = plantilla.construir(perfil, datos.oferta(), datos.redSocial());
        String texto = ia.generarTexto(prompt.sistema(), prompt.usuario());

        // HU 13, criterio 1: todo texto generado pasa por el filtro antes de poder usarse.
        ResultadoRevision revision = filtro.revisar(texto);

        Contenido contenido = new Contenido();
        contenido.setUsuarioId(usuarioId);
        contenido.setTipo(Contenido.TIPO_ANUNCIO);
        contenido.setRedSocial(datos.redSocial());
        contenido.setTono(perfil.tono());
        contenido.setOferta(datos.oferta());
        contenido.setTexto(texto);
        // HU 13, criterio 2: si incumple alguna regla queda DUDOSO y no se publica.
        contenido.setEstado(revision.aprobado() ? EstadoContenido.APROBADO : EstadoContenido.DUDOSO);
        contenido.setMotivoRevision(recortar(revision.motivoTexto()));

        return AnuncioGeneradoResponse.desde(contenidos.save(contenido), ia.esSimulado());
    }

    // La columna motivo_revision acepta hasta 500 caracteres.
    private static String recortar(String motivo) {
        return motivo == null || motivo.length() <= 500 ? motivo : motivo.substring(0, 497) + "...";
    }
}