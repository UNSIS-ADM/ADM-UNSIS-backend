package com.unsis.admunsisbackend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

/**
 * Configuración de seguridad a nivel de método.
 * Habilita la seguridad basada en anotaciones para métodos.
 */
@Configuration
@EnableMethodSecurity
public class MethodSecurityConfig {

}
