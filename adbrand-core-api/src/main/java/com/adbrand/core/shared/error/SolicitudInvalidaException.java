package com.adbrand.core.shared.error;

// La petición tiene el formato correcto pero no se puede aceptar, por ejemplo un código vencido. Responde 400.
public class SolicitudInvalidaException extends RuntimeException {

    private final String codigo;

    public SolicitudInvalidaException(String codigo, String mensaje) {
        super(mensaje);
        this.codigo = codigo;
    }

    public String getCodigo() {
        return codigo;
    }
}
