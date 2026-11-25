package com.adbrand.core.contenido.dto;

// HU 64: algo que conviene corregir en el anuncio para cumplir las reglas de publicidad (INDECOPI).
// codigo: FALTA_VIGENCIA, FALTA_STOCK, AFIRMACION_SIN_RESPALDO o PRECIO_SIN_IMPUESTOS.
public record ObservacionLegal(String codigo, String titulo, String detalle) {
}
