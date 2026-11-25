package com.adbrand.core.imagen;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.adbrand.core.ia.entity.OrigenConsumo;
import com.adbrand.core.ia.service.ServicioIa;
import com.adbrand.core.imagen.service.InterpreteRetoque;
import org.junit.jupiter.api.Test;

// El pedido del dueño se convierte en una instrucción visual (no se manda tal cual a la IA de imágenes).
class InterpreteRetoqueTest {

    @Test
    void convierteElPedidoEnUnaInstruccionVisual() {
        ServicioIa ia = mock(ServicioIa.class);
        when(ia.generarTexto(anyString(), eq("pon el fondo más claro"), eq(4L), any(OrigenConsumo.class)))
                .thenReturn("\"Make the background brighter and softer.\"");

        assertThat(new InterpreteRetoque(ia).interpretar("pon el fondo más claro", 4L, OrigenConsumo.ANUNCIO))
                .isEqualTo("Make the background brighter and softer.");
    }

    @Test
    void siLaIaFallaOEstaEnModoPruebaUsaElPedidoTalCual() {
        ServicioIa simulada = mock(ServicioIa.class);
        when(simulada.esSimulado()).thenReturn(true);
        assertThat(new InterpreteRetoque(simulada).interpretar(" más luz ", 4L, OrigenConsumo.ANUNCIO)).isEqualTo("más luz");

        ServicioIa caida = mock(ServicioIa.class);
        when(caida.generarTexto(anyString(), anyString(), any(), any())).thenThrow(new IllegalStateException("sin IA"));
        assertThat(new InterpreteRetoque(caida).interpretar("más luz", 4L, OrigenConsumo.ANUNCIO)).isEqualTo("más luz");
    }
}
