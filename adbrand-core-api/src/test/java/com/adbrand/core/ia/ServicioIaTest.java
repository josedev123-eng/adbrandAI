package com.adbrand.core.ia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.adbrand.core.config.IaProperties;
import com.adbrand.core.ia.client.ClienteIa;
import com.adbrand.core.ia.service.IaNoDisponibleException;
import com.adbrand.core.ia.service.ServicioIa;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.ResourceAccessException;

// Prueba el modo simulado y los fallos de la IA sin conectarse a ningún servidor.
class ServicioIaTest {

    private final ClienteIa cliente = mock(ClienteIa.class);

    private ServicioIa servicio(boolean mock) {
        return new ServicioIa(cliente, new IaProperties("http://ia", "", "modelo", "/chat", 5, mock));
    }

    @Test
    void enModoSimuladoUsaLaOfertaYNoLlamaAlServidor() {
        String texto = servicio(true).generarTexto("reglas", "Negocio: X\nOferta a anunciar: 3x2 en panes\n");

        assertThat(texto).contains("3x2 en panes");
        verify(cliente, never()).completar(anyString(), anyString());
    }

    @Test
    void enModoRealDevuelveElTextoSinEspaciosSobrantes() {
        when(cliente.completar("reglas", "pedido")).thenReturn("  Anuncio listo  \n");

        assertThat(servicio(false).generarTexto("reglas", "pedido")).isEqualTo("Anuncio listo");
    }

    @Test
    void siElServidorNoRespondeLanzaIaNoDisponible() {
        when(cliente.completar(anyString(), anyString())).thenThrow(new ResourceAccessException("timeout"));

        assertThatThrownBy(() -> servicio(false).generarTexto("reglas", "pedido"))
                .isInstanceOf(IaNoDisponibleException.class);
    }

    @Test
    void siLaIaRespondeVacioLanzaIaNoDisponible() {
        when(cliente.completar(anyString(), anyString())).thenReturn("   ");

        assertThatThrownBy(() -> servicio(false).generarTexto("reglas", "pedido"))
                .isInstanceOf(IaNoDisponibleException.class);
    }

    @Test
    void quitaElRazonamientoThinkDeQwen() {
        when(cliente.completar("reglas", "pedido"))
                .thenReturn("<think>\nEl usuario quiere un anuncio...\n</think>\n\nAnuncio listo");

        assertThat(servicio(false).generarTexto("reglas", "pedido")).isEqualTo("Anuncio listo");
    }

    @Test
    void siSoloLlegaElCierreThinkSeQuedaConLoQueSigue() {
        when(cliente.completar("reglas", "pedido")).thenReturn("pensando...\n</think>\nAnuncio listo");

        assertThat(servicio(false).generarTexto("reglas", "pedido")).isEqualTo("Anuncio listo");
    }

    @Test
    void siLaIaSoloPiensaYNoRespondeLanzaIaNoDisponible() {
        when(cliente.completar(anyString(), anyString())).thenReturn("<think>solo pienso</think>");

        assertThatThrownBy(() -> servicio(false).generarTexto("reglas", "pedido"))
                .isInstanceOf(IaNoDisponibleException.class);
    }
}
