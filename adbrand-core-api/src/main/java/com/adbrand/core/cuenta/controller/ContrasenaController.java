package com.adbrand.core.cuenta.controller;

import com.adbrand.core.auth.dto.CambiarMiContrasenaRequest;
import com.adbrand.core.auth.dto.MensajeResponse;
import com.adbrand.core.auth.service.CambioContrasenaService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// Mi cuenta: cambiar la contraseña de la empresa conectada. Pide token; la cuenta es la del token ("usuarioId").
@RestController
@RequestMapping("/api/cuenta/contrasena")
public class ContrasenaController {

    private final CambioContrasenaService servicio;

    public ContrasenaController(CambioContrasenaService servicio) {
        this.servicio = servicio;
    }

    // 200 si se cambió; 400 CONTRASENA_ACTUAL_INCORRECTA, CONTRASENA_IGUAL o DATOS_INVALIDOS (contraseña débil).
    @PostMapping
    public MensajeResponse cambiar(@RequestAttribute("usuarioId") Long usuarioId,
                                   @Valid @RequestBody CambiarMiContrasenaRequest datos) {
        servicio.cambiar(usuarioId, datos.actual(), datos.nueva());
        return new MensajeResponse("Tu contraseña se cambió.");
    }
}
