package com.adbrand.core.cuenta.controller;

import com.adbrand.core.cuenta.dto.EmpresaRequest;
import com.adbrand.core.cuenta.dto.EmpresaResponse;
import com.adbrand.core.cuenta.service.CuentaService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// Controller: solo recibe la petición y llama al service. Sin lógica.
// HU 23: no hay id en la URL; la empresa es la del usuario que inició sesión ("usuarioId" de FiltroJwt).
@RestController
@RequestMapping("/api/cuenta/empresa")
public class CuentaController {

    private final CuentaService servicio;

    public CuentaController(CuentaService servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    public EmpresaResponse obtener(@RequestAttribute("usuarioId") Long usuarioId) {
        return servicio.obtener(usuarioId);
    }

    @PutMapping
    public EmpresaResponse guardar(@RequestAttribute("usuarioId") Long usuarioId,
                                   @Valid @RequestBody EmpresaRequest datos) {
        return servicio.guardar(usuarioId, datos);
    }
}
