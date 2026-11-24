package com.adbrand.core.pago.service;

import com.adbrand.core.pago.dto.ConfiguracionPagoResponse;
import com.adbrand.core.pago.dto.PagoMercadoPagoRequest;
import com.adbrand.core.pago.dto.PagoResponse;
import com.adbrand.core.pago.dto.PagoYapeMercadoPagoRequest;
import com.adbrand.core.pago.entity.MetodoPago;
import com.adbrand.core.pago.entity.Pago;
import com.adbrand.core.pago.entity.Plan;
import com.adbrand.core.shared.error.SolicitudInvalidaException;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// HU 26 con Mercado Pago: mismas reglas que el pago simulado (plan activo, sin suscripción repetida),
// pero el cobro lo hace Mercado Pago con el token de la tarjeta. El pago se guarda en la misma tabla.
@Service
public class PagoMercadoPagoService {

    private final PagoService pagos;
    private final ClienteMercadoPago mercadoPago;

    public PagoMercadoPagoService(PagoService pagos, ClienteMercadoPago mercadoPago) {
        this.pagos = pagos;
        this.mercadoPago = mercadoPago;
    }

    public ConfiguracionPagoResponse configuracion() {
        return mercadoPago.disponible()
                ? new ConfiguracionPagoResponse(ConfiguracionPagoResponse.MERCADO_PAGO, mercadoPago.llavePublica())
                : new ConfiguracionPagoResponse(ConfiguracionPagoResponse.SIMULADA, null);
    }

    @Transactional
    public PagoResponse pagar(Long usuarioId, PagoMercadoPagoRequest datos) {
        if (!mercadoPago.disponible()) {
            throw new SolicitudInvalidaException("PASARELA_NO_CONFIGURADA",
                    "El pago con Mercado Pago no está activo. Usa el formulario de tarjeta.");
        }
        Plan plan = pagos.planActivo(datos.planId());
        pagos.verificarSinSuscripcionActiva(usuarioId, plan);

        CobroMercadoPago cobro = mercadoPago.cobrar(plan.getPrecio(), "AdBrand - plan " + plan.getNombre(),
                datos, UUID.randomUUID().toString());
        if (!cobro.resultado().aprobado()) {
            throw new SolicitudInvalidaException("PAGO_RECHAZADO", cobro.resultado().motivo());
        }

        String titular = primeroConTexto(cobro.titular(), datos.titular(), "Tarjeta (Mercado Pago)");
        String digitos = primeroConTexto(cobro.ultimosDigitos(), datos.ultimosDigitos(), null);
        Pago pago = pagos.registrarPago(usuarioId, plan, MetodoPago.TARJETA, cobro.resultado().referencia(),
                titular, digitos);
        return PagoResponse.desde(pago, plan);
    }

    // HU 27 con Mercado Pago: Yape se cobra al momento; ya no hace falta el botón "Ya pagué".
    @Transactional
    public PagoResponse pagarConYape(Long usuarioId, PagoYapeMercadoPagoRequest datos) {
        if (!mercadoPago.disponible()) {
            throw new SolicitudInvalidaException("PASARELA_NO_CONFIGURADA",
                    "El pago con Yape por Mercado Pago no está activo. Usa el QR de Yape.");
        }
        Plan plan = pagos.planActivo(datos.planId());
        pagos.verificarSinSuscripcionActiva(usuarioId, plan);

        CobroMercadoPago cobro = mercadoPago.cobrarConYape(plan.getPrecio(), "AdBrand - plan " + plan.getNombre(),
                datos, UUID.randomUUID().toString());
        if (!cobro.resultado().aprobado()) {
            throw new SolicitudInvalidaException("PAGO_RECHAZADO", cobro.resultado().motivo());
        }

        // En el comprobante (HU 28) se muestran los últimos 4 dígitos del celular.
        String digitos = datos.celular().substring(datos.celular().length() - 4);
        Pago pago = pagos.registrarPago(usuarioId, plan, MetodoPago.YAPE, cobro.resultado().referencia(),
                "Yape " + "*****" + digitos, digitos);
        return PagoResponse.desde(pago, plan);
    }

    private static String primeroConTexto(String primero, String segundo, String porDefecto) {
        if (primero != null && !primero.isBlank()) return primero.trim();
        if (segundo != null && !segundo.isBlank()) return segundo.trim();
        return porDefecto;
    }
}
