package com.adbrand.core.notificacion.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

// HU 49 y 52: habilita las tareas diarias de recordatorios (un solo punto para
// toda la aplicación, aunque cada programador viva en su servicio).
@Configuration
@EnableScheduling
public class ProgramacionConfig {
}
