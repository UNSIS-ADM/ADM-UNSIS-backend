package com.unsis.admunsisbackend.repository;

import com.unsis.admunsisbackend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

/* Repositorio para los usuarios */
public interface UserRepository extends JpaRepository<User, Long> {
    // Buscar por username
    Optional<User> findByUsername(String username);
    // Verificar existencia por username
    boolean existsByUsername(String username);
    // Verifica si el CURP ya está registrado
    boolean existsByCurp(String curp); 
}
