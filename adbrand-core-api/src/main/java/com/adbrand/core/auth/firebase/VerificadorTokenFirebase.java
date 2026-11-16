package com.adbrand.core.auth.firebase;

// Revisa que un token de Firebase (ID token) sea real, vigente y de nuestro proyecto.
// Es una interfaz para poder probar el inicio de sesión sin conectarse a Google.
public interface VerificadorTokenFirebase {

    // Lanza NoAutenticadoException si el token no sirve.
    UsuarioFirebase verificar(String idToken);
}
