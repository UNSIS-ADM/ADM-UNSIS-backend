package com.unsis.admunsisbackend.service;

import com.unsis.admunsisbackend.dto.LoginRequest;
import com.unsis.admunsisbackend.dto.LoginResponse;

public interface AuthService {
    LoginResponse login(LoginRequest loginRequest);
}