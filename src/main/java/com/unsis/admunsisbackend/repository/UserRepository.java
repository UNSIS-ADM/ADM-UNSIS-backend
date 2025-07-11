package com.unsis.admunsisbackend.repository;

import com.unsis.admunsisbackend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    
    boolean existsByUsername(String username);    
    //boolean existsByCurp(String curp); // Verifica si el CURP ya está registrado
}
