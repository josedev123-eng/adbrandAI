package com.adbrand.core.contenido;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.adbrand.core.auth.repository.UsuarioRepository;
import com.adbrand.core.contenido.repository.ContenidoRepository;
import com.adbrand.core.contenido.service.MarcaDelNegocio;
import com.adbrand.core.contenido.service.VideoAnuncioService;
import com.adbrand.core.ia.dto.ConsumoMesResponse;
import com.adbrand.core.ia.service.LimiteConsumoIa;
import com.adbrand.core.previsualizacion.service.PrevisualizacionService;
import com.adbrand.core.shared.error.AccesoDenegadoException;
import org.junit.jupiter.api.Test;

// El video para Reels y Estados es de los planes de pago.
class VideoPlanTest {

    private VideoAnuncioService servicio(String origen) {
        LimiteConsumoIa limites = mock(LimiteConsumoIa.class);
        when(limites.consumoDelMes(3L)).thenReturn(new ConsumoMesResponse(0, 10000, 0, origen, null, false, false));
        return new VideoAnuncioService(mock(ContenidoRepository.class), mock(PrevisualizacionService.class),
                mock(MarcaDelNegocio.class), mock(UsuarioRepository.class), limites);
    }

    @Test
    void conElPlanGratisNoSePuedeCrearElVideo() {
        assertThat(servicio(LimiteConsumoIa.GRATUITO).disponiblePara(3L)).isFalse();
        assertThatThrownBy(() -> servicio(LimiteConsumoIa.GRATUITO).generar(3L, 9L))
                .isInstanceOf(AccesoDenegadoException.class)
                .hasMessageContaining("planes de pago");
    }

    @Test
    void conUnPlanPagadoOLimitePersonalizadoSiSePuede() {
        assertThat(servicio(LimiteConsumoIa.PLAN).disponiblePara(3L)).isTrue();
        assertThat(servicio(LimiteConsumoIa.PERSONALIZADO).disponiblePara(3L)).isTrue();
    }
}
