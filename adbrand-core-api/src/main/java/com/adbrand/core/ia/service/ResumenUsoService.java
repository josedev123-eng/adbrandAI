package com.adbrand.core.ia.service;

import com.adbrand.core.contenido.repository.GeneracionAnuncioRepository;
import com.adbrand.core.ia.dto.ResumenUsoResponse;
import com.adbrand.core.imagen.service.ControlGastoImagenes;
import java.time.LocalDate;
import java.time.ZoneId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// "Tu uso este mes" en Planes: consumo de IA + anuncios creados + retoques usados, desde el día 1 (hora de Lima).
@Service
public class ResumenUsoService {

    private static final ZoneId LIMA = ZoneId.of("America/Lima");

    private final LimiteConsumoIa limite;
    private final GeneracionAnuncioRepository generaciones;
    private final ControlGastoImagenes imagenes;

    public ResumenUsoService(LimiteConsumoIa limite, GeneracionAnuncioRepository generaciones,
                             ControlGastoImagenes imagenes) {
        this.limite = limite;
        this.generaciones = generaciones;
        this.imagenes = imagenes;
    }

    @Transactional(readOnly = true)
    public ResumenUsoResponse delMes(Long usuarioId) {
        var inicio = LocalDate.now(LIMA).withDayOfMonth(1).atStartOfDay();
        return new ResumenUsoResponse(
                limite.consumoDelMes(usuarioId),
                generaciones.countByUsuarioIdAndFechaCreacionGreaterThanEqual(usuarioId, inicio),
                imagenes.retoquesDelMes(usuarioId),
                imagenes.limiteRetoquesMes());
    }
}
