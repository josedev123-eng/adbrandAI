package com.adbrand.core.auth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

// Configuración de las cuentas de usuario PYME (HU 21).
@Configuration
public class SeguridadConfig {

    // BCrypt guarda la contraseña con su propia sal: dos cuentas con la misma contraseña tienen hashes distintos.
    @Bean
    public PasswordEncoder codificadorContrasenas() {
        return new BCryptPasswordEncoder();
    }
}
