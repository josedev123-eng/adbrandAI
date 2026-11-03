package com.adbrand.core.shared.error;

// Un dato que debe ser único ya existe, por ejemplo un correo registrado. Responde 409.
public class ConflictoException extends RuntimeException {

    private final String codigo;

    public ConflictoException(String codigo, String mensaje) {
        super(mensaje);
        this.codigo = codigo;
    }

    public String getCodigo() {
        return codigo;
    }
}
