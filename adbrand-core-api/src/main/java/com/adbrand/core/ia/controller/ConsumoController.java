package com.adbrand.core.ia.controller;

import com.adbrand.core.ia.dto.ConsumoMesResponse;
import com.adbrand.core.ia.service.LimiteConsumoIa;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// Consumo de IA del mes de la empresa que inició sesión (tarjeta "Tu consumo de IA" en Inicio).
@RestController
@RequestMapping("/api/consumo")
public class ConsumoController {

    private final LimiteConsumoIa limite;

    public ConsumoController(LimiteConsumoIa limite) {
        this.limite = limite;
    }

    @GetMapping("/mes")
    public ConsumoMesResponse delMes(@RequestAttribute("usuarioId") Long usuarioId) {
        return limite.consumoDelMes(usuarioId);
    }
}
