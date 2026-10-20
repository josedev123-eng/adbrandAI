package com.adbrand.core.auth.controller;

import com.adbrand.core.auth.dto.LoginRequest;
import com.adbrand.core.auth.dto.LoginResponse;
import com.adbrand.core.auth.dto.RegistroRequest;
import com.adbrand.core.auth.dto.UsuarioResponse;
import com.adbrand.core.auth.service.LoginService;
import com.adbrand.core.auth.service.RegistroService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

// Controller: cuentas de los usuarios PYME. Solo recibe y responde; la lógica está en los services.
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final RegistroService registro;
    private final LoginService login;

    public AuthController(RegistroService registro, LoginService login) {
        this.registro = registro;
        this.login = login;
    }

    // HU 21: 201 con la cuenta creada, 400 si falta algo y 409 si el correo ya existe.
    @PostMapping("/registro")
    @ResponseStatus(HttpStatus.CREATED)
    public UsuarioResponse registrar(@Valid @RequestBody RegistroRequest datos) {
        return registro.registrar(datos);
    }

    // HU 22: 200 con el token, 401 si el correo o la contraseña están mal y 403 si la cuenta está desactivada.
    @PostMapping("/login")
    public LoginResponse iniciarSesion(@Valid @RequestBody LoginRequest datos) {
        return login.iniciarSesion(datos);
    }

    // HU 22: el usuario del token. La web lo pide al recargar la página. Esta ruta sí pide token.
    @GetMapping("/sesion")
    public UsuarioResponse sesion(@RequestAttribute("usuarioId") Long usuarioId) {
        return login.sesion(usuarioId);
    }
}
