package com.adbrand.core.shared.error;

import java.util.Map;

// Formato único de error del proyecto: {"codigo": "...", "mensaje": "...", "campos": {...}}
public record ErrorResponse(String codigo, String mensaje, Map<String, String> campos) {
}