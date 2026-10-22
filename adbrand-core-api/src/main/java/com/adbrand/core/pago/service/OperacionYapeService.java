package com.adbrand.core.pago.service;

import com.adbrand.core.auth.repository.UsuarioRepository;
import com.adbrand.core.pago.dto.OperacionYapeResponse;
import com.adbrand.core.pago.dto.PagoResponse;
import com.adbrand.core.pago.dto.RegistrarYapeRequest;
import com.adbrand.core.pago.entity.EstadoOperacionYape;
import com.adbrand.core.pago.entity.MetodoPago;
import com.adbrand.core.pago.entity.OperacionYape;
import com.adbrand.core.pago.entity.Pago;
import com.adbrand.core.pago.entity.Plan;
import com.adbrand.core.pago.repository.OperacionYapeRepository;
import com.adbrand.core.pago.repository.PlanRepository;
import com.adbrand.core.shared.error.ConflictoException;
import com.adbrand.core.shared.error.RecursoNoEncontradoException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// HU 27: pago con Yape en dos pasos. Primero se genera el QR (operación PENDIENTE) y luego,
// cuando el usuario ya pagó desde su app, se confirma y se registra el pago real.
@Service
public class OperacionYapeService {

    private final OperacionYapeRepository operaciones;
    private final PlanRepository planes;
    private final PagoService pagos;
    private final UsuarioRepository usuarios;

    public OperacionYapeService(OperacionYapeRepository operaciones, PlanRepository planes,
                                PagoService pagos, UsuarioRepository usuarios) {
        this.operaciones = operaciones;
        this.planes = planes;
        this.pagos = pagos;
        this.usuarios = usuarios;
    }

    @Transactional
    public OperacionYapeResponse registrar(Long usuarioId, RegistrarYapeRequest datos) {
        Plan plan = pagos.planActivo(datos.planId());
        pagos.verificarSinSuscripcionActiva(usuarioId, plan);

        OperacionYape operacion = new OperacionYape();
        operacion.setUsuarioId(usuarioId);
        operacion.setPlanId(plan.getId());
        operacion.setMonto(plan.getPrecio());
        operacion.setCodigo(generarCodigo());
        operacion.setEstado(EstadoOperacionYape.PENDIENTE);
        operacion.setCelular(celular(datos.celular()));
        operaciones.saveAndFlush(operacion);

        return OperacionYapeResponse.desde(operacion, plan.getNombre());
    }

    @Transactional
    public PagoResponse confirmar(Long usuarioId, Long operacionId) {
        OperacionYape operacion = operaciones.findByIdAndUsuarioId(operacionId, usuarioId)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "OPERACION_NO_ENCONTRADA", "La operación de Yape no existe o no es tuya."));

        if (operacion.getEstado() == EstadoOperacionYape.CONFIRMADO) {
            throw new ConflictoException("OPERACION_YA_CONFIRMADA",
                    "Esta operación de Yape ya fue confirmada.");
        }
        if (operacion.getEstado() == EstadoOperacionYape.ANULADO) {
            throw new ConflictoException("OPERACION_ANULADA",
                    "Esta operación de Yape fue anulada. Genera un QR nuevo.");
        }

        Plan plan = pagos.planActivo(operacion.getPlanId());
        pagos.verificarSinSuscripcionActiva(usuarioId, plan);

        Pago pago = pagos.registrarPago(usuarioId, plan, MetodoPago.YAPE, operacion.getCodigo(),
                nombreTitular(usuarioId), null);

        operacion.setEstado(EstadoOperacionYape.CONFIRMADO);
        operacion.setFechaConfirmacion(LocalDateTime.now());
        operaciones.save(operacion);

        return PagoResponse.desde(pago, plan);
    }

    @Transactional(readOnly = true)
    public List<OperacionYapeResponse> pendientes(Long usuarioId) {
        return operaciones
                .findByUsuarioIdAndEstadoOrderByFechaCreacionDesc(usuarioId, EstadoOperacionYape.PENDIENTE)
                .stream()
                .map(operacion -> OperacionYapeResponse.desde(operacion, nombrePlan(operacion.getPlanId())))
                .toList();
    }

    private String generarCodigo() {
        return "YPE-" + UUID.randomUUID().toString()
                .replace("-", "")
                .substring(0, 8)
                .toUpperCase(Locale.ROOT);
    }

    private String celular(String celular) {
        if (celular == null || celular.isBlank()) {
            return null;
        }
        return celular.trim();
    }

    private String nombreTitular(Long usuarioId) {
        return usuarios.findById(usuarioId)
                .map(usuario -> usuario.getNombre())
                .orElse("Cliente Yape");
    }

    private String nombrePlan(Long planId) {
        return planes.findById(planId)
                .map(Plan::getNombre)
                .orElse("Plan #" + planId);
    }
}
