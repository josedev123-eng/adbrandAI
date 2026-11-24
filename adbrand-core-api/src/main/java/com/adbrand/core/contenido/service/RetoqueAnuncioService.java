package com.adbrand.core.contenido.service;

import com.adbrand.core.contenido.dto.VersionAnuncioResponse;
import com.adbrand.core.contenido.dto.VersionesAnuncioResponse;
import com.adbrand.core.contenido.entity.OpcionAnuncio;
import com.adbrand.core.contenido.entity.VersionAnuncio;
import com.adbrand.core.contenido.repository.OpcionAnuncioRepository;
import com.adbrand.core.contenido.repository.VersionAnuncioRepository;
import com.adbrand.core.imagen.dto.ImagenGenerada;
import com.adbrand.core.imagen.entity.ConsumoImagen;
import com.adbrand.core.imagen.service.ControlGastoImagenes;
import com.adbrand.core.imagen.service.ServicioImagenes;
import com.adbrand.core.shared.error.RecursoNoEncontradoException;
import com.adbrand.core.shared.error.SolicitudInvalidaException;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// "Retocar con prompts cortos": sobre la opción ELEGIDA, cada instrucción ("pon el fondo más claro")
// edita la imagen actual con la IA de pago (OpenRouter) y crea una versión nueva (v2, v3…).
// v1 es la imagen original. "Usar esta versión" permite volver a cualquier versión anterior (deshacer).
@Service
public class RetoqueAnuncioService {

    private final OpcionAnuncioRepository opciones;
    private final VersionAnuncioRepository versiones;
    private final ServicioImagenes imagenes;
    private final ControlGastoImagenes gasto;

    public RetoqueAnuncioService(OpcionAnuncioRepository opciones, VersionAnuncioRepository versiones,
                                 ServicioImagenes imagenes, ControlGastoImagenes gasto) {
        this.opciones = opciones;
        this.versiones = versiones;
        this.imagenes = imagenes;
        this.gasto = gasto;
    }

    @Transactional
    public VersionesAnuncioResponse retocar(Long usuarioId, Long opcionId, String instruccion) {
        OpcionAnuncio opcion = opcionElegida(usuarioId, opcionId);
        // Control de gasto: los retoques reales usan crédito; en modo de prueba no se limitan.
        if (!imagenes.esSimulado()) {
            gasto.verificarRetoqueDisponible(usuarioId);
        }
        List<VersionAnuncio> historial = historialConOriginal(opcion);

        ImagenGenerada retocada = imagenes.finalORetoque(descripcionDeRetoque(instruccion), opcion.imagen());
        gasto.registrar(usuarioId, ConsumoImagen.Tipo.RETOQUE, retocada);
        int numero = historial.get(historial.size() - 1).getNumero() + 1;
        versiones.save(VersionAnuncio.de(opcion, numero, instruccion, retocada));

        opcion.ponerImagen(retocada);
        opcion.setVersionActual(numero);
        opciones.save(opcion);
        return respuesta(opcion);
    }

    @Transactional(readOnly = true)
    public VersionesAnuncioResponse versiones(Long usuarioId, Long opcionId) {
        return respuesta(opcionDelUsuario(usuarioId, opcionId));
    }

    // Deshacer: vuelve a usar una versión anterior (o la siguiente, si ya había retrocedido).
    @Transactional
    public VersionesAnuncioResponse usarVersion(Long usuarioId, Long opcionId, int numero) {
        OpcionAnuncio opcion = opcionElegida(usuarioId, opcionId);
        VersionAnuncio version = versiones.findByOpcionIdAndNumero(opcion.getId(), numero)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "VERSION_NO_ENCONTRADA", "Esa versión de la imagen no existe."));
        opcion.ponerImagen(version.imagen());
        opcion.setVersionActual(numero);
        opciones.save(opcion);
        return respuesta(opcion);
    }

    // Lo que se le pide a la IA. La imagen actual va aparte (la IA la recibe para editarla).
    static String descripcionDeRetoque(String instruccion) {
        return ("Edit this social media advertising image. Change only this: %s. "
                + "Keep everything else (composition, style and main subject) the same. "
                + "Do not add text, letters or words unless the instruction asks for them.")
                .formatted(instruccion.trim());
    }

    private OpcionAnuncio opcionDelUsuario(Long usuarioId, Long opcionId) {
        return opciones.findByIdAndGeneracionUsuarioId(opcionId, usuarioId)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "OPCION_NO_ENCONTRADA", "La opción de anuncio no existe."));
    }

    // Solo se retoca la opción que la PYME eligió (así la IA de pago se usa en una sola imagen).
    private OpcionAnuncio opcionElegida(Long usuarioId, Long opcionId) {
        OpcionAnuncio opcion = opcionDelUsuario(usuarioId, opcionId);
        if (!opcion.isElegida()) {
            throw new SolicitudInvalidaException("OPCION_NO_ELEGIDA", "Primero elige esta opción para poder retocarla.");
        }
        return opcion;
    }

    // La primera vez guarda la imagen original como v1, para poder volver a ella.
    private List<VersionAnuncio> historialConOriginal(OpcionAnuncio opcion) {
        List<VersionAnuncio> historial = versiones.findByOpcionIdOrderByNumero(opcion.getId());
        if (historial.isEmpty()) {
            versiones.save(VersionAnuncio.de(opcion, 1, null, opcion.imagen()));
            historial = versiones.findByOpcionIdOrderByNumero(opcion.getId());
        }
        return historial;
    }

    private VersionesAnuncioResponse respuesta(OpcionAnuncio opcion) {
        int actual = opcion.getVersionActual() == null ? 1 : opcion.getVersionActual();
        List<VersionAnuncioResponse> lista = versiones.findByOpcionIdOrderByNumero(opcion.getId()).stream()
                .map(version -> VersionAnuncioResponse.desde(version, actual))
                .toList();
        // null en modo de prueba (no hay límite); si no, cuántos retoques de pago le quedan este mes a la empresa.
        Integer restantes = imagenes.esSimulado() ? null : gasto.retoquesRestantes(opcion.getGeneracion().getUsuarioId());
        return new VersionesAnuncioResponse(opcion.getId(), actual, opcion.imagen().dataUrl(), restantes, lista);
    }
}
