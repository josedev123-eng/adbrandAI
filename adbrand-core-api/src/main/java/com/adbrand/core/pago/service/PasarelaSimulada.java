package com.adbrand.core.pago.service;

import com.adbrand.core.pago.dto.PagoTarjetaRequest;
import java.time.YearMonth;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Component;

// HU 26: pasarela simulada. No cobra de verdad: valida la tarjeta como lo haría el banco
// y devuelve una referencia de operación. Las tarjetas que terminan en 0000 se rechazan
// para poder probar el camino de error sin salir del sistema.
@Component
public class PasarelaSimulada implements PasarelaPago {

    @Override
    public ResultadoCobro cobrar(PagoTarjetaRequest datos) {
        String numero = datos.numeroTarjeta().replaceAll("\\s+", "");

        if (!pasaAlgoritmoLuhn(numero)) {
            return ResultadoCobro.rechazado("El número de la tarjeta no es válido. Revísalo e intenta de nuevo.");
        }
        if (!estaVigente(datos.vencimiento())) {
            return ResultadoCobro.rechazado("La tarjeta está vencida. Usa otra tarjeta.");
        }
        if (!datos.cvv().matches("[0-9]{3,4}")) {
            return ResultadoCobro.rechazado("El CVV no es válido.");
        }
        if (numero.endsWith("0000")) {
            return ResultadoCobro.rechazado("La tarjeta fue rechazada por el banco. Usa otra tarjeta.");
        }

        String referencia = "OP-" + UUID.randomUUID().toString()
                .replace("-", "")
                .substring(0, 10)
                .toUpperCase(Locale.ROOT);
        return ResultadoCobro.aprobado(referencia);
    }

    // Algoritmo de Luhn: el último dígito es el de control de los anteriores.
    private boolean pasaAlgoritmoLuhn(String numero) {
        if (!numero.matches("[0-9]{13,19}")) {
            return false;
        }
        int suma = 0;
        boolean duplicar = false;
        for (int i = numero.length() - 1; i >= 0; i--) {
            int digito = numero.charAt(i) - '0';
            if (duplicar) {
                digito *= 2;
                if (digito > 9) {
                    digito -= 9;
                }
            }
            suma += digito;
            duplicar = !duplicar;
        }
        return suma % 10 == 0;
    }

    private boolean estaVigente(String vencimiento) {
        int mes = Integer.parseInt(vencimiento.substring(0, 2));
        int anio = 2000 + Integer.parseInt(vencimiento.substring(3));
        return !YearMonth.of(anio, mes).isBefore(YearMonth.now());
    }
}
