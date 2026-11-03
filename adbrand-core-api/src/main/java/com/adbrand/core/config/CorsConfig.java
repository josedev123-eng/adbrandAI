package com.adbrand.core.config;

import java.util.List;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

// Permite que la web del usuario (Vite, puerto 5173) llame a esta API.
// HU 22: es un filtro con el orden más alto, para que corra antes que FiltroJwt. Así el 401 del token
// también lleva las cabeceras CORS y el navegador muestra el error en vez de "Failed to fetch".
@Configuration
public class CorsConfig {

    @Bean
    public FilterRegistrationBean<CorsFilter> filtroCors() {
        CorsConfiguration reglas = new CorsConfiguration();
        reglas.setAllowedOrigins(List.of("http://localhost:5173"));
        reglas.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        reglas.setAllowedHeaders(List.of("Content-Type", "Authorization"));
        reglas.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource fuente = new UrlBasedCorsConfigurationSource();
        fuente.registerCorsConfiguration("/api/**", reglas);

        FilterRegistrationBean<CorsFilter> registro = new FilterRegistrationBean<>(new CorsFilter(fuente));
        registro.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registro;
    }
}
