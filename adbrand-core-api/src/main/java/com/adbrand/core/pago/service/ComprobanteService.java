package com.adbrand.core.pago.service;

import com.adbrand.core.auth.entity.Usuario;
import com.adbrand.core.auth.repository.UsuarioRepository;
import com.adbrand.core.notificacion.service.ServicioCorreo;
import com.adbrand.core.pago.dto.ComprobanteResponse;
import com.adbrand.core.pago.entity.Pago;
import com.adbrand.core.pago.entity.Plan;
import com.adbrand.core.pago.repository.PagoRepository;
import com.adbrand.core.pago.repository.PlanRepository;
import com.adbrand.core.shared.error.RecursoNoEncontradoException;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// HU 28: genera el número de comprobante (ADB-000001) y lo envía por correo la primera vez.
// El número sale del id del pago, así que nunca se repite.
@Service
public class ComprobanteService {

    private static final Logger log = LoggerFactory.getLogger(ComprobanteService.class);
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final PagoRepository pagos;
    private final PlanRepository planes;
    private final UsuarioRepository usuarios;
    private final ServicioCorreo correo;

    public ComprobanteService(PagoRepository pagos, PlanRepository planes,
                              UsuarioRepository usuarios, ServicioCorreo correo) {
        this.pagos = pagos;
        this.planes = planes;
        this.usuarios = usuarios;
        this.correo = correo;
    }

    @Transactional
    public ComprobanteResponse obtener(Long usuarioId, Long pagoId) {
        Pago pago = pagos.findByIdAndUsuarioId(pagoId, usuarioId)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "PAGO_NO_ENCONTRADO", "El pago no existe o no es tuyo."));

        boolean recienGenerado = pago.getNumeroComprobante() == null;
        if (recienGenerado) {
            pago.setNumeroComprobante(numeroComprobante(pago.getId()));
            pagos.saveAndFlush(pago);
        }

        Usuario usuario = usuarios.findById(usuarioId).orElse(null);
        String cliente = usuario == null ? "Empresa #" + usuarioId : usuario.getNombreEmpresa();
        String correoDestino = usuario == null ? null : usuario.getCorreo();
        String nombrePlan = nombrePlan(pago.getPlanId());

        boolean enviado = false;
        if (recienGenerado && correoDestino != null && !correoDestino.isBlank()) {
            enviado = enviarComprobante(correoDestino, cliente, nombrePlan, pago);
        }

        return new ComprobanteResponse(pago.getNumeroComprobante(), pago.getId(), cliente, correoDestino,
                nombrePlan, pago.getMonto(), pago.getMetodo().name(), pago.getReferencia(),
                pago.getFechaPago(), enviado);
    }

    private String numeroComprobante(Long pagoId) {
        return "ADB-" + String.format("%06d", pagoId);
    }

    private String nombrePlan(Long planId) {
        if (planId == null) {
            return "Suscripción AdBrand.AI";
        }
        return planes.findById(planId)
                .map(Plan::getNombre)
                .orElse("Plan #" + planId);
    }

    private boolean enviarComprobante(String destino, String cliente, String plan, Pago pago) {
        try {
            correo.enviar(destino,
                    "Tu comprobante AdBrand.AI " + pago.getNumeroComprobante(),
                    textoComprobante(cliente, plan, pago));
            return true;
        } catch (RuntimeException ex) {
            // El pago ya está cobrado: si el correo falla, igual se muestra el comprobante en pantalla.
            log.warn("No se pudo enviar el comprobante {} a {}", pago.getNumeroComprobante(), destino, ex);
            return false;
        }
    }

    private String textoComprobante(String cliente, String plan, Pago pago) {
        return """
                Hola %s:

                Registramos tu pago del plan %s por %s el %s.
                Tu comprobante es %s.

                Gracias por confiar en AdBrand.AI.
                """.formatted(cliente, plan, monto(pago.getMonto()), pago.getFechaPago().format(FECHA),
                pago.getNumeroComprobante());
    }

    private String monto(BigDecimal monto) {
        return String.format(Locale.ROOT, "S/ %.2f", monto);
    }
}
