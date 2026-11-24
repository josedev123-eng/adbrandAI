package com.adbrand.core.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

// Activa las llaves de Mercado Pago (HU 26).
@Configuration
@EnableConfigurationProperties(MercadoPagoProperties.class)
public class PagoConfig {
}
