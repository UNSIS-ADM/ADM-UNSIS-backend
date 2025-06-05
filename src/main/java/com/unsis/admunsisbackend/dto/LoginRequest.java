package com.unsis.admunsisbackend.dto;

/**
 * Clase que representa la solicitud de inicio de sesión.
 * Contiene el nombre de usuario y la contraseña del usuario.
 */
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
