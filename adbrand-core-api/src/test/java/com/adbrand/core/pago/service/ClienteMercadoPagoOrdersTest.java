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
import com.adbrand.core.pago.dto.PagoYapeMercadoPagoRequest;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

// HU 26 con la API de Orders de Mercado Pago (la nueva), con un servidor simulado.
class ClienteMercadoPagoOrdersTest {

    private static final String URL = "https://api.mercadopago.com/v1/orders";

    private MockRestServiceServer servidor;
    private ClienteMercadoPago cliente;

    @BeforeEach
    void preparar() {
        RestClient.Builder builder = RestClient.builder().baseUrl(ClienteMercadoPago.API);
        servidor = MockRestServiceServer.bindTo(builder).build();
        cliente = new ClienteMercadoPago(new MercadoPagoProperties("TEST-acceso", "TEST-publica", 30, "orders"),
                builder.build());
    }

    private PagoMercadoPagoRequest datos(String tipoTarjeta) {
        return new PagoMercadoPagoRequest(1L, "tok_123", "master", "", "comprador@testuser.com", "APRO", "0604", tipoTarjeta);
    }

    @Test
    void creaLaOrdenConLosMontosComoTextoYLeeElPagoAprobado() {
        servidor.expect(requestTo(URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer TEST-acceso"))
                .andExpect(header("X-Idempotency-Key", "clave-1"))
                .andExpect(jsonPath("$.type").value("online"))
                .andExpect(jsonPath("$.processing_mode").value("automatic"))
                .andExpect(jsonPath("$.total_amount").value("59.90"))
                .andExpect(jsonPath("$.payer.email").value("comprador@testuser.com"))
                .andExpect(jsonPath("$.transactions.payments[0].amount").value("59.90"))
                .andExpect(jsonPath("$.transactions.payments[0].payment_method.id").value("master"))
                .andExpect(jsonPath("$.transactions.payments[0].payment_method.type").value("debit_card"))
                .andExpect(jsonPath("$.transactions.payments[0].payment_method.token").value("tok_123"))
                .andExpect(jsonPath("$.transactions.payments[0].payment_method.installments").value(1))
                .andRespond(withStatus(HttpStatus.CREATED).contentType(MediaType.APPLICATION_JSON).body("""
                        {"id": "ORD01ABC", "status": "processed", "status_detail": "accredited",
                         "transactions": {"payments": [{"id": "PAY01XYZ", "status": "processed",
                                                         "status_detail": "accredited"}]}}"""));

        CobroMercadoPago cobro = cliente.cobrar(new BigDecimal("59.9"), "AdBrand - plan Pro", datos("debit_card"), "clave-1");

        assertThat(cobro.resultado().aprobado()).isTrue();
        assertThat(cobro.resultado().referencia()).isEqualTo("MP-ORD01ABC");
        servidor.verify();
    }

    @Test
    void sinTipoDeTarjetaSeUsaCredito() {
        servidor.expect(requestTo(URL))
                .andExpect(jsonPath("$.transactions.payments[0].payment_method.type").value("credit_card"))
                .andRespond(withSuccess("""
                        {"id": "ORD02", "status": "processed", "status_detail": "accredited",
                         "transactions": {"payments": [{"status": "processed", "status_detail": "accredited"}]}}""",
                        MediaType.APPLICATION_JSON));

        assertThat(cliente.cobrar(new BigDecimal("29.90"), "AdBrand", datos(null), "clave-2").resultado().aprobado()).isTrue();
    }

    @Test
    void yapeSeCobraConSuTokenComoDebitoYConElCelularDelPagador() {
        servidor.expect(requestTo(URL))
                .andExpect(header("X-Idempotency-Key", "clave-y"))
                .andExpect(jsonPath("$.total_amount").value("29.90"))
                .andExpect(jsonPath("$.payer.email").value("comprador@testuser.com"))
                .andExpect(jsonPath("$.payer.phone.area_code").value("51"))
                .andExpect(jsonPath("$.payer.phone.number").value("111111111"))
                .andExpect(jsonPath("$.transactions.payments[0].amount").value("29.90"))
                .andExpect(jsonPath("$.transactions.payments[0].payment_method.id").value("yape"))
                .andExpect(jsonPath("$.transactions.payments[0].payment_method.type").value("debit_card"))
                .andExpect(jsonPath("$.transactions.payments[0].payment_method.token").value("yape_tok"))
                .andRespond(withStatus(HttpStatus.CREATED).contentType(MediaType.APPLICATION_JSON).body("""
                        {"id": "ORDYAPE1", "status": "processed", "status_detail": "accredited",
                         "transactions": {"payments": [{"status": "processed", "status_detail": "accredited"}]}}"""));

        CobroMercadoPago cobro = cliente.cobrarConYape(new BigDecimal("29.9"), "AdBrand - plan Básico",
                new PagoYapeMercadoPagoRequest(1L, "yape_tok", "111111111", "comprador@testuser.com"), "clave-y");

        assertThat(cobro.resultado().aprobado()).isTrue();
        assertThat(cobro.resultado().referencia()).isEqualTo("MP-ORDYAPE1");
        servidor.verify();
    }

    @Test
    void unYapeConCodigoVencidoExplicaQueGenereOtro() {
        servidor.expect(requestTo(URL))
                .andRespond(withStatus(HttpStatus.PAYMENT_REQUIRED).contentType(MediaType.APPLICATION_JSON).body("""
                        {"id": "ORDYAPE2", "status": "failed",
                         "transactions": {"payments": [{"status": "failed", "status_detail": "invalid_card_token"}]}}"""));

        CobroMercadoPago cobro = cliente.cobrarConYape(new BigDecimal("29.90"), "AdBrand",
                new PagoYapeMercadoPagoRequest(1L, "yape_tok", "111111115", "comprador@testuser.com"), "clave-y2");

        assertThat(cobro.resultado().aprobado()).isFalse();
        assertThat(cobro.resultado().motivo()).contains("Genera uno nuevo en Yape");
    }

    @Test
    void unPagoRechazadoLlegaComo402YSeExplicaElMotivo() {
        servidor.expect(requestTo(URL))
                .andRespond(withStatus(HttpStatus.PAYMENT_REQUIRED).contentType(MediaType.APPLICATION_JSON).body("""
                        {"id": "ORD03", "status": "failed", "status_detail": "failed",
                         "transactions": {"payments": [{"status": "failed", "status_detail": "insufficient_amount"}]}}"""));

        CobroMercadoPago cobro = cliente.cobrar(new BigDecimal("59.90"), "AdBrand", datos("credit_card"), "clave-3");

        assertThat(cobro.resultado().aprobado()).isFalse();
        assertThat(cobro.resultado().motivo()).isEqualTo("La tarjeta no tiene fondos suficientes.");
    }
}
