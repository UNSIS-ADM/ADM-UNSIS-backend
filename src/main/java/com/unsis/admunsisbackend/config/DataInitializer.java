package com.unsis.admunsisbackend.config;

import com.unsis.admunsisbackend.model.Role;
import com.unsis.admunsisbackend.model.User;
import com.unsis.admunsisbackend.repository.RoleRepository;
import com.unsis.admunsisbackend.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.util.Collections;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner initData(UserRepository userRepository,
                               RoleRepository roleRepository,
                               PasswordEncoder passwordEncoder,
                               JdbcTemplate jdbcTemplate) { // Inyectamos JdbcTemplate para el SQL de contenidos
        return args -> {
            
            // --- 1. ROLES ---
            Role adminRole = roleRepository.findByName("ROLE_ADMIN")
                    .orElseGet(() -> {
                        Role r = new Role();
                        r.setName("ROLE_ADMIN");
                        r.setDescription("Administrador del sistema");
                        return roleRepository.save(r);
                    });

            Role userRole = roleRepository.findByName("ROLE_USER")
                    .orElseGet(() -> {
                        Role r = new Role();
                        r.setName("ROLE_USER");
                        r.setDescription("Secretaria / Personal administrativo");
                        return roleRepository.save(r);
                    });

            Role applicantRole = roleRepository.findByName("ROLE_APPLICANT")
                    .orElseGet(() -> {
                        Role r = new Role();
                        r.setName("ROLE_APPLICANT");
                        r.setDescription("Aspirante / Alumno");
                        return roleRepository.save(r);
                    });

            // --- 2. USUARIOS ---
            if (!userRepository.existsByUsername("administrador")) {
                User admin = new User();
                admin.setUsername("administrador");
                admin.setPassword(passwordEncoder.encode("ADM-ASPIRANTES"));
                admin.setFullName("Administrador del Sistema");
                admin.setRoles(Collections.singleton(adminRole));
                userRepository.save(admin);
            }

            if (!userRepository.existsByUsername("secretaria")) {
                User secretaria = new User();
                secretaria.setUsername("secretaria");
                secretaria.setPassword(passwordEncoder.encode("SEC-UNSIS2026"));
                secretaria.setFullName("Secretaria Académica");
                secretaria.setRoles(Collections.singleton(userRole));
                userRepository.save(secretaria);
            }

            // --- 3. SEED DE CONTENIDOS (SQL) ---
            System.out.println("⏳ Inicializando contenidos de mensajes (Aceptado/Reprobado)...");

            // Insertar Contents
            jdbcTemplate.execute("INSERT IGNORE INTO contents (key_name, title, language, active) VALUES " +
                    "('Mensaje_aceptado', 'Mensaje Aceptado 2025', 'es', true), " +
                    "('Mensaje_reprobado', 'Mensaje Reprobado 2025', 'es', true)");

            // Obtener IDs
            Integer idAceptado = jdbcTemplate.queryForObject("SELECT id FROM contents WHERE key_name='Mensaje_aceptado' LIMIT 1", Integer.class);
            Integer idReprobado = jdbcTemplate.queryForObject("SELECT id FROM contents WHERE key_name='Mensaje_reprobado' LIMIT 1", Integer.class);

            // Limpiar partes previas para evitar duplicados al reiniciar (opcional pero recomendado)
            jdbcTemplate.update("DELETE FROM content_parts WHERE content_id IN (?, ?)", idAceptado, idReprobado);

            // Insertar Partes Mensaje Aceptado
            String sqlAceptado = "INSERT INTO content_parts (content_id, part_key, title, html_content, order_index) VALUES (?, ?, ?, ?, ?)";
            jdbcTemplate.update(sqlAceptado, idAceptado, "greeting", "Aceptado", "<p class=\"text-center font-semibold text-[#14645c]\">¡Felicidades!</p>", 0);
            jdbcTemplate.update(sqlAceptado, idAceptado, "welcome_note", "Bienvenida", "<p>Estimado aspirante, le damos la bienvenida al curso propedéutico 2025.</p>", 1);
            jdbcTemplate.update(sqlAceptado, idAceptado, "inscription_dates", "Fechas e inscripciones", "<p>Se informa que las inscripciones serán del <span class=\"font-semibold\">14 al 25 de julio de 2025</span> de forma presencial en el Departamento de Servicios Escolares de la Universidad de la Sierra Sur...</p>", 2);
            jdbcTemplate.update(sqlAceptado, idAceptado, "survey", "Encuesta", "<p>1. Ingrese en el siguiente enlace: <a href=\"https://survey.unsis.edu.mx/index.php/32638?lang=es-MX\" class=\"text-blue-700 underline\" target=\"_blank\">...</a></p>", 3);
            jdbcTemplate.update(sqlAceptado, idAceptado, "documents_list", "Documentos requeridos", "<p>2. Acudir al Departamento de Servicios Escolares de la UNSIS...</p>", 4);
            jdbcTemplate.update(sqlAceptado, idAceptado, "note", "Nota", "<p class=\"text-red-700 font-semibold\">NOTA: El acuse del estudio socioeconómico realizado...</p>", 5);
            jdbcTemplate.update(sqlAceptado, idAceptado, "start_date", "Inicio curso", "<p>El curso propedéutico inicia el <span class=\"font-semibold\">28 de julio de 2025</span>...</p>", 6);
            jdbcTemplate.update(sqlAceptado, idAceptado, "contact", "Contacto", "<p>En caso de dudas llamar al teléfono <span class=\"font-semibold\">9515724100 EXt. 1203, 1204</span>...</p>", 7);

            // Insertar Partes Mensaje Reprobado
            String sqlReprobado = "INSERT INTO content_parts (content_id, part_key, title, html_content, order_index) VALUES (?, ?, ?, ?, ?)";
            jdbcTemplate.update(sqlReprobado, idReprobado, "header", "Rechazado", "<p class=\"text-center font-semibold text-[#6a1b1b]\">Oportunidad de continuar</p>", 0);
            jdbcTemplate.update(sqlReprobado, idReprobado, "body", "Bienvenida", "<p>Estimado aspirante, desafortunadamente no puedes inscribirte a la licenciatura en medicina...</p>", 1);
            jdbcTemplate.update(sqlReprobado, idReprobado, "suggested_programs", "Carreras dinámicas", "%CARRERAS_LIST%", 2);
            jdbcTemplate.update(sqlReprobado, idReprobado, "suneo_options", "Opciones SUNEO", "<p>Si deseas ser parte de la comunidad SUNEO, tenemos las siguientes opciones:...</p>", 3);
            jdbcTemplate.update(sqlReprobado, idReprobado, "contact", "Contacto", "<p>Si te interesa formar parte de alguno de nuestros programas envía un correo electrónico...</p>", 4);
            jdbcTemplate.update(sqlReprobado, idReprobado, "deadline_note", "Plazo", "<p class=\"text-red-700 font-semibold\">Tienes hasta las 19:00 horas del 12 de julio de 2024...</p>", 5);

            System.out.println("✅ Roles, Usuarios y Contenidos inicializados exitosamente.");
        };
    }
}