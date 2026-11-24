package com.adbrand.core.contenido.dto;

import java.util.List;

// Las 4 opciones generadas para una oferta.
public record OpcionesAnuncioResponse(Long generacionId, String oferta, RedSocial redSocial,
                                      List<OpcionAnuncioResponse> opciones) {
}
