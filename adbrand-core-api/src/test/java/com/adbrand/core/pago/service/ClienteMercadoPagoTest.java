package com.adbrand.core.pago.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.adbrand.core.config.MercadoPagoProperties;
import com.adbrand.core.pago.dto.PagoMercadoPagoRequest;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

// HU 26: Mercado Pago con un servidor simulado. No se llama a Mercado Pago de verdad.
class ClienteMercadoPagoTest {

    private static final String URL = "https://api.mercadopago.com/v1/payments";

    private MockRestServiceServer servidor;
    private ClienteMercadoPago cliente;

    @BeforeEach
    void preparar() {
        RestClient.Builder builder = RestClient.builder().baseUrl(ClienteMercadoPago.API);
        servidor = MockRestServiceServer.bindTo(builder).build();
        // Estas pruebas usan la API antigua (Payments); las de Orders están en ClienteMercadoPagoOrdersTest.
        cliente = new ClienteMercadoPago(new MercadoPagoProperties("TEST-acceso", "TEST-publica", 30, "payments"),
                builder.build());
    }

    private PagoMercadoPagoRequest datos() {
        return new PagoMercadoPagoRequest(1L, "tok_123", "visa", "310", "ana@correo.pe", "APRO", "3704", null);
    }

    @Test
    void mandaElTokenConElMontoYLaClaveDeIdempotenciaYLeeElPagoAprobado() {
        servidor.expect(requestTo(URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer TEST-acceso"))
                .andExpect(header("X-Idempotency-Key", "clave-1"))
                .andExpect(jsonPath("$.transaction_amount").value(59.9))
                .andExpect(jsonPath("$.token").value("tok_123"))
                .andExpect(jsonPath("$.payment_method_id").value("visa"))
                .andExpect(jsonPath("$.issuer_id").value(310))
                .andExpect(jsonPath("$.installments").value(1))
                .andExpect(jsonPath("$.payer.email").value("ana@correo.pe"))
                .andRespond(withSuccess("""
                        {"id": 1234567890, "status": "approved", "status_detail": "accredited",
                         "card": {"last_four_digits": "3704", "cardholder": {"name": "APRO"}}}""",
                        MediaType.APPLICATION_JSON));

        CobroMercadoPago cobro = cliente.cobrar(new BigDecimal("59.90"), "AdBrand - plan Pro", datos(), "clave-1");

        assertThat(cobro.resultado().aprobado()).isTrue();
        assertThat(cobro.resultado().referencia()).isEqualTo("MP-1234567890");
        assertThat(cobro.titular()).isEqualTo("APRO");
        assertThat(cobro.ultimosDigitos()).isEqualTo("3704");
        servidor.verify();
    }

    @Test
    void unPagoRechazadoExplicaElMotivoEnPalabrasSencillas() {
        servidor.expect(requestTo(URL))
                .andRespond(withSuccess("""
                        {"id": 1, "status": "rejected", "status_detail": "cc_rejected_insufficient_amount"}""",
                        MediaType.APPLICATION_JSON));

        CobroMercadoPago cobro = cliente.cobrar(new BigDecimal("59.90"), "AdBrand - plan Pro", datos(), "clave-2");

        assertThat(cobro.resultado().aprobado()).isFalse();
        assertThat(cobro.resultado().motivo()).isEqualTo("La tarjeta no tiene fondos suficientes.");
    }

    @Test
    void unPagoEnRevisionNoActivaElPlan() {
        servidor.expect(requestTo(URL))
                .andRespond(withSuccess("""
                        {"id": 2, "status": "in_process", "status_detail": "pending_contingency"}""",
                        MediaType.APPLICATION_JSON));

        assertThat(cliente.cobrar(new BigDecimal("59.90"), "AdBrand", datos(), "clave-3").resultado().aprobado()).isFalse();
    }

    @Test
    void siMercadoPagoRespondeConErrorNoSeCobra() {
        servidor.expect(requestTo(URL))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST).contentType(MediaType.APPLICATION_JSON)
                        .body("{\"message\": \"invalid card token\"}"));

        CobroMercadoPago cobro = cliente.cobrar(new BigDecimal("59.90"), "AdBrand", datos(), "clave-4");

        assertThat(cobro.resultado().aprobado()).isFalse();
        assertThat(cobro.resultado().motivo()).contains("Revísalos");
    }

    @Test
    void conCredencialesQueMercadoPagoNoAceptaLoExplica() {
        servidor.expect(requestTo(URL))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED).contentType(MediaType.APPLICATION_JSON)
                        .body("{\"message\": \"Unauthorized use of live credentials\", \"status\": 401}"));

        CobroMercadoPago cobro = cliente.cobrar(new BigDecimal("59.90"), "AdBrand", datos(), "clave-5");

        assertThat(cobro.resultado().aprobado()).isFalse();
        assertThat(cobro.resultado().motivo()).contains("credenciales de prueba");
    }

    @Test
    void conLlavesTestSeActivaSinPreguntarYSinLlavesNo() {
        assertThat(cliente.disponible()).isTrue();
        assertThat(cliente.llavePublica()).isEqualTo("TEST-publica");

        ClienteMercadoPago sinLlaves = new ClienteMercadoPago(new MercadoPagoProperties("", "", 30, "orders"), RestClient.create());
        assertThat(sinLlaves.disponible()).isFalse();
    }

    private ClienteMercadoPago conLlavesAppUsr(String respuestaUsuario) {
        RestClient.Builder builder = RestClient.builder().baseUrl(ClienteMercadoPago.API);
        MockRestServiceServer servidorUsuario = MockRestServiceServer.bindTo(builder).build();
        servidorUsuario.expect(requestTo("https://api.mercadopago.com/users/me"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer APP_USR-acceso"))
                .andRespond(withSuccess(respuestaUsuario, MediaType.APPLICATION_JSON));
        return new ClienteMercadoPago(new MercadoPagoProperties("APP_USR-acceso", "APP_USR-publica", 30, "orders"), builder.build());
    }

    @Test
    void conLlavesAppUsrDeUnaCuentaDePruebaSeActiva() {
        ClienteMercadoPago prueba = conLlavesAppUsr("""
                {"id": 3869733072, "nickname": "TESTUSER7402080441231164898", "tags": ["normal", "test_user"]}""");

        assertThat(prueba.disponible()).isTrue();
        assertThat(prueba.llavePublica()).isEqualTo("APP_USR-publica");
    }

    @Test
    void conLlavesAppUsrDeUnaCuentaRealNoSeActivaParaNoCobrarDineroReal() {
        ClienteMercadoPago real = conLlavesAppUsr("""
                {"id": 123, "nickname": "BODEGASANMARTIN", "tags": ["normal"]}""");

        assertThat(real.disponible()).isFalse();
        assertThat(real.llavePublica()).isNull();
    }
}
