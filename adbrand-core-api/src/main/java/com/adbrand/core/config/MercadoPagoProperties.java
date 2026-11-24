package com.adbrand.core.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

// Llaves de Mercado Pago (HU 26). Se leen de application.properties, que a su vez las toma del .env.
// - accessToken (MP_ACCESS_TOKEN): secreta, solo la usa el servidor para cobrar.
// - publicKey (MP_PUBLIC_KEY): pública, la web la usa para convertir la tarjeta en un token.
// - api (MP_API): "orders" (por defecto, la API nueva) o "payments" (la antigua). Debe coincidir con el
//   "Tipo de API" que se eligió al crear la app en el panel de Mercado Pago.
// Si falta alguna llave, el pago con tarjeta sigue usando la pasarela simulada.
@ConfigurationProperties(prefix = "mercadopago")
public record MercadoPagoProperties(String accessToken, String publicKey, int timeoutSeconds, String api) {

    public boolean configurado() {
        return tieneTexto(accessToken) && tieneTexto(publicKey);
    }

    public boolean usaApiDeOrders() {
        return api == null || !"payments".equalsIgnoreCase(api.trim());
    }

    // Las llaves de prueba antiguas empiezan con TEST-. Las nuevas usan APP_USR- (como producción):
    // para esas, ClienteMercadoPago confirma con Mercado Pago que la cuenta sea de prueba.
    public boolean dePrueba() {
        return configurado() && accessToken.trim().startsWith("TEST-");
    }

    private static boolean tieneTexto(String valor) {
        return valor != null && !valor.isBlank();
    }
}
