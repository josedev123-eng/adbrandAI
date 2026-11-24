package com.adbrand.core.pago.service;

import com.adbrand.core.config.MercadoPagoProperties;
import com.adbrand.core.pago.dto.PagoMercadoPagoRequest;
import com.adbrand.core.pago.dto.PagoYapeMercadoPagoRequest;
import java.math.BigDecimal;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

// HU 26: cobra con Mercado Pago (Checkout API). La web manda el token de la tarjeta y aquí se crea el pago.
// Docs: POST https://api.mercadopago.com/v1/payments con el Access Token y una X-Idempotency-Key,
// para que un reintento no cobre dos veces. Responde status "approved", "rejected", "in_process"...
@Component
public class ClienteMercadoPago {

    static final String API = "https://api.mercadopago.com";
    private static final Logger LOG = LoggerFactory.getLogger(ClienteMercadoPago.class);

    private final MercadoPagoProperties propiedades;
    private final RestClient cliente;
    private final ObjectMapper lector = new ObjectMapper();

    @Autowired
    public ClienteMercadoPago(MercadoPagoProperties propiedades) {
        this(propiedades, crearCliente(propiedades));
    }

    // Las pruebas pasan su propio RestClient (con un servidor simulado).
    ClienteMercadoPago(MercadoPagoProperties propiedades, RestClient cliente) {
        this.propiedades = propiedades;
        this.cliente = cliente;
    }

    private static RestClient crearCliente(MercadoPagoProperties propiedades) {
        HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
        JdkClientHttpRequestFactory fabrica = new JdkClientHttpRequestFactory(http);
        fabrica.setReadTimeout(Duration.ofSeconds(Math.max(propiedades.timeoutSeconds(), 10)));
        return RestClient.builder().baseUrl(API).requestFactory(fabrica).build();
    }

    // Por seguridad del proyecto solo se usan credenciales de prueba: nunca se cobra dinero real.
    // Las antiguas empiezan con TEST-. Las nuevas de la pestaña "Prueba" empiezan con APP_USR-, igual que
    // las de producción; en ese caso se pregunta a Mercado Pago (GET /users/me) si la cuenta es de prueba.
    public boolean disponible() {
        if (!propiedades.configurado()) return false;
        if (propiedades.dePrueba()) return true;
        if (cuentaDePrueba == null) cuentaDePrueba = consultarSiEsCuentaDePrueba();
        return Boolean.TRUE.equals(cuentaDePrueba);
    }

    // null = todavía no se sabe (o Mercado Pago no respondió y se vuelve a preguntar la próxima vez).
    private volatile Boolean cuentaDePrueba;

