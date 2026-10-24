package com.adbrand.core.pago.controller;

import com.adbrand.core.pago.dto.ComprobanteResponse;
import com.adbrand.core.pago.service.ComprobanteService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// HU 28: comprobante del pago. Vive en su propio controller porque es una consulta aparte
// del cobro; comparte la ruta /api/pagos con PagoController.
@RestController
@RequestMapping("/api/pagos")
public class ComprobanteController {

    // Se usa solo si la petición llega sin el filtro de sesión (pruebas o arranque local).
    private static final Long USUARIO_DE_PRUEBA = 1L;

    private final ComprobanteService servicio;

    public ComprobanteController(ComprobanteService servicio) {
        this.servicio = servicio;
    }

    @GetMapping("/{id}/comprobante")
    public ComprobanteResponse comprobante(
            @RequestAttribute(value = "usuarioId", required = false) Long usuarioId,
            @PathVariable Long id) {
        return servicio.obtener(usuarioId == null ? USUARIO_DE_PRUEBA : usuarioId, id);
    }
}
