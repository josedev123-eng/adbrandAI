package com.adbrand.core.auth.firebase;

// Lo que Spring sabe de un usuario después de verificar su token de Firebase.
public record UsuarioFirebase(String uid, String correo, boolean correoVerificado) {
}
