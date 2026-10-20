package com.adbrand.core.auth.controller;

import com.adbrand.core.auth.dto.CambiarContrasenaRequest;
import com.adbrand.core.auth.dto.LoginRequest;
import com.adbrand.core.auth.dto.LoginResponse;
import com.adbrand.core.auth.dto.MensajeResponse;
import com.adbrand.core.auth.dto.RegistroRequest;
import com.adbrand.core.auth.dto.SolicitarCodigoRequest;
import com.adbrand.core.auth.dto.UsuarioResponse;
import com.adbrand.core.auth.service.LoginService;
import com.adbrand.core.auth.service.RecuperacionService;
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
    private final RecuperacionService recuperacion;

    public AuthController(RegistroService registro, LoginService login, RecuperacionService recuperacion) {
        this.registro = registro;
        this.login = login;
        this.recuperacion = recuperacion;
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

    // HU 24: siempre 200 con el mismo mensaje, exista o no el correo.
    @PostMapping("/recuperar/solicitar")
    public MensajeResponse solicitarCodigo(@Valid @RequestBody SolicitarCodigoRequest datos) {
        recuperacion.solicitarCodigo(datos.correo());
        return new MensajeResponse(RecuperacionService.MENSAJE_SOLICITUD);
    }

    // HU 24: 200 si el código es correcto, no venció y no se usó; si no, 400 CODIGO_INVALIDO.
    @PostMapping("/recuperar/cambiar")
    public MensajeResponse cambiarContrasena(@Valid @RequestBody CambiarContrasenaRequest datos) {
        recuperacion.cambiarContrasena(datos);
        return new MensajeResponse(RecuperacionService.MENSAJE_CAMBIADA);
    }
}
