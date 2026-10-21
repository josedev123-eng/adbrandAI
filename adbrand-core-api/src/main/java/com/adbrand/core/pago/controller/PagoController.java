package com.adbrand.core.pago.controller;

import com.adbrand.core.pago.dto.PagoResponse;
import com.adbrand.core.pago.dto.PagoTarjetaRequest;
import com.adbrand.core.pago.service.PagoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

// HU 26: pago con tarjeta. Igual que los demás controllers, no tiene lógica: todo va en PagoService.
@RestController
@RequestMapping("/api/pagos")
public class PagoController {

    // Se usa solo si la petición llega sin el filtro de sesión (pruebas o arranque local).
    private static final Long USUARIO_DE_PRUEBA = 1L;

    private final PagoService servicio;

    public PagoController(PagoService servicio) {
        this.servicio = servicio;
    }

    @PostMapping("/tarjeta")
    @ResponseStatus(HttpStatus.CREATED)
    public PagoResponse pagarConTarjeta(
            @RequestAttribute(value = "usuarioId", required = false) Long usuarioId,
            @Valid @RequestBody PagoTarjetaRequest datos) {
        return servicio.pagarConTarjeta(usuarioId == null ? USUARIO_DE_PRUEBA : usuarioId, datos);
    }
}
