package com.adbrand.core.pago.controller;

import com.adbrand.core.pago.dto.OperacionYapeResponse;
import com.adbrand.core.pago.dto.PagoResponse;
import com.adbrand.core.pago.dto.PagoTarjetaRequest;
import com.adbrand.core.pago.dto.RegistrarYapeRequest;
import com.adbrand.core.pago.service.OperacionYapeService;
import com.adbrand.core.pago.service.PagoService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

// HU 26 y 27: pagos con tarjeta y con Yape. Igual que los demás controllers, no tiene lógica:
// todo va en PagoService y OperacionYapeService.
@RestController
@RequestMapping("/api/pagos")
public class PagoController {

    // Se usa solo si la petición llega sin el filtro de sesión (pruebas o arranque local).
    private static final Long USUARIO_DE_PRUEBA = 1L;

    private final PagoService servicio;
    private final OperacionYapeService yape;

    public PagoController(PagoService servicio, OperacionYapeService yape) {
        this.servicio = servicio;
        this.yape = yape;
    }

    @PostMapping("/tarjeta")
    @ResponseStatus(HttpStatus.CREATED)
    public PagoResponse pagarConTarjeta(
            @RequestAttribute(value = "usuarioId", required = false) Long usuarioId,
            @Valid @RequestBody PagoTarjetaRequest datos) {
        return servicio.pagarConTarjeta(usuario(usuarioId), datos);
    }

    // HU 27: genera el QR de Yape. El pago todavía no existe: queda una operación PENDIENTE.
    @PostMapping("/yape")
    @ResponseStatus(HttpStatus.CREATED)
    public OperacionYapeResponse registrarYape(
            @RequestAttribute(value = "usuarioId", required = false) Long usuarioId,
            @Valid @RequestBody RegistrarYapeRequest datos) {
        return yape.registrar(usuario(usuarioId), datos);
    }

    // HU 27: el usuario ya pagó desde su app y confirma la operación. Aquí se crea el pago real.
    @PostMapping("/yape/{id}/confirmar")
    public PagoResponse confirmarYape(
            @RequestAttribute(value = "usuarioId", required = false) Long usuarioId,
            @PathVariable Long id) {
        return yape.confirmar(usuario(usuarioId), id);
    }

    // HU 27: operaciones que quedaron pendientes, para retomarlas sin generar otro QR.
    @GetMapping("/yape/pendientes")
    public List<OperacionYapeResponse> yapePendientes(
            @RequestAttribute(value = "usuarioId", required = false) Long usuarioId) {
        return yape.pendientes(usuario(usuarioId));
    }

    private Long usuario(Long usuarioId) {
        return usuarioId == null ? USUARIO_DE_PRUEBA : usuarioId;
    }
}
