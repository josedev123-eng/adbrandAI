package com.adbrand.core.auth.controller;

import com.adbrand.core.auth.dto.RegistroRequest;
import com.adbrand.core.auth.dto.UsuarioResponse;
import com.adbrand.core.auth.service.RegistroService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

// Controller: cuentas de los usuarios PYME. Solo recibe y responde; la lógica está en los services.
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final RegistroService registro;

    public AuthController(RegistroService registro) {
        this.registro = registro;
    }

    // HU 21: 201 con la cuenta creada, 400 si falta algo y 409 si el correo ya existe.
    @PostMapping("/registro")
    @ResponseStatus(HttpStatus.CREATED)
    public UsuarioResponse registrar(@Valid @RequestBody RegistroRequest datos) {
        return registro.registrar(datos);
    }
}
