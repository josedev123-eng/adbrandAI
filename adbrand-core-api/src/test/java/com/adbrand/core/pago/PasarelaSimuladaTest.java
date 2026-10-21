package com.adbrand.core.pago;

import static org.assertj.core.api.Assertions.assertThat;

import com.adbrand.core.pago.dto.PagoTarjetaRequest;
import com.adbrand.core.pago.service.PasarelaSimulada;
import com.adbrand.core.pago.service.ResultadoCobro;
import org.junit.jupiter.api.Test;

// HU 26: la pasarela simulada valida la tarjeta igual que el banco (Luhn, vencimiento y CVV).
class PasarelaSimuladaTest {

    private final PasarelaSimulada pasarela = new PasarelaSimulada();

    private PagoTarjetaRequest datos(String numero, String vencimiento, String cvv) {
        return new PagoTarjetaRequest(1L, numero, "Ana Quispe", vencimiento, cvv);
    }

    @Test
    void apruebaUnaTarjetaValidaYDevuelveReferencia() {
        ResultadoCobro resultado = pasarela.cobrar(datos("4111 1111 1111 1111", "12/30", "123"));

        assertThat(resultado.aprobado()).isTrue();
        assertThat(resultado.referencia()).startsWith("OP-");
        assertThat(resultado.motivo()).isNull();
    }

    @Test
    void rechazaUnNumeroQueNoPasaElAlgoritmoDeLuhn() {
        ResultadoCobro resultado = pasarela.cobrar(datos("4111 1111 1111 1112", "12/30", "123"));

        assertThat(resultado.aprobado()).isFalse();
        assertThat(resultado.motivo()).contains("no es válido");
    }

    @Test
    void rechazaUnaTarjetaVencida() {
        ResultadoCobro resultado = pasarela.cobrar(datos("4111 1111 1111 1111", "12/20", "123"));

        assertThat(resultado.aprobado()).isFalse();
        assertThat(resultado.motivo()).contains("vencida");
    }

    @Test
    void rechazaUnCvvConLetras() {
        ResultadoCobro resultado = pasarela.cobrar(datos("4111 1111 1111 1111", "12/30", "12a"));

        assertThat(resultado.aprobado()).isFalse();
        assertThat(resultado.motivo()).contains("CVV");
    }

    @Test
    void rechazaLaTarjetaDePruebaDelBanco() {
        ResultadoCobro resultado = pasarela.cobrar(datos("4000 0000 0000 0002", "12/30", "123"));

        assertThat(resultado.aprobado()).isFalse();
        assertThat(resultado.motivo()).contains("rechazada por el banco");
    }
}
