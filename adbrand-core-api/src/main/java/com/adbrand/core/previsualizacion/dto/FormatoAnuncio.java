package com.adbrand.core.previsualizacion.dto;

import com.adbrand.core.shared.error.SolicitudInvalidaException;
import java.util.Locale;

// HU 46: formatos en los que se puede previsualizar un anuncio, con su tamaño (px) y cuánto texto entra.
// Las medidas son las recomendadas por Instagram/Facebook; el texto máximo es el que se lee bien sobre la imagen.
public enum FormatoAnuncio {
    CUADRADO("Cuadrado", "Publicación", 1080, 1080, 125),
    VERTICAL("Vertical", "Historia", 1080, 1920, 90),
    HORIZONTAL("Horizontal", "Horizontal", 1200, 628, 100);

    public static final String PUNTOS_SUSPENSIVOS = "…";

    private final String nombre;
    private final String uso;
    private final int ancho;
    private final int alto;
    private final int maxCaracteres;

    FormatoAnuncio(String nombre, String uso, int ancho, int alto, int maxCaracteres) {
        this.nombre = nombre;
        this.uso = uso;
        this.ancho = ancho;
        this.alto = alto;
        this.maxCaracteres = maxCaracteres;
    }

    public String nombre() { return nombre; }
    public String uso() { return uso; }
    public int ancho() { return ancho; }
    public int alto() { return alto; }
    public int maxCaracteres() { return maxCaracteres; }

    // "vertical", "VERTICAL" o vacío (cuadrado por defecto). Otro valor → 400 FORMATO_INVALIDO.
    public static FormatoAnuncio desde(String valor) {
        if (valor == null || valor.isBlank()) {
            return CUADRADO;
        }
        try {
            return valueOf(valor.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new SolicitudInvalidaException("FORMATO_INVALIDO",
                    "Elige un formato válido: cuadrado, vertical u horizontal.");
        }
    }

    // Criterio 2: si el texto no entra, se corta en la última palabra completa y se agrega "…".
    public String ajustar(String texto) {
        if (texto == null) {
            return "";
        }
        String limpio = texto.strip();
        if (limpio.length() <= maxCaracteres) {
            return limpio;
        }
        String corte = limpio.substring(0, maxCaracteres - 1);
        int espacio = corte.lastIndexOf(' ');
        if (espacio > maxCaracteres / 2) {
            corte = corte.substring(0, espacio);
        }
        return corte.stripTrailing().replaceAll("[\\s,.;:!¡?¿-]+$", "") + PUNTOS_SUSPENSIVOS;
    }
}
