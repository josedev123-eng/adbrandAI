package com.adbrand.core.contacto.controller;

import com.adbrand.core.auth.service.ServicioJwt;
import com.adbrand.core.contacto.dto.ContactoDtos.EnviarMensajeRequest;
import com.adbrand.core.contacto.dto.ContactoDtos.MensajeEnviadoResponse;
import com.adbrand.core.contacto.service.ContactoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

// Formulario de Contacto del pie de página. No pide sesión (se puede escribir desde la página de presentación);
// si llega el token de una sesión válida, el mensaje queda ligado a esa empresa.
@RestController
@RequestMapping("/api/contacto")
public class ContactoController {

    private final ContactoService contacto;
    private final ServicioJwt jwt;

    public ContactoController(ContactoService contacto, ServicioJwt jwt) {
        this.contacto = contacto;
        this.jwt = jwt;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MensajeEnviadoResponse enviar(@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String cabecera,
                                         @Valid @RequestBody EnviarMensajeRequest datos) {
        return contacto.enviar(usuarioDe(cabecera), datos);
    }

    private Long usuarioDe(String cabecera) {
        if (cabecera == null || !cabecera.startsWith("Bearer ")) {
            return null;
        }
        try {
            return jwt.leerUsuarioId(cabecera.substring("Bearer ".length()).trim());
        } catch (ServicioJwt.TokenInvalidoException ex) {
            return null;
        }
    }
}
