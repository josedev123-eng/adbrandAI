package com.adbrand.core.auth.controller;

import com.adbrand.core.auth.dto.LoginResponse;
import com.adbrand.core.auth.dto.MensajeResponse;
import com.adbrand.core.auth.dto.SolicitarCodigoRequest;
import com.adbrand.core.auth.dto.VerificarCorreoRequest;
import com.adbrand.core.auth.service.VerificacionCorreoService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// Controller: verificar el correo después de registrarse en la web. Rutas públicas (/api/auth/**).
@RestController
@RequestMapping("/api/auth/verificar-correo")
public class VerificacionCorreoController {

    private final VerificacionCorreoService verificacion;

    public VerificacionCorreoController(VerificacionCorreoService verificacion) {
        this.verificacion = verificacion;
    }

    // 200 con la sesión (token y usuario) si el código es correcto; 400 CODIGO_INVALIDO si no.
    @PostMapping
    public LoginResponse verificar(@Valid @RequestBody VerificarCorreoRequest datos) {
        return verificacion.verificar(datos.correo(), datos.codigo());
    }

    // Siempre 200 con el mismo mensaje, exista o no la cuenta.
    @PostMapping("/reenviar")
    public MensajeResponse reenviar(@Valid @RequestBody SolicitarCodigoRequest datos) {
        verificacion.reenviar(datos.correo());
        return new MensajeResponse(VerificacionCorreoService.MENSAJE_REENVIO);
    }
}
