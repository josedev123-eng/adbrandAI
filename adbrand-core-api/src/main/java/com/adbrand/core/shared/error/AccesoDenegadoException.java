package com.adbrand.core.shared.error;

// Se sabe quién es el usuario, pero no puede hacer esto, por ejemplo una cuenta desactivada. Responde 403.
public class AccesoDenegadoException extends RuntimeException {

    private final String codigo;

    public AccesoDenegadoException(String codigo, String mensaje) {
        super(mensaje);
        this.codigo = codigo;
    }

    public String getCodigo() {
        return codigo;
    }
}
