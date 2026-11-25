package com.adbrand.core.contenido.service;

import com.adbrand.core.auth.repository.UsuarioRepository;
import com.adbrand.core.contenido.dto.OpcionAnuncioResponse;
import com.adbrand.core.contenido.dto.OpcionElegidaResponse;
import com.adbrand.core.contenido.dto.OpcionesAnuncioResponse;
import com.adbrand.core.contenido.repository.ContenidoRepository;
import com.adbrand.core.contenido.service.DisenoAnuncio.Plantilla;
import java.util.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

// Ajuste 14: a cada una de las 4 opciones se le arma su diseño de anuncio (una plantilla distinta por opción)
// con la foto de la IA, la oferta, el precio, el botón y la marca del kit. Va como "diseno" (JPG de 720 px) para
// que la web lo muestre en las tarjetas; la descarga final (ImagenFinalService) lo dibuja en tamaño completo.
@Service
public class DisenadorOpciones {

    static final int LADO_VISTA = 720;
    private static final Logger log = LoggerFactory.getLogger(DisenadorOpciones.class);

    private final MarcaDelNegocio marcas;
    private final UsuarioRepository usuarios;
    private final ContenidoRepository contenidos;

    public DisenadorOpciones(MarcaDelNegocio marcas, UsuarioRepository usuarios, ContenidoRepository contenidos) {
        this.marcas = marcas;
        this.usuarios = usuarios;
        this.contenidos = contenidos;
    }

    public OpcionesAnuncioResponse conDisenos(Long usuarioId, OpcionesAnuncioResponse respuesta) {
        MarcaDelNegocio.Marca marca = marcas.de(usuarioId);
        boolean conWhatsapp = tieneWhatsapp(usuarioId);
        return new OpcionesAnuncioResponse(respuesta.generacionId(), respuesta.oferta(), respuesta.redSocial(),
                respuesta.opciones().stream()
                        .map(o -> conDiseno(o, respuesta.oferta(), marca, conWhatsapp))
                        .toList());
    }

    public OpcionElegidaResponse conDiseno(Long usuarioId, OpcionElegidaResponse elegida) {
        String oferta = elegida.anuncio() == null || elegida.anuncio().id() == null ? null
                : contenidos.findById(elegida.anuncio().id()).map(c -> c.getOferta()).orElse(null);
        return new OpcionElegidaResponse(elegida.anuncio(),
                conDiseno(elegida.opcion(), oferta, marcas.de(usuarioId), tieneWhatsapp(usuarioId)));
    }

    private OpcionAnuncioResponse conDiseno(OpcionAnuncioResponse o, String oferta, MarcaDelNegocio.Marca marca,
                                            boolean conWhatsapp) {
        try {
            var imagen = DisenoAnuncio.dibujar(MarcaDelNegocio.leerDataUrl(o.imagen()),
                    DisenoAnuncio.textosDe(oferta, o.texto(), conWhatsapp), marca, Plantilla.deOpcion(o.numero()),
                    LADO_VISTA, LADO_VISTA);
            String diseno = "data:image/jpeg;base64," + Base64.getEncoder().encodeToString(ImagenFinalService.jpg(imagen));
            return o.conDiseno(diseno);
        } catch (RuntimeException ex) {
            // Si algo falla al dibujar, la opción igual se muestra con su foto.
            log.warn("No se pudo armar el diseño de la opción {}: {}", o.id(), ex.getMessage());
            return o;
        }
    }

    private boolean tieneWhatsapp(Long usuarioId) {
        return usuarios.findById(usuarioId)
                .map(u -> PublicacionService.celularWhatsapp(u.getTelefono()) != null)
                .orElse(false);
    }
}
