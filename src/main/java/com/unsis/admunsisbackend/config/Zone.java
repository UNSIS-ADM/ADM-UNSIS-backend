package com.unsis.admunsisbackend.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuración de zona horaria.
 */
@Configuration
public class Zone {
    // Configura el bean de Clock para usar la zona horaria del sistema
    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
}
