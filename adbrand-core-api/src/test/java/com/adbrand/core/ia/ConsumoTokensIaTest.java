package com.adbrand.core.ia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.adbrand.core.config.IaProperties;
import com.adbrand.core.ia.client.ClienteIa;
import com.adbrand.core.ia.dto.MensajeIa;
import com.adbrand.core.ia.dto.RespuestaChatIa;
import com.adbrand.core.ia.entity.ConsumoToken;
import com.adbrand.core.ia.entity.OrigenConsumo;
import com.adbrand.core.ia.repository.ConsumoTokenRepository;
import com.adbrand.core.ia.service.RegistroConsumoIa;
import com.adbrand.core.ia.service.ServicioIa;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

// HU 17: cada generación con IA guarda los tokens que gastó la empresa, sin conectarse a la IA ni a la base.
class ConsumoTokensIaTest {

    private ClienteIa cliente;
    private ConsumoTokenRepository consumos;

    @BeforeEach
    void preparar() {
        cliente = mock(ClienteIa.class);
        consumos = mock(ConsumoTokenRepository.class);
        when(consumos.save(any(ConsumoToken.class))).thenAnswer(invocacion -> invocacion.getArgument(0));
    }

    private ServicioIa servicio(boolean mock) {
        IaProperties props = new IaProperties("http://ia", "", "Qwen/Qwen3", "/chat", 5, mock);
        return new ServicioIa(cliente, props, new RegistroConsumoIa(consumos, props));
    }

    private static RespuestaChatIa respuesta(String texto, RespuestaChatIa.Uso uso) {
        return new RespuestaChatIa(List.of(new RespuestaChatIa.Opcion(new MensajeIa("assistant", texto))), uso);
    }

    private ConsumoToken consumoGuardado() {
        ArgumentCaptor<ConsumoToken> captor = ArgumentCaptor.forClass(ConsumoToken.class);
        verify(consumos).save(captor.capture());
        return captor.getValue();
    }

    @Test
    void guardaLosTokensQueMandaElServidor() {
        when(cliente.completarConUso("reglas", "pedido"))
                .thenReturn(respuesta("Anuncio listo", new RespuestaChatIa.Uso(120, 80, 200)));

        String texto = servicio(false).generarTexto("reglas", "pedido", 7L, OrigenConsumo.ANUNCIO);

        assertThat(texto).isEqualTo("Anuncio listo");
        ConsumoToken consumo = consumoGuardado();
        assertThat(consumo.getUsuarioId()).isEqualTo(7L);
        assertThat(consumo.getOrigen()).isEqualTo(OrigenConsumo.ANUNCIO);
        assertThat(consumo.getModelo()).isEqualTo("Qwen/Qwen3");
        assertThat(consumo.getTokensEntrada()).isEqualTo(120);
        assertThat(consumo.getTokensSalida()).isEqualTo(80);
        assertThat(consumo.getTokensTotal()).isEqualTo(200);
        assertThat(consumo.isSimulado()).isFalse();
    }

    @Test
    void siElServidorNoMandaUsageEstimaLosTokensYLosMarcaComoSimulados() {
        // 8 caracteres enviados ("reglas" + "ab") y 12 recibidos: 2 tokens de entrada y 3 de salida.
        when(cliente.completarConUso("reglas", "ab")).thenReturn(respuesta("Anuncio 1234", null));

        servicio(false).generarTexto("reglas", "ab", 7L, OrigenConsumo.REGENERAR_ANUNCIO);

        ConsumoToken consumo = consumoGuardado();
        assertThat(consumo.getTokensEntrada()).isEqualTo(2);
        assertThat(consumo.getTokensSalida()).isEqualTo(3);
        assertThat(consumo.getTokensTotal()).isEqualTo(5);
        assertThat(consumo.isSimulado()).isTrue();
    }

    @Test
    void enModoSimuladoTambienRegistraParaQueElPanelFuncioneEnCasa() {
        String texto = servicio(true).generarTexto("reglas", "Oferta a anunciar: 3x2 en panes\n", 3L,
                OrigenConsumo.KIT_MARCA);

        assertThat(texto).contains("3x2 en panes");
        verify(cliente, never()).completarConUso(anyString(), anyString());
        ConsumoToken consumo = consumoGuardado();
        assertThat(consumo.getUsuarioId()).isEqualTo(3L);
        assertThat(consumo.getOrigen()).isEqualTo(OrigenConsumo.KIT_MARCA);
        assertThat(consumo.getTokensTotal()).isPositive();
        assertThat(consumo.isSimulado()).isTrue();
    }

    @Test
    void siNoSePuedeGuardarElConsumoElTextoIgualSeDevuelve() {
        when(cliente.completarConUso("reglas", "pedido"))
                .thenReturn(respuesta("Anuncio listo", new RespuestaChatIa.Uso(10, 5, 15)));
        when(consumos.save(any(ConsumoToken.class))).thenThrow(new IllegalStateException("sin tabla"));

        String texto = servicio(false).generarTexto("reglas", "pedido", 7L, OrigenConsumo.ANUNCIO);

        assertThat(texto).isEqualTo("Anuncio listo");
    }

    @Test
    void sinEmpresaElMetodoDeSiempreNoRegistraNada() {
        when(cliente.completar("reglas", "pedido")).thenReturn("Anuncio listo");

        assertThat(servicio(false).generarTexto("reglas", "pedido")).isEqualTo("Anuncio listo");
        verify(consumos, never()).save(any(ConsumoToken.class));
    }
}
