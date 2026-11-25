package com.adbrand.core.ia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.adbrand.core.contenido.repository.GeneracionAnuncioRepository;
import com.adbrand.core.ia.dto.ConsumoMesResponse;
import com.adbrand.core.ia.dto.ResumenUsoResponse;
import com.adbrand.core.ia.service.LimiteConsumoIa;
import com.adbrand.core.ia.service.ResumenUsoService;
import com.adbrand.core.imagen.service.ControlGastoImagenes;
import org.junit.jupiter.api.Test;

// "Tu uso este mes" en Planes: junta el consumo de IA, los anuncios creados y los retoques usados.
class ResumenUsoTest {

    @Test
    void juntaElConsumoLosAnunciosYLosRetoquesDelMes() {
        LimiteConsumoIa limite = mock(LimiteConsumoIa.class);
        GeneracionAnuncioRepository generaciones = mock(GeneracionAnuncioRepository.class);
        ControlGastoImagenes imagenes = mock(ControlGastoImagenes.class);
        ConsumoMesResponse consumo = new ConsumoMesResponse(75000, 150000, 50, "PLAN", "Pro", false, false);
        when(limite.consumoDelMes(7L)).thenReturn(consumo);
        when(generaciones.countByUsuarioIdAndFechaCreacionGreaterThanEqual(eq(7L), any())).thenReturn(12L);
        when(imagenes.retoquesDelMes(7L)).thenReturn(3L);
        when(imagenes.limiteRetoquesMes()).thenReturn(20);

        ResumenUsoResponse resumen = new ResumenUsoService(limite, generaciones, imagenes).delMes(7L);

        assertThat(resumen.consumo()).isEqualTo(consumo);
        assertThat(resumen.anunciosCreados()).isEqualTo(12);
        assertThat(resumen.retoquesUsados()).isEqualTo(3);
        assertThat(resumen.limiteRetoques()).isEqualTo(20);
    }
}
