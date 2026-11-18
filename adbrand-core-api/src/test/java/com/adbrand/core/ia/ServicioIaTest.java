package com.adbrand.core.ia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.adbrand.core.config.IaProperties;
import com.adbrand.core.ia.client.ClienteIa;
import com.adbrand.core.ia.service.IaNoDisponibleException;
import com.adbrand.core.ia.service.RegistroConsumoIa;
import com.adbrand.core.ia.service.ServicioIa;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

// Prueba el modo simulado y los fallos de la IA sin conectarse a ningún servidor.
class ServicioIaTest {

    private final ClienteIa cliente = mock(ClienteIa.class);

    private ServicioIa servicio(boolean mock) {
        return new ServicioIa(cliente, new IaProperties("http://ia", "", "modelo", "/chat", 5, mock),
                mock(RegistroConsumoIa.class));
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

    private ServicioIa servicioSinEspera() {
        ServicioIa servicio = servicio(false);
        servicio.cambiarEsperaEntreReintentos(0);
        return servicio;
    }

    // Gemini gratis a veces responde 503 "high demand" unos segundos: se reintenta y sale bien.
    @Test
    void siLaIaEstaSaturadaReintentaYDevuelveElTexto() {
        when(cliente.completar("reglas", "pedido"))
                .thenThrow(new HttpServerErrorException(HttpStatus.SERVICE_UNAVAILABLE))
                .thenReturn("Anuncio listo");

        assertThat(servicioSinEspera().generarTexto("reglas", "pedido")).isEqualTo("Anuncio listo");
        verify(cliente, times(2)).completar("reglas", "pedido");
    }

    @Test
    void siSigueSaturadaDespuesDeLosReintentosLanzaIaNoDisponible() {
        when(cliente.completar(anyString(), anyString()))
                .thenThrow(new HttpClientErrorException(HttpStatus.TOO_MANY_REQUESTS));

        assertThatThrownBy(() -> servicioSinEspera().generarTexto("reglas", "pedido"))
                .isInstanceOf(IaNoDisponibleException.class);
        verify(cliente, times(ServicioIa.REINTENTOS + 1)).completar("reglas", "pedido");
    }

    // Un error que no es pasajero (por ejemplo, modelo inexistente o clave mala) no se reintenta.
    @Test
    void unErrorQueNoEsPasajeroNoSeReintenta() {
        when(cliente.completar(anyString(), anyString()))
                .thenThrow(new HttpClientErrorException(HttpStatus.NOT_FOUND));

        assertThatThrownBy(() -> servicioSinEspera().generarTexto("reglas", "pedido"))
                .isInstanceOf(IaNoDisponibleException.class);
        verify(cliente, times(1)).completar("reglas", "pedido");
    }
}
