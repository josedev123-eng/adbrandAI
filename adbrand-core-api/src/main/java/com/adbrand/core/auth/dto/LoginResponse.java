package com.adbrand.core.auth.dto;

// Respuesta del inicio de sesión: el token JWT y los datos del usuario (HU 22).
public record LoginResponse(String token, UsuarioResponse usuario) {
}
