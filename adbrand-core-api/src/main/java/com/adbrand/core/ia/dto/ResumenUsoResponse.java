package com.adbrand.core.ia.dto;

// "Tu uso este mes" (pantalla Planes): el consumo de IA del plan y lo que el dueño entiende sin hablar de tokens:
// cuántos anuncios creó y cuántos retoques de imagen usó de los que trae su plan.
public record ResumenUsoResponse(ConsumoMesResponse consumo, long anunciosCreados, long retoquesUsados, int limiteRetoques) {
}
