package com.unsis.admunsisbackend.controller;

import com.unsis.admunsisbackend.dto.UserResponseDTO;
import com.unsis.admunsisbackend.dto.ApplicantResponseDTO;
import com.unsis.admunsisbackend.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import com.unsis.admunsisbackend.dto.AdminUserUpdateDTO;
import java.util.List;

@RestController
@RequestMapping("/api")
public class UserController {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    @Autowired
    private UserService userService;

    // -------------------------
    // Admin endpoints
    // -------------------------
    // Listar todos los usuarios (solo ADMIN)
    @GetMapping("/admin/users")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')") // Usar hasAuthority en lugar de hasRole
    public ResponseEntity<List<UserResponseDTO>> getAllUsers() {
        logger.info("Solicitud para obtener todos los usuarios por parte del administrador");
        List<UserResponseDTO> users = userService.getAllUsers();
        return ResponseEntity.ok(users);
    }

    // Crear o actualizar usuario (credenciales, roles, active) (solo ADMIN)
    @PostMapping("/admin/users")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<UserResponseDTO> createOrUpdateUser(@RequestBody AdminUserUpdateDTO dto) {
        logger.info("Admin request to create/update user: {}", dto == null ? "null" : dto.getUsername());
        UserResponseDTO result = userService.adminCreateOrUpdateUser(dto);
        return ResponseEntity.ok(result);
    }

    // Activar / desactivar usuario (solo ADMIN)
    @PatchMapping("/admin/users/{id}/active")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<Void> changeStatus(
            @PathVariable Long id,
            @RequestParam boolean active) {

        logger.info("Admin request to change active={} for user id={}", active, id);

        AdminUserUpdateDTO dto = new AdminUserUpdateDTO();
        dto.setId(id);
        dto.setActive(active);

        userService.adminCreateOrUpdateUser(dto);
        return ResponseEntity.ok().build();
    }

    // -------------------------
    // Applicant endpoints
    // -------------------------

    // Obtener perfil del aspirante (solo APPLICANT)
    @GetMapping("/applicant/profile")
    @PreAuthorize("hasAuthority('ROLE_APPLICANT')")
    public ResponseEntity<ApplicantResponseDTO> getApplicantProfile() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        logger.info("Solicitud para obtener el perfil del solicitante del usuario: {}", username);

        ApplicantResponseDTO profile = userService.getApplicantProfile(username);
        return ResponseEntity.ok(profile);
    }

    // -------------------------
    // Opcional: info del usuario logueado (USER/ADMIN/APP)
    // -------------------------
    @GetMapping("/user/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserResponseDTO> getMyUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        logger.info("Request to get current user info for: {}", username);

        // Reusar getAllUsers() no es apropiado; la forma sencilla es filtrar en
        // service, pero si no tienes un método para obtener por username, puedes crear uno.
        // Aquí asumiremos que getAllUsers + filter es suficiente (pequeño workaround).
        UserResponseDTO found = userService.getAllUsers().stream()
                .filter(u -> u.getUsername().equals(username))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        return ResponseEntity.ok(found);
    }
}