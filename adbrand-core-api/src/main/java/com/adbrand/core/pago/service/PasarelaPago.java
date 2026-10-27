package com.adbrand.core.pago.service;

import com.adbrand.core.pago.dto.PagoTarjetaRequest;

// HU 26: contrato de la pasarela de pago. Hoy hay una simulada; mañana puede ser la real
// sin cambiar el resto del código.
public interface PasarelaPago {

    ResultadoCobro cobrar(PagoTarjetaRequest datos);
}
