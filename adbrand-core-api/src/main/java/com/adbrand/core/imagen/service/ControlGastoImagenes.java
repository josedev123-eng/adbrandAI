package com.adbrand.core.imagen.service;

import com.adbrand.core.imagen.dto.ImagenGenerada;
import com.adbrand.core.imagen.entity.ConsumoImagen;
import com.adbrand.core.imagen.repository.ConsumoImagenRepository;
import com.adbrand.core.shared.error.SolicitudInvalidaException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

// Control de gasto de las IA de imágenes (como el de tokens de la HU 17 y su límite de la HU 19):
// - anota cada imagen generada (quién, qué IA, borrador o retoque);
// - limita los retoques reales (los que cuestan crédito de OpenRouter) por empresa y por mes.
@Service
public class ControlGastoImagenes {

    private static final Logger log = LoggerFactory.getLogger(ControlGastoImagenes.class);
    private static final ZoneId LIMA = ZoneId.of("America/Lima");

    private final ConsumoImagenRepository consumos;
    private final int limiteRetoquesMes;
    private final Clock reloj;

    @Autowired
    public ControlGastoImagenes(ConsumoImagenRepository consumos,
                                @Value("${ia.imagen.limite-retoques-mes:20}") int limiteRetoquesMes) {
        this(consumos, limiteRetoquesMes, Clock.system(LIMA));
    }

    // Las pruebas pasan su propio reloj para fijar el mes.
    public ControlGastoImagenes(ConsumoImagenRepository consumos, int limiteRetoquesMes, Clock reloj) {
        this.consumos = consumos;
        this.limiteRetoquesMes = limiteRetoquesMes;
        this.reloj = reloj;
    }

    public int limiteRetoquesMes() {
        return limiteRetoquesMes;
    }

    // Transacción propia: si anotar el consumo falla, la imagen igual se entrega.
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrar(Long usuarioId, ConsumoImagen.Tipo tipo, ImagenGenerada imagen) {
        try {
            consumos.save(ConsumoImagen.de(usuarioId, tipo, imagen.proveedor(), imagen.simulado()));
        } catch (RuntimeException ex) {
            log.warn("No se pudo anotar el consumo de imagen de la empresa {}: {}", usuarioId, ex.getMessage());
        }
    }

    public long retoquesDelMes(Long usuarioId) {
        return consumos.countByUsuarioIdAndTipoAndSimuladoFalseAndFechaGreaterThanEqual(
                usuarioId, ConsumoImagen.Tipo.RETOQUE, inicioDelMes());
    }

    public int retoquesRestantes(Long usuarioId) {
        return (int) Math.max(0, limiteRetoquesMes - retoquesDelMes(usuarioId));
    }

    // Antes de gastar crédito: si la empresa ya usó todos sus retoques del mes, se detiene con un mensaje claro.
    public void verificarRetoqueDisponible(Long usuarioId) {
        if (retoquesRestantes(usuarioId) <= 0) {
            throw new SolicitudInvalidaException("LIMITE_RETOQUES",
                    "Llegaste a tu límite de %d retoques de imagen este mes. Podrás volver a retocar el próximo mes."
                            .formatted(limiteRetoquesMes));
        }
    }

    // Primer instante del mes actual en la hora de Lima (igual que el panel de consumo de Django).
    private OffsetDateTime inicioDelMes() {
        LocalDate hoy = LocalDate.now(reloj.withZone(LIMA));
        return hoy.withDayOfMonth(1).atStartOfDay(LIMA).toOffsetDateTime();
    }
}
