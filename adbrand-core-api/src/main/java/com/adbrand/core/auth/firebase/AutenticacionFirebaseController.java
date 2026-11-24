package com.adbrand.core.auth.firebase;

import com.adbrand.core.auth.dto.LoginResponse;
import com.adbrand.core.auth.dto.UsuarioResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

// App móvil: entrada con Firebase Authentication. Sin lógica: todo está en AutenticacionFirebaseService.
// /api/auth/** es pública (FiltroJwt); vincular va en /api/cuenta porque necesita la sesión de siempre.
@RestController
public class AutenticacionFirebaseController {

    private final AutenticacionFirebaseService servicio;

    public AutenticacionFirebaseController(AutenticacionFirebaseService servicio) {
        this.servicio = servicio;
    }

    @PostMapping("/api/auth/firebase")
    public LoginResponse iniciarSesion(@Valid @RequestBody FirebaseDtos.TokenFirebaseRequest datos) {
        return servicio.iniciarSesion(datos.idToken());
    }

    @PostMapping("/api/auth/firebase/registro")
    @ResponseStatus(HttpStatus.CREATED)
    public UsuarioResponse registrar(@Valid @RequestBody FirebaseDtos.RegistroFirebaseRequest datos) {
        return servicio.registrar(datos);
    }

    @PostMapping("/api/cuenta/firebase/vincular")
    public UsuarioResponse vincular(@RequestAttribute("usuarioId") Long usuarioId,
                                    @Valid @RequestBody FirebaseDtos.TokenFirebaseRequest datos) {
        return servicio.vincular(usuarioId, datos.idToken());
    }
}
