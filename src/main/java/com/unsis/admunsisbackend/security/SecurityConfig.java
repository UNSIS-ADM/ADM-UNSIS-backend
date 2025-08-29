package com.unsis.admunsisbackend.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.Collections;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {
    @Autowired
    private RoleAccessRestrictionFilter roleAccessRestrictionFilter;

    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())// Desactivar CSRF para pruebas con Postman
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // 1 .requestMatchers("/auth/**").permitAll()
                        // 5 .requestMatchers("/api/admin/**").hasAuthority("ROLE_ADMIN")
                        // 6 .requestMatchers("/api/user/**") .hasAuthority("ROLE_USER")
                        // 7 .requestMatchers("/api/applicant/**").hasAuthority("ROLE_APPLICANT")
                        // 2 .requestMatchers("/api/admin/upload-results").hasAuthority("ROLE_ADMIN")
                        // 3 .requestMatchers("/api/admin/results").hasAnyAuthority("ROLE_ADMIN","ROLE_USER")
                        // 4 .requestMatchers("/api/admin/access-restriction/**").hasRole("ADMIN")

                        .requestMatchers("/auth/**").permitAll()
                        // Reglas específicas primero
                        .requestMatchers("/api/admin/upload-results").hasAuthority("ROLE_ADMIN")
                        .requestMatchers("/api/admin/access-restriction/**").hasRole("ADMIN")

                        .requestMatchers("/api/admin/results").hasAnyAuthority("ROLE_ADMIN", "ROLE_USER")
                        .requestMatchers("/api/admin/change-career/requests").hasAnyRole("ADMIN", "USER")

                        // Regla general después, más permisiva si corresponde
                        .requestMatchers("/api/admin/**").hasAnyAuthority("ROLE_ADMIN", "ROLE_USER")
                        .requestMatchers("/api/user/**").hasAuthority("ROLE_USER")
                        .requestMatchers("/api/applicant/**").hasAuthority("ROLE_APPLICANT")
                        .anyRequest().authenticated());
        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        http.addFilterAfter(roleAccessRestrictionFilter, JwtAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Collections.singletonList("http://localhost:4200"));
        // configuration.setAllowedOrigins(Collections.singletonList("*")); // Cambia
        // esto por tu URL de frontend
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList(
                "Authorization",
                "Content-Type",
                "X-Requested-With",
                "Accept",
                "Origin",
                "Access-Control-Request-Method",
                "Access-Control-Request-Headers"));
        configuration.setExposedHeaders(Arrays.asList(
                "Access-Control-Allow-Origin",
                "Access-Control-Allow-Credentials"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    // PasswordEncoder para contraseñas en la BD
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
