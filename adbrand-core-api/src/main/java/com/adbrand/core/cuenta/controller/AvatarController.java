package com.adbrand.core.cuenta.controller;

import com.adbrand.core.auth.dto.UsuarioResponse;
import com.adbrand.core.cuenta.service.AvatarService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// Avatar de Mi Cuenta: PUT { "imagen": "data:image/png;base64,..." } o { "usarLogoDelKit": true }; DELETE lo quita.
@RestController
@RequestMapping("/api/cuenta/avatar")
public class AvatarController {

    public record CambiarAvatarRequest(String imagen, Boolean usarLogoDelKit) {
    }

    private final AvatarService avatares;

    public AvatarController(AvatarService avatares) {
        this.avatares = avatares;
    }

    @PutMapping
    public UsuarioResponse cambiar(@RequestAttribute("usuarioId") Long usuarioId, @RequestBody CambiarAvatarRequest datos) {
        return avatares.cambiar(usuarioId, datos.imagen(), Boolean.TRUE.equals(datos.usarLogoDelKit()));
    }

    @DeleteMapping
    public UsuarioResponse quitar(@RequestAttribute("usuarioId") Long usuarioId) {
        return avatares.quitar(usuarioId);
    }
}
