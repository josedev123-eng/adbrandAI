package com.adbrand.core.contenido.dto;

import java.util.List;

// Las 4 opciones generadas para una oferta.
// "avisoFoto" (HU 67): por qué la foto del dueño se usó tal cual cuando pidió una escena (plan, tope o falla); null si no.
public record OpcionesAnuncioResponse(Long generacionId, String oferta, RedSocial redSocial,
                                      List<OpcionAnuncioResponse> opciones, String avisoFoto) {

    public OpcionesAnuncioResponse(Long generacionId, String oferta, RedSocial redSocial,
                                   List<OpcionAnuncioResponse> opciones) {
        this(generacionId, oferta, redSocial, opciones, null);
    }
}
