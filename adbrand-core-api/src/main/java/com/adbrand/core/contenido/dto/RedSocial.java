package com.adbrand.core.contenido.dto;

// Redes para las que se puede generar un anuncio. Cada una tiene su propio largo y hashtags.
public enum RedSocial {
    INSTAGRAM(80, 4),
    FACEBOOK(120, 2);

    private final int maximoPalabras;
    private final int hashtags;

    RedSocial(int maximoPalabras, int hashtags) {
        this.maximoPalabras = maximoPalabras;
        this.hashtags = hashtags;
    }

    public int getMaximoPalabras() {
        return maximoPalabras;
    }

    public int getHashtags() {
        return hashtags;
    }
}