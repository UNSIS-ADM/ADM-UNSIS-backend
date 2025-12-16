package com.unsis.admunsisbackend.service.impl;

import com.unsis.admunsisbackend.dto.LoginRequest;
import com.unsis.admunsisbackend.dto.LoginResponse;
import com.unsis.admunsisbackend.model.Role;
import com.unsis.admunsisbackend.model.User;
import com.unsis.admunsisbackend.repository.UserRepository;
import com.unsis.admunsisbackend.service.AuthService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

@Service
public class AuthServiceImpl implements AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder; // Inyección del BCryptPasswordEncoder

    @Override
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
            .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));        
            if (Boolean.FALSE.equals(user.getActive())) {
            throw new RuntimeException("Usuario desactivado. Contacte al administrador.");
        }
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Credenciales inválidas");
        }
        
        
        // Actualizar el campo lastLogin al momento actual
        user.setLastLogin(LocalDateTime.now());
        userRepository.save(user);

        LoginResponse response = new LoginResponse();
        response.setUsername(user.getUsername());
        response.setFullName(user.getFullName());
        response.setRoles(
        user.getRoles().stream().map(Role::getName).collect(Collectors.toSet())
        );

        if (user.getApplicant() != null) {
            response.setCurp(user.getApplicant().getCurp());
        }

        return response;
    }
}
