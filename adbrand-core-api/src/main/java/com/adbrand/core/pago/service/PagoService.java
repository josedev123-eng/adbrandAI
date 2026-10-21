package com.adbrand.core.pago.service;

import com.adbrand.core.auth.repository.UsuarioRepository;
import com.adbrand.core.pago.dto.PagoResponse;
import com.adbrand.core.pago.dto.PagoTarjetaRequest;
import com.adbrand.core.pago.entity.EstadoPago;
import com.adbrand.core.pago.entity.EstadoSuscripcion;
import com.adbrand.core.pago.entity.MetodoPago;
import com.adbrand.core.pago.entity.Pago;
import com.adbrand.core.pago.entity.Plan;
import com.adbrand.core.pago.entity.Suscripcion;
import com.adbrand.core.pago.repository.PagoRepository;
import com.adbrand.core.pago.repository.PlanRepository;
import com.adbrand.core.pago.repository.SuscripcionRepository;
import com.adbrand.core.shared.error.ConflictoException;
import com.adbrand.core.shared.error.RecursoNoEncontradoException;
import com.adbrand.core.shared.error.SolicitudInvalidaException;
import java.time.LocalDate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// HU 26: cobra con la pasarela, crea la suscripción y guarda el pago. El panel financiero de Django
// sigue leyendo la misma tabla pago.
@Service
public class PagoService {

    private final PlanRepository planes;
    private final SuscripcionRepository suscripciones;
    private final PagoRepository pagos;
    private final PasarelaPago pasarela;
    private final UsuarioRepository usuarios;

    public PagoService(PlanRepository planes, SuscripcionRepository suscripciones, PagoRepository pagos,
                       PasarelaPago pasarela, UsuarioRepository usuarios) {
        this.planes = planes;
        this.suscripciones = suscripciones;
        this.pagos = pagos;
        this.pasarela = pasarela;
        this.usuarios = usuarios;
    }

    @Transactional
    public PagoResponse pagarConTarjeta(Long usuarioId, PagoTarjetaRequest datos) {
        Plan plan = planActivo(datos.planId());
        verificarSinSuscripcionActiva(usuarioId, plan);

        ResultadoCobro cobro = pasarela.cobrar(datos);
        if (!cobro.aprobado()) {
            throw new SolicitudInvalidaException("PAGO_RECHAZADO", cobro.motivo());
        }

        Pago pago = registrarPago(usuarioId, plan, MetodoPago.TARJETA, cobro.referencia(),
                datos.titular().trim(), ultimosDigitos(datos.numeroTarjeta()));
        return PagoResponse.desde(pago, plan);
    }

    // Lo usan también los pagos con Yape (HU 27) cuando se confirma la operación.
    @Transactional
    Pago registrarPago(Long usuarioId, Plan plan, MetodoPago metodo, String referencia,
                       String titular, String ultimosDigitos) {
        Suscripcion suscripcion = crearSuscripcion(usuarioId, plan);

        Pago pago = new Pago();
        pago.setSuscripcionId(suscripcion.getId());
        pago.setUsuarioId(usuarioId);
        pago.setPlanId(plan.getId());
        pago.setMonto(plan.getPrecio());
        pago.setFechaPago(LocalDate.now());
        pago.setMetodo(metodo);
        pago.setEstado(EstadoPago.APROBADO);
        pago.setReferencia(referencia);
        pago.setTitular(titular);
        pago.setUltimosDigitos(ultimosDigitos);
        return pagos.saveAndFlush(pago);
    }

    Plan planActivo(Long planId) {
        return planes.findById(planId)
                .filter(Plan::isActivo)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "PLAN_NO_ENCONTRADO", "El plan seleccionado no existe o ya no está disponible."));
    }

    void verificarSinSuscripcionActiva(Long usuarioId, Plan plan) {
        boolean yaTiene = suscripciones.existsByUsuarioIdAndPlanAndEstadoAndFechaVencimientoGreaterThanEqual(
                usuarioId, plan.getNombre(), EstadoSuscripcion.ACTIVA, LocalDate.now());
        if (yaTiene) {
            throw new ConflictoException("SUSCRIPCION_ACTIVA",
                    "Ya tienes una suscripción activa al plan " + plan.getNombre() + ".");
        }
    }

    private Suscripcion crearSuscripcion(Long usuarioId, Plan plan) {
        LocalDate hoy = LocalDate.now();
        Suscripcion suscripcion = new Suscripcion();
        suscripcion.setUsuarioId(usuarioId);
        suscripcion.setCliente(nombreCliente(usuarioId));
        suscripcion.setPlan(plan.getNombre());
        suscripcion.setMonto(plan.getPrecio());
        suscripcion.setEstado(EstadoSuscripcion.ACTIVA);
        suscripcion.setFechaInicio(hoy);
        suscripcion.setFechaVencimiento(hoy.plusMonths(1));
        return suscripciones.saveAndFlush(suscripcion);
    }

    private String nombreCliente(Long usuarioId) {
        return usuarios.findById(usuarioId)
                .map(usuario -> usuario.getNombreEmpresa())
                .orElse("Empresa #" + usuarioId);
    }

    private String ultimosDigitos(String numeroTarjeta) {
        String digitos = numeroTarjeta.replaceAll("\\D", "");
        return digitos.substring(digitos.length() - 4);
    }
}
