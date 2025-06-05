package com.unsis.admunsisbackend.service;

import com.unsis.admunsisbackend.dto.LoginRequest;
import com.unsis.admunsisbackend.dto.LoginResponse;

public interface AuthService {
    LoginResponse login(LoginRequest loginRequest);
}



/*
import com.unsis.admunsisbackend.dto.LoginRequest;
import com.unsis.admunsisbackend.dto.LoginResponse;
import com.unsis.admunsisbackend.model.Applicant;
import com.unsis.admunsisbackend.model.User;
import com.unsis.admunsisbackend.repository.ApplicantRepository;
import com.unsis.admunsisbackend.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository usuarioRepository;
    private final ApplicantRepository aspiranteRepository;

    public AuthService(UserRepository usuarioRepository, ApplicantRepository aspiranteRepository) {
        this.usuarioRepository = usuarioRepository;
        this.aspiranteRepository = aspiranteRepository;
    }

    public LoginResponse login(LoginRequest request) {
        Applicant aspirante = aspiranteRepository.findByFicha(request.getFicha())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        User usuario = aspirante.getUsuario();

        if (!usuario.getCurp().equals(request.getCurp())) {
            throw new RuntimeException("Credenciales inválidas");
        }

        return new LoginResponse("TOKEN-FALSO", usuario.getRol().getNombre());
    }
}

*/