    private Boolean consultarSiEsCuentaDePrueba() {
        try {
            return cliente.get()
                    .uri("/users/me")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + propiedades.accessToken().trim())
                    .exchange((solicitud, respuesta) -> {
                        String texto = new String(respuesta.getBody().readAllBytes(), StandardCharsets.UTF_8);
                        if (respuesta.getStatusCode().isError()) {
                            LOG.warn("Mercado Pago no confirmó la cuenta ({}).", respuesta.getStatusCode().value());
                            return respuesta.getStatusCode().value() == 401 ? Boolean.FALSE : null;
                        }
                        JsonNode usuario = lector.readTree(texto);
                        boolean esPrueba = esCuentaDePrueba(usuario);
                        // Sin datos secretos: solo qué cuenta vendedora quedó conectada.
                        LOG.info("Mercado Pago conectado a la cuenta {} ({}), site {}, de prueba: {}",
                                usuario.path("id").asString(""), usuario.path("nickname").asString(""),
                                usuario.path("site_id").asString(""), esPrueba);
                        if (!esPrueba) {
                            LOG.warn("Las credenciales de Mercado Pago son de una cuenta real: no se usan para no cobrar dinero real.");
                        }
                        return esPrueba;
                    });
        } catch (RestClientException ex) {
            LOG.warn("No se pudo consultar la cuenta de Mercado Pago: {}", ex.getMessage());
            return null;
        }
    }

    // Las cuentas de prueba de Mercado Pago tienen la etiqueta "test_user" y un apodo que empieza con TESTUSER.
    static boolean esCuentaDePrueba(JsonNode usuario) {
        for (JsonNode etiqueta : usuario.path("tags")) {
            if ("test_user".equals(etiqueta.asString(""))) return true;
        }
        return usuario.path("nickname").asString("").toUpperCase().startsWith("TESTUSER");
    }

    public String llavePublica() {
        return disponible() ? propiedades.publicKey().trim() : null;
    }

    public CobroMercadoPago cobrar(BigDecimal monto, String descripcion, PagoMercadoPagoRequest datos, String claveIdempotencia) {
        return propiedades.usaApiDeOrders()
                ? cobrarConOrders(monto, descripcion, datos, claveIdempotencia)
                : cobrarConPayments(monto, descripcion, datos, claveIdempotencia);
    }

    // API de Orders (la nueva): POST /v1/orders. Las apps creadas con "API de Orders" solo aceptan esta.
    // Los montos van como texto ("59.90") y total_amount debe ser igual a la suma de los pagos.
    private CobroMercadoPago cobrarConOrders(BigDecimal monto, String descripcion, PagoMercadoPagoRequest datos,
                                             String claveIdempotencia) {
        String montoTexto = monto.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
        Map<String, Object> metodo = new LinkedHashMap<>();
        metodo.put("id", datos.metodoPago());
        metodo.put("type", tipoDeTarjeta(datos.tipoTarjeta()));
        metodo.put("token", datos.token());
        metodo.put("installments", 1);

        Map<String, Object> pedido = new LinkedHashMap<>();
        pedido.put("type", "online");
        pedido.put("processing_mode", "automatic");
        pedido.put("total_amount", montoTexto);
        pedido.put("external_reference", claveIdempotencia.length() > 64 ? claveIdempotencia.substring(0, 64) : claveIdempotencia);
        pedido.put("description", descripcion);
        pedido.put("payer", Map.of("email", datos.correo().trim()));
        pedido.put("transactions", Map.of("payments", java.util.List.of(Map.of("amount", montoTexto, "payment_method", metodo))));

        return enviar("/v1/orders", pedido, claveIdempotencia, true);
    }

    // HU 27: Yape solo existe en la API de Orders. Mercado Pago lo trata como "debit_card" con id "yape",
    // y pide el celular del pagador (código de país 51).
    public CobroMercadoPago cobrarConYape(BigDecimal monto, String descripcion, PagoYapeMercadoPagoRequest datos,
                                          String claveIdempotencia) {
        String montoTexto = monto.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
        Map<String, Object> metodo = new LinkedHashMap<>();
        metodo.put("id", "yape");
        metodo.put("type", "debit_card");
        metodo.put("token", datos.token());

        Map<String, Object> pagador = new LinkedHashMap<>();
        pagador.put("email", datos.correo().trim());
        pagador.put("phone", Map.of("area_code", "51", "number", datos.celular()));

        Map<String, Object> pedido = new LinkedHashMap<>();
        pedido.put("type", "online");
        pedido.put("processing_mode", "automatic");
        pedido.put("total_amount", montoTexto);
        pedido.put("external_reference", claveIdempotencia.length() > 64 ? claveIdempotencia.substring(0, 64) : claveIdempotencia);
        pedido.put("description", descripcion);
        pedido.put("payer", pagador);
        pedido.put("transactions", Map.of("payments", java.util.List.of(Map.of("amount", montoTexto, "payment_method", metodo))));

        return enviar("/v1/orders", pedido, claveIdempotencia, true);
    }

    static String tipoDeTarjeta(String tipo) {
        return tipo != null && tipo.matches("credit_card|debit_card|prepaid_card") ? tipo : "credit_card";
    }

    // API de Payments (la antigua): POST /v1/payments.
    private CobroMercadoPago cobrarConPayments(BigDecimal monto, String descripcion, PagoMercadoPagoRequest datos,
                                               String claveIdempotencia) {
        Map<String, Object> pedido = new LinkedHashMap<>();
        pedido.put("transaction_amount", monto);
        pedido.put("token", datos.token());
        pedido.put("description", descripcion);
        pedido.put("installments", 1);
        pedido.put("payment_method_id", datos.metodoPago());
        if (datos.emisorId() != null && !datos.emisorId().isBlank()) {
            pedido.put("issuer_id", Long.parseLong(datos.emisorId()));
        }
        pedido.put("payer", Map.of("email", datos.correo().trim()));

        return enviar("/v1/payments", pedido, claveIdempotencia, false);
    }

    private CobroMercadoPago enviar(String ruta, Map<String, Object> pedido, String claveIdempotencia, boolean orders) {
        try {
            return cliente.post()
                    .uri(ruta)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + propiedades.accessToken().trim())
                    .header("X-Idempotency-Key", claveIdempotencia)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(pedido)
                    .exchange((solicitud, respuesta) -> {
                        String texto = new String(respuesta.getBody().readAllBytes(), StandardCharsets.UTF_8);
                        int codigo = respuesta.getStatusCode().value();
                        // Orders responde 402 cuando la orden se creó pero el pago fue rechazado: trae el detalle.
                        if (orders && codigo == 402) {
                            return leerOrden(lector.readTree(texto));
                        }
                        if (respuesta.getStatusCode().isError()) {
                            LOG.warn("Mercado Pago respondió {}: {}", codigo, texto);
                            return CobroMercadoPago.sinTarjeta(ResultadoCobro.rechazado(motivoDeError(codigo, texto)));
                        }
                        JsonNode cuerpo = lector.readTree(texto);
                        return orders ? leerOrden(cuerpo) : leerPago(cuerpo);
                    });
        } catch (RestClientException ex) {
            LOG.warn("No se pudo conectar con Mercado Pago: {}", ex.getMessage());
            return CobroMercadoPago.sinTarjeta(
                    ResultadoCobro.rechazado("Mercado Pago no responde en este momento. Intenta en unos minutos."));
        }
    }

    // Orden aprobada: status "processed" con el pago "accredited". El titular y los dígitos no vienen aquí:
    // la web los manda desde el formulario de Mercado Pago (additionalData).
    private CobroMercadoPago leerOrden(JsonNode orden) {
        JsonNode pago = orden.path("transactions").path("payments").path(0);
        String estadoOrden = orden.path("status").asString("");
        String estadoPago = pago.path("status").asString(estadoOrden);
        String detalle = pago.path("status_detail").asString(orden.path("status_detail").asString(""));
        ResultadoCobro resultado;
        if ("processed".equals(estadoOrden) && ("processed".equals(estadoPago) || "accredited".equals(detalle))) {
            resultado = ResultadoCobro.aprobado("MP-" + orden.path("id").asString(""));
        } else if ("action_required".equals(estadoOrden) || "in_process".equals(estadoPago) || "pending".equals(estadoPago)) {
            resultado = ResultadoCobro.rechazado(
                    "Mercado Pago dejó el pago en revisión y no se activó tu plan. Intenta con otra tarjeta.");
        } else {
            LOG.info("Mercado Pago rechazó la orden: estado {}, pago {}, detalle {}", estadoOrden, estadoPago, detalle);
            resultado = ResultadoCobro.rechazado(motivoDeRechazo(detalle));
        }
        return CobroMercadoPago.sinTarjeta(resultado);
    }

    private CobroMercadoPago leerPago(JsonNode pago) {
        String estado = pago.path("status").asString("");
        ResultadoCobro resultado;
        if ("approved".equals(estado)) {
            resultado = ResultadoCobro.aprobado("MP-" + pago.path("id").asString(""));
        } else if ("in_process".equals(estado) || "pending".equals(estado)) {
            resultado = ResultadoCobro.rechazado(
                    "Mercado Pago dejó el pago en revisión y no se activó tu plan. Intenta con otra tarjeta.");
        } else {
            resultado = ResultadoCobro.rechazado(motivoDeRechazo(pago.path("status_detail").asString("")));
        }
        JsonNode tarjeta = pago.path("card");
        String titular = tarjeta.path("cardholder").path("name").asString("").trim();
        String digitos = tarjeta.path("last_four_digits").asString("");
        return new CobroMercadoPago(resultado,
                titular.isEmpty() ? null : titular,
                digitos.matches("[0-9]{4}") ? digitos : null);
    }

    // Errores HTTP de Mercado Pago. "live credentials": con credenciales de prueba nuevas (APP_USR-),
    // el comprador también tiene que ser una cuenta de prueba de Mercado Pago.
    static String motivoDeError(int estado, String cuerpo) {
        if (estado == 401 && cuerpo != null && cuerpo.contains("live credentials")) {
            return "Mercado Pago no aceptó las credenciales de prueba. Revisa que sean de la misma app y que el tipo"
                    + " de API (MP_API) coincida con el de la app.";
        }
        if (estado == 401) {
            return "El pago con tarjeta no está bien configurado. Avisa al equipo de AdBrand.";
        }
        return "Mercado Pago no aceptó los datos de la tarjeta. Revísalos e intenta de nuevo.";
    }

    // Los motivos más comunes de rechazo, en palabras sencillas.
    static String motivoDeRechazo(String detalle) {
        // La API de Payments usa "cc_rejected_..."; la de Orders, los mismos nombres sin ese prefijo.
        String clave = detalle == null ? "" : detalle.replaceFirst("^cc_rejected_", "");
        return switch (clave) {
            case "insufficient_amount" -> "La tarjeta no tiene fondos suficientes.";
            case "bad_filled_security_code" -> "El código de seguridad (CVV) no es correcto.";
            case "bad_filled_date" -> "La fecha de vencimiento no es correcta.";
            case "bad_filled_card_number", "bad_filled_other" -> "Revisa los datos de la tarjeta e intenta de nuevo.";
            case "call_for_authorize" -> "Tu banco pide que autorices este pago. Llama a tu banco o usa otra tarjeta.";
            case "card_disabled" -> "La tarjeta está desactivada. Actívala con tu banco o usa otra.";
            case "duplicated_payment" -> "Ya hiciste un pago igual hace un momento.";
            case "high_risk" -> "El pago fue rechazado por seguridad. Usa otra tarjeta.";
            // Rechazos de Yape (API de Orders).
            case "required_call_for_authorize" -> "Tu banco pide que autorices este pago. Revisa tu app o usa otro medio.";
            case "invalid_card_token" -> "El código de aprobación ya no es válido. Genera uno nuevo en Yape.";
            case "max_attempts_exceeded" -> "Superaste los intentos permitidos. Espera un momento e intenta de nuevo.";
            case "bad_filled_card_data" -> "Revisa tu número de celular y el código de aprobación.";
            case "processing_error" -> "Mercado Pago no pudo procesar el pago. Intenta de nuevo.";
            default -> "La tarjeta fue rechazada. Usa otra tarjeta.";
        };
    }
}
