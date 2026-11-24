package com.adbrand.core.ia.service;

import com.adbrand.core.ia.dto.ConsumoMesResponse;
import com.adbrand.core.ia.entity.LimiteTokens;
import com.adbrand.core.ia.repository.ConsumoTokenRepository;
import com.adbrand.core.ia.repository.LimiteTokensRepository;
import com.adbrand.core.pago.entity.EstadoSuscripcion;
import com.adbrand.core.pago.entity.Plan;
import com.adbrand.core.pago.repository.PlanRepository;
import com.adbrand.core.pago.repository.SuscripcionRepository;
import com.adbrand.core.shared.error.SolicitudInvalidaException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Hace cumplir el límite mensual de tokens de IA (el modelo de negocio: cada plan da un cupo al mes).
// Regla, la misma que muestra el panel del administrador (Django, tokens/services.py):
//   1. Si el administrador le puso un límite propio a la empresa: ese.
//   2. Si no, y tiene un plan pagado activo: el límite del plan.
//   3. Si no: el límite por defecto, que funciona como plan gratuito (V43).
// El mes se cuenta desde el día 1 a las 00:00 en hora de Lima.
@Service
public class LimiteConsumoIa {

    public static final String CODIGO = "LIMITE_TOKENS";
    public static final String PERSONALIZADO = "PERSONALIZADO";
    public static final String PLAN = "PLAN";
    public static final String GRATUITO = "GRATUITO";

    static final ZoneId LIMA = ZoneId.of("America/Lima");
    // Si no existiera la fila por defecto, el plan gratuito usa estos valores.
    static final long LIMITE_GRATUITO_DE_RESPALDO = 10_000;
    static final int AVISO_DE_RESPALDO = 80;

    private final ConsumoTokenRepository consumos;
    private final LimiteTokensRepository limites;
    private final SuscripcionRepository suscripciones;
    private final PlanRepository planes;
    private final Clock reloj;

    @Autowired
    public LimiteConsumoIa(ConsumoTokenRepository consumos, LimiteTokensRepository limites,
                           SuscripcionRepository suscripciones, PlanRepository planes) {
        this(consumos, limites, suscripciones, planes, Clock.system(LIMA));
    }

    public LimiteConsumoIa(ConsumoTokenRepository consumos, LimiteTokensRepository limites,
                           SuscripcionRepository suscripciones, PlanRepository planes, Clock reloj) {
        this.consumos = consumos;
        this.limites = limites;
        this.suscripciones = suscripciones;
        this.planes = planes;
        this.reloj = reloj;
    }

    record Limite(long limite, int porcentajeAviso, String origen, String plan) {
    }

    Limite limiteDe(Long usuarioId) {
        Optional<LimiteTokens> porDefecto = limites.findFirstByUsuarioIdIsNull();
        int aviso = porDefecto.map(LimiteTokens::getPorcentajeAviso).orElse(AVISO_DE_RESPALDO);

        Optional<LimiteTokens> propio = limites.findByUsuarioId(usuarioId);
        if (propio.isPresent()) {
            return new Limite(propio.get().getLimiteMensual(), propio.get().getPorcentajeAviso(), PERSONALIZADO, null);
        }

        Optional<Plan> plan = suscripciones
                .findFirstByUsuarioIdAndEstadoAndFechaVencimientoGreaterThanEqualOrderByFechaVencimientoDescIdDesc(
                        usuarioId, EstadoSuscripcion.ACTIVA, LocalDate.now(reloj))
                .flatMap(suscripcion -> planes.findFirstByNombre(suscripcion.getPlan()))
                .filter(encontrado -> encontrado.getLimiteTokens() != null);
        if (plan.isPresent()) {
            return new Limite(plan.get().getLimiteTokens(), aviso, PLAN, plan.get().getNombre());
        }

        long gratuito = porDefecto.map(LimiteTokens::getLimiteMensual).orElse(LIMITE_GRATUITO_DE_RESPALDO);
        return new Limite(gratuito, aviso, GRATUITO, null);
    }

    @Transactional(readOnly = true)
    public ConsumoMesResponse consumoDelMes(Long usuarioId) {
        Limite limite = limiteDe(usuarioId);
        long usados = consumos.sumarTokensDesde(usuarioId, inicioDelMes().toOffsetDateTime());
        int porcentaje = (int) Math.min(100, Math.round(usados * 100.0 / limite.limite()));
        return new ConsumoMesResponse(usados, limite.limite(), porcentaje, limite.origen(), limite.plan(),
                usados * 100 >= limite.limite() * limite.porcentajeAviso(), usados >= limite.limite());
    }

    // Se llama ANTES de usar la IA de texto. Si ya gastó su cupo del mes, se detiene con un mensaje claro.
    @Transactional(readOnly = true)
    public void verificarDisponible(Long usuarioId) {
        if (usuarioId == null) return;
        ConsumoMesResponse consumo = consumoDelMes(usuarioId);
        if (consumo.agotado()) {
            String cantidad = String.format("%,d", consumo.limite()).replace(',', ' ');
            String mensaje = GRATUITO.equals(consumo.origen())
                    ? "Usaste los " + cantidad + " tokens de IA del plan gratuito este mes. Elige un plan para seguir creando."
                    : "Llegaste al límite de " + cantidad + " tokens de IA de tu plan este mes. Mejora tu plan para seguir creando.";
            throw new SolicitudInvalidaException(CODIGO, mensaje);
        }
    }

    ZonedDateTime inicioDelMes() {
        return LocalDate.now(reloj).withDayOfMonth(1).atStartOfDay(LIMA);
    }
}
