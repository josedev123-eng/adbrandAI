package com.adbrand.core.auth.dto;

// Respuesta del inicio de sesión: el token JWT, el token de renovación y los datos del usuario.
// El token de renovación se agregó en la HU 43 para que la app móvil pueda renovar sin volver a pedir la contraseña.
public record LoginResponse(String token, String tokenRenovacion, UsuarioResponse usuario) {

    // Compatibilidad con las pruebas y la web que solo esperan el token de acceso.
    public LoginResponse(String token, UsuarioResponse usuario) {
        this(token, null, usuario);
    }
}
