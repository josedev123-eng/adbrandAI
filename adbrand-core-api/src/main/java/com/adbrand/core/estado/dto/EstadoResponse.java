package com.adbrand.core.estado.dto;

// HU 18: lo que responde GET /api/estado. Si Spring contesta, el backend está ACTIVO.
public record EstadoResponse(String backend, EstadoIa ia) {
}
