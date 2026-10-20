package com.adbrand.core.auth.dto;

// La misma regla de contraseña que los administradores (HU 1): mínimo 8 caracteres,
// con una mayúscula, una minúscula y un número, y sin espacios.
// BCrypt solo usa los primeros 72 bytes, por eso el máximo.
public final class ReglasContrasena {

    public static final int MINIMO = 8;
    public static final int MAXIMO = 72;
    public static final String PATRON = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)\\S+$";
    public static final String MENSAJE_LARGO = "Debe tener entre 8 y 72 caracteres.";
    public static final String MENSAJE_PATRON = "Debe tener una mayúscula, una minúscula y un número, sin espacios.";

    private ReglasContrasena() {
    }
}
