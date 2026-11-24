package com.adbrand.core.pago.controller;

import com.adbrand.core.pago.dto.ConfiguracionPagoResponse;
import com.adbrand.core.pago.dto.PagoMercadoPagoRequest;
import com.adbrand.core.pago.dto.PagoResponse;
import com.adbrand.core.pago.dto.PagoYapeMercadoPagoRequest;
import com.adbrand.core.pago.service.PagoMercadoPagoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

// HU 26: pago con tarjeta por Mercado Pago. La lógica está en PagoMercadoPagoService.
@RestController
@RequestMapping("/api/pagos")
public class MercadoPagoController {

    private final PagoMercadoPagoService servicio;

    public MercadoPagoController(PagoMercadoPagoService servicio) {
        this.servicio = servicio;
    }

    // Qué pasarela usar y, si es Mercado Pago, su llave pública (no es secreta).
    @GetMapping("/configuracion")
    public ConfiguracionPagoResponse configuracion() {
        return servicio.configuracion();
    }

    @PostMapping("/mercadopago")
    @ResponseStatus(HttpStatus.CREATED)
    public PagoResponse pagar(@RequestAttribute("usuarioId") Long usuarioId,
                              @Valid @RequestBody PagoMercadoPagoRequest datos) {
        return servicio.pagar(usuarioId, datos);
    }

    // HU 27: Yape con el celular y el código de aprobación (convertidos en token por la web).
    @PostMapping("/mercadopago/yape")
    @ResponseStatus(HttpStatus.CREATED)
    public PagoResponse pagarConYape(@RequestAttribute("usuarioId") Long usuarioId,
                                     @Valid @RequestBody PagoYapeMercadoPagoRequest datos) {
        return servicio.pagarConYape(usuarioId, datos);
    }
}
