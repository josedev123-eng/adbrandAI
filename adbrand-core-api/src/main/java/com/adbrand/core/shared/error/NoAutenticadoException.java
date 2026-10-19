package com.adbrand.core.shared.error;

// No se sabe quién es el usuario: credenciales incorrectas o sesión inválida. Responde 401.
public class NoAutenticadoException extends RuntimeException {

    private final String codigo;

    public NoAutenticadoException(String codigo, String mensaje) {
        super(mensaje);
        this.codigo = codigo;
    }

    public String getCodigo() {
        return codigo;
    }
}
