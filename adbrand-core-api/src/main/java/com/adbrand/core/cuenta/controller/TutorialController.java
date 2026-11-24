package com.adbrand.core.cuenta.controller;

import com.adbrand.core.cuenta.service.CuentaService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// Tutorial guiado de la web: al terminarlo o saltarlo, la web avisa para no volver a mostrarlo solo.
// La cuenta es la del token ("usuarioId" de FiltroJwt), nunca un id de la URL.
@RestController
@RequestMapping("/api/cuenta/tutorial-visto")
public class TutorialController {

    private final CuentaService servicio;

    public TutorialController(CuentaService servicio) {
        this.servicio = servicio;
    }

    @PostMapping
    public ResponseEntity<Void> marcarVisto(@RequestAttribute("usuarioId") Long usuarioId) {
        servicio.marcarTutorialVisto(usuarioId);
        return ResponseEntity.noContent().build();
    }
}
