package com.adbrand.core.contenido.dto;

import java.util.List;

// El historial de versiones de la opción elegida y cuál se está usando (con su imagen actual).
public record VersionesAnuncioResponse(Long opcionId, int versionActual, String imagenActual,
                                       List<VersionAnuncioResponse> versiones) {
}
