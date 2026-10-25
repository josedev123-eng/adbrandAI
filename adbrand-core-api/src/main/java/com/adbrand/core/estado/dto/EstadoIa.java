package com.adbrand.core.estado.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

// HU 18: si el servidor de IA responde. estado es ACTIVO o CAIDO; detalle explica por qué cayó.
public record EstadoIa(String estado, @JsonProperty("tiempo_ms") Long tiempoMs, String detalle) {
}
