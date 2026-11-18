package com.adbrand.core.contenido.dto;

import java.util.List;

// El historial de versiones de la opción elegida y cuál se está usando (con su imagen actual).
// retoquesRestantes: cuántos retoques de pago le quedan este mes a la empresa (null en modo de prueba).
public record VersionesAnuncioResponse(Long opcionId, int versionActual, String imagenActual,
                                       Integer retoquesRestantes, List<VersionAnuncioResponse> versiones) {
}
