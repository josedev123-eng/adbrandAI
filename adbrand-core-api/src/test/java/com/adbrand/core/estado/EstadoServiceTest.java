package com.adbrand.core.estado;

import static org.assertj.core.api.Assertions.assertThat;

import com.adbrand.core.config.IaProperties;
import com.adbrand.core.estado.dto.EstadoIa;
import com.adbrand.core.estado.service.EstadoService;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import org.junit.jupiter.api.Test;

// HU 18: revisa el estado de la IA sin conectarse al servidor del instituto.
class EstadoServiceTest {

    private static EstadoService servicio(String baseUrl, boolean mock) {
        return new EstadoService(new IaProperties(baseUrl, "clave-secreta", "modelo", "/chat", 5, mock));
    }

    @Test
    void enModoSimuladoLaIaEstaActiva() {
        EstadoIa ia = servicio("", true).verificarIa();

        assertThat(ia.estado()).isEqualTo("ACTIVO");
        assertThat(ia.detalle()).isEqualTo("Modo simulado");
    }

    @Test
    void siElServidorDeIaContestaEstaActivoAunqueResponda404() throws Exception {
        // Un servidor HTTP de mentira en esta PC que responde 404 a todo.
        HttpServer servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        servidor.createContext("/", intercambio -> {
            intercambio.sendResponseHeaders(404, -1);
            intercambio.close();
        });
        servidor.start();
        try {
            EstadoIa ia = servicio("http://127.0.0.1:" + servidor.getAddress().getPort(), false).verificarIa();

            assertThat(ia.estado()).isEqualTo("ACTIVO");
            assertThat(ia.tiempoMs()).isNotNull();
        } finally {
            servidor.stop(0);
        }
    }

    @Test
    void siNadieContestaLaIaQuedaCaidaConElMotivoYSinLaClave() throws Exception {
        int puertoLibre;
        try (ServerSocket socket = new ServerSocket(0)) {
            puertoLibre = socket.getLocalPort();
        }

        EstadoIa ia = servicio("http://127.0.0.1:" + puertoLibre, false).verificarIa();

        assertThat(ia.estado()).isEqualTo("CAIDO");
        assertThat(ia.detalle()).startsWith("No se pudo conectar con la IA").doesNotContain("clave-secreta");
    }

    @Test
    void sinDireccionDeIaQuedaCaida() {
        EstadoIa ia = servicio("", false).verificarIa();

        assertThat(ia.estado()).isEqualTo("CAIDO");
        assertThat(ia.detalle()).contains("AI_BASE_URL");
    }

    @Test
    void elBackendSiempreRespondeActivo() {
        assertThat(servicio("", true).verificar().backend()).isEqualTo("ACTIVO");
    }
}
