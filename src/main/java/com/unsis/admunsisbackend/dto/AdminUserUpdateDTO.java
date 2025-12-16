package com.unsis.admunsisbackend.dto;

import java.util.Set;

public class AdminUserUpdateDTO {
    private Long id;                // null -> crear nuevo usuario
    private String username;
    private String password;        // si vacío o null -> no cambia
    private String fullName;
    private Set<String> roles;      // ej: ["ROLE_ADMIN","ROLE_USER"]
    private Boolean active;         // null -> no cambia, true/false -> establece estado

    // getters y setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public Set<String> getRoles() { return roles; }
    public void setRoles(Set<String> roles) { this.roles = roles; }
    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
}
