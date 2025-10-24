package com.unsis.admunsisbackend.config;

import com.unsis.admunsisbackend.model.Role;
import com.unsis.admunsisbackend.model.User;
import com.unsis.admunsisbackend.repository.RoleRepository;
import com.unsis.admunsisbackend.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Collections;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner initSuperAdmin(UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder) {
        return args -> {
            if (userRepository.existsByUsername("administrador")) {
                System.out.println("ℹSuper Admin ya existe, no se creó de nuevo.");
                return;
            }

            // Asegurar que exista el rol ROLE_ADMIN
            Role adminRole = roleRepository.findByName("ROLE_ADMIN")
                    .orElseGet(() -> {
                        Role role = new Role();
                        role.setName("ROLE_ADMIN");
                        role.setDescription("Administrador del sistema");
                        return roleRepository.save(role);
                    });

            // Crear super admin
            User superAdmin = new User();
            superAdmin.setUsername("administrador");
            superAdmin.setPassword(passwordEncoder.encode("AMD-ASPIRNTES")); // Pass encriptada
            superAdmin.setFullName("Administrador del sistema");
            superAdmin.setRoles(Collections.singleton(adminRole));

            userRepository.save(superAdmin);

            System.out.println("Super Admin creado exitosamente.");
        };
    }
}
