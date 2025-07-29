package com.unsis.admunsisbackend.controller;

import com.unsis.admunsisbackend.dto.LoginRequest;
import com.unsis.admunsisbackend.dto.LoginResponse;
import com.unsis.admunsisbackend.dto.RegisterRequest;
import com.unsis.admunsisbackend.repository.RoleRepository;
import com.unsis.admunsisbackend.repository.UserRepository;
import com.unsis.admunsisbackend.security.JwtTokenProvider;
import com.unsis.admunsisbackend.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import com.unsis.admunsisbackend.model.User;
import com.unsis.admunsisbackend.model.Role;
import java.util.Set;



@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private AuthService authService;
    @Autowired
    private JwtTokenProvider tokenProvider;

    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest loginRequest) {
        // 1) Validar credenciales y generar LoginResponse (sin token todavía)
        LoginResponse response = authService.login(loginRequest);
        
        // 2) Generar token JWT usando el username
        String token = tokenProvider.generateToken(response.getUsername());
        response.setToken(token);
        
        // 3) Devolver el LoginResponse ya con el campo `token`
        return ResponseEntity.ok(response);
    }

     @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest req) {
        if (userRepository.existsByUsername(req.getUsername())) {
            return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body("El usuario ya existe");
        }

        User user = new User();
        user.setUsername(req.getUsername());
        // Aquí es donde se hashea:
        user.setPassword(passwordEncoder.encode(req.getPassword()));
        user.setFullName(req.getFullName());
        user.setActive(true);

        // Asignamos rol de ADMIN (o el que quieras):
        Role adminRole = roleRepository.findByName("ROLE_ADMIN")
            .orElseThrow(() -> new RuntimeException("Rol ADMIN no existe"));
        user.setRoles(Set.of(adminRole));

        userRepository.save(user);
        return ResponseEntity.ok("Usuario registrado con éxito");
    }
}





