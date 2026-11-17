package com.adbrand.core.shared.error;

// Demasiados intentos seguidos, por ejemplo una cuenta bloqueada por contraseñas equivocadas. Responde 429.
public class DemasiadosIntentosException extends RuntimeException {

    private final String codigo;

    public DemasiadosIntentosException(String codigo, String mensaje) {
        super(mensaje);
        this.codigo = codigo;
    }

    public String getCodigo() {
        return codigo;
    }
}
