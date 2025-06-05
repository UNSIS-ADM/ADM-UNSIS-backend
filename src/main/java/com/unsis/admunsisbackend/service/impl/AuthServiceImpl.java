package com.unsis.admunsisbackend.service.impl;

import com.unsis.admunsisbackend.dto.LoginRequest;
import com.unsis.admunsisbackend.dto.LoginResponse;
import com.unsis.admunsisbackend.model.Role;
import com.unsis.admunsisbackend.model.User;
import com.unsis.admunsisbackend.repository.UserRepository;
import com.unsis.admunsisbackend.service.AuthService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

@Service
public class AuthServiceImpl implements AuthService {

    @Autowired
    private UserRepository userRepository;

    @Override
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
            .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        if (!user.getPassword().equals(request.getPassword())) {
            throw new IllegalArgumentException("Invalid credentials");
        }

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
