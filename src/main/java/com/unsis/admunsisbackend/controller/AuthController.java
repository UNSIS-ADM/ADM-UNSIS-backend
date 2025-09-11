package com.unsis.admunsisbackend.controller;

import com.unsis.admunsisbackend.dto.LoginRequest;
import com.unsis.admunsisbackend.dto.LoginResponse;
import com.unsis.admunsisbackend.dto.RegisterRequest;
import com.unsis.admunsisbackend.repository.RoleRepository;
import com.unsis.admunsisbackend.repository.UserRepository;
import com.unsis.admunsisbackend.security.JwtTokenProvider;
import com.unsis.admunsisbackend.service.AuthService;
import com.unsis.admunsisbackend.service.RefreshTokenService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import com.unsis.admunsisbackend.model.User;
import com.unsis.admunsisbackend.model.RefreshToken;
import com.unsis.admunsisbackend.model.Role;
import java.util.Set;
import java.util.Map;


@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private AuthService authService;
    @Autowired
    private JwtTokenProvider tokenProvider;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private RefreshTokenService refreshTokenService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest loginRequest) {
        // 1) Validar credenciales y generar LoginResponse (sin token todavía)
        LoginResponse response = authService.login(loginRequest);

        // 2) Generar access token JWT usando el username
        String accessToken = tokenProvider.generateToken(response.getUsername());
        response.setToken(accessToken);

        // 3) Crear refresh token y devolverlo también
        User user = userRepository.findByUsername(response.getUsername())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        // define duración del refresh token (ejemplo: 7 días) o para pruebas 1 minuto
        //long refreshDurationMs = 7L * 24 * 60 * 60 * 1000; // producción: 7 días
        // para pruebas locales puedes usar: 
        //long refreshDurationMs = 60L * 1000; // 1
        long refreshDurationMs = 2L * 60 * 1000; // 2 minutos


        RefreshToken rt = refreshTokenService.createRefreshToken(user.getId(), refreshDurationMs);

        response.setRefreshToken(rt.getToken());
        // usa el getter del tokenProvider para calcular expiry del access token
        response.setAccessTokenExpiry(System.currentTimeMillis() + tokenProvider.getJwtExpirationInMs());

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

    // Da de alta un usuario del sistema. Sólo ADMIN puede llamarlo.
    @PostMapping("/users")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<?> registerSecretary(@RequestBody RegisterRequest req) {
        // 1) existe?
        if (userRepository.existsByUsername(req.getUsername())) {
            return ResponseEntity
                    .status(409)
                    .body("El usuario ya existe");
        }
        // 2) construye user
        User user = new User();
        user.setUsername(req.getUsername());
        user.setPassword(passwordEncoder.encode(req.getPassword()));
        user.setFullName(req.getFullName());
        user.setActive(true);
        // 3) asigna rol ROLE_USER
        Role secRole = roleRepository.findByName("ROLE_USER")
                .orElseThrow(() -> new RuntimeException("ROLE_USER no existe"));
        user.setRoles(Set.of(secRole));
        // 4) guardar
        userRepository.save(user);
        return ResponseEntity.ok("Usuario registrado con éxito");
    }
    
    // Endpoint para refrescar token
    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@RequestBody Map<String, String> body) {
        String refreshToken = body.get("refreshToken");
        var opt = refreshTokenService.findByToken(refreshToken);

        if (opt.isEmpty() || !refreshTokenService.isValid(opt.get())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "INVALID_REFRESH", "message", "Refresh token inválido o expirado"));
        }

        RefreshToken rt = opt.get();
        // Buscar el usuario dueño del refreshToken
        User user = userRepository.findById(rt.getUserId())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado para el refresh token"));

        // Generar nuevo accessToken usando el username correcto
        String newAccessToken = jwtTokenProvider.generateToken(user.getUsername());

        // Opción: rotar el refresh token (recomendado)
    //    long refreshDurationMs = 7L * 24 * 60 * 60 * 1000; // mismo tiempo que usaste en login
        //long refreshDurationMs = 60L * 1000; // 1 min
        long refreshDurationMs = 2L * 60 * 1000; // 2 minutos

        RefreshToken newRt = refreshTokenService.createRefreshToken(user.getId(), refreshDurationMs);

        return ResponseEntity.ok(Map.of(
                "accessToken", newAccessToken,
                "refreshToken", newRt.getToken()));
    }

    // Endpoint para logout
    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestBody Map<String, String> body) {
        String refreshToken = body.get("refreshToken");
        var opt = refreshTokenService.findByToken(refreshToken);
        opt.ifPresent(rt -> refreshTokenService.deleteByUserId(rt.getUserId()));
        return ResponseEntity.ok(Map.of("message", "Sesión cerrada"));
    }
}
