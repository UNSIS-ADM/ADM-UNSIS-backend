package com.unsis.admunsisbackend.dto;

/* Objeto de transferencia de datos para la solicitud de inicio de sesión */
public class LoginRequest {
    private String username;
    private String password;

    // Getters y setters
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
