package com.adbrand.core.auth.config;

import com.adbrand.core.auth.service.ServicioJwt;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

// Configuración de las cuentas de usuario PYME (HU 21 y 22).
@Configuration
public class SeguridadConfig {

    // BCrypt guarda la contraseña con su propia sal: dos cuentas con la misma contraseña tienen hashes distintos.
    @Bean
    public PasswordEncoder codificadorContrasenas() {
        return new BCryptPasswordEncoder();
    }

    // HU 22: el filtro del token, justo después del de CORS (CorsConfig), solo para las rutas /api/*.
    @Bean
    public FilterRegistrationBean<FiltroJwt> filtroJwt(ServicioJwt jwt) {
        FilterRegistrationBean<FiltroJwt> registro = new FilterRegistrationBean<>(new FiltroJwt(jwt));
        registro.addUrlPatterns("/api/*");
        registro.setOrder(Ordered.HIGHEST_PRECEDENCE + 10);
        return registro;
    }
}
