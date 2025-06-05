package com.unsis.admunsisbackend.controller;

import com.unsis.admunsisbackend.dto.LoginRequest;
import com.unsis.admunsisbackend.dto.LoginResponse;
import com.unsis.admunsisbackend.security.JwtTokenProvider;
import com.unsis.admunsisbackend.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @Autowired
    private JwtTokenProvider tokenProvider;

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
}